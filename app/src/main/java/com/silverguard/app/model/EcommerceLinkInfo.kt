package com.silverguard.app.model

data class EcommerceLinkInfo(
    val rawText: String,
    val extractedUrl: String?,
    val normalizedUrl: String?,
    val platform: EcommercePlatform,
    val productId: String?,
    val status: EcommerceLinkStatus,
    val message: String,
    val isShortLink: Boolean = false
) {
    val isSupportedPlatform: Boolean
        get() = platform != EcommercePlatform.UNKNOWN &&
            status != EcommerceLinkStatus.UNSUPPORTED &&
            status != EcommerceLinkStatus.ERROR &&
            status != EcommerceLinkStatus.NO_LINK

    val shareableUrl: String?
        get() = normalizedUrl ?: extractedUrl
}
