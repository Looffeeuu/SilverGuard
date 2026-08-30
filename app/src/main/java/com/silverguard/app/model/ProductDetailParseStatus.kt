package com.silverguard.app.model

enum class ProductDetailParseStatus(val displayName: String) {
    NOT_STARTED("尚未开始读取"),
    LINK_READY("商品链接已准备"),
    FETCHING("正在读取商品信息"),
    SUCCESS("已读取商品信息"),
    PARTIAL("已读取部分商品信息"),
    LOGIN_REQUIRED("商品页面需要登录，暂时无法自动读取"),
    ACCESS_RESTRICTED("平台限制了自动读取"),
    CAPTCHA_REQUIRED("商品页面需要安全验证"),
    NETWORK_ERROR("网络连接失败"),
    UNSUPPORTED_PAGE("当前页面暂不支持自动读取"),
    PARSE_FAILED("没有从页面中读取到可靠商品信息")
}
