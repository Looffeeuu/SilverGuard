package com.silverguard.app.engine

import java.net.URI
import org.jsoup.parser.Parser

object EcommerceProductSanitizer {

    private val fixedTitleSuffix = Regex(
        """\s*(?:[-_–—|]\s*)?(?:淘宝网|淘宝|天猫商城|天猫|Taobao(?:\.com)?|Tmall(?:\.com)?)\s*$""",
        RegexOption.IGNORE_CASE
    )

    fun cleanTitle(value: String?): String? {
        var cleaned = cleanText(value, maxLength = 200) ?: return null
        cleaned = cleaned.replace(fixedTitleSuffix, "").trim()
        return cleaned.takeIf {
            it.length >= 2 && !INVALID_WRAPPER_TITLE.matches(it)
        }
    }

    fun cleanText(value: String?, maxLength: Int = 1_000): String? {
        if (value.isNullOrBlank()) return null
        val decoded = try {
            Parser.unescapeEntities(value, false)
        } catch (_: Exception) {
            value
        }
        return decoded
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(maxLength)
            .takeIf { it.length >= 2 }
    }

    fun cleanPrice(value: String?): String? {
        val text = cleanText(value, maxLength = 80) ?: return null
        val match = Regex(
            """\d{1,8}(?:,\d{3})*(?:\.\d{1,2})?(?:\s*[-~—至]\s*\d{1,8}(?:,\d{3})*(?:\.\d{1,2})?)?"""
        ).find(text)?.value ?: return null
        return "¥${match.replace(",", "").replace(Regex("\\s+"), "")}"
    }

    fun cleanImageUrl(value: String?): String? {
        val candidate = cleanText(value, maxLength = 2_000) ?: return null
        return try {
            val uri = URI(candidate)
            candidate.takeIf {
                uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
            }
        } catch (_: Exception) {
            null
        }
    }

    private val INVALID_WRAPPER_TITLE = Regex(
        "(?:打开|进入|访问)?(?:淘宝|天猫)(?:网|商城|页面|客户端)?|页面跳转|打开",
        RegexOption.IGNORE_CASE
    )
}
