package com.silverguard.app.engine

object AiTextRedactor {
    private const val MAX_TEXT_LENGTH = 6_000

    private val emailPattern = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val mobilePattern = Regex("(?<!\\d)1[3-9]\\d{9}(?!\\d)")
    private val idCardPattern = Regex("(?<!\\d)\\d{17}[0-9Xx](?!\\d)")
    private val contactPattern = Regex(
        "(?i)(微信|weixin|wx|qq|联系方式|联系人)(\\s*[：:]?\\s*)[A-Za-z0-9_-]{5,24}"
    )

    fun redact(text: String): String {
        val withoutUrls = UrlAnalysisSanitizer.stripUrlsForAnalysis(text)
        return redactValue(withoutUrls).take(MAX_TEXT_LENGTH).trim()
    }

    fun redactValue(value: String?): String = value
        .orEmpty()
        .replace(emailPattern, "[已隐藏邮箱]")
        .replace(mobilePattern, "[已隐藏手机号]")
        .replace(idCardPattern, "[已隐藏证件号]")
        .replace(contactPattern) { match -> "${match.groupValues[1]}：[已隐藏联系方式]" }
        .trim()
}
