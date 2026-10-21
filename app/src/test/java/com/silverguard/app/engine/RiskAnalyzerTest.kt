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
        assertTrue("绝对承诺 / 短期见效" in flagTypes("百分百保证收益"))
    }

    @Test
    fun typicalScamWordingStaysHighRisk() {
        val analysis = RiskAnalyzer.analyze("量子理疗仪，七天见效，不用吃药，根治高血压，仅限今天")

        assertEquals(RiskLevel.HIGH, analysis.level)
        assertFalse(analysis.claimConflicts.isEmpty())
    }

    @Test
    fun distinctWordingsWithinOneRuleAccumulate() {
        val flag = RiskAnalyzer.analyze("内部项目，零风险，日赚300").flags.single()

        assertEquals("异常赚钱承诺", flag.type)
        assertEquals("内部项目、零风险、日赚", flag.matched)
        assertEquals(30 + 15 + 15, flag.weight)
        assertEquals(RiskLevel.HIGH, RiskAnalyzer.analyze("内部项目，零风险，日赚300").level)
    }

    @Test
    fun repeatingTheSameWordAddsNothing() {
        assertEquals(30, RiskAnalyzer.analyze("稳赚！稳赚！稳赚！").score)
    }

    @Test
    fun extraHitsAreCappedAtTwo() {
        assertEquals(60, RiskAnalyzer.analyze("稳赚 保本 零风险 日赚 躺赚").flags.single().weight)
    }

    @Test
    fun drugLabelIndicationIsNotATreatmentClaim() {
        val analysis = RiskAnalyzer.analyze("硝苯地平控释片 国药准字H20000079 适应症：用于治疗高血压，可降血压")

        assertEquals(RiskLevel.LOW, analysis.level)
        assertTrue(analysis.flags.isEmpty())
    }

    @Test
    fun cureWordingStaysFlaggedEvenWithDrugNumber() {
        val flag = RiskAnalyzer.analyze("国药准字H20000079 用于治疗高血压，三个月根治").flags
            .single { it.type == "疾病治疗或强功效承诺" }
        assertEquals("根治", flag.matched)
    }

    @Test
    fun registeredDeviceMayStateItsIndication() {
        val analysis = RiskAnalyzer.analyze("颈椎牵引器 医疗器械注册证编号：苏械注准20202090123 用于治疗颈椎病")

        assertEquals(RiskLevel.LOW, analysis.level)
        assertTrue(analysis.claimConflicts.isEmpty())
    }

    @Test
    fun singleTreatmentClaimOnNonDrugIsHighRisk() {
        val noRegistration = RiskAnalyzer.analyze("保健酒每天一杯，降血压")
        assertEquals(RiskLevel.HIGH, noRegistration.level)
        assertTrue(noRegistration.score >= 55)

        val healthFood = RiskAnalyzer.analyze("保健食品注册号：国食健注G20150123 本品可以治疗糖尿病")
        assertEquals(RiskLevel.HIGH, healthFood.level)
    }

    @Test
    fun exaggerationWithoutDiseaseClaimIsNotForcedToHigh() {
        assertEquals(RiskLevel.MEDIUM, RiskAnalyzer.analyze("排毒养颜胶囊，三天见效").level)
    }

    @Test
    fun twoDigitReturnIsAMoneyPromiseButBondRatesAreNot() {
        assertTrue("异常赚钱承诺" in flagTypes("养老理财，年化收益15%"))
        assertTrue("异常赚钱承诺" in flagTypes("每年返利12%"))
        assertTrue(flagTypes("储蓄国债：三年期票面利率2.38%").isEmpty())
        assertTrue(flagTypes("年化收益率3.2%，产品有风险").isEmpty())
    }
}
