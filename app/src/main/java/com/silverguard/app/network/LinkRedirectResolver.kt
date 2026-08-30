package com.silverguard.app.network

sealed interface LinkRedirectResult {
    data class Resolved(
        val finalUrl: String,
        val response: EcommerceHttpResponse,
        val redirectCount: Int
    ) : LinkRedirectResult

    data class LoginRequired(val message: String) : LinkRedirectResult
    data class CaptchaRequired(val message: String) : LinkRedirectResult
    data class Rejected(val message: String) : LinkRedirectResult
    data class NetworkError(val message: String) : LinkRedirectResult
}

class LinkRedirectResolver(
    private val client: EcommercePageClient,
    private val maxRedirects: Int = 5
) {

    suspend fun resolve(startUrl: String): LinkRedirectResult {
        if (!AllowedEcommerceHosts.isAllowedHttpsUrl(startUrl)) {
            return LinkRedirectResult.Rejected("链接不是允许访问的淘宝或天猫 HTTPS 地址。")
        }

        var currentUrl = startUrl
        var redirectCount = 0

        while (true) {
            val response = try {
                client.execute(currentUrl)
            } catch (_: SecurityException) {
                return LinkRedirectResult.Rejected("跳转地址不在安全白名单中，已停止自动读取。")
            } catch (error: Exception) {
                return LinkRedirectResult.NetworkError(
                    error.message?.takeIf { it.isNotBlank() } ?: "网络连接失败。"
                )
            }

            if (response.statusCode !in REDIRECT_CODES) {
                return LinkRedirectResult.Resolved(currentUrl, response, redirectCount)
            }

            val location = response.header("Location")
                ?: return LinkRedirectResult.Rejected("短链接没有提供可用的跳转地址。")
            val nextUrl = AllowedEcommerceHosts.resolveRedirect(currentUrl, location)
                ?: return LinkRedirectResult.Rejected("短链接返回了无法识别的跳转地址。")

            if (AllowedEcommerceHosts.isLikelyCaptchaUrl(nextUrl)) {
                return LinkRedirectResult.CaptchaRequired("页面要求完成安全验证。")
            }
            if (AllowedEcommerceHosts.isKnownLoginUrl(nextUrl)) {
                return LinkRedirectResult.LoginRequired("商品页面要求登录后查看。")
            }
            if (!AllowedEcommerceHosts.isAllowedHttpsUrl(nextUrl)) {
                return LinkRedirectResult.Rejected("短链接跳转到未允许的地址，已停止自动读取。")
            }

            redirectCount += 1
            if (redirectCount > maxRedirects) {
                return LinkRedirectResult.Rejected("短链接跳转次数过多，已停止自动读取。")
            }
            currentUrl = nextUrl
        }
    }

    private companion object {
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}
