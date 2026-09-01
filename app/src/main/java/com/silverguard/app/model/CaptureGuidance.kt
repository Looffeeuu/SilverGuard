package com.silverguard.app.model

data class CaptureGuidance(
    val shouldSuggestSupplement: Boolean,
    val informationIsLimited: Boolean,
    val missingFields: List<String>,
    val suggestedShots: List<String>,
    val message: String
)
