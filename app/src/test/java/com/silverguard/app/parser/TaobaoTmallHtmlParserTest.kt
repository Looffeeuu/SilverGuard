package com.silverguard.app.parser

import com.silverguard.app.model.EcommerceFieldSource
import com.silverguard.app.model.EcommerceFieldType
import com.silverguard.app.model.EcommercePlatform
import com.silverguard.app.model.ProductDetailParseStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaobaoTmallHtmlParserTest {
    private val parser = TaobaoTmallHtmlParser()

    @Test
    fun extractsTitleFromOpenGraph() {
        val result = parse(TaobaoHtmlFixtures.ogOnly)

        assertEquals("测试理疗仪", result.title)
        assertEquals(
            EcommerceFieldSource.OPEN_GRAPH,
            result.fields.first { it.type == EcommerceFieldType.TITLE }.source
        )
    }

    @Test
    fun extractsProductFieldsFromJsonLd() {
        val result = parse(TaobaoHtmlFixtures.jsonLdOnly)

        assertEquals("JSON商品名称", result.title)
        assertEquals("安心牌", result.brand)
        assertEquals("¥2980", result.price)
        assertEquals("安心旗舰店", result.sellerName)
        assertEquals(ProductDetailParseStatus.SUCCESS, result.parseStatus)
    }

    @Test
    fun jsonLdTitleHasPriorityOverOpenGraphAndHtmlTitle() {
        val result = parse(TaobaoHtmlFixtures.jsonLdAndOpenGraph)

        assertEquals("JSON优先标题", result.title)
        assertEquals(
            EcommerceFieldSource.JSON_LD,
            result.fields.first { it.type == EcommerceFieldType.TITLE }.source
        )
    }

    @Test
    fun missingPriceRemainsNull() {
        val result = parse(TaobaoHtmlFixtures.ogOnly)

        assertNull(result.price)
    }

    @Test
    fun titleOnlyIsPartial() {
        val result = parse(TaobaoHtmlFixtures.titleOnly)

        assertEquals("只有标题的商品", result.title)
        assertEquals(ProductDetailParseStatus.PARTIAL, result.parseStatus)
    }

    @Test
    fun titleAndPriceAreSuccessfulBasicInformation() {
        val result = parse(TaobaoHtmlFixtures.titleAndPrice)

        assertEquals("测试理疗仪", result.title)
        assertEquals("¥2980.00", result.price)
        assertEquals(ProductDetailParseStatus.SUCCESS, result.parseStatus)
    }

    @Test
    fun loginPageIsDetectedWithoutParsingFakeFields() {
        val result = parse(TaobaoHtmlFixtures.loginPage)

        assertEquals(ProductDetailParseStatus.LOGIN_REQUIRED, result.parseStatus)
        assertNull(result.title)
    }

    @Test
    fun captchaPageIsDetectedWithoutCrashing() {
        val result = parse(TaobaoHtmlFixtures.captchaPage)

        assertEquals(ProductDetailParseStatus.CAPTCHA_REQUIRED, result.parseStatus)
        assertTrue(result.fields.isEmpty())
    }

    private fun parse(html: String) = parser.parse(
        html = html,
        platform = EcommercePlatform.TAOBAO,
        sourceUrl = "https://item.taobao.com/item.htm?id=123456789",
        productId = "123456789",
        fetchedAt = 123L
    )
}
