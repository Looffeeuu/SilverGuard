package com.silverguard.app.engine

import com.silverguard.app.model.RiskAnalysis
import com.silverguard.app.model.RiskFlag
import com.silverguard.app.model.RiskLevel
import com.silverguard.app.model.EcommerceProduct
import com.silverguard.app.model.PriceReferenceInput
import com.silverguard.app.model.RegistrationType

object RiskAnalyzer {

    // Each additional distinct wording within one rule adds half the rule weight, up to two extras.
    // "零风险、日赚、内部项目" then outweighs a single hit, while repeating one word adds nothing.
    private const val MAX_EXTRA_HITS = 2
    private const val HIGH_THRESHOLD = 55
    private const val MEDIUM_THRESHOLD = 25

    // Drugs and medical devices may state their registered indications, such as "用于治疗高血压".
    private val indicationRegistrations = setOf(RegistrationType.DRUG_APPROVAL, RegistrationType.MEDICAL_DEVICE)

    private data class Rule(
        val type: String,
        val weight: Int,
        val regex: Regex,
        val explanation: String,
        /** Wording that is a normal indication once a drug or device registration number is found. */
        val registeredIndication: Regex? = null
    )

    private val rules = listOf(
        Rule(
            "疾病治疗或强功效承诺",
            26,
            // A device name such as "颈椎治疗仪" is not by itself a treatment claim.
            Regex("根治|治愈|治疗(?![仪器])|抗癌|降血糖|降血压|疏通血管|清除血栓|改善鼻炎|排毒|逆转糖尿病"),
            "出现疾病治疗或强功效承诺。普通保健食品、一般商品不能因为广告话术就视为具有治疗效果。",
            // Cure promises such as "根治" stay flagged even with a registration number.
            registeredIndication = Regex("治疗|降血糖|降血压|改善鼻炎|疏通血管")
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
            // Bare "100%" also appears in fabric and purity labels, so only promise phrases count.
            Regex("(?:百分之?百|100\\s*[%％])(?:有效|见效|治愈|根治|康复|安全|放心|保证|收益|回本|赚)|保证有效|一定有效|包治|无效退款|立刻见效|七天见效|三天见效"),
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
            // A two-digit yearly or monthly return is far above deposits and government bonds.
            // "不保本" and "非保本" are compliant risk disclosures, not promises.
            Regex("稳赚|(?<![不非])保本|(?<![不非])保底|零风险|日赚|月入过万|轻松月入|躺赚|退休赚钱|内部项目|(?:年化|收益率?|月息|年息|返利|回报)[^，,。；;\\d]{0,4}[1-9]\\d(?:\\.\\d+)?\\s*[%％]"),
            "固定收益、零风险、轻松赚钱等承诺属于高风险信号，需要核验收费、返利和拉人模式。"
        ),
        Rule(
            "转入私域成交",
            10,
            Regex("加微信|扫码进群|私聊老师|联系助理|进群领取"),
            "引导进入私聊或群聊后继续成交，会降低公开平台的监督和留痕。"
        )
    )

    fun analyze(text: String, ecommerceProduct: EcommerceProduct? = null): RiskAnalysis {
        val ecommerceLinkInfo = EcommerceLinkParser.parse(text)
        val localText = UrlAnalysisSanitizer.stripUrlsForAnalysis(text)
        val ecommerceText = ecommerceProduct
            ?.let(EcommerceProductToProductInfoMapper::riskAnalysisText)
            .orEmpty()
        val clean = listOf(localText, ecommerceText)
            .filter { it.isNotBlank() }
            .joinToString("\n")
        val localProductInfo = ProductInfoExtractor.extract(localText)
        val productInfo = ecommerceProduct?.let { product ->
            EcommerceProductToProductInfoMapper.merge(
                preferred = localProductInfo,
                fallback = EcommerceProductToProductInfoMapper.toProductInfo(product)
            )
        } ?: localProductInfo
        val verification = VerificationPlanner.plan(productInfo)
        val registrationType = verification.registration.type
        val claimConflicts = ClaimRegistrationConflictAnalyzer.analyze(
            text = clean,
            registrationType = registrationType
        )

        val flags = mutableListOf<RiskFlag>()
        var score = 0

        rules.forEach { rule ->
            val matches = AffirmedClaimMatcher.findAll(rule.regex, clean)
                .map { it.value }
                .distinct()
                .filterNot { value ->
                    registrationType in indicationRegistrations &&
                        rule.registeredIndication?.matches(value) == true
                }
                .toList()
            if (matches.isNotEmpty()) {
                val weight = rule.weight + rule.weight / 2 * minOf(matches.size - 1, MAX_EXTRA_HITS)
                flags += RiskFlag(
                    type = rule.type,
                    matched = matches.joinToString("、"),
                    explanation = rule.explanation,
                    weight = weight
                )
                score += weight
            }
        }

        if (flags.size >= 3) score += 8
        // Treatment claims beyond what the product's registration allows mean "pause before paying",
        // even when only one rule matched.
        if (claimConflicts.isNotEmpty()) score = score.coerceAtLeast(HIGH_THRESHOLD)
        score = score.coerceAtMost(100)

        val level = when {
            score >= HIGH_THRESHOLD -> RiskLevel.HIGH
            score >= MEDIUM_THRESHOLD -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val title = when (level) {
            RiskLevel.HIGH -> "高风险，建议暂缓付款"
            RiskLevel.MEDIUM -> "需要谨慎，建议进一步核验"
            RiskLevel.LOW -> "暂未发现明显高风险"
        }

        val category = classify(clean)

        return RiskAnalysis(
            rawText = clean,
            category = category,
            score = score,
            level = level,
            title = title,
            flags = flags,
            productInfo = productInfo,
            verification = verification,
            claimConflicts = claimConflicts,
            ecommerceLinkInfo = ecommerceLinkInfo,
            ecommerceProduct = ecommerceProduct,
            priceReference = PriceReferenceEvaluator.evaluate(
                PriceReferenceInput(askingPrice = productInfo.price.orEmpty())
            ),
            relatedCases = RiskCaseMatcher.match(clean)
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

}
