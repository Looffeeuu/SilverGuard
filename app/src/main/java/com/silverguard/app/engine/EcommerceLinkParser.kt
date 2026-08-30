package com.silverguard.app.engine

import com.silverguard.app.model.EcommerceLinkInfo
import com.silverguard.app.model.EcommerceLinkStatus
import com.silverguard.app.model.EcommercePlatform
import java.net.URI
import java.net.URLDecoder

object EcommerceLinkParser {

    private data class Candidate(
        val url: String,
        val uri: URI,
        val platform: EcommercePlatform,
        val isShortLink: Boolean,
        val productId: String?,
        val priority: Int
    )

    private val hostsByPlatform = mapOf(
        EcommercePlatform.TAOBAO to setOf(
            "item.taobao.com",
            "m.taobao.com",
            "m.tb.cn",
            "e.tb.cn",
            "s.click.taobao.com"
        ),
        EcommercePlatform.TMALL to setOf(
            "detail.tmall.com",
            "m.tmall.com"
        ),
        EcommercePlatform.PINDUODUO to setOf(
            "mobile.yangkeduo.com",
            "yangkeduo.com",
            "p.pinduoduo.com"
        ),
        EcommercePlatform.JD to setOf(
            "item.jd.com",
            "item.m.jd.com",
            "m.jd.com",
            "u.jd.com"
        ),
        EcommercePlatform.DOUYIN to setOf(
            "douyin.com",
            "www.douyin.com",
            "v.douyin.com",
            "haohuo.jinritemai.com"
        )
    )

    private val shortLinkHosts = setOf(
        "m.tb.cn",
        "e.tb.cn",
        "s.click.taobao.com",
        "p.pinduoduo.com",
        "u.jd.com",
        "v.douyin.com"
    )

    private val trackingParameters = setOf(
        "spm",
        "scm",
        "pvid",
        "utm_source",
        "utm_medium",
        "utm_campaign",
        "utm_term",
        "utm_content"
    )

    private val imagePathRegex = Regex("(?i)\\.(?:jpg|jpeg|png|gif|webp|svg)(?:$|/)")
    private val jdProductPathRegex = Regex("(?:^|/)(\\d{5,20})\\.html$", RegexOption.IGNORE_CASE)
    private val douyinProductPathRegex = Regex("(?:^|/)(?:product|goods)/(\\d{5,20})(?:/|$)")

    fun parse(text: String): EcommerceLinkInfo {
        val urls = UrlExtractor.extractAll(text)
        if (urls.isEmpty()) {
            return noLink(text)
        }

        return try {
            val candidates = urls.mapNotNull(::toCandidate)
            if (candidates.isEmpty()) {
                return EcommerceLinkInfo(
                    rawText = text,
                    extractedUrl = urls.first(),
                    normalizedUrl = null,
                    platform = EcommercePlatform.UNKNOWN,
                    productId = null,
                    status = EcommerceLinkStatus.ERROR,
                    message = "检测到链接，但链接格式不完整，请检查后重试。"
                )
            }

            val selected = candidates
                .filter { it.platform != EcommercePlatform.UNKNOWN }
                .maxByOrNull { it.priority }
                ?: candidates.first()

            if (selected.platform == EcommercePlatform.UNKNOWN) {
                return EcommerceLinkInfo(
                    rawText = text,
                    extractedUrl = selected.url,
                    normalizedUrl = null,
                    platform = EcommercePlatform.UNKNOWN,
                    productId = null,
                    status = EcommerceLinkStatus.UNSUPPORTED,
                    message = "检测到一个网页链接，但当前版本暂不支持这个平台。"
                )
            }

            val normalized = normalize(selected.url, selected.uri)
            val status = when {
                selected.isShortLink -> EcommerceLinkStatus.NEED_REDIRECT
                selected.productId != null -> EcommerceLinkStatus.PRODUCT_ID_EXTRACTED
                normalized != selected.url -> EcommerceLinkStatus.NORMALIZED
                else -> EcommerceLinkStatus.DETECTED
            }
            val message = when (status) {
                EcommerceLinkStatus.NEED_REDIRECT -> if (selected.platform.canReadPublicDetails) {
                    "这是${selected.platform.displayName}短链接，点击下方按钮后将尝试安全读取公开商品信息。"
                } else {
                    "这是${selected.platform.displayName}短链接，需要进一步解析后才能确认具体商品。"
                }
                EcommerceLinkStatus.PRODUCT_ID_EXTRACTED -> if (selected.platform.canReadPublicDetails) {
                    "商品链接已识别，点击下方按钮可尝试读取公开商品信息。"
                } else {
                    "商品链接已识别，当前版本尚未自动读取完整商品详情。"
                }
                else -> if (selected.platform.canReadPublicDetails) {
                    "已识别为${selected.platform.displayName}商品链接，可以尝试读取公开商品信息。"
                } else {
                    "已识别为${selected.platform.displayName}商品链接，当前版本尚未自动读取完整商品详情。"
                }
            }

            EcommerceLinkInfo(
                rawText = text,
                extractedUrl = selected.url,
                normalizedUrl = normalized,
                platform = selected.platform,
                productId = selected.productId,
                status = status,
                message = message,
                isShortLink = selected.isShortLink
            )
        } catch (_: Exception) {
            EcommerceLinkInfo(
                rawText = text,
                extractedUrl = urls.firstOrNull(),
                normalizedUrl = null,
                platform = EcommercePlatform.UNKNOWN,
                productId = null,
                status = EcommerceLinkStatus.ERROR,
                message = "链接解析没有完成，请检查链接后再试。"
            )
        }
    }

