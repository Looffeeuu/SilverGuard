package com.silverguard.app.model

enum class RiskLevel {
    LOW, MEDIUM, HIGH
}

data class RiskFlag(
    val type: String,
    val matched: String,
    val explanation: String,
    val weight: Int
)

data class RiskAnalysis(
    val rawText: String,
    val category: String,
    val score: Int,
    val level: RiskLevel,
    val title: String,
    val flags: List<RiskFlag>,
    val extractedPrice: String? = null,
    val extractedModel: String? = null
)

data class OfficialSource(
    val name: String,
    val description: String,
    val url: String
)
