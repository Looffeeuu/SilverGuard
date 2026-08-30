package com.silverguard.app.model

enum class ScreenshotComparisonStatus(val displayName: String) {
    MATCHED("截图文字一致"),
    POSSIBLE_MISMATCH("截图文字可能不一致"),
    NOT_RECOGNIZED("截图中未识别到"),
    NEEDS_CONFIRMATION("需要人工确认")
}

data class OfficialScreenshotComparison(
    val field: VerificationEvidenceField,
    val expectedValue: String?,
    val detectedText: String?,
    val status: ScreenshotComparisonStatus,
    val explanation: String
)

data class OfficialScreenshotReview(
    val processedAt: Long,
    val recognizedCharacterCount: Int,
    val comparisons: List<OfficialScreenshotComparison>
) {
    val matchedCount: Int
        get() = comparisons.count { it.status == ScreenshotComparisonStatus.MATCHED }

    val possibleMismatchCount: Int
        get() = comparisons.count { it.status == ScreenshotComparisonStatus.POSSIBLE_MISMATCH }

    val hasPossibleMismatch: Boolean
        get() = possibleMismatchCount > 0

    val summary: String
        get() = when {
            recognizedCharacterCount == 0 -> "截图中没有识别到可用文字"
            hasPossibleMismatch -> "截图辅助比对发现可能不一致"
            matchedCount > 0 -> "截图辅助比对找到 $matchedCount 项一致文字"
            else -> "截图文字不足，需要人工确认"
        }
}
