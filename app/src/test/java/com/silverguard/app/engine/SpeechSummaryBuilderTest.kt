package com.silverguard.app.engine

import com.silverguard.app.model.ProductInfo
import com.silverguard.app.model.RiskAnalysis
import com.silverguard.app.model.RiskLevel
import com.silverguard.app.model.VerificationReadiness
import com.silverguard.app.model.VerificationResult
import com.silverguard.app.model.VerificationStatus
import com.silverguard.app.model.RegistrationClassification
import com.silverguard.app.model.RegistrationType
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechSummaryBuilderTest {
    @Test
    fun spokenSummaryContainsRiskVerificationAndAction() {
        val analysis = RiskAnalysis(
            rawText = "普通商品",
            category = "普通商品",
            score = 0,
            level = RiskLevel.LOW,
            title = "暂未发现明显高风险",
            flags = emptyList(),
            productInfo = ProductInfo(name = "普通商品"),
            verification = VerificationResult(
                status = VerificationStatus.NOT_READY,
                registration = RegistrationClassification(
                    rawNumber = "",
                    normalizedNumber = "",
                    type = RegistrationType.UNKNOWN,
                    confidence = 0f,
                    reason = "未识别到编号"
                ),
                readiness = VerificationReadiness(
                    ready = false,
                    missingFields = emptyList(),
                    suggestedAction = "补充信息"
                ),
                officialSources = emptyList(),
                checkedAt = null
            )
        )

        val text = SpeechSummaryBuilder.build(analysis)

        assertTrue(text.contains("宣传风险为低"))
        assertTrue(text.contains("官方核验信息还不完整"))
        assertTrue(text.contains("仍需自行核对"))
    }
}
