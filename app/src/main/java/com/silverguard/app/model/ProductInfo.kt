package com.silverguard.app.model

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

    val completenessLabel: String
        get() = when (detectedCount) {
            in 5..FIELD_COUNT -> "较完整"
            in 3..4 -> "部分完整"
            else -> "不完整"
        }

    companion object {
        const val FIELD_COUNT = 7
    }
}
