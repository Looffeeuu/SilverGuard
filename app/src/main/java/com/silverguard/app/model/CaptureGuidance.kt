package com.silverguard.app.model

enum class AnalysisInputMethod(val displayName: String) {
    PHOTO("商品照片"),
    SCREENSHOT("商品截图"),
    TEXT("文字说明"),
    PRODUCT_LINK("商品链接")
}

enum class SupplementAction {
    TAKE_PHOTO,
    SELECT_SCREENSHOT
}

data class CaptureGuidance(
    val shouldSuggestSupplement: Boolean,
    val informationIsLimited: Boolean,
    val missingFields: List<String>,
    val suggestedShots: List<String>,
    val title: String,
    val message: String,
    val actions: List<SupplementAction>
)
