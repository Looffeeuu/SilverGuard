package com.silverguard.app.engine

import com.silverguard.app.model.RiskAnalysis
import com.silverguard.app.model.RiskLevel
import com.silverguard.app.model.EcommerceFieldType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        val sourceSection = analysis.ecommerceLinkInfo
            ?.takeIf { it.isSupportedPlatform }
            ?.let { linkInfo ->
                val product = analysis.ecommerceProduct
                val parseSummary = product?.parseStatus?.displayName ?: if (linkInfo.isShortLink) {
                    "已识别平台短链接，需要进一步解析"
                } else {
                    "已识别商品链接，尚未自动读取完整商品详情"
                }
                val titleSource = product?.fields
                    ?.firstOrNull { it.type == EcommerceFieldType.TITLE }
                    ?.source
                    ?.displayName
                    ?: "尚未读取"
                """
                    【商品来源】
                    平台：${linkInfo.platform.displayName}
                    商品：${product?.title ?: "暂未读取"}
                    页面显示价格：${product?.price ?: "暂未读取"}
                    店铺/卖家：${product?.shopName ?: product?.sellerName ?: "暂未读取"}
                    商品 ID：${product?.productId ?: linkInfo.productId ?: "暂未识别"}
                    商品链接：${product?.canonicalUrl ?: linkInfo.shareableUrl ?: "暂未识别"}
                    解析状态：$parseSummary
                    读取时间：${product?.fetchedAt?.let(::formatTime) ?: "尚未完成"}
                    商品名称来源：$titleSource

                """.trimIndent()
            }
            .orEmpty()

        return """
            【银龄安心查 · v0.3.2】

            $sourceSection

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

    private fun formatTime(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(timestamp))
}
