package com.silverguard.app.parser

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.silverguard.app.engine.EcommerceProductSanitizer
import com.silverguard.app.model.EcommerceFieldSource
import com.silverguard.app.model.EcommerceFieldType
import com.silverguard.app.model.EcommercePlatform
import com.silverguard.app.model.EcommerceProduct
import com.silverguard.app.model.EcommerceProductField
import com.silverguard.app.model.ProductDetailParseStatus
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class TaobaoTmallHtmlParser {

    private data class Candidate(
        val value: String,
        val source: EcommerceFieldSource,
        val confidence: Float
    )

    fun detectRestriction(html: String): ProductDetailParseStatus? {
        val sample = html.take(500_000)
        return when {
            CAPTCHA_PATTERN.containsMatchIn(sample) -> ProductDetailParseStatus.CAPTCHA_REQUIRED
            LOGIN_PATTERN.containsMatchIn(sample) -> ProductDetailParseStatus.LOGIN_REQUIRED
            RESTRICTED_PATTERN.containsMatchIn(sample) -> ProductDetailParseStatus.ACCESS_RESTRICTED
            else -> null
        }
    }

    fun parse(
        html: String,
        platform: EcommercePlatform,
        sourceUrl: String,
        productId: String?,
        fetchedAt: Long
    ): EcommerceProduct {
        val restriction = detectRestriction(html)
        if (restriction != null) {
            return emptyProduct(
                platform = platform,
                productId = productId,
                sourceUrl = sourceUrl,
                status = restriction,
                fetchedAt = fetchedAt,
                message = restriction.displayName
            )
        }

        val document = Jsoup.parse(html, sourceUrl)
        val jsonProducts = jsonLdProducts(document)

        val title = firstCandidate(
            jsonProducts.firstValue("name", EcommerceFieldSource.JSON_LD, 0.97f),
            metaCandidate(document, "meta[property=og:title]", EcommerceFieldSource.OPEN_GRAPH, 0.92f),
            metaCandidate(document, "meta[name=title]", EcommerceFieldSource.META_TAG, 0.82f),
            publicCandidate(document, "[itemprop=name]", 0.76f),
            document.title().takeIf { it.isNotBlank() }?.let {
                Candidate(it, EcommerceFieldSource.HTML_TITLE, 0.70f)
            }
        )?.let { it.copy(value = EcommerceProductSanitizer.cleanTitle(it.value).orEmpty()) }
            ?.takeIf { it.value.isNotBlank() }

        val price = firstCandidate(
            jsonProducts.firstOfferValue("price", EcommerceFieldSource.JSON_LD, 0.96f),
            jsonProducts.firstOfferValue("lowPrice", EcommerceFieldSource.JSON_LD, 0.92f),
            metaCandidate(document, "meta[property=product:price:amount]", EcommerceFieldSource.OPEN_GRAPH, 0.90f),
            metaCandidate(document, "meta[itemprop=price]", EcommerceFieldSource.META_TAG, 0.84f),
            publicCandidate(document, "[itemprop=price]", 0.72f)
        )?.let { candidate ->
            EcommerceProductSanitizer.cleanPrice(candidate.value)?.let { candidate.copy(value = it) }
        }

        val originalPrice = firstCandidate(
            metaCandidate(document, "meta[property=product:original_price]", EcommerceFieldSource.META_TAG, 0.80f),
            metaCandidate(document, "meta[name=originalPrice]", EcommerceFieldSource.META_TAG, 0.75f)
        )?.let { candidate ->
            EcommerceProductSanitizer.cleanPrice(candidate.value)?.let { candidate.copy(value = it) }
        }

        val brand = jsonProducts.firstNestedName(
            key = "brand",
            source = EcommerceFieldSource.JSON_LD,
            confidence = 0.92f
        )?.cleanTextCandidate() ?: publicCandidate(document, "[itemprop=brand]", 0.72f)
            ?.cleanTextCandidate()

        val seller = firstCandidate(
            jsonProducts.firstSellerName(EcommerceFieldSource.JSON_LD, 0.90f),
            metaCandidate(document, "meta[name=seller]", EcommerceFieldSource.META_TAG, 0.75f)
        )?.cleanTextCandidate()

        val shop = firstCandidate(
            metaCandidate(document, "meta[name=shop_name]", EcommerceFieldSource.META_TAG, 0.78f),
            metaCandidate(document, "meta[name=shopName]", EcommerceFieldSource.META_TAG, 0.78f)
        )?.cleanTextCandidate()

        val model = jsonProducts.firstValue(
            "model",
            EcommerceFieldSource.JSON_LD,
            0.90f
        )?.cleanTextCandidate()

        val specification = jsonProducts.firstValue(
            "size",
            EcommerceFieldSource.JSON_LD,
            0.82f
        )?.cleanTextCandidate()

        val description = firstCandidate(
            jsonProducts.firstValue("description", EcommerceFieldSource.JSON_LD, 0.88f),
            metaCandidate(document, "meta[property=og:description]", EcommerceFieldSource.OPEN_GRAPH, 0.82f),
            metaCandidate(document, "meta[name=description]", EcommerceFieldSource.META_TAG, 0.76f),
            publicCandidate(document, "[itemprop=description]", 0.68f)
        )?.let { candidate ->
            EcommerceProductSanitizer.cleanText(candidate.value)?.let { candidate.copy(value = it) }
        }

        val imageCandidates = buildList {
            addAll(jsonProducts.imageCandidates())
            metaCandidate(
                document,
                "meta[property=og:image]",
                EcommerceFieldSource.OPEN_GRAPH,
                0.90f
            )?.let(::add)
        }.mapNotNull { candidate ->
            EcommerceProductSanitizer.cleanImageUrl(candidate.value)
                ?.let { candidate.copy(value = it) }
        }.distinctBy { it.value }.take(3)

        val fields = buildList {
            title?.let { add(it.toField(EcommerceFieldType.TITLE)) }
            price?.let { add(it.toField(EcommerceFieldType.PRICE)) }
            originalPrice?.let { add(it.toField(EcommerceFieldType.ORIGINAL_PRICE)) }
            shop?.let { add(it.toField(EcommerceFieldType.SHOP_NAME)) }
            seller?.let { add(it.toField(EcommerceFieldType.SELLER_NAME)) }
            brand?.let { add(it.toField(EcommerceFieldType.BRAND)) }
            model?.let { add(it.toField(EcommerceFieldType.MODEL)) }
            specification?.let { add(it.toField(EcommerceFieldType.SPECIFICATION)) }
            description?.let { add(it.toField(EcommerceFieldType.DESCRIPTION)) }
            imageCandidates.forEach { add(it.toField(EcommerceFieldType.IMAGE)) }
        }

        val status = when {
            title != null && (price != null || seller != null || shop != null ||
                brand != null || imageCandidates.isNotEmpty()) -> ProductDetailParseStatus.SUCCESS
            fields.isNotEmpty() -> ProductDetailParseStatus.PARTIAL
            else -> ProductDetailParseStatus.PARSE_FAILED
        }
        val message = when (status) {
            ProductDetailParseStatus.SUCCESS -> "已读取商品基础信息，页面价格可能随地区、规格和优惠变化。"
            ProductDetailParseStatus.PARTIAL -> "已读取部分商品信息，完整详情建议补充截图。"
            else -> "页面可以访问，但没有读取到可靠的公开商品字段。"
        }

        return EcommerceProduct(
            platform = platform,
            productId = productId,
            sourceUrl = sourceUrl,
            canonicalUrl = canonicalUrl(document),
            title = title?.value,
            price = price?.value,
            originalPrice = originalPrice?.value,
            shopName = shop?.value,
            brand = brand?.value,
            sellerName = seller?.value,
            model = model?.value,
            specification = specification?.value,
            description = description?.value,
            images = imageCandidates.map { it.value },
            parseStatus = status,
            fields = fields,
            fetchedAt = fetchedAt,
            message = message
        )
    }

    private fun emptyProduct(
        platform: EcommercePlatform,
        productId: String?,
        sourceUrl: String,
        status: ProductDetailParseStatus,
        fetchedAt: Long?,
        message: String
    ) = EcommerceProduct(
        platform = platform,
        productId = productId,
        sourceUrl = sourceUrl,
        parseStatus = status,
        fetchedAt = fetchedAt,
        message = message
    )

    private fun canonicalUrl(document: Document): String? = document
        .selectFirst("link[rel=canonical]")
        ?.attr("abs:href")
        ?.takeIf { it.isNotBlank() }

    private fun metaCandidate(
        document: Document,
        selector: String,
        source: EcommerceFieldSource,
        confidence: Float
    ): Candidate? = document.selectFirst(selector)
        ?.attr("content")
        ?.takeIf { it.isNotBlank() }
        ?.let { Candidate(it, source, confidence) }

    private fun publicCandidate(
        document: Document,
        selector: String,
        confidence: Float
    ): Candidate? = document.selectFirst(selector)?.let { element ->
        val value = element.attr("content").ifBlank { element.text() }
        value.takeIf { it.isNotBlank() }
            ?.let { Candidate(it, EcommerceFieldSource.PUBLIC_HTML, confidence) }
    }

    private fun jsonLdProducts(document: Document): List<JsonObject> = buildList {
        document.select("script[type=application/ld+json]").forEach { script ->
            try {
                collectProductObjects(JsonParser.parseString(script.data()), this, depth = 0)
            } catch (_: Exception) {
                // One malformed JSON-LD block must not prevent parsing other public fields.
            }
        }
    }

    private fun collectProductObjects(
        element: JsonElement,
        destination: MutableList<JsonObject>,
        depth: Int
    ) {
        if (depth > 20 || element.isJsonNull) return
        when {
            element.isJsonArray -> element.asJsonArray.forEach {
                collectProductObjects(it, destination, depth + 1)
            }
            element.isJsonObject -> {
                val objectValue = element.asJsonObject
                if (objectValue["@type"].isProductType()) destination += objectValue
                objectValue.entrySet().forEach { (_, value) ->
                    collectProductObjects(value, destination, depth + 1)
                }
            }
        }
    }

    private fun JsonElement?.isProductType(): Boolean = when {
        this == null || isJsonNull -> false
        isJsonPrimitive -> asString.equals("Product", ignoreCase = true)
        isJsonArray -> asJsonArray.any { it.isProductType() }
        else -> false
    }

    private fun List<JsonObject>.firstValue(
        key: String,
        source: EcommerceFieldSource,
        confidence: Float
    ): Candidate? = firstNotNullOfOrNull { product ->
        product[key].asCleanString()?.let { Candidate(it, source, confidence) }
    }

    private fun List<JsonObject>.firstOfferValue(
        key: String,
        source: EcommerceFieldSource,
        confidence: Float
    ): Candidate? = firstNotNullOfOrNull { product ->
        product["offers"].findFirstObjectValue(key)
            ?.let { Candidate(it, source, confidence) }
    }

    private fun List<JsonObject>.firstNestedName(
        key: String,
        source: EcommerceFieldSource,
        confidence: Float
    ): Candidate? = firstNotNullOfOrNull { product ->
        product[key].nestedName()?.let { Candidate(it, source, confidence) }
    }

    private fun List<JsonObject>.firstSellerName(
        source: EcommerceFieldSource,
        confidence: Float
    ): Candidate? = firstNotNullOfOrNull { product ->
        val value = product["seller"].nestedName()
            ?: product["offers"].findFirstNestedName("seller")
        value?.let { Candidate(it, source, confidence) }
    }

    private fun List<JsonObject>.imageCandidates(): List<Candidate> = buildList {
        this@imageCandidates.forEach { product ->
            product["image"].imageStrings().forEach { value ->
                add(Candidate(value, EcommerceFieldSource.JSON_LD, 0.94f))
            }
        }
    }

    private fun JsonElement?.findFirstObjectValue(key: String): String? = when {
        this == null || isJsonNull -> null
        isJsonObject -> asJsonObject[key].asCleanString()
        isJsonArray -> asJsonArray.firstNotNullOfOrNull { it.findFirstObjectValue(key) }
        else -> null
    }

    private fun JsonElement?.findFirstNestedName(key: String): String? = when {
        this == null || isJsonNull -> null
        isJsonObject -> asJsonObject[key].nestedName()
        isJsonArray -> asJsonArray.firstNotNullOfOrNull { it.findFirstNestedName(key) }
        else -> null
    }

    private fun JsonElement?.nestedName(): String? = when {
        this == null || isJsonNull -> null
        isJsonPrimitive -> asCleanString()
        isJsonObject -> asJsonObject["name"].asCleanString()
        isJsonArray -> asJsonArray.firstNotNullOfOrNull { it.nestedName() }
        else -> null
    }

    private fun JsonElement?.imageStrings(): List<String> = when {
        this == null || isJsonNull -> emptyList()
        isJsonPrimitive -> listOfNotNull(asCleanString())
        isJsonObject -> listOfNotNull(
            asJsonObject["url"].asCleanString(),
            asJsonObject["contentUrl"].asCleanString()
        )
        isJsonArray -> asJsonArray.flatMap { it.imageStrings() }
        else -> emptyList()
    }

    private fun JsonElement?.asCleanString(): String? = try {
        this?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    private fun Candidate.cleanTextCandidate(): Candidate? =
        EcommerceProductSanitizer.cleanText(value)?.let { copy(value = it) }

    private fun Candidate.toField(type: EcommerceFieldType) = EcommerceProductField(
        type = type,
        value = value,
        source = source,
        confidence = confidence
    )

    private fun firstCandidate(vararg candidates: Candidate?): Candidate? =
        candidates.firstOrNull { it != null }

    private companion object {
        val LOGIN_PATTERN = Regex(
            "亲[，,]?请登录|请先登录|会员登录|账号登录|login\\.taobao\\.com|login\\.tmall\\.com",
            RegexOption.IGNORE_CASE
        )
        val CAPTCHA_PATTERN = Regex(
            "验证码|滑动验证|安全验证|captcha|nc_1_n1z|请完成验证",
            RegexOption.IGNORE_CASE
        )
        val RESTRICTED_PATTERN = Regex(
            "访问受限|访问过于频繁|请求被拒绝|被挤爆|系统繁忙|Access Denied",
            RegexOption.IGNORE_CASE
        )
    }
}
