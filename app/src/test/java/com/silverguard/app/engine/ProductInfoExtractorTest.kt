package com.silverguard.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductInfoExtractorTest {

    @Test
    fun extractsLabeledPackagingFields() {
        val text = """
            商品名称：安心牌太赫兹理疗仪
            品牌：安心
            生产企业：上海安心健康科技有限公司
            型号：TD-01
            规格：1台/盒
            备案编号：粤械备20261234号
            现价：￥2,980
        """.trimIndent()

        val result = ProductInfoExtractor.extract(text)

        assertEquals("安心牌太赫兹理疗仪", result.name)
        assertEquals("安心", result.brand)
        assertEquals("上海安心健康科技有限公司", result.manufacturer)
        assertEquals("TD-01", result.model)
        assertEquals("1台/盒", result.specification)
        assertEquals("粤械备20261234号", result.registrationNumber)
        assertEquals("¥2980", result.price)
        assertEquals(7, result.detectedCount)
    }

    @Test
    fun infersNameAndExtractsModelAndPriceFromAdvertisingText() {
        val text = "太赫兹理疗仪 疏通血管 改善鼻炎 专家推荐 现价￥2980 型号 TD-01"

        val result = ProductInfoExtractor.extract(text)

        assertEquals("太赫兹理疗仪", result.name)
        assertEquals("TD-01", result.model)
        assertEquals("¥2980", result.price)
        assertNull(result.manufacturer)
        assertNull(result.registrationNumber)
    }

    @Test
    fun keepsUnknownFieldsEmpty() {
        val result = ProductInfoExtractor.extract("专家推荐 七天见效 保证有效")

        assertEquals(0, result.detectedCount)
        assertNull(result.name)
        assertNull(result.brand)
        assertNull(result.manufacturer)
        assertNull(result.registrationNumber)
    }
}
