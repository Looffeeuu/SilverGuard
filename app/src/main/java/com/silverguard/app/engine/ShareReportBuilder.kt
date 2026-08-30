package com.silverguard.app.engine

import com.silverguard.app.model.RiskAnalysis
import com.silverguard.app.model.RiskLevel

object ShareReportBuilder {

    fun build(analysis: RiskAnalysis): String {
        val info = analysis.productInfo
        val flags = if (analysis.flags.isEmpty()) {
            "当前规则未发现明显高风险词"
        } else {
            analysis.flags.joinToString("、") { "${it.type}（${it.matched}）" }
        }
        val conflicts = if (analysis.claimConflicts.isEmpty()) {
            "暂未发现宣传内容与登记类型的明显冲突"
        } else {
            analysis.claimConflicts.joinToString("；") { it.title }
        }
        val sourceNames = analysis.verification.officialSources
            .map { it.organization }
            .distinct()
            .joinToString("、")
            .ifBlank { "尚未确定" }
        val advice = when (analysis.level) {
            RiskLevel.HIGH -> "建议暂缓付款，先保存包装信息并完成官方人工核验。"
            RiskLevel.MEDIUM -> "建议先核对资质、企业、型号规格和登记用途，再决定是否购买。"
            RiskLevel.LOW -> "未发现明显高风险词，但仍建议核对商品信息和官方记录。"
        }

        return """
            【银龄安心查 · v0.3.0】

            【商品信息】
            商品：${info.name ?: "未识别"}
            品牌：${info.brand ?: "未识别"}
            生产企业：${info.manufacturer ?: "未识别"}
            型号：${info.model ?: "未识别"}
            规格：${info.specification ?: "未识别"}
            注册/备案号：${info.registrationNumber ?: "未识别"}

            【风险分析】
            风险等级：${riskLevelText(analysis.level)}（${analysis.score}/100）
            主要风险信号：$flags
            宣传与登记类型：$conflicts

            【官方核验】
            可能类型：${analysis.verification.registration.type.displayName}
            当前状态：${analysis.verification.status.displayName}
            官方来源：$sourceNames
            核验时间：${analysis.verification.checkedAt ?: "尚未查询"}

            【建议】
            $advice

            当前结果为消费风险辅助判断，不代表行政认定、医学诊断或官方认证。
        """.trimIndent()
    }

    private fun riskLevelText(level: RiskLevel): String = when (level) {
        RiskLevel.HIGH -> "高"
        RiskLevel.MEDIUM -> "中"
        RiskLevel.LOW -> "低"
    }
}
