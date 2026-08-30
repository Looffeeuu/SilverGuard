package com.silverguard.app.engine

import com.silverguard.app.model.ProductInfo
import com.silverguard.app.model.RegistrationClassification
import com.silverguard.app.model.RegistrationType
import com.silverguard.app.model.VerificationChecklistItem
import com.silverguard.app.model.VerificationEvidenceField

object VerificationChecklistFactory {

    fun create(
        productInfo: ProductInfo,
        registration: RegistrationClassification
    ): List<VerificationChecklistItem> {
        val modelOrSpecification = listOfNotNull(
            productInfo.model?.takeIf { it.isNotBlank() },
            productInfo.specification?.takeIf { it.isNotBlank() }
        ).joinToString(" / ").ifBlank { null }

        return buildList {
            add(
                item(
                    field = VerificationEvidenceField.REGISTRATION_NUMBER,
                    expectedValue = registration.normalizedNumber.ifBlank {
                        productInfo.registrationNumber
                    },
                    guidance = "确认官方页面中的编号与包装完全一致。",
                    required = true
                )
            )
            add(
                item(
                    field = VerificationEvidenceField.PRODUCT_NAME,
                    expectedValue = productInfo.name,
                    guidance = "核对产品全名，不要只比较宣传标题。",
                    required = !productInfo.name.isNullOrBlank()
                )
            )
            add(
                item(
                    field = VerificationEvidenceField.REGISTRANT,
                    expectedValue = productInfo.manufacturer,
                    guidance = "核对注册人或备案人，店铺名称不能替代生产企业。",
                    required = !productInfo.manufacturer.isNullOrBlank()
                )
            )
            if (registration.type == RegistrationType.MEDICAL_DEVICE || modelOrSpecification != null) {
                add(
                    item(
                        field = VerificationEvidenceField.MODEL_OR_SPECIFICATION,
                        expectedValue = modelOrSpecification,
                        guidance = "同一名称可能对应多个型号，请核对包装上的具体型号规格。",
                        required = modelOrSpecification != null
                    )
                )
            }
            add(
                item(
                    field = VerificationEvidenceField.REGISTERED_SCOPE,
                    expectedValue = null,
                    guidance = scopeGuidance(registration.type),
                    required = true
                )
            )
            add(
                item(
                    field = VerificationEvidenceField.REGISTRATION_STATUS,
                    expectedValue = null,
                    guidance = "确认登记状态、有效期以及是否存在注销或变更提示。",
                    required = true
                )
            )
        }
    }

    private fun item(
        field: VerificationEvidenceField,
        expectedValue: String?,
        guidance: String,
        required: Boolean
    ) = VerificationChecklistItem(
        field = field,
        expectedValue = expectedValue?.trim()?.takeIf { it.isNotBlank() },
        guidance = guidance,
        required = required
    )

    private fun scopeGuidance(type: RegistrationType): String = when (type) {
        RegistrationType.MEDICAL_DEVICE ->
            "核对官方适用范围是否包含广告声称的疾病或用途。"
        RegistrationType.DRUG_APPROVAL ->
            "核对药品说明书中的适应症，不要只看商家宣传。"
        RegistrationType.HEALTH_FOOD ->
            "核对获准保健功能；保健功能不等于疾病治疗用途。"
        RegistrationType.COSMETIC ->
            "核对注册类别和用途；化妆品登记不代表具有疾病治疗作用。"
        RegistrationType.UNKNOWN ->
            "先确认商品登记类型，再判断宣传用途是否与官方信息一致。"
    }
}
