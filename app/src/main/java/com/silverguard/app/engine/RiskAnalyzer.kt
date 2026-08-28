package com.silverguard.app.engine

import com.silverguard.app.model.RiskAnalysis
import com.silverguard.app.model.RiskFlag
import com.silverguard.app.model.RiskLevel

object RiskAnalyzer {

    private data class Rule(
        val type: String,
        val weight: Int,
        val regex: Regex,
        val explanation: String
    )

    private val rules = listOf(
        Rule(
            "疾病治疗或强功效承诺",
            26,
            Regex("根治|治愈|治疗|抗癌|降血糖|降血压|疏通血管|清除血栓|改善鼻炎|排毒|逆转糖尿病"),
            "出现疾病治疗或强功效承诺。普通保健食品、一般商品不能因为广告话术就视为具有治疗效果。"
        ),
        Rule(
            "替代正规治疗",
            32,
            Regex("不用吃药|无需吃药|不用看医生|替代药物|停药|不打针|无需手术"),
            "出现替代正规治疗的暗示，这是健康消费中需要优先警惕的信号。"
        ),
        Rule(
            "绝对承诺 / 短期见效",
            18,
            Regex("百分百|100%|保证有效|一定有效|包治|无效退款|立刻见效|七天见效|三天见效"),
            "使用绝对化、短期见效或保证有效的承诺，需要进一步核验证据。"
        ),
        Rule(
            "权威包装",
            12,
            Regex("国家级|国家专利|院士|教授推荐|专家推荐|央视推荐|军工技术|航天技术|诺贝尔|祖传秘方"),
            "专利、专家、电视台、军工等背书本身不能证明商品功效，需要核对具体机构、人员和证据。"
        ),
        Rule(
            "科技热词包装",
            14,
            Regex("量子|太赫兹|负离子|细胞修复|纳米能量|生物波|经络共振|频率疗法"),
            "使用高科技或难验证术语包装功效。术语存在，不代表该商品宣传的效果成立。"
        ),
        Rule(
            "催促付款",
            12,
            Regex("仅限今天|最后\\d*台|最后\\d*名|马上抢|错过不再|限时秒杀|今天恢复原价"),
            "通过稀缺、限时制造紧迫感，容易让消费者跳过核验。"
        ),
        Rule(
            "异常节电承诺",
            28,
            Regex("省电\\s*\\d{2,3}%|节电\\s*\\d{2,3}%|电费.*减少\\s*\\d{2,3}%|插上.*省电"),
            "声称仅靠简单装置即可大幅降低家庭总用电，需要非常谨慎，并核对检测与适用条件。"
        ),
        Rule(
            "异常赚钱承诺",
            30,
            Regex("稳赚|保本|零风险|日赚|月入过万|轻松月入|躺赚|退休赚钱|内部项目"),
            "固定收益、零风险、轻松赚钱等承诺属于高风险信号，需要核验收费、返利和拉人模式。"
        ),
        Rule(
            "转入私域成交",
            10,
            Regex("加微信|扫码进群|私聊老师|联系助理|进群领取"),
            "引导进入私聊或群聊后继续成交，会降低公开平台的监督和留痕。"
        )
    )

    fun analyze(text: String): RiskAnalysis {
        val clean = text.trim()
        val flags = mutableListOf<RiskFlag>()
        var score = 0

        rules.forEach { rule ->
            val match = rule.regex.find(clean)
            if (match != null) {
                flags += RiskFlag(
                    type = rule.type,
                    matched = match.value,
                    explanation = rule.explanation,
                    weight = rule.weight
                )
                score += rule.weight
            }
        }

        if (flags.size >= 3) score += 8
        score = score.coerceAtMost(100)

        val level = when {
            score >= 55 -> RiskLevel.HIGH
            score >= 25 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val title = when (level) {
            RiskLevel.HIGH -> "高风险，建议暂缓付款"
            RiskLevel.MEDIUM -> "需要谨慎，建议进一步核验"
            RiskLevel.LOW -> "暂未发现明显高风险"
        }

        return RiskAnalysis(
            rawText = clean,
            category = classify(clean),
            score = score,
            level = level,
            title = title,
            flags = flags,
            extractedPrice = extractPrice(clean),
            extractedModel = extractModel(clean)
        )
    }

    private fun classify(text: String): String = when {
        Regex("理疗|医疗|保健|降血糖|降血压|鼻炎|血管|疼痛|太赫兹|量子|睡眠|颈椎|腰椎|磁疗").containsMatchIn(text) ->
            "健康理疗 / 医疗相关"
        Regex("节电|省电|电费|插座|稳压|节能器").containsMatchIn(text) ->
            "节能 / 家用电器"
        Regex("赚钱|月入|投资|收益|秘籍|兼职|副业|返利|保本").containsMatchIn(text) ->
            "赚钱课程 / 投资信息"
        else -> "一般消费品"
    }

    private fun extractPrice(text: String): String? {
        val patterns = listOf(
            Regex("[¥￥]\\s*(\\d+(?:\\.\\d{1,2})?)"),
            Regex("(?:售价|现价|价格)[:：\\s]*(\\d+(?:\\.\\d{1,2})?)\\s*元?")
        )
        for (pattern in patterns) {
            val match = pattern.find(text)
            val value = match?.groupValues?.getOrNull(1)
            if (!value.isNullOrBlank()) return "¥$value"
        }
        return null
    }

    private fun extractModel(text: String): String? {
        val match = Regex("(?:型号|Model)[:：\\s]*([A-Za-z0-9_-]{2,24})", RegexOption.IGNORE_CASE).find(text)
        return match?.groupValues?.getOrNull(1)
    }
}
