package com.silverguard.app.engine

import com.silverguard.app.model.EcommerceLinkStatus
import com.silverguard.app.model.EcommercePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EcommerceLinkParserTest {

    @Test
    fun extractsTaobaoProductId() {
        val result = EcommerceLinkParser.parse(
            "https://item.taobao.com/item.htm?id=123456789"
        )

        assertEquals(EcommercePlatform.TAOBAO, result.platform)
        assertEquals("123456789", result.productId)
        assertEquals(EcommerceLinkStatus.PRODUCT_ID_EXTRACTED, result.status)
    }

    @Test
    fun extractsTmallProductId() {
        val result = EcommerceLinkParser.parse(
            "https://detail.tmall.com/item.htm?id=99887766"
        )

        assertEquals(EcommercePlatform.TMALL, result.platform)
        assertEquals("99887766", result.productId)
    }

    @Test
    fun extractsPinduoduoProductId() {
        val result = EcommerceLinkParser.parse(
            "https://mobile.yangkeduo.com/goods.html?goods_id=11223344"
        )

        assertEquals(EcommercePlatform.PINDUODUO, result.platform)
        assertEquals("11223344", result.productId)
    }

    @Test
    fun extractsJdProductIdFromPath() {
        val result = EcommerceLinkParser.parse(
            "https://item.jd.com/100012345678.html"
        )

        assertEquals(EcommercePlatform.JD, result.platform)
        assertEquals("100012345678", result.productId)
    }

    @Test
    fun marksDouyinShortLinkAsNeedingRedirect() {
        val result = EcommerceLinkParser.parse("https://v.douyin.com/xxxxxx/")

        assertEquals(EcommercePlatform.DOUYIN, result.platform)
        assertEquals(EcommerceLinkStatus.NEED_REDIRECT, result.status)
        assertNull(result.productId)
    }

    @Test
    fun findsTaobaoShortLinkInsideShareText() {
        val result = EcommerceLinkParser.parse(
            """
                复制打开淘宝
                https://m.tb.cn/abcdef
                真的很好用
            """.trimIndent()
        )

        assertEquals(EcommercePlatform.TAOBAO, result.platform)
        assertEquals(EcommerceLinkStatus.NEED_REDIRECT, result.status)
    }

    @Test
    fun findsCurrentTaobaoShortLinkInsidePasswordShareText() {
        val result = EcommerceLinkParser.parse(
            """
                【淘宝】
                https://e.tb.cn/h.8mx5YusWbiu1wK8?tk=cLeqT4WVH9Y HU006 「拷贝链接」
                点击链接直接打开 或者 淘宝搜索直接打开
            """.trimIndent()
        )

        assertEquals(EcommercePlatform.TAOBAO, result.platform)
        assertEquals(EcommerceLinkStatus.NEED_REDIRECT, result.status)
        assertEquals(
            "https://e.tb.cn/h.8mx5YusWbiu1wK8?tk=cLeqT4WVH9Y",
            result.extractedUrl
        )
        assertNull(result.productId)
    }

    @Test
    fun findsJdShortLinkInsideShareText() {
        val result = EcommerceLinkParser.parse(
            "京东分享\nhttps://u.jd.com/abcdef\n复制链接打开京东"
        )

        assertEquals(EcommercePlatform.JD, result.platform)
        assertEquals(EcommerceLinkStatus.NEED_REDIRECT, result.status)
    }

    @Test
    fun findsPinduoduoShortLinkInsideShareText() {
        val result = EcommerceLinkParser.parse(
            "拼多多分享 https://p.pinduoduo.com/abcdef 复制链接打开"
        )

        assertEquals(EcommercePlatform.PINDUODUO, result.platform)
        assertEquals(EcommerceLinkStatus.NEED_REDIRECT, result.status)
        assertNull(result.productId)
    }

    @Test
    fun ordinaryTextReturnsNoLink() {
        val result = EcommerceLinkParser.parse("太赫兹理疗仪 改善鼻炎")

        assertEquals(EcommercePlatform.UNKNOWN, result.platform)
        assertEquals(EcommerceLinkStatus.NO_LINK, result.status)
        assertNull(result.extractedUrl)
    }

    @Test
    fun localAddressIsNeverTreatedAsSupportedPlatform() {
        val result = EcommerceLinkParser.parse("http://127.0.0.1/test")

        assertEquals(EcommercePlatform.UNKNOWN, result.platform)
        assertEquals(EcommerceLinkStatus.UNSUPPORTED, result.status)
        assertFalse(result.isSupportedPlatform)
    }

    @Test
    fun unsupportedWebsiteReturnsNaturalUnsupportedResult() {
        val result = EcommerceLinkParser.parse("https://example.com/product/123")

        assertEquals(EcommercePlatform.UNKNOWN, result.platform)
        assertEquals(EcommerceLinkStatus.UNSUPPORTED, result.status)
        assertTrue(result.message.contains("暂不支持"))
    }

    @Test
    fun deceptiveDomainIsNotRecognizedAsTaobao() {
        val result = EcommerceLinkParser.parse(
            "https://fake-taobao.com/item.htm?id=123"
        )

        assertEquals(EcommercePlatform.UNKNOWN, result.platform)
        assertEquals(EcommerceLinkStatus.UNSUPPORTED, result.status)
    }

    @Test
    fun removesKnownTrackingParametersAndKeepsProductId() {
        val result = EcommerceLinkParser.parse(
            "https://item.taobao.com/item.htm?id=123&spm=abc&utm_source=test"
        )

        assertEquals("123", result.productId)
        assertEquals(
            "https://item.taobao.com/item.htm?id=123",
            result.normalizedUrl
        )
    }

    @Test
    fun normalizationKeepsUnknownProductParameters() {
        val result = EcommerceLinkParser.parse(
            "https://item.taobao.com/item.htm?id=123&skuId=9988&utm_medium=share"
        )

        assertEquals(
            "https://item.taobao.com/item.htm?id=123&skuId=9988",
            result.normalizedUrl
        )
    }

    @Test
    fun extractsSupportedProductLinkFromLongShareText() {
        val longPrefix = "限时优惠，复制下面的内容打开手机应用。".repeat(80)
        val result = EcommerceLinkParser.parse(
            "$longPrefix\nhttps://item.taobao.com/item.htm?id=55667788\n复制完成"
        )

        assertEquals(EcommercePlatform.TAOBAO, result.platform)
        assertEquals("55667788", result.productId)
    }

    @Test
    fun supportedProductLinkWinsOverUnrelatedImageUrl() {
        val result = EcommerceLinkParser.parse(
            "图片 https://example.com/ad.jpg 商品 https://item.jd.com/100012345678.html"
        )

        assertEquals(EcommercePlatform.JD, result.platform)
        assertEquals("100012345678", result.productId)
    }

    @Test
    fun hostMatchingIsCaseInsensitiveButStillExact() {
        val result = EcommerceLinkParser.parse(
            "https://ITEM.TAOBAO.COM/item.htm?id=123456"
        )

        assertEquals(EcommercePlatform.TAOBAO, result.platform)
        assertEquals("123456", result.productId)
    }
}
