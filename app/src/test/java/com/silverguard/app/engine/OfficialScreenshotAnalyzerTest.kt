package com.silverguard.app.engine

import com.silverguard.app.model.EvidenceMatchStatus
import com.silverguard.app.model.ProductInfo
import com.silverguard.app.model.ScreenshotComparisonStatus
import com.silverguard.app.model.VerificationEvidenceField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialScreenshotAnalyzerTest {

    private fun verification() = VerificationPlanner.plan(
        ProductInfo(
            name = "示例理疗仪",
            manufacturer = "示例医疗科技有限公司",
            model = "SG-01",
            registrationNumber = "国械注准XXXXXXXX"
        )
    )

    @Test
    fun matchingScreenshotFindsNumberNameRegistrantAndModel() {
        val review = OfficialScreenshotAnalyzer.analyze(
            """
                产品名称：示例理疗仪
                注册人：示例医疗科技有限公司
                注册证编号：国械注准XXXXXXXX
                型号规格：SG-01
                适用范围：用于示例用途
                注册状态：有效
            """.trimIndent(),
            verification(),
            now = 1_000L
        )

        fun status(field: VerificationEvidenceField) =
            review.comparisons.first { it.field == field }.status

        assertEquals(ScreenshotComparisonStatus.MATCHED, status(VerificationEvidenceField.REGISTRATION_NUMBER))
        assertEquals(ScreenshotComparisonStatus.MATCHED, status(VerificationEvidenceField.PRODUCT_NAME))
        assertEquals(ScreenshotComparisonStatus.MATCHED, status(VerificationEvidenceField.REGISTRANT))
        assertEquals(ScreenshotComparisonStatus.MATCHED, status(VerificationEvidenceField.MODEL_OR_SPECIFICATION))
        assertEquals(ScreenshotComparisonStatus.NEEDS_CONFIRMATION, status(VerificationEvidenceField.REGISTERED_SCOPE))
        assertEquals(ScreenshotComparisonStatus.MATCHED, status(VerificationEvidenceField.REGISTRATION_STATUS))
        assertFalse(review.hasPossibleMismatch)
    }

    @Test
    fun differentOfficialNumberAndCancelledStatusAreOnlyPossibleMismatches() {
        val review = OfficialScreenshotAnalyzer.analyze(
            "注册证编号：国械注准YYYYYYYY\n注册状态：已注销",
            verification(),
            now = 2_000L
        )

        val number = review.comparisons.first {
            it.field == VerificationEvidenceField.REGISTRATION_NUMBER
        }
        val status = review.comparisons.first {
            it.field == VerificationEvidenceField.REGISTRATION_STATUS
        }

        assertEquals(ScreenshotComparisonStatus.POSSIBLE_MISMATCH, number.status)
        assertEquals("国械注准YYYYYYYY", number.detectedText)
        assertEquals(ScreenshotComparisonStatus.POSSIBLE_MISMATCH, status.status)
        assertTrue(review.summary.contains("可能不一致"))
    }

    @Test
    fun blankScreenshotDoesNotInventMatchesOrMismatches() {
        val review = OfficialScreenshotAnalyzer.analyze("   ", verification(), now = 3_000L)

        assertEquals(0, review.recognizedCharacterCount)
        assertEquals(0, review.matchedCount)
        assertEquals(0, review.possibleMismatchCount)
        assertTrue(review.comparisons.all {
            it.status == ScreenshotComparisonStatus.NOT_RECOGNIZED
        })
    }

    @Test
    fun explicitManualMatchResolvesScreenshotWarningForThatField() {
        val initial = verification()
        val opened = ManualVerificationRecorder.markSourceOpened(
            initial,
            initial.officialSources.first(),
            now = 1_000L
        )
        val review = OfficialScreenshotAnalyzer.analyze(
            "注册证编号：国械注准YYYYYYYY",
            opened,
            now = 2_000L
        )
        val withReview = ManualVerificationRecorder.attachScreenshotReview(opened, review)

        assertTrue(withReview.hasUnresolvedScreenshotMismatch)

        val manuallyConfirmed = ManualVerificationRecorder.recordFinding(
            withReview,
            VerificationEvidenceField.REGISTRATION_NUMBER,
            EvidenceMatchStatus.MATCHED,
            now = 3_000L
        )

        assertFalse(manuallyConfirmed.hasUnresolvedScreenshotMismatch)
    }
}
