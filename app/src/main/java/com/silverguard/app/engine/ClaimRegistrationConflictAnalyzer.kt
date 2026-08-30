package com.silverguard.app.engine

import com.silverguard.app.model.ClaimRegistrationConflict
import com.silverguard.app.model.ConflictPriority
import com.silverguard.app.model.RegistrationType

object ClaimRegistrationConflictAnalyzer {

    private val treatmentClaims = Regex(
        "治疗|治愈|根治|降血糖|治(?:疗)?高血压|不用吃药|无需吃药|替代药物|消炎|治鼻炎|改善鼻炎|修复疾病|抗癌"
    )
    private val replacementClaims = Regex("不用吃药|无需吃药|替代药物|停药|不用看医生")

    fun analyze(
        text: String,
        registrationType: RegistrationType
    ): List<ClaimRegistrationConflict> {
        val matchedClaims = treatmentClaims.findAll(text)
            .map { it.value }
            .distinct()
            .toList()
        if (matchedClaims.isEmpty()) return emptyList()

        val shouldWarn = when (registrationType) {
            RegistrationType.DRUG_APPROVAL -> false
            RegistrationType.MEDICAL_DEVICE -> replacementClaims.containsMatchIn(text)
            RegistrationType.HEALTH_FOOD,
            RegistrationType.COSMETIC,
            RegistrationType.UNKNOWN -> true
        }
        if (!shouldWarn) return emptyList()

        val typeExplanation = when (registrationType) {
            RegistrationType.HEALTH_FOOD -> "当前编号更像保健食品注册 / 备案信息，保健功能不等同于疾病治疗用途。"
            RegistrationType.COSMETIC -> "当前编号更像化妆品注册 / 备案信息，不能仅凭广告视为具有疾病治疗用途。"
            RegistrationType.MEDICAL_DEVICE -> "即使属于医疗器械，也需要核对登记的适用范围，不能仅凭宣传替代药物或正规治疗。"
            RegistrationType.UNKNOWN -> "目前尚未识别到可确认药品身份的批准文号，需要先核对商品登记类型。"
            RegistrationType.DRUG_APPROVAL -> ""
        }

        return listOf(
            ClaimRegistrationConflict(
                priority = ConflictPriority.HIGH,
                matchedClaims = matchedClaims,
                title = "宣传内容与商品登记类型存在明显冲突风险",
                explanation = "$typeExplanation 宣传内容可能超出商品本身允许宣传的范围，需要进一步核验。"
            )
        )
    }
}
