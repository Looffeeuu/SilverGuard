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

data class ProductInfo(
    val name: String? = null,
    val brand: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val specification: String? = null,
    val registrationNumber: String? = null,
    val price: String? = null
) {
    val detectedCount: Int
        get() = listOf(
            name,
            brand,
            manufacturer,
            model,
            specification,
            registrationNumber,
            price
        ).count { !it.isNullOrBlank() }

    companion object {
        const val FIELD_COUNT = 7
    }
}

data class RiskAnalysis(
    val rawText: String,
    val category: String,
    val score: Int,
    val level: RiskLevel,
    val title: String,
    val flags: List<RiskFlag>,
    val productInfo: ProductInfo = ProductInfo()
)

data class OfficialSource(
    val name: String,
    val description: String,
    val url: String
)
