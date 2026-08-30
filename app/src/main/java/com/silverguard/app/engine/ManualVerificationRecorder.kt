package com.silverguard.app.engine

import com.silverguard.app.model.EvidenceMatchStatus
import com.silverguard.app.model.ManualVerificationRecord
import com.silverguard.app.model.OfficialSearchOutcome
import com.silverguard.app.model.OfficialScreenshotReview
import com.silverguard.app.model.OfficialSource
import com.silverguard.app.model.VerificationEvidenceField
import com.silverguard.app.model.VerificationFinding
import com.silverguard.app.model.VerificationResult
import com.silverguard.app.model.VerificationStatus

object ManualVerificationRecorder {

    fun attachScreenshotReview(
        result: VerificationResult,
        review: OfficialScreenshotReview
    ): VerificationResult = result.copy(screenshotReview = review)

    fun markSourceOpened(
        result: VerificationResult,
        source: OfficialSource,
        now: Long = System.currentTimeMillis()
    ): VerificationResult {
        val existing = result.manualRecord?.takeIf { it.source.id == source.id }
        val record = existing?.copy(openedAt = now) ?: ManualVerificationRecord(
            source = source,
            openedAt = now,
            findings = result.checklist.map { VerificationFinding(it) }
        )
        return result.copy(
            status = if (record.searchOutcome == OfficialSearchOutcome.NOT_RECORDED) {
                VerificationStatus.MANUAL_REQUIRED
            } else {
                statusFor(record.searchOutcome)
            },
            manualRecord = record,
            screenshotReview = if (existing == null) null else result.screenshotReview
        )
    }

    fun recordSearchOutcome(
        result: VerificationResult,
        outcome: OfficialSearchOutcome,
        now: Long = System.currentTimeMillis()
    ): VerificationResult {
        val current = result.manualRecord ?: return result
        val record = current.copy(
            reviewedAt = if (outcome == OfficialSearchOutcome.NOT_RECORDED) null else now,
            searchOutcome = outcome
        )
        return result.copy(
            status = statusFor(outcome),
            manualRecord = record
        )
    }

    fun recordFinding(
        result: VerificationResult,
        field: VerificationEvidenceField,
        status: EvidenceMatchStatus,
        now: Long = System.currentTimeMillis()
    ): VerificationResult {
        val current = result.manualRecord ?: return result
        val updated = current.findings.map { finding ->
            if (finding.item.field == field) finding.copy(status = status) else finding
        }
        val record = current.copy(
            reviewedAt = now,
            searchOutcome = OfficialSearchOutcome.RECORD_FOUND,
            findings = updated
        )
        return result.copy(
            status = VerificationStatus.MANUAL_REVIEWED,
            manualRecord = record
        )
    }

    private fun statusFor(outcome: OfficialSearchOutcome): VerificationStatus = when (outcome) {
        OfficialSearchOutcome.NOT_RECORDED -> VerificationStatus.MANUAL_REQUIRED
        OfficialSearchOutcome.RECORD_FOUND -> VerificationStatus.MANUAL_REVIEWED
        OfficialSearchOutcome.RECORD_NOT_FOUND -> VerificationStatus.NOT_FOUND
        OfficialSearchOutcome.PAGE_UNAVAILABLE -> VerificationStatus.ERROR
    }
}