    private fun noLink(text: String) = EcommerceLinkInfo(
        rawText = text,
        extractedUrl = null,
        normalizedUrl = null,
        platform = EcommercePlatform.UNKNOWN,
        productId = null,
        status = EcommerceLinkStatus.NO_LINK,
        message = "没有检测到网页链接。"
    )

    private fun toCandidate(url: String): Candidate? {
        val uri = try {
            URI(url)
        } catch (_: Exception) {
            return null
        }
        if (!uri.scheme.equals("https", ignoreCase = true) &&
            !uri.scheme.equals("http", ignoreCase = true)
        ) {
            return null
        }
        val host = uri.host?.lowercase()?.trimEnd('.') ?: return null
        val platform = platformForHost(host)
        val isShortLink = host in shortLinkHosts
        val productId = if (platform == EcommercePlatform.UNKNOWN || isShortLink) {
            null
        } else {
            extractProductId(platform, uri)
        }
        val path = uri.path.orEmpty()
        val priority = when {
            productId != null -> 100
            isShortLink -> 80
            imagePathRegex.containsMatchIn(path) -> 10
            isLikelyProductPage(platform, path, uri.rawQuery) -> 60
            platform != EcommercePlatform.UNKNOWN -> 40
            else -> 0
        }
        return Candidate(url, uri, platform, isShortLink, productId, priority)
    }

    private fun platformForHost(host: String): EcommercePlatform = hostsByPlatform
        .entries
        .firstOrNull { host in it.value }
        ?.key
        ?: EcommercePlatform.UNKNOWN

    private fun extractProductId(platform: EcommercePlatform, uri: URI): String? = when (platform) {
        EcommercePlatform.TAOBAO,
        EcommercePlatform.TMALL -> queryParameter(uri, "id")
        EcommercePlatform.PINDUODUO -> queryParameter(uri, "goods_id")
        EcommercePlatform.JD -> jdProductPathRegex
            .find(uri.path.orEmpty())
            ?.groupValues
            ?.getOrNull(1)
        EcommercePlatform.DOUYIN -> {
            douyinProductPathRegex.find(uri.path.orEmpty())?.groupValues?.getOrNull(1)
                ?: if (uri.host.equals("haohuo.jinritemai.com", ignoreCase = true)) {
                    queryParameter(uri, "id") ?: queryParameter(uri, "product_id")
                } else {
                    null
                }
        }
        EcommercePlatform.UNKNOWN -> null
    }?.takeIf { it.matches(Regex("\\d{3,20}")) }

    private fun queryParameter(uri: URI, wantedName: String): String? = uri.rawQuery
        ?.split('&')
        ?.asSequence()
        ?.map { part -> part.substringBefore('=') to part.substringAfter('=', "") }
        ?.firstOrNull { (name, _) -> decode(name).equals(wantedName, ignoreCase = true) }
        ?.second
        ?.let(::decode)

    private fun normalize(url: String, uri: URI): String {
        val rawQuery = uri.rawQuery ?: return url
        val kept = rawQuery.split('&').filter { part ->
            val rawName = part.substringBefore('=')
            decode(rawName).lowercase() !in trackingParameters
        }
        if (kept.size == rawQuery.split('&').size) return url

        val queryStart = url.indexOf('?')
        if (queryStart < 0) return url
        val fragmentStart = url.indexOf('#', startIndex = queryStart)
        val base = url.substring(0, queryStart)
        val fragment = if (fragmentStart >= 0) url.substring(fragmentStart) else ""
        return if (kept.isEmpty()) {
            base + fragment
        } else {
            "$base?${kept.joinToString("&")}$fragment"
        }
    }

    private fun isLikelyProductPage(
        platform: EcommercePlatform,
        path: String,
        rawQuery: String?
    ): Boolean = when (platform) {
        EcommercePlatform.TAOBAO,
        EcommercePlatform.TMALL -> path.contains("item", ignoreCase = true) ||
            rawQuery.orEmpty().contains("id=", ignoreCase = true)
        EcommercePlatform.PINDUODUO -> path.contains("goods", ignoreCase = true)
        EcommercePlatform.JD -> path.contains("product", ignoreCase = true) ||
            path.endsWith(".html", ignoreCase = true)
        EcommercePlatform.DOUYIN -> path.contains("product", ignoreCase = true) ||
            path.contains("goods", ignoreCase = true)
        EcommercePlatform.UNKNOWN -> false
    }

    private fun decode(value: String): String = try {
        URLDecoder.decode(value, Charsets.UTF_8.name())
    } catch (_: Exception) {
        value
    }

    private val EcommercePlatform.canReadPublicDetails: Boolean
        get() = this == EcommercePlatform.TAOBAO || this == EcommercePlatform.TMALL
}
