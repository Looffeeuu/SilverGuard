package com.silverguard.app.network

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.CookieJar

data class EcommerceHttpResponse(
    val requestUrl: String,
    val statusCode: Int,
    val headers: Map<String, List<String>>,
    val body: String,
    val contentType: String?
) {
    fun header(name: String): String? = headers.entries
        .firstOrNull { it.key.equals(name, ignoreCase = true) }
        ?.value
        ?.firstOrNull()
}

fun interface EcommercePageClient {
    suspend fun execute(url: String): EcommerceHttpResponse
}

class ResponseBodyTooLargeException : IOException("商品页面超过允许读取的大小")

class EcommerceHttpClient(
    private val client: OkHttpClient = defaultClient(),
    private val maxBodyBytes: Int = DEFAULT_MAX_BODY_BYTES
) : EcommercePageClient {

    override suspend fun execute(url: String): EcommerceHttpResponse = withContext(Dispatchers.IO) {
        if (!AllowedEcommerceHosts.isAllowedHttpsUrl(url)) {
            throw SecurityException("拒绝访问不在电商白名单中的地址")
        }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml;q=0.9")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body
            val body = if (response.code in 300..399) {
                ""
            } else {
                val bytes = responseBody.byteStream().use { stream ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8_192)
                    var total = 0
                    while (true) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > maxBodyBytes) throw ResponseBodyTooLargeException()
                        output.write(buffer, 0, read)
                    }
                    output.toByteArray()
                }
                val charset = responseBody.contentType()?.charset(StandardCharsets.UTF_8)
                    ?: StandardCharsets.UTF_8
                bytes.toString(charset)
            }

            EcommerceHttpResponse(
                requestUrl = response.request.url.toString(),
                statusCode = response.code,
                headers = response.headers.toMultimap(),
                body = body,
                contentType = responseBody.contentType()?.toString()
            )
        }
    }

    companion object {
        const val DEFAULT_MAX_BODY_BYTES = 3 * 1024 * 1024
        private const val USER_AGENT =
            "SilverGuard/0.3.4 (Android; public product information reader)"

        private fun defaultClient() = OkHttpClient.Builder()
            .cookieJar(CookieJar.NO_COOKIES)
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(7, TimeUnit.SECONDS)
            .readTimeout(9, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .build()
    }
}
