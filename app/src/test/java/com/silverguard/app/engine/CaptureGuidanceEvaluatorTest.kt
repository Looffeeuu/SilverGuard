package com.silverguard.app.engine

import com.silverguard.app.model.ProductInfo
import com.silverguard.app.model.AnalysisInputMethod
import com.silverguard.app.model.SupplementAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureGuidanceEvaluatorTest {
    @Test
    fun sparseInformationWarnsThatAccuracyIsLower() {
        val result = CaptureGuidanceEvaluator.evaluate(ProductInfo(name = "测试商品"))

        assertTrue(result.informationIsLimited)
        assertTrue(result.shouldSuggestSupplement)
        assertTrue(result.message.contains("分析结果准确性降低"))
    }

    @Test
    fun sufficientlyCompleteInformationDoesNotShowAccuracyWarning() {
        val result = CaptureGuidanceEvaluator.evaluate(
            ProductInfo(
                name = "测试商品",
                brand = "安心",
                manufacturer = "安心公司",
                model = "A1",
                specification = "20克",
                registrationNumber = "国械注准20260000001"
            )
        )

        assertFalse(result.informationIsLimited)
        assertFalse(result.message.contains("准确性降低"))
    }

    @Test
    fun supplementalOcrIsAppendedWithoutDiscardingOriginalText() {
        val result = CaptureGuidanceEvaluator.mergeRecognizedText("商品正面", "生产企业：安心公司")

        assertEquals("商品正面\n\n【补充识别文字】\n生产企业：安心公司", result)
    }

    @Test
    fun textInputSuggestsPhotoAndScreenshotInsteadOfOnlyRetakingPhoto() {
        val result = CaptureGuidanceEvaluator.evaluate(
            ProductInfo(name = "测试商品"),
            AnalysisInputMethod.TEXT
        )

        assertEquals(
            listOf(SupplementAction.TAKE_PHOTO, SupplementAction.SELECT_SCREENSHOT),
            result.actions
        )
        assertTrue(result.message.contains("文字"))
    }

    @Test
    fun screenshotInputSuggestsAnotherScreenshot() {
        val result = CaptureGuidanceEvaluator.evaluate(
            ProductInfo(name = "测试商品"),
            AnalysisInputMethod.SCREENSHOT
        )

        assertEquals(listOf(SupplementAction.SELECT_SCREENSHOT), result.actions)
        assertTrue(result.title.contains("补充截图"))
    }

    @Test
    fun mixedInputSourcesAreCombinedAndOnlyMissingSourceIsSuggested() {
        val result = CaptureGuidanceEvaluator.evaluate(
            ProductInfo(name = "测试商品"),
            setOf(AnalysisInputMethod.TEXT, AnalysisInputMethod.PHOTO)
        )

        assertTrue(result.title.contains("商品照片"))
        assertTrue(result.title.contains("文字说明"))
        assertEquals(listOf(SupplementAction.SELECT_SCREENSHOT), result.actions)
    }
}
