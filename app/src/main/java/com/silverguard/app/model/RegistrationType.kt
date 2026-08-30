package com.silverguard.app.model

enum class RegistrationType(val displayName: String) {
    MEDICAL_DEVICE("医疗器械注册证 / 备案凭证"),
    DRUG_APPROVAL("药品批准文号"),
    HEALTH_FOOD("保健食品注册 / 备案编号"),
    COSMETIC("化妆品注册 / 备案编号"),
    UNKNOWN("暂时无法判断编号类型")
}

data class RegistrationClassification(
    val rawNumber: String,
    val normalizedNumber: String,
    val type: RegistrationType,
    val confidence: Float,
    val reason: String
)
