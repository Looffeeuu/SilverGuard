package com.silverguard.app.engine

import com.silverguard.app.model.CaptureGuidance
import com.silverguard.app.model.AnalysisInputMethod
import com.silverguard.app.model.ProductInfo
import com.silverguard.app.model.SupplementAction

object CaptureGuidanceEvaluator {
    fun evaluate(
        productInfo: ProductInfo,
        inputMethod: AnalysisInputMethod = AnalysisInputMethod.PHOTO
    ): CaptureGuidance = evaluate(productInfo, setOf(inputMethod))

    fun evaluate(
        productInfo: ProductInfo,
        inputMethods: Set<AnalysisInputMethod>
    ): CaptureGuidance {
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

        val accuracyPrefix = if (informationIsLimited) {
            "识别到的信息较少，分析结果准确性降低。"
        } else {
            "当前信息可以继续分析。"
        }
        val effectiveMethods = inputMethods.ifEmpty { setOf(AnalysisInputMethod.TEXT) }
        val hasPhoto = AnalysisInputMethod.PHOTO in effectiveMethods
        val hasScreenshot = AnalysisInputMethod.SCREENSHOT in effectiveMethods
        val hasText = AnalysisInputMethod.TEXT in effectiveMethods ||
            AnalysisInputMethod.PRODUCT_LINK in effectiveMethods
        val title: String
        val message: String
        val actions: List<SupplementAction>
        when {
            effectiveMethods.size > 1 -> {
                val sourceSummary = effectiveMethods.joinToString("、") { it.displayName }
                title = "已综合：$sourceSummary"
                message = "$accuracyPrefix 当前结论已经综合多种信息；还可以在上方继续补充文字、照片或截图。"
                actions = buildList {
                    if (!hasPhoto) add(SupplementAction.TAKE_PHOTO)
                    if (!hasScreenshot) add(SupplementAction.SELECT_SCREENSHOT)
                }
            }
            hasPhoto -> {
                title = "想看得更详细，可以补拍包装"
                message = "$accuracyPrefix 建议补拍包装背面或侧面的标签；不补拍也可以继续查看。"
                actions = if (missingFields.isEmpty()) emptyList() else listOf(SupplementAction.TAKE_PHOTO)
            }
            hasScreenshot -> {
                title = "想看得更详细，可以补充截图"
                message = "$accuracyPrefix 建议补充商品详情、包装参数或宣传内容截图。"
                actions = if (missingFields.isEmpty()) emptyList() else listOf(SupplementAction.SELECT_SCREENSHOT)
            }
            else -> {
                title = if (AnalysisInputMethod.PRODUCT_LINK in effectiveMethods) {
                    "商品链接可能没有完整包装信息"
                } else {
                    "目前主要依据文字描述"
                }
                message = if (hasText) {
                    "$accuracyPrefix 文字或链接可能没有包含包装细节，建议拍商品包装或选择商品截图。"
                } else {
                    "$accuracyPrefix 建议继续补充商品包装照片或详情截图。"
                }
                actions = listOf(
                    SupplementAction.TAKE_PHOTO,
                    SupplementAction.SELECT_SCREENSHOT
                )
            }
        }

        return CaptureGuidance(
            shouldSuggestSupplement = true,
            informationIsLimited = informationIsLimited,
            missingFields = missingFields,
            suggestedShots = suggestedShots,
            title = title,
            message = message,
            actions = actions
        )
    }

    fun mergeRecognizedText(existingText: String, newText: String): String {
        val existing = existingText.trim()
        val supplement = newText.trim()
        if (existing.isBlank()) return supplement
        if (supplement.isBlank() || existing.contains(supplement)) return existing
        return "$existing\n\n【补充识别文字】\n$supplement"
    }
}
