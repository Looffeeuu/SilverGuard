package com.silverguard.app.engine

import com.silverguard.app.model.ProductInfo
import com.silverguard.app.model.RegistrationType
import com.silverguard.app.model.VerificationField
import com.silverguard.app.model.VerificationReadiness

object VerificationReadinessEvaluator {

    fun evaluate(
        productInfo: ProductInfo,
        registrationType: RegistrationType
    ): VerificationReadiness {
        val missing = buildList {
            if (productInfo.name.isNullOrBlank()) add(VerificationField.PRODUCT_NAME)
            if (productInfo.manufacturer.isNullOrBlank()) add(VerificationField.MANUFACTURER)
            if (productInfo.registrationNumber.isNullOrBlank()) add(VerificationField.REGISTRATION_NUMBER)
            if (
                registrationType == RegistrationType.MEDICAL_DEVICE &&
                productInfo.model.isNullOrBlank() &&
                productInfo.specification.isNullOrBlank()
            ) {
                add(VerificationField.MODEL_OR_SPECIFICATION)
            }
        }

        val hasRegistrationNumber = !productInfo.registrationNumber.isNullOrBlank()
        val action = when {
            hasRegistrationNumber ->
                "已识别到可查询的编号。请前往对应官方平台，逐项核对产品名称、企业、型号规格和登记状态。"
            registrationType == RegistrationType.MEDICAL_DEVICE ->
                "请补拍包装背面或侧面标签，重点寻找医疗器械注册证编号或备案编号。"
            else ->
                "请补拍包装背面或侧面标签，寻找完整商品名称、生产企业和注册 / 备案信息。普通商品可能没有此类编号，缺少编号本身不代表商品有问题。"
        }

        return VerificationReadiness(
            ready = hasRegistrationNumber,
            missingFields = missing,
            suggestedAction = action
        )
    }
}
