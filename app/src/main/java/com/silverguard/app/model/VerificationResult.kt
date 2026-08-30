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
    val checkedAt: String? = null
) {
    val statusSummary: String
        get() = when (status) {
            VerificationStatus.NOT_READY -> "缺少核验信息"
            VerificationStatus.READY -> "待核验"
            VerificationStatus.MANUAL_REQUIRED -> "待人工核验"
            VerificationStatus.VERIFIED -> "已有官方数据结果"
            VerificationStatus.NOT_FOUND -> "暂未找到记录"
            VerificationStatus.ERROR -> "查询异常"
        }
}
