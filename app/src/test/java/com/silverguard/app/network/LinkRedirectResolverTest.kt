package com.silverguard.app.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkRedirectResolverTest {

    @Test
    fun rejectsRedirectToLocalAddressWithoutRequestingIt() = runBlocking {
        val client = QueueClient(
            mutableListOf(redirect("http://127.0.0.1/test"))
        )

        val result = LinkRedirectResolver(client).resolve("https://e.tb.cn/h.test")

        assertTrue(result is LinkRedirectResult.Rejected)
        assertEquals(listOf("https://e.tb.cn/h.test"), client.requestedUrls)
    }

    @Test
    fun rejectsRedirectToUnknownHostWithoutRequestingIt() = runBlocking {
        val client = QueueClient(
            mutableListOf(redirect("https://example.com/product/123"))
        )

        val result = LinkRedirectResolver(client).resolve("https://m.tb.cn/h.test")

        assertTrue(result is LinkRedirectResult.Rejected)
        assertEquals(1, client.requestedUrls.size)
    }

    @Test
    fun followsAllowedRedirectAndStopsAtProductPage() = runBlocking {
        val finalUrl = "https://item.taobao.com/item.htm?id=123456789"
        val client = QueueClient(
            mutableListOf(
                redirect(finalUrl),
                response(200, TaobaoTmallResponseFixtures.basicHtml)
            )
        )

        val result = LinkRedirectResolver(client).resolve("https://e.tb.cn/h.test")

        assertTrue(result is LinkRedirectResult.Resolved)
        result as LinkRedirectResult.Resolved
        assertEquals(finalUrl, result.finalUrl)
        assertEquals(1, result.redirectCount)
        assertEquals(2, client.requestedUrls.size)
    }

    private class QueueClient(
        private val responses: MutableList<EcommerceHttpResponse>
    ) : EcommercePageClient {
        val requestedUrls = mutableListOf<String>()

        override suspend fun execute(url: String): EcommerceHttpResponse {
            requestedUrls += url
            return responses.removeAt(0).copy(requestUrl = url)
        }
    }
}

internal object TaobaoTmallResponseFixtures {
    const val basicHtml =
        "<html><head><meta property=\"og:title\" content=\"测试商品\"></head></html>"
}

internal fun redirect(location: String) = EcommerceHttpResponse(
    requestUrl = "",
    statusCode = 302,
    headers = mapOf("Location" to listOf(location)),
    body = "",
    contentType = "text/html"
)

internal fun response(statusCode: Int, body: String) = EcommerceHttpResponse(
    requestUrl = "",
    statusCode = statusCode,
    headers = emptyMap(),
    body = body,
    contentType = "text/html; charset=utf-8"
)
