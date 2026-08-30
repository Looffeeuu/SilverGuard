package com.silverguard.app.engine

import com.silverguard.app.model.ProductInfo
import com.silverguard.app.model.VerificationField
import com.silverguard.app.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VerificationPlannerTest {

    @Test
    fun missingRegistrationNumberIsNotReady() {
        val result = VerificationPlanner.plan(
            ProductInfo(name = "家用清洁杯", manufacturer = "安心日用品有限公司")
        )

        assertEquals(VerificationStatus.NOT_READY, result.status)
        assertFalse(result.readiness.ready)
        assertTrue(result.readiness.missingFields.contains(VerificationField.REGISTRATION_NUMBER))
        assertTrue(result.officialSources.isEmpty())
    }

    @Test
    fun registrationNumberRequiresManualOfficialCheck() {
        val result = VerificationPlanner.plan(
            ProductInfo(
                name = "示例医疗器械",
                manufacturer = "示例医疗科技有限公司",
                registrationNumber = "国械注准XXXXXXXX"
            )
        )

        assertEquals(VerificationStatus.MANUAL_REQUIRED, result.status)
        assertTrue(result.readiness.ready)
        assertTrue(result.checkedAt == null)
    }

    @Test
    fun plannerNeverPretendsAutomaticVerification() {
        val result = VerificationPlanner.plan(
            ProductInfo(registrationNumber = "国药准字H20261234")
        )

        assertTrue(result.status != VerificationStatus.VERIFIED)
        assertTrue(result.checkedAt == null)
    }
}
