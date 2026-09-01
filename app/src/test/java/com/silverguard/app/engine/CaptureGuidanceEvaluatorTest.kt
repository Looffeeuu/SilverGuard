package com.silverguard.app.engine

import com.silverguard.app.model.ProductInfo
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

        assertEquals("商品正面\n\n【补拍包装文字】\n生产企业：安心公司", result)
    }
}
