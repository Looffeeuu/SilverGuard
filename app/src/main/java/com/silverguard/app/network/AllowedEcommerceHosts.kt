package com.silverguard.app.network

import com.silverguard.app.model.EcommercePlatform
import java.net.URI

object AllowedEcommerceHosts {

    private val taobaoProductHosts = setOf(
        "item.taobao.com",
        "m.taobao.com"
    )

    private val tmallProductHosts = setOf(
        "detail.tmall.com",
        "m.tmall.com"
    )

    private val shortLinkHosts = setOf(
        "m.tb.cn",
        "e.tb.cn",
        "s.click.taobao.com"
    )

    private val knownLoginHosts = setOf(
        "login.taobao.com",
        "login.m.taobao.com",
        "login.tmall.com"
    )

    val all: Set<String> = taobaoProductHosts + tmallProductHosts + shortLinkHosts

    fun isAllowedHttpsUrl(url: String): Boolean = parseHttpsUri(url)?.let { uri ->
        normalizedHost(uri) in all && uri.userInfo == null && (uri.port == -1 || uri.port == 443)
    } ?: false

    fun isAllowedProductUrl(url: String): Boolean = parseHttpsUri(url)?.let { uri ->
        normalizedHost(uri) in taobaoProductHosts + tmallProductHosts &&
            uri.userInfo == null && (uri.port == -1 || uri.port == 443)
    } ?: false

    fun isKnownLoginUrl(url: String): Boolean = parseHttpsUri(url)?.let { uri ->
        normalizedHost(uri) in knownLoginHosts ||
            uri.path.orEmpty().contains("login", ignoreCase = true)
    } ?: false

    fun isLikelyCaptchaUrl(url: String): Boolean = try {
        val uri = URI(url)
        val searchable = "${uri.host.orEmpty()}${uri.path.orEmpty()}${uri.query.orEmpty()}"
        Regex("captcha|verify|punish|security", RegexOption.IGNORE_CASE)
            .containsMatchIn(searchable)
    } catch (_: Exception) {
        false
    }

    fun platformForUrl(url: String): EcommercePlatform = parseHttpsUri(url)?.let { uri ->
        when (normalizedHost(uri)) {
            in taobaoProductHosts, in shortLinkHosts -> EcommercePlatform.TAOBAO
            in tmallProductHosts -> EcommercePlatform.TMALL
            else -> EcommercePlatform.UNKNOWN
        }
    } ?: EcommercePlatform.UNKNOWN

    fun resolveRedirect(currentUrl: String, location: String): String? = try {
        URI(currentUrl).resolve(location).toASCIIString()
    } catch (_: Exception) {
        null
    }

    private fun parseHttpsUri(url: String): URI? = try {
        URI(url).takeIf {
            it.scheme.equals("https", ignoreCase = true) && !it.host.isNullOrBlank()
        }
    } catch (_: Exception) {
        null
    }

    private fun normalizedHost(uri: URI): String = uri.host.orEmpty().lowercase().trimEnd('.')
}
