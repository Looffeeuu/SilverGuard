package com.silverguard.app.engine

import com.silverguard.app.model.ProductInfo
import com.silverguard.app.model.RegistrationType
import com.silverguard.app.model.VerificationEvidenceField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VerificationChecklistFactoryTest {

    @Test
    fun medicalDeviceChecklistKeepsExpectedValuesAndCriticalFields() {
        val product = ProductInfo(
            name = "示例理疗仪",
            manufacturer = "示例医疗科技有限公司",
            model = "SG-01",
            specification = "家用型",
            registrationNumber = "国械注准XXXXXXXX"
        )
        val registration = RegistrationNumberClassifier.classify(product.registrationNumber!!)

        val checklist = VerificationChecklistFactory.create(product, registration)

        assertEquals(RegistrationType.MEDICAL_DEVICE, registration.type)
        assertTrue(checklist.any {
            it.field == VerificationEvidenceField.REGISTRATION_NUMBER &&
                it.expectedValue == "国械注准XXXXXXXX" && it.required
        })
        assertTrue(checklist.any {
            it.field == VerificationEvidenceField.MODEL_OR_SPECIFICATION &&
                it.expectedValue == "SG-01 / 家用型"
        })
        assertTrue(checklist.any {
            it.field == VerificationEvidenceField.REGISTERED_SCOPE && it.required
        })
        assertTrue(checklist.any {
            it.field == VerificationEvidenceField.REGISTRATION_STATUS && it.required
        })
    }

    @Test
    fun healthFoodChecklistExplainsThatHealthFunctionIsNotTreatment() {
        val product = ProductInfo(registrationNumber = "国食健字G20261234")

        val checklist = VerificationChecklistFactory.create(
            product,
            RegistrationNumberClassifier.classify(product.registrationNumber!!)
        )

        val scope = checklist.first { it.field == VerificationEvidenceField.REGISTERED_SCOPE }
        assertTrue(scope.guidance.contains("保健功能"))
        assertTrue(scope.guidance.contains("不等于疾病治疗用途"))
    }
}
