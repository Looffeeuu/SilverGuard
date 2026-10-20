package com.silverguard.app.engine

import com.silverguard.app.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskAnalyzerTest {

    private fun flagTypes(text: String) = RiskAnalyzer.analyze(text).flags.map { it.type }

    @Test
    fun mandatoryHealthFoodDisclaimerIsNotATreatmentClaim() {
        val analysis = RiskAnalyzer.analyze(
            """
            商品名称：安心牌维生素C片
            保健食品注册号：国食健注G20150123
            保健功能：补充维生素C
            保健食品不是药物，不能代替药物治疗疾病
            本品不能代替药物
            """.trimIndent()
        )

        assertEquals(RiskLevel.LOW, analysis.level)
        assertEquals(0, analysis.score)
        assertTrue(analysis.flags.isEmpty())
        assertTrue(analysis.claimConflicts.isEmpty())
        assertTrue(analysis.relatedCases.isEmpty())
    }

    @Test
    fun disclaimerWrappedByOcrLineBreakIsStillRecognised() {
        assertTrue(flagTypes("保健食品不是药物，不能代替药物\n治疗疾病").isEmpty())
        assertTrue(flagTypes("本品不具有疾病预防、治疗功能").isEmpty())
    }

    @Test
    fun negatedClaimsAreIgnored() {
        assertTrue(flagTypes("本产品不能根治颈椎病").isEmpty())
        assertTrue(flagTypes("医生提醒：慢性病患者不能停药").isEmpty())
        assertTrue(flagTypes("任何投资都并非零风险").isEmpty())
    }

    @Test
    fun negationDoesNotCrossClauseBoundary() {
        assertTrue("疾病治疗或强功效承诺" in flagTypes("机会不能等，根治只要七天"))
    }

    @Test
    fun noCurePromiseIsStillFlagged() {
        assertTrue("疾病治疗或强功效承诺" in flagTypes("不治愈不收费"))
    }

    @Test
    fun laterAffirmedClaimIsFoundAfterNegatedOne() {
        val flag = RiskAnalyzer.analyze("本品不能代替药物治疗疾病，但能根治高血压").flags
            .single { it.type == "疾病治疗或强功效承诺" }
        assertEquals("根治", flag.matched)
    }

    @Test
    fun deviceNameAloneIsNotATreatmentClaim() {
        assertTrue(flagTypes("商品名称：颈椎治疗仪\n注册证号：苏械注准20202090123").isEmpty())
        assertTrue("疾病治疗或强功效承诺" in flagTypes("颈椎治疗仪可以治疗颈椎病"))
    }

    @Test
    fun bareHundredPercentIsNotAnAbsolutePromise() {
        assertTrue(flagTypes("面料：100%棉").isEmpty())
        assertTrue(flagTypes("纯度100％，百分百原装进口").isEmpty())
        assertTrue("绝对承诺 / 短期见效" in flagTypes("100%有效"))
        assertTrue("绝对承诺 / 短期见效" in flagTypes("百分之百治愈"))
    }

    @Test
    fun typicalScamWordingStaysHighRisk() {
        val analysis = RiskAnalyzer.analyze("量子理疗仪，七天见效，不用吃药，根治高血压，仅限今天")

        assertEquals(RiskLevel.HIGH, analysis.level)
        assertFalse(analysis.claimConflicts.isEmpty())
    }
}
