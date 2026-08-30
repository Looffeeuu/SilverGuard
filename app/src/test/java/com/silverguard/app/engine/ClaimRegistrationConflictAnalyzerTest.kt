package com.silverguard.app.engine

import com.silverguard.app.model.RegistrationType
import com.silverguard.app.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClaimRegistrationConflictAnalyzerTest {

    @Test
    fun healthProductTreatmentClaimsTriggerConflict() {
        val conflicts = ClaimRegistrationConflictAnalyzer.analyze(
            text = "普通保健品，七天降血糖，不用吃药",
            registrationType = RegistrationType.HEALTH_FOOD
        )

        assertEquals(1, conflicts.size)
        assertTrue(conflicts.first().matchedClaims.contains("降血糖"))
        assertTrue(conflicts.first().matchedClaims.contains("不用吃药"))
    }

    @Test
    fun ordinaryProductWithoutRegistrationNumberIsNotAutomaticallyHighRisk() {
        val analysis = RiskAnalyzer.analyze(
            "商品名称：家用清洁杯\n生产企业：安心日用品有限公司\n规格：500毫升"
        )

        assertEquals(RiskLevel.LOW, analysis.level)
        assertEquals(0, analysis.score)
        assertTrue(analysis.flags.isEmpty())
        assertTrue(analysis.claimConflicts.isEmpty())
    }
}
