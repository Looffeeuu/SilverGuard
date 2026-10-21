package com.silverguard.app.engine

import com.google.gson.Gson
import com.silverguard.app.model.RiskLevel
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs the local risk rules over the labeled samples in `src/test/resources/risk-eval/samples.json`.
 * Labeling rules and how to update the baseline are in docs/RISK_EVALUATION.md.
 * The report is written to app/build/reports/risk-eval/report.md.
 */
class RiskRuleEvaluationTest {

    private class RawSample(
        val id: String?,
        val category: String?,
        val expected: String?,
        val source: String?,
        val text: String?,
        val note: String?
    )

    private data class Sample(
        val id: String,
        val category: String,
        val expected: RiskLevel,
        val source: String,
        val text: String,
        val note: String?
    )

    private data class Outcome(val sample: Sample, val predicted: RiskLevel, val score: Int, val flags: List<String>) {
        val falseAlarm get() = sample.expected == RiskLevel.LOW && predicted != RiskLevel.LOW
        val missed get() = sample.expected != RiskLevel.LOW && predicted == RiskLevel.LOW
        val underRated get() = sample.expected == RiskLevel.HIGH && predicted == RiskLevel.MEDIUM
    }

    private val samples: List<Sample> by lazy {
        val json = requireNotNull(javaClass.classLoader?.getResource("risk-eval/samples.json")) {
            "risk-eval/samples.json is missing from test resources"
        }.readText(Charsets.UTF_8)
        Gson().fromJson(json, Array<RawSample>::class.java).map { raw ->
            Sample(
                id = requireNotNull(raw.id) { "sample without id" },
                category = requireNotNull(raw.category) { "${raw.id}: missing category" },
                expected = RiskLevel.valueOf(requireNotNull(raw.expected) { "${raw.id}: missing expected" }),
                source = requireNotNull(raw.source) { "${raw.id}: missing source" },
                text = requireNotNull(raw.text) { "${raw.id}: missing text" },
                note = raw.note
            )
        }
    }

    @Test
    fun datasetIsWellFormed() {
        assertEquals("duplicate sample ids", samples.size, samples.map { it.id }.distinct().size)
        samples.forEach { sample ->
            assertTrue("${sample.id}: blank text", sample.text.isNotBlank())
            assertTrue("${sample.id}: category must be snake_case", sample.category.matches(Regex("[a-z_]+")))
        }
        RiskLevel.entries.forEach { level ->
            assertTrue("no samples labeled $level", samples.any { it.expected == level })
        }
    }

    @Test
    fun rulesDoNotRegressBelowBaseline() {
        val outcomes = samples.map { sample ->
            val analysis = RiskAnalyzer.analyze(sample.text)
            Outcome(sample, analysis.level, analysis.score, analysis.flags.map { "${it.type}「${it.matched}」" })
        }
        val report = buildReport(outcomes)
        File("build/reports/risk-eval").apply { mkdirs() }.resolve("report.md").writeText(report, Charsets.UTF_8)
        println(report.substringBefore("## 分类明细"))

        val exact = outcomes.count { it.predicted == it.sample.expected }
        val falseAlarms = outcomes.count { it.falseAlarm }
        val missed = outcomes.count { it.missed }
        val underRated = outcomes.count { it.underRated }

        // Baseline updated on 2026-10-21. Tighten these numbers whenever a rule change improves them.
        assertTrue("exact matches fell to $exact (baseline $BASELINE_EXACT)", exact >= BASELINE_EXACT)
        assertTrue("false alarms rose to $falseAlarms (baseline $BASELINE_FALSE_ALARMS)", falseAlarms <= BASELINE_FALSE_ALARMS)
        assertTrue("missed rose to $missed (baseline $BASELINE_MISSED)", missed <= BASELINE_MISSED)
        assertTrue("under-rated rose to $underRated (baseline $BASELINE_UNDER_RATED)", underRated <= BASELINE_UNDER_RATED)
    }

    private fun buildReport(outcomes: List<Outcome>): String = buildString {
        val total = outcomes.size
        val lowLabeled = outcomes.count { it.sample.expected == RiskLevel.LOW }
        val riskLabeled = total - lowLabeled
        val highLabeled = outcomes.count { it.sample.expected == RiskLevel.HIGH }
        fun pct(part: Int, whole: Int) = if (whole == 0) "-" else "%.1f%%".format(part * 100.0 / whole)

        appendLine("# 风险规则评测报告")
        appendLine()
        appendLine("样本数：$total（标注低风险 $lowLabeled，标注中 / 高风险 $riskLabeled）")
        appendLine()
        appendLine("| 指标 | 数量 | 比例 |")
        appendLine("| --- | --- | --- |")
        val exact = outcomes.count { it.predicted == it.sample.expected }
        appendLine("| 等级完全一致 | $exact | ${pct(exact, total)} |")
        val falseAlarms = outcomes.count { it.falseAlarm }
        appendLine("| 误报：标注低风险，判为中 / 高 | $falseAlarms | ${pct(falseAlarms, lowLabeled)} |")
        val missed = outcomes.count { it.missed }
        appendLine("| 漏报：标注中 / 高风险，判为低 | $missed | ${pct(missed, riskLabeled)} |")
        val underRated = outcomes.count { it.underRated }
        appendLine("| 低估：标注高风险，判为中 | $underRated | ${pct(underRated, highLabeled)} |")
        appendLine()
        appendLine("混淆矩阵（行：标注，列：规则判定）")
        appendLine()
        appendLine("| 标注 \\ 判定 | LOW | MEDIUM | HIGH |")
        appendLine("| --- | --- | --- | --- |")
        RiskLevel.entries.forEach { expected ->
            val row = RiskLevel.entries.map { predicted ->
                outcomes.count { it.sample.expected == expected && it.predicted == predicted }
            }
            appendLine("| $expected | ${row.joinToString(" | ")} |")
        }
        appendLine()
        appendLine("## 分类明细")
        appendLine()
        appendLine("| 分类 | 样本 | 一致 | 误报 | 漏报 | 低估 |")
        appendLine("| --- | --- | --- | --- | --- | --- |")
        outcomes.groupBy { it.sample.category }.forEach { (category, group) ->
            appendLine(
                "| $category | ${group.size} | ${group.count { it.predicted == it.sample.expected }} | " +
                    "${group.count { it.falseAlarm }} | ${group.count { it.missed }} | ${group.count { it.underRated }} |"
            )
        }
        appendLine()
        appendLine("## 不一致样本")
        appendLine()
        appendLine("| id | 标注 | 判定 | 分数 | 命中规则 | 文本 |")
        appendLine("| --- | --- | --- | --- | --- | --- |")
        outcomes.filter { it.predicted != it.sample.expected }.forEach { outcome ->
            val text = outcome.sample.text.replace("\n", " / ").replace("|", "\\|")
            appendLine(
                "| ${outcome.sample.id} | ${outcome.sample.expected} | ${outcome.predicted} | ${outcome.score} | " +
                    "${outcome.flags.joinToString("；").ifEmpty { "-" }} | $text |"
            )
        }
    }

    private companion object {
        const val BASELINE_EXACT = 56
        const val BASELINE_FALSE_ALARMS = 0
        const val BASELINE_MISSED = 6
        const val BASELINE_UNDER_RATED = 2
    }
}
