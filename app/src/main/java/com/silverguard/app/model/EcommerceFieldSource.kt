package com.silverguard.app.model

enum class EcommerceFieldSource(val displayName: String) {
    URL("商品链接"),
    HTML_TITLE("商品页面标题"),
    META_TAG("商品页面公开信息"),
    OPEN_GRAPH("商品页面公开信息"),
    JSON_LD("商品页面结构化信息"),
    PUBLIC_HTML("商品页面公开内容"),
    OCR("包装识别"),
    UNKNOWN("来源尚未确定")
}
