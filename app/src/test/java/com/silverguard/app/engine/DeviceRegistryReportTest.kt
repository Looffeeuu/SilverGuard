package com.silverguard.app.engine

import com.silverguard.app.data.AnalysisHistoryCodec
import com.silverguard.app.model.AnalysisHistoryEntry
import com.silverguard.app.model.AnalysisInputMethod
import com.silverguard.app.model.DeviceRegistryRecord
import com.silverguard.app.model.ProductInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** How a local registry lookup shows up in the family share report and in saved history. */
class DeviceRegistryReportTest {

    private val record = DeviceRegistryRecord("国械注准20263090001", "安心医疗科技有限公司", listOf("电子血压计"), "器械")
    private val analysis = RiskAnalyzer.analyze("量子太赫兹理疗仪 医疗器械注册证编号：国械注准20263090001 生产企业：深圳某某健康科技有限公司")

    @Test
    fun borrowedNumberStopsPaymentInReport() {
        val check = DeviceRegistryComparator.check(
            "国械注准20263090001", record,
            ProductInfo(name = "量子太赫兹理疗仪", manufacturer = "深圳某某健康科技有限公司"), "2026-10-05"
        )
        val report = ShareReportBuilder.build(analysis.copy(deviceRegistry = check))

        assertTrue(report.contains("本机登记数据（截至 2026-10-05）：该编号登记为「电子血压计」"))
        assertTrue(report.contains("与包装信息不一致"))
        assertTrue(report.contains("核对编号是否被借用"))
    }

    @Test
    fun borrowedNumberChangesHeadlineAndSpeechButNotTheScore() {
        val check = DeviceRegistryComparator.check(
            "国械注准20263090001", record,
            ProductInfo(name = "量子太赫兹理疗仪", manufacturer = "深圳某某健康科技有限公司"), "2026-10-05"
        )
        val flagged = analysis.copy(deviceRegistry = check)

        assertTrue(flagged.hasVerificationMismatch)
        assertEquals("信息对不上，建议暂缓付款", flagged.headline)
        assertEquals(analysis.score, flagged.score)
        val speech = SpeechSummaryBuilder.build(flagged)
        assertTrue(speech.contains("信息对不上，建议暂缓付款"))
        assertTrue(speech.contains("对应的是别的产品或企业"))
        assertFalse(speech.contains("当前未发现明显高风险"))
    }

    @Test
    fun matchingRecordKeepsTheNormalHeadline() {
        val check = DeviceRegistryComparator.check("国械注准20263090001", record, ProductInfo(name = "电子血压计"), "2026-10-05")
        val matched = analysis.copy(deviceRegistry = check)

        assertFalse(matched.hasVerificationMismatch)
        assertEquals(analysis.title, matched.headline)
    }

    @Test
    fun notFoundIsReportedWithoutCallingItFake() {
        val check = DeviceRegistryComparator.check("国械注准20263090001", null, ProductInfo(), "2026-10-05")
        val report = ShareReportBuilder.build(analysis.copy(deviceRegistry = check))

        assertTrue(report.contains("未找到该编号"))
        assertTrue(report.contains("未找到不等于假货"))
        assertFalse(report.contains("核对编号是否被借用"))
    }

    @Test
    fun reportOmitsRegistryLineWhenNothingWasLookedUp() {
        assertFalse(ShareReportBuilder.build(analysis).contains("本机登记数据"))
    }

    @Test
    fun registryResultIsKeptInHistoryAndOldRecordsStillLoad() {
        val check = DeviceRegistryComparator.check("国械注准20263090001", record, ProductInfo(name = "电子血压计"), "2026-10-05")
        val entry = AnalysisHistoryEntry("one", 1234, analysis.copy(deviceRegistry = check), setOf(AnalysisInputMethod.TEXT))

        assertEquals(entry, AnalysisHistoryCodec.decode(AnalysisHistoryCodec.encode(listOf(entry))).single())

        val withoutRegistry = AnalysisHistoryEntry("two", 1234, analysis, setOf(AnalysisInputMethod.TEXT))
        val oldJson = AnalysisHistoryCodec.encode(listOf(withoutRegistry))
        assertFalse(oldJson.contains("deviceRegistry"))
        assertNull(AnalysisHistoryCodec.decode(oldJson).single().analysis.deviceRegistry)
    }
}
