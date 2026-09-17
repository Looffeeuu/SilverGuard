package com.silverguard.app.engine

import com.silverguard.app.data.RiskCaseRepository
import com.silverguard.app.model.RiskAnalysis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ConsumerReferenceReportBuilder {
    fun build(analysis: RiskAnalysis): String = buildString {
        val price = analysis.priceReference
        appendLine("【价格参考】")
        appendLine("状态：${price.status.displayName}")
        if (price.hasRange) {
            appendLine("当前到手价：${PriceReferenceEvaluator.formatPrice(requireNotNull(price.askingCents))}")
            appendLine("已录报价区间：${PriceReferenceEvaluator.formatPrice(requireNotNull(price.minCents))} ～ ${PriceReferenceEvaluator.formatPrice(requireNotNull(price.maxCents))}")
            price.input.quotes.filter { it.source.isNotBlank() }.forEach { quote ->
                appendLine("报价来源：${quote.source.trim()}；到手价：${quote.price.trim()}")
            }
            appendLine("可比条件：由用户确认同款、同规格、同数量和购买条件")
            price.recordedAt?.let {
                appendLine("录入时间：${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(it))}")
            }
        }
        appendLine(price.message)
        appendLine("依据为用户录入报价，不是实时市场价、官方指导价或商品真假结论。")
        appendLine()
        appendLine("【类似案例与消费提示】")
        if (analysis.relatedCases.isEmpty()) {
            appendLine("暂未匹配到本地条目；案例库范围有限，没有匹配不代表安全。")
        }
        analysis.relatedCases.forEach { match ->
            val case = match.case
            appendLine("${case.kind.displayName}：${case.title}")
            appendLine("相似话术：${match.matchedTerms.joinToString("、")}")
            appendLine(case.summary)
            appendLine("来源：${case.sourceName}；发布：${case.publishedOn}")
            appendLine("原文：${case.sourceTitle}\n${case.sourceUrl}")
        }
        appendLine("本地资料整理日期：${RiskCaseRepository.REVIEWED_ON}")
        append("仅为话术相似提醒，不代表当前商品或商家属于案例对象，不改变宣传风险分数。")
    }
}
