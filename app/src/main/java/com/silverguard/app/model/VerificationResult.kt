package com.silverguard.app.model

enum class ConflictPriority {
    HIGH
}

data class ClaimRegistrationConflict(
    val priority: ConflictPriority,
    val matchedClaims: List<String>,
    val title: String,
    val explanation: String
)

data class VerificationResult(
    val status: VerificationStatus,
    val readiness: VerificationReadiness,
    val registration: RegistrationClassification,
    val officialSources: List<OfficialSource>,
    val checkedAt: String? = null,
    val checklist: List<VerificationChecklistItem> = emptyList(),
    val manualRecord: ManualVerificationRecord? = null,
    val screenshotReview: OfficialScreenshotReview? = null
) {
    val hasUnresolvedScreenshotMismatch: Boolean
        get() = screenshotReview?.comparisons?.any { comparison ->
            comparison.status == ScreenshotComparisonStatus.POSSIBLE_MISMATCH &&
                manualRecord?.findings
                    ?.firstOrNull { it.item.field == comparison.field }
                    ?.status != EvidenceMatchStatus.MATCHED
        } == true

    val statusSummary: String
        get() = when {
            hasUnresolvedScreenshotMismatch -> "截图辅助比对发现可能不一致"
            else -> when (status) {
            VerificationStatus.NOT_READY -> "缺少核验信息"
            VerificationStatus.READY -> "待核验"
            VerificationStatus.MANUAL_REQUIRED -> "待人工核验"
            VerificationStatus.MANUAL_REVIEWED -> manualRecord?.summary ?: "已记录人工核对"
            VerificationStatus.VERIFIED -> "已有官方数据结果"
            VerificationStatus.NOT_FOUND -> "人工查询暂未找到"
            VerificationStatus.ERROR -> "官方页面暂时不可用"
            }
        }
}
