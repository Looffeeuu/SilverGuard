package com.silverguard.app.engine

import com.silverguard.app.model.RiskAnalysis
import com.silverguard.app.model.RiskLevel
import com.silverguard.app.model.VerificationStatus

object SpeechSummaryBuilder {
    fun build(analysis: RiskAnalysis): String {
        val riskLevel = when (analysis.level) {
            RiskLevel.HIGH -> "高"
            RiskLevel.MEDIUM -> "中"
            RiskLevel.LOW -> "低"
        }
        val signals = buildList {
            analysis.claimConflicts.take(1).forEach { add(it.title) }
            analysis.flags.take(2 - size).forEach { add(it.type) }
        }
        val signalText = if (signals.isEmpty()) {
            "暂未发现明显高风险宣传词"
        } else {
            "主要提醒有${signals.joinToString("、")}"
        }
        val verificationText = when (analysis.verification.status) {
            VerificationStatus.NOT_READY -> "官方核验信息还不完整"
            VerificationStatus.READY,
            VerificationStatus.MANUAL_REQUIRED -> "已经找到可查询信息，仍需到官方页面人工核对"
            VerificationStatus.MANUAL_REVIEWED -> "已经保存人工核对记录，请不要把它当成自动官方认证"
            VerificationStatus.VERIFIED -> "已经取得官方数据结果，仍需核对登记用途"
            VerificationStatus.NOT_FOUND -> "人工查询暂时没有找到，请先检查编号和查询类别"
            VerificationStatus.ERROR -> "官方页面本次无法正常查询，可以稍后重试"
        }
        val action = when (analysis.level) {
            RiskLevel.HIGH -> "建议先不要付款，请家人一起核对包装和官方登记信息。"
            RiskLevel.MEDIUM -> "建议先核对企业、型号、规格和登记用途，再决定是否购买。"
            RiskLevel.LOW -> "当前未发现明显高风险，但商品资质、功效和价格仍需自行核对。"
        }
        val aiText = analysis.aiAnalysis.insight?.let { insight ->
            "AI补充分析认为，${insight.summary}。"
        }.orEmpty()
        val priceText = if (analysis.priceReference.hasRange) {
            "价格参考：${analysis.priceReference.status.displayName}。仅依据你录入的报价。"
        } else ""
        val caseText = if (analysis.relatedCases.isNotEmpty()) {
            "本地资料中有相似话术提醒，可展开详细信息查看，不代表当前商品属于案例对象。"
        } else ""
        return "银龄安心查结果。${analysis.title}。宣传风险为${riskLevel}。$signalText。$verificationText。$aiText$priceText$caseText$action"
    }
}
