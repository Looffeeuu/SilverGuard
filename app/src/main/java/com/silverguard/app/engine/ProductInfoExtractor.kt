package com.silverguard.app.engine

import com.silverguard.app.model.ProductInfo

/**
 * Extracts packaging fields from OCR or manually entered text.
 * Values are deliberately treated as clues for the user to verify, not official records.
 */
object ProductInfoExtractor {

    private val boundaryLabels = listOf(
        "商品名称", "产品名称", "品名", "名称", "品牌", "商标",
        "生产企业", "生产厂家", "制造商", "厂名", "企业名称",
        "型号", "Model", "规格型号", "规格", "净含量", "容量", "包装规格",
        "医疗器械注册证编号", "化妆品注册证编号", "保健食品注册号",
        "注册证号", "注册编号", "备案编号", "备案号", "批准文号", "产品标准号", "执行标准",
        "售价", "现价", "价格", "零售价", "地址", "电话", "生产日期", "保质期",
        "配料", "成分", "适用人群", "注意事项"
    )

    fun extract(text: String): ProductInfo {
        val clean = text
            .replace('\u3000', ' ')
            .replace(Regex("[ \\t]+"), " ")
            .trim()

        return ProductInfo(
            name = extractLabeledValue(
                clean,
                listOf("商品名称", "产品名称", "品名"),
                maxLength = 40
            ) ?: inferProductName(clean),
            brand = extractLabeledValue(
                clean,
                listOf("品牌", "商标"),
                maxLength = 30
            ),
            manufacturer = extractLabeledValue(
                clean,
                listOf("生产企业", "生产厂家", "制造商", "厂名", "企业名称"),
                maxLength = 60
            ),
            model = extractModel(clean),
            specification = extractLabeledValue(
                clean,
                listOf("包装规格", "规格", "净含量", "容量"),
                maxLength = 36
            ),
            registrationNumber = extractLabeledValue(
                clean,
                listOf(
                    "医疗器械注册证编号", "化妆品注册证编号", "保健食品注册号",
                    "注册证号", "注册编号", "备案编号", "备案号", "批准文号"
                ),
                maxLength = 48
            ) ?: RegistrationNumberClassifier.findCandidate(clean),
            price = extractPrice(clean)
        )
    }

    private fun extractLabeledValue(
        text: String,
        labels: List<String>,
        maxLength: Int
    ): String? {
        val requestedLabels = labels.joinToString("|") { Regex.escape(it) }
        val boundaries = boundaryLabels.joinToString("|") { Regex.escape(it) }
        val pattern = Regex(
            "(?:$requestedLabels)\\s*[:：]?\\s*([^\\n\\r，,；;]{1,$maxLength}?)(?=\\s+(?:$boundaries)\\s*[:：]?|[\\n\\r，,；;]|$)",
            RegexOption.IGNORE_CASE
        )

        return pattern.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.cleanValue()
    }

    private fun extractPrice(text: String): String? {
        val patterns = listOf(
            Regex("[¥￥]\\s*([0-9]{1,7}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)"),
            Regex("(?:售价|现价|价格|零售价)\\s*[:：]?\\s*([0-9]{1,7}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)\\s*元?", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val value = pattern.find(text)?.groupValues?.getOrNull(1)
            if (!value.isNullOrBlank()) return "¥${value.replace(",", "")}"
        }
        return null
    }

    private fun extractModel(text: String): String? {
        val pattern = Regex(
            "(?:规格型号|型号|Model)\\s*[:：]?\\s*([A-Za-z0-9][A-Za-z0-9._/-]{1,31})",
            RegexOption.IGNORE_CASE
        )
        return pattern.find(text)?.groupValues?.getOrNull(1)?.cleanValue()
    }

    private fun inferProductName(text: String): String? {
        val readableLine = text
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { line ->
                line.length in 2..36 &&
                    boundaryLabels.none { label -> line.startsWith(label, ignoreCase = true) } &&
                    !Regex("百分百|保证有效|专家推荐|仅限今天|马上抢").containsMatchIn(line)
            }
        if (readableLine != null) return readableLine.cleanValue()

        val productSuffixes = listOf(
            "理疗仪", "治疗仪", "按摩仪", "节能器", "保健品", "口服液", "胶囊",
            "冲剂", "片剂", "贴膏", "药膏", "课程", "插座", "净水器", "按摩器"
        ).joinToString("|")
        return Regex("([\\u4E00-\\u9FFFA-Za-z0-9·-]{2,28}?(?:$productSuffixes))")
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.cleanValue()
    }

    private fun String.cleanValue(): String? {
        val cleaned = trim()
            .trim(':', '：', '-', '—', '·', '.', '。')
            .replace(Regex("\\s{2,}"), " ")
            .trim()
        return cleaned.takeIf { it.length >= 2 }
    }
}
