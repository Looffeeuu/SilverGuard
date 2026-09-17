package com.silverguard.app.model

enum class AiAnalysisStatus {
    NOT_REQUESTED,
    NOT_CONFIGURED,
    SUCCESS,
    UNAVAILABLE,
    INVALID_RESPONSE,
    ERROR
}

enum class AiConfidence(val displayName: String) {
    LOW("较低"),
    MEDIUM("中等"),
    HIGH("较高")
}

data class AiRiskInsight(
    val summary: String,
    val implicitClaims: List<String>,
    val persuasionTactics: List<String>,
    val verificationQuestions: List<String>,
    val consumerAdvice: String,
    val confidence: AiConfidence,
    val providerName: String = "智谱 AI",
    val modelName: String = "GLM-4.7-Flash",
    val analyzedAt: Long = System.currentTimeMillis()
)

data class AiAnalysisResult(
    val status: AiAnalysisStatus = AiAnalysisStatus.NOT_REQUESTED,
    val insight: AiRiskInsight? = null,
    val message: String = "尚未进行 AI 深入分析"
)
