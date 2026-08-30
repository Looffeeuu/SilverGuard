package com.silverguard.app.engine

import com.silverguard.app.model.RegistrationClassification
import com.silverguard.app.model.RegistrationType

object RegistrationNumberClassifier {

    private data class ClassificationRule(
        val type: RegistrationType,
        val confidence: Float,
        val regex: Regex,
        val reason: String
    )

    private val rules = listOf(
        ClassificationRule(
            type = RegistrationType.MEDICAL_DEVICE,
            confidence = 0.96f,
            regex = Regex("(?:国|[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼])械(?:注(?:准|进|许)|备)[A-Z0-9]{6,24}号?"),
            reason = "编号包含“械注”或“械备”等医疗器械注册 / 备案格式特征。"
        ),
        ClassificationRule(
            type = RegistrationType.DRUG_APPROVAL,
            confidence = 0.98f,
            regex = Regex("国药准字[A-Z]?[A-Z0-9]{6,16}"),
            reason = "编号以“国药准字”开头，符合常见药品批准文号特征。"
        ),
        ClassificationRule(
            type = RegistrationType.HEALTH_FOOD,
            confidence = 0.95f,
            regex = Regex("(?:国食健(?:字|注)|卫食健字|食健备)[A-Z0-9]{6,24}"),
            reason = "编号包含“国食健”“卫食健”或“食健备”等保健食品格式特征。"
        ),
        ClassificationRule(
            type = RegistrationType.COSMETIC,
            confidence = 0.95f,
            regex = Regex("(?:国妆特字|国妆网备进字|国妆网备字|国妆备进字|妆网备字)[A-Z0-9]{4,24}"),
            reason = "编号包含“国妆”或“妆网备”等化妆品注册 / 备案格式特征。"
        )
    )

    fun classify(rawNumber: String): RegistrationClassification {
        val normalizedInput = normalize(rawNumber)
        val matchedRule = rules.firstNotNullOfOrNull { rule ->
            rule.regex.find(normalizedInput)?.let { match -> rule to match.value }
        }

        if (matchedRule != null) {
            val (rule, matchedNumber) = matchedRule
            return RegistrationClassification(
                rawNumber = rawNumber.trim(),
                normalizedNumber = matchedNumber,
                type = rule.type,
                confidence = rule.confidence,
                reason = rule.reason
            )
        }

        return RegistrationClassification(
            rawNumber = rawNumber.trim(),
            normalizedNumber = normalizedInput,
            type = RegistrationType.UNKNOWN,
            confidence = if (normalizedInput.isBlank()) 0f else 0.2f,
            reason = if (normalizedInput.isBlank()) {
                "尚未提取到注册或备案编号。"
            } else {
                "编号暂未命中当前支持的常见格式，需要在官方平台人工确认。"
            }
        )
    }

    fun findCandidate(text: String): String? {
        val normalized = normalize(text)
        return rules.firstNotNullOfOrNull { rule ->
            rule.regex.find(normalized)?.value
        }
    }

    private fun normalize(value: String): String = value
        .uppercase()
        .replace(Regex("[\\s:：()（）\\-—]"), "")
        .trim()
}
