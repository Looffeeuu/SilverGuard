package com.silverguard.app.network

import com.silverguard.app.engine.EcommerceLinkParser
import com.silverguard.app.model.EcommerceLinkInfo
import com.silverguard.app.model.EcommercePlatform
import com.silverguard.app.model.EcommerceProduct
import com.silverguard.app.model.ProductDetailParseStatus
import com.silverguard.app.parser.TaobaoTmallHtmlParser
import com.silverguard.app.parser.TaobaoStaticProductLinkExtractor
import kotlinx.coroutines.CancellationException

class TaobaoTmallProductResolver(
    client: EcommercePageClient = EcommerceHttpClient(),
    private val htmlParser: TaobaoTmallHtmlParser = TaobaoTmallHtmlParser(),
    private val clock: () -> Long = System::currentTimeMillis,
    maxRedirects: Int = 5
) {
    private val redirectResolver = LinkRedirectResolver(client, maxRedirects)

    fun canResolve(info: EcommerceLinkInfo): Boolean =
        info.platform == EcommercePlatform.TAOBAO || info.platform == EcommercePlatform.TMALL

    suspend fun resolve(info: EcommerceLinkInfo): EcommerceProduct = try {
        resolveSafely(info)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        failure(
            info = info,
            sourceUrl = info.normalizedUrl ?: info.extractedUrl.orEmpty(),
            status = ProductDetailParseStatus.NETWORK_ERROR,
            message = "商品信息读取没有完成：${error.message ?: "未知错误"}"
        )
    }

    private suspend fun resolveSafely(info: EcommerceLinkInfo): EcommerceProduct {
        val startUrl = info.normalizedUrl ?: info.extractedUrl
        if (!canResolve(info) || startUrl.isNullOrBlank()) {
            return failure(
                info,
                startUrl.orEmpty(),
                ProductDetailParseStatus.UNSUPPORTED_PAGE,
                "当前版本只自动读取淘宝和天猫商品页面。"
            )
        }
        if (!AllowedEcommerceHosts.isAllowedHttpsUrl(startUrl)) {
            return failure(
                info,
                startUrl,
                ProductDetailParseStatus.ACCESS_RESTRICTED,
                "链接不是允许自动访问的淘宝或天猫 HTTPS 地址。"
            )
        }

        return when (val resolution = redirectResolver.resolve(startUrl)) {
            is LinkRedirectResult.LoginRequired -> failure(
                info,
                startUrl,
                ProductDetailParseStatus.LOGIN_REQUIRED,
                resolution.message
            )
            is LinkRedirectResult.CaptchaRequired -> failure(
                info,
                startUrl,
                ProductDetailParseStatus.CAPTCHA_REQUIRED,
                resolution.message
            )
            is LinkRedirectResult.Rejected -> failure(
                info,
                startUrl,
                ProductDetailParseStatus.ACCESS_RESTRICTED,
                resolution.message
            )
            is LinkRedirectResult.NetworkError -> failure(
                info,
                startUrl,
                ProductDetailParseStatus.NETWORK_ERROR,
                "网络连接失败：${resolution.message}"
            )
            is LinkRedirectResult.Resolved -> parseResponse(info, startUrl, resolution)
        }
    }

    private suspend fun parseResponse(
        info: EcommerceLinkInfo,
        startUrl: String,
        resolution: LinkRedirectResult.Resolved
    ): EcommerceProduct {
        val response = resolution.response
        val fetchedAt = clock()
        val statusFailure = when (response.statusCode) {
            401 -> ProductDetailParseStatus.LOGIN_REQUIRED to "商品页面要求登录后查看。"
            403, 429 -> ProductDetailParseStatus.ACCESS_RESTRICTED to "平台限制了当前自动读取请求。"
            408 -> ProductDetailParseStatus.NETWORK_ERROR to "读取商品页面超时。"
            in 500..599 -> ProductDetailParseStatus.NETWORK_ERROR to "商品页面暂时无法访问。"
            else -> null
        }
        if (statusFailure != null) {
            return failure(
                info,
                startUrl,
                statusFailure.first,
                statusFailure.second,
                canonicalUrl = resolution.finalUrl,
                fetchedAt = fetchedAt
            )
        }
        if (response.statusCode !in 200..299) {
            return failure(
                info,
                startUrl,
                ProductDetailParseStatus.UNSUPPORTED_PAGE,
                "商品页面返回了暂不支持的状态（${response.statusCode}）。",
                canonicalUrl = resolution.finalUrl,
                fetchedAt = fetchedAt
            )
        }

        if (!AllowedEcommerceHosts.isAllowedProductUrl(resolution.finalUrl)) {
            val staticProductUrl = TaobaoStaticProductLinkExtractor
                .extractCleanProductUrl(response.body)
            if (staticProductUrl != null) {
                return when (val productPage = redirectResolver.resolve(staticProductUrl)) {
                    is LinkRedirectResult.Resolved ->
                        parseResponse(info, startUrl, productPage)
                    is LinkRedirectResult.LoginRequired -> failure(
                        info,
                        startUrl,
                        ProductDetailParseStatus.LOGIN_REQUIRED,
                        productPage.message
                    )
                    is LinkRedirectResult.CaptchaRequired -> failure(
                        info,
                        startUrl,
                        ProductDetailParseStatus.CAPTCHA_REQUIRED,
                        productPage.message
                    )
                    is LinkRedirectResult.Rejected -> failure(
                        info,
                        startUrl,
                        ProductDetailParseStatus.ACCESS_RESTRICTED,
                        productPage.message
                    )
                    is LinkRedirectResult.NetworkError -> failure(
                        info,
                        startUrl,
                        ProductDetailParseStatus.NETWORK_ERROR,
                        "网络连接失败：${productPage.message}"
                    )
                }
            }
        }

        val looksLikeHtml = response.contentType
            ?.contains("html", ignoreCase = true) == true ||
            response.body.trimStart().startsWith("<")
        if (!looksLikeHtml) {
            return failure(
                info,
                startUrl,
                ProductDetailParseStatus.UNSUPPORTED_PAGE,
                "页面返回的不是可读取的公开商品网页。",
                canonicalUrl = resolution.finalUrl,
                fetchedAt = fetchedAt
            )
        }

        val finalPlatform = AllowedEcommerceHosts.platformForUrl(resolution.finalUrl)
            .takeIf { it == EcommercePlatform.TAOBAO || it == EcommercePlatform.TMALL }
            ?: info.platform
        val finalLinkInfo = EcommerceLinkParser.parse(resolution.finalUrl)
        val finalProductId = finalLinkInfo.productId ?: info.productId
        val parsed = htmlParser.parse(
            html = response.body,
            platform = finalPlatform,
            sourceUrl = resolution.finalUrl,
            productId = finalProductId,
            fetchedAt = fetchedAt
        )
        val safeCanonical = parsed.canonicalUrl
            ?.takeIf(AllowedEcommerceHosts::isAllowedProductUrl)
            ?: resolution.finalUrl.takeIf(AllowedEcommerceHosts::isAllowedProductUrl)

        return parsed.copy(
            sourceUrl = startUrl,
            canonicalUrl = safeCanonical,
            productId = finalProductId
        )
    }

    private fun failure(
        info: EcommerceLinkInfo,
        sourceUrl: String,
        status: ProductDetailParseStatus,
        message: String,
        canonicalUrl: String? = null,
        fetchedAt: Long? = null
    ) = EcommerceProduct(
        platform = info.platform,
        productId = info.productId,
        sourceUrl = sourceUrl,
        canonicalUrl = canonicalUrl,
        parseStatus = status,
        fetchedAt = fetchedAt,
        message = message
    )
}
