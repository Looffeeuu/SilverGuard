package com.silverguard.app.engine

import com.silverguard.app.model.CaptureGuidance
import com.silverguard.app.model.ProductInfo

object CaptureGuidanceEvaluator {
    fun evaluate(productInfo: ProductInfo): CaptureGuidance {
        val missingFields = buildList {
            if (productInfo.name.isNullOrBlank()) add("商品名称")
            if (productInfo.manufacturer.isNullOrBlank()) add("生产企业")
            if (productInfo.model.isNullOrBlank() && productInfo.specification.isNullOrBlank()) {
                add("型号或规格")
            }
            if (productInfo.registrationNumber.isNullOrBlank()) add("注册 / 备案号（如包装上有）")
        }
        val informationIsLimited = productInfo.detectedCount <= 2
        val suggestedShots = buildList {
            if (productInfo.name.isNullOrBlank() || productInfo.brand.isNullOrBlank()) {
                add("包装正面")
            }
            if (productInfo.manufacturer.isNullOrBlank()) add("包装背面的生产企业")
            if (
                productInfo.model.isNullOrBlank() ||
                productInfo.specification.isNullOrBlank() ||
                productInfo.registrationNumber.isNullOrBlank()
            ) {
                add("包装侧面的型号、规格和编号")
            }
        }.distinct()

        return CaptureGuidance(
            shouldSuggestSupplement = missingFields.isNotEmpty(),
            informationIsLimited = informationIsLimited,
            missingFields = missingFields,
            suggestedShots = suggestedShots,
            message = if (informationIsLimited) {
                "识别到的商品信息较少，分析结果准确性降低。你可以继续查看，也可以补拍包装后再分析。"
            } else {
                "当前结果可以继续查看；如方便，补拍包装能帮助核对更多信息。"
            }
        )
    }

    fun mergeRecognizedText(existingText: String, newText: String): String {
        val existing = existingText.trim()
        val supplement = newText.trim()
        if (existing.isBlank()) return supplement
        if (supplement.isBlank() || existing.contains(supplement)) return existing
        return "$existing\n\n【补拍包装文字】\n$supplement"
    }
}
