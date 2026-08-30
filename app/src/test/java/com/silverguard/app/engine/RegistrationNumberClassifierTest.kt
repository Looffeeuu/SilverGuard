package com.silverguard.app.engine

import com.silverguard.app.model.RegistrationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrationNumberClassifierTest {

    @Test
    fun recognizesMedicalDeviceRegistrationNumber() {
        val result = RegistrationNumberClassifier.classify("国械注准XXXXXXXX")

        assertEquals(RegistrationType.MEDICAL_DEVICE, result.type)
        assertEquals("国械注准XXXXXXXX", result.normalizedNumber)
        assertTrue(result.confidence >= 0.9f)
    }

    @Test
    fun recognizesDrugApprovalNumber() {
        val result = RegistrationNumberClassifier.classify("国药准字H20261234")

        assertEquals(RegistrationType.DRUG_APPROVAL, result.type)
    }

    @Test
    fun recognizesHealthFoodRegistrationNumber() {
        val result = RegistrationNumberClassifier.classify("国食健注 G 20261234")

        assertEquals(RegistrationType.HEALTH_FOOD, result.type)
    }

    @Test
    fun recognizesCosmeticRegistrationNumber() {
        val result = RegistrationNumberClassifier.classify("国妆特字G20261234")

        assertEquals(RegistrationType.COSMETIC, result.type)
    }
}
