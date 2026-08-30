package com.silverguard.app.engine

import com.silverguard.app.data.OfficialSources
import com.silverguard.app.model.ProductInfo
import com.silverguard.app.model.VerificationResult
import com.silverguard.app.model.VerificationStatus

object VerificationPlanner {

    fun plan(productInfo: ProductInfo): VerificationResult {
        val classification = RegistrationNumberClassifier.classify(
            productInfo.registrationNumber.orEmpty()
        )
        val readiness = VerificationReadinessEvaluator.evaluate(
            productInfo = productInfo,
            registrationType = classification.type
        )

        return VerificationResult(
            status = if (readiness.ready) {
                VerificationStatus.MANUAL_REQUIRED
            } else {
                VerificationStatus.NOT_READY
            },
            readiness = readiness,
            registration = classification,
            officialSources = if (readiness.ready) {
                OfficialSources.forType(classification.type)
            } else {
                emptyList()
            },
            checkedAt = null,
            checklist = if (readiness.ready) {
                VerificationChecklistFactory.create(productInfo, classification)
            } else {
                emptyList()
            }
        )
    }
}
