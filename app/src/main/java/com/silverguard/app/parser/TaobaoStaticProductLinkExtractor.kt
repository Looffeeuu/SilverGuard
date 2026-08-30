package com.silverguard.app.parser

import com.silverguard.app.engine.EcommerceLinkParser
import com.silverguard.app.model.EcommercePlatform
import org.jsoup.parser.Parser

object TaobaoStaticProductLinkExtractor {

    private val directProductUrl = Regex(
        """https://(?:item\.taobao\.com|detail\.tmall\.com)/[^\s\"'<>\\]+""",
        RegexOption.IGNORE_CASE
    )

    fun extractCleanProductUrl(html: String): String? {
        val decoded = try {
            Parser.unescapeEntities(html.replace("\\/", "/"), false)
        } catch (_: Exception) {
            html.replace("\\/", "/")
        }
        return directProductUrl.findAll(decoded)
            .map { it.value.trimEnd(',', ';', ')', ']', '}') }
            .map(EcommerceLinkParser::parse)
            .firstNotNullOfOrNull { info ->
                val id = info.productId ?: return@firstNotNullOfOrNull null
                when (info.platform) {
                    EcommercePlatform.TAOBAO ->
                        "https://item.taobao.com/item.htm?id=$id"
                    EcommercePlatform.TMALL ->
                        "https://detail.tmall.com/item.htm?id=$id"
                    else -> null
                }
            }
    }
}
