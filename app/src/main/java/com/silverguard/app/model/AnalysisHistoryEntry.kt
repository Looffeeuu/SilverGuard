package com.silverguard.app.model

data class AnalysisHistoryEntry(
    val id: String,
    val savedAt: Long,
    val analysis: RiskAnalysis,
    val inputMethods: Set<AnalysisInputMethod>,
    val draftText: String = analysis.rawText
) {
    val displayTitle: String
        get() = analysis.productInfo.name?.takeIf { it.isNotBlank() }
            ?: analysis.rawText.lineSequence().firstOrNull { it.isNotBlank() }?.take(60)
            ?: "商品分析"
}
