package com.silverguard.app.model

data class EcommerceProductField(
    val type: EcommerceFieldType,
    val value: String,
    val source: EcommerceFieldSource,
    val confidence: Float
)
