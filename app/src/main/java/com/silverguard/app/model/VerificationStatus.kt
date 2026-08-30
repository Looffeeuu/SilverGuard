package com.silverguard.app.model

enum class VerificationStatus(val displayName: String) {
    NOT_READY("信息不足，暂时无法核验"),
    READY("信息已具备，等待核验"),
    MANUAL_REQUIRED("需要前往官方平台人工核验"),
    MANUAL_REVIEWED("已记录人工核对结果"),
    VERIFIED("已取得官方数据匹配结果"),
    NOT_FOUND("人工查询暂未找到记录"),
    ERROR("官方页面暂时无法查询")
}

enum class VerificationField(val displayName: String) {
    PRODUCT_NAME("商品名称"),
    MANUFACTURER("完整生产企业名称"),
    REGISTRATION_NUMBER("注册 / 备案号"),
    MODEL_OR_SPECIFICATION("型号或规格")
}

data class VerificationReadiness(
    val ready: Boolean,
    val missingFields: List<VerificationField>,
    val suggestedAction: String
)
