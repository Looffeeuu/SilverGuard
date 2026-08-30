package com.silverguard.app.network

import com.silverguard.app.engine.EcommerceLinkParser
import com.silverguard.app.model.EcommercePlatform
import com.silverguard.app.model.ProductDetailParseStatus
import com.silverguard.app.parser.TaobaoHtmlFixtures
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaobaoTmallProductResolverTest {

    @Test
    fun standardTaobaoUrlEntersResolver() = runBlocking {
        val requested = mutableListOf<String>()
        val resolver = resolverWith { url ->
            requested += url
            response(200, TaobaoHtmlFixtures.titleAndPrice).copy(requestUrl = url)
        }
        val info = EcommerceLinkParser.parse(
            "https://item.taobao.com/item.htm?id=123456789"
        )

        val product = resolver.resolve(info)

        assertEquals(listOf(info.normalizedUrl), requested)
        assertEquals(EcommercePlatform.TAOBAO, product.platform)
        assertEquals("123456789", product.productId)
        assertEquals(ProductDetailParseStatus.SUCCESS, product.parseStatus)
    }

    @Test
    fun standardTmallUrlEntersResolver() = runBlocking {
        val resolver = resolverWith { url ->
            response(200, TaobaoHtmlFixtures.ogOnly).copy(requestUrl = url)
        }
        val info = EcommerceLinkParser.parse(
            "https://detail.tmall.com/item.htm?id=99887766"
        )

        val product = resolver.resolve(info)

        assertEquals(EcommercePlatform.TMALL, product.platform)
        assertEquals("99887766", product.productId)
        assertEquals("测试理疗仪", product.title)
    }

    @Test
    fun http403ReturnsAccessRestricted() = runBlocking {
        val resolver = resolverWith { url -> response(403, "").copy(requestUrl = url) }
        val info = EcommerceLinkParser.parse(
            "https://item.taobao.com/item.htm?id=123456789"
        )

        val product = resolver.resolve(info)

        assertEquals(ProductDetailParseStatus.ACCESS_RESTRICTED, product.parseStatus)
        assertNull(product.title)
    }

    @Test
    fun loginHtmlReturnsLoginRequired() = runBlocking {
        val resolver = resolverWith { url ->
            response(200, TaobaoHtmlFixtures.loginPage).copy(requestUrl = url)
        }
        val info = EcommerceLinkParser.parse(
            "https://item.taobao.com/item.htm?id=123456789"
        )

        val product = resolver.resolve(info)

        assertEquals(ProductDetailParseStatus.LOGIN_REQUIRED, product.parseStatus)
    }

    @Test
    fun captchaHtmlReturnsCaptchaRequired() = runBlocking {
        val resolver = resolverWith { url ->
            response(200, TaobaoHtmlFixtures.captchaPage).copy(requestUrl = url)
        }
        val info = EcommerceLinkParser.parse(
            "https://item.taobao.com/item.htm?id=123456789"
        )

        val product = resolver.resolve(info)

        assertEquals(ProductDetailParseStatus.CAPTCHA_REQUIRED, product.parseStatus)
    }

    @Test
    fun timeoutReturnsNetworkError() = runBlocking {
        val resolver = resolverWith { throw IOException("timeout") }
        val info = EcommerceLinkParser.parse(
            "https://item.taobao.com/item.htm?id=123456789"
        )

        val product = resolver.resolve(info)

        assertEquals(ProductDetailParseStatus.NETWORK_ERROR, product.parseStatus)
        assertTrue(product.message.contains("网络连接失败"))
    }

    @Test
    fun emptyPageReturnsParseFailedWithoutCrashing() = runBlocking {
        val resolver = resolverWith { url ->
            response(200, TaobaoHtmlFixtures.emptyPage).copy(requestUrl = url)
        }
        val info = EcommerceLinkParser.parse(
            "https://item.taobao.com/item.htm?id=123456789"
        )

        val product = resolver.resolve(info)

        assertEquals(ProductDetailParseStatus.PARSE_FAILED, product.parseStatus)
        assertTrue(product.fields.isEmpty())
    }

    @Test
    fun shortPageUsesOnlyCleanStaticProductTarget() = runBlocking {
        val requested = mutableListOf<String>()
        val resolver = resolverWith { url ->
            requested += url
            when {
                url.startsWith("https://e.tb.cn/") ->
                    response(200, TaobaoHtmlFixtures.shortPageWithStaticProductUrl)
                        .copy(requestUrl = url)
                else -> response(200, TaobaoHtmlFixtures.titleAndPrice)
                    .copy(requestUrl = url)
            }
        }
        val info = EcommerceLinkParser.parse("https://e.tb.cn/h.test?tk=secret")

        val product = resolver.resolve(info)

        assertEquals(
            listOf(
                "https://e.tb.cn/h.test?tk=secret",
                "https://item.taobao.com/item.htm?id=1044769261323"
            ),
            requested
        )
        assertEquals("1044769261323", product.productId)
        assertEquals(ProductDetailParseStatus.SUCCESS, product.parseStatus)
    }

    private fun resolverWith(block: suspend (String) -> EcommerceHttpResponse) =
        TaobaoTmallProductResolver(
            client = EcommercePageClient(block),
            clock = { 123L }
        )
}
