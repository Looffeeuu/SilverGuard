package com.silverguard.app.engine

import com.silverguard.app.model.EvidenceMatchStatus
import com.silverguard.app.model.OfficialSearchOutcome
import com.silverguard.app.model.ProductInfo
import com.silverguard.app.model.VerificationEvidenceField
import com.silverguard.app.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualVerificationRecorderTest {

    private fun readyResult() = VerificationPlanner.plan(
        ProductInfo(
            name = "示例理疗仪",
            manufacturer = "示例医疗科技有限公司",
            model = "SG-01",
            registrationNumber = "国械注准XXXXXXXX"
        )
    )

    @Test
    fun openingOfficialSourceCreatesManualRecordWithoutClaimingVerification() {
        val initial = readyResult()

        val updated = ManualVerificationRecorder.markSourceOpened(
            initial,
            initial.officialSources.first(),
            now = 1_000L
        )

        assertEquals(VerificationStatus.MANUAL_REQUIRED, updated.status)
        assertNotNull(updated.manualRecord)
        assertEquals(1_000L, updated.manualRecord?.openedAt)
        assertEquals(OfficialSearchOutcome.NOT_RECORDED, updated.manualRecord?.searchOutcome)
        assertTrue(updated.status != VerificationStatus.VERIFIED)
    }

    @Test
    fun foundRecordIsOnlyMarkedAsManuallyReviewed() {
        val opened = readyResult().let {
            ManualVerificationRecorder.markSourceOpened(it, it.officialSources.first(), now = 1_000L)
        }

        val updated = ManualVerificationRecorder.recordSearchOutcome(
            opened,
            OfficialSearchOutcome.RECORD_FOUND,
            now = 2_000L
        )

        assertEquals(VerificationStatus.MANUAL_REVIEWED, updated.status)
        assertEquals(2_000L, updated.manualRecord?.reviewedAt)
        assertTrue(updated.status != VerificationStatus.VERIFIED)
    }

    @Test
    fun mismatchedFindingIsRecordedAndExplained() {
        val opened = readyResult().let {
            ManualVerificationRecorder.markSourceOpened(it, it.officialSources.first(), now = 1_000L)
        }

        val updated = ManualVerificationRecorder.recordFinding(
            opened,
            VerificationEvidenceField.REGISTRANT,
            EvidenceMatchStatus.MISMATCHED,
            now = 2_000L
        )

        assertTrue(updated.manualRecord?.hasMismatch == true)
        assertTrue(updated.manualRecord?.summary?.contains("不一致") == true)
        assertEquals(VerificationStatus.MANUAL_REVIEWED, updated.status)
    }

    @Test
    fun notFoundAndUnavailableRemainCautiousManualOutcomes() {
        val opened = readyResult().let {
            ManualVerificationRecorder.markSourceOpened(it, it.officialSources.first(), now = 1_000L)
        }
        val notFound = ManualVerificationRecorder.recordSearchOutcome(
            opened,
            OfficialSearchOutcome.RECORD_NOT_FOUND,
            now = 2_000L
        )
        val unavailable = ManualVerificationRecorder.recordSearchOutcome(
            opened,
            OfficialSearchOutcome.PAGE_UNAVAILABLE,
            now = 3_000L
        )

        assertEquals(VerificationStatus.NOT_FOUND, notFound.status)
        assertEquals(VerificationStatus.ERROR, unavailable.status)
        assertFalse(notFound.manualRecord?.summary.orEmpty().contains("假货"))
        assertTrue(notFound.status != VerificationStatus.VERIFIED)
        assertTrue(unavailable.status != VerificationStatus.VERIFIED)
    }
}
