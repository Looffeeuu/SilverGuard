package com.silverguard.app.engine

import com.silverguard.app.model.RiskAnalysis
import com.silverguard.app.model.RiskLevel
import com.silverguard.app.model.EcommerceFieldType
import com.silverguard.app.model.EvidenceMatchStatus
import com.silverguard.app.model.VerificationStatus
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
        val advice = when {
            analysis.verification.manualRecord?.hasMismatch == true ->
                "人工核对发现不一致，建议暂停付款并请家人复核编号、企业和登记用途。"
            analysis.verification.hasUnresolvedScreenshotMismatch ->
                "截图辅助比对发现可能不一致，建议暂停付款并逐项人工确认；OCR 提醒不能直接作为真假结论。"
            analysis.verification.status == VerificationStatus.NOT_FOUND ->
                "人工查询暂未找到记录，请检查编号和查询类别；暂时没找到不等于假货。"
            analysis.verification.status == VerificationStatus.ERROR ->
                "官方页面本次无法正常查询，建议稍后重试；页面异常不能说明商品有问题。"
            analysis.level == RiskLevel.HIGH ->
                "建议暂缓付款，先保存包装信息并完成官方人工核验。"
            analysis.level == RiskLevel.MEDIUM ->
                "建议先核对资质、企业、型号规格和登记用途，再决定是否购买。"
            else ->
                "未发现明显高风险词，但仍建议核对商品信息和官方记录。"
        }
        val manualRecord = analysis.verification.manualRecord
        val manualSection = manualRecord?.let { record ->
            val findings = record.findings
                .filter { it.status != EvidenceMatchStatus.NOT_CHECKED }
                .joinToString("；") { finding ->
                    val result = when (finding.status) {
                        EvidenceMatchStatus.MATCHED -> "一致"
                        EvidenceMatchStatus.MISMATCHED -> "不一致"
                        EvidenceMatchStatus.NOT_CHECKED -> "未核对"
                    }
                    "${finding.item.field.displayName}：$result"
                }
                .ifBlank { "尚未逐项核对" }
            """
                人工核对来源：${record.source.organization}（${record.source.name}）
                人工记录结果：${record.summary}
                打开时间：${formatTime(record.openedAt)}
                人工记录时间：${record.reviewedAt?.let(::formatTime) ?: "尚未记录"}
                逐项核对：$findings
            """.trimIndent()
        } ?: "人工核对记录：尚未记录"
        val screenshotSection = analysis.verification.screenshotReview?.let { review ->
            val comparisons = review.comparisons.joinToString("；") { comparison ->
                "${comparison.field.displayName}：${comparison.status.displayName}"
            }
            """
                查询截图辅助比对：${review.summary}
                截图识别时间：${formatTime(review.processedAt)}
                截图字段结果：$comparisons
            """.trimIndent()
        } ?: "查询截图辅助比对：尚未导入截图"
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
            【银龄安心查 · v0.3.4】

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
            自动核验时间：${analysis.verification.checkedAt ?: "尚未自动查询"}
            $screenshotSection
            $manualSection

            【建议】
            $advice

            当前结果为消费风险辅助判断，不代表行政认定、医学诊断或官方认证。
            查询截图由手机本地 OCR 辅助比对，可能识别错误，不代表已自动查询官方数据库。
            人工核对内容由用户根据官方页面记录，不代表 SilverGuard 已自动连接或验证官方数据库。
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
