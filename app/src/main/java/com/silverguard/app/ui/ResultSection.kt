package com.silverguard.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverguard.app.engine.CaptureGuidanceEvaluator
import com.silverguard.app.model.EvidenceMatchStatus
import com.silverguard.app.model.OfficialSearchOutcome
import com.silverguard.app.model.OfficialSource
import com.silverguard.app.model.ProductDetailParseStatus
import com.silverguard.app.model.ProductInfo
import com.silverguard.app.model.RiskAnalysis
import com.silverguard.app.model.RiskLevel
import com.silverguard.app.model.VerificationEvidenceField
import com.silverguard.app.model.VerificationStatus

@Composable
internal fun ResultSection(
    analysis: RiskAnalysis,
    onOpenOfficialSource: (OfficialSource) -> Unit,
    onSelectOfficialScreenshot: () -> Unit,
    isOfficialScreenshotOcrRunning: Boolean,
    officialScreenshotMessage: String,
    onRecordSearchOutcome: (OfficialSearchOutcome) -> Unit,
    onRecordFinding: (VerificationEvidenceField, EvidenceMatchStatus) -> Unit,
    onOpenProductPage: (String) -> Unit,
    onSelectScreenshot: () -> Unit,
    onSupplementPhoto: () -> Unit,
    onSpeakResult: () -> Unit,
    onStopSpeaking: () -> Unit,
    isSpeaking: Boolean,
    speechMessage: String?,
    onReset: () -> Unit
) {
    val guidance = remember(analysis.productInfo) {
        CaptureGuidanceEvaluator.evaluate(analysis.productInfo)
    }
    var showProductDetails by remember(analysis.rawText) { mutableStateOf(false) }
    var showRiskDetails by remember(analysis.rawText) { mutableStateOf(false) }
    var showSourceDetails by remember(analysis.rawText) { mutableStateOf(false) }

    ResultSummary(analysis)

    Spacer(Modifier.height(12.dp))
    NextActionCard(
        analysis = analysis,
        isSpeaking = isSpeaking,
        speechMessage = speechMessage,
        onSpeakResult = onSpeakResult,
        onStopSpeaking = onStopSpeaking
    )

    if (guidance.shouldSuggestSupplement) {
        Spacer(Modifier.height(12.dp))
        SupplementSuggestionCard(
            guidanceMessage = guidance.message,
            suggestedShots = guidance.suggestedShots,
            informationIsLimited = guidance.informationIsLimited,
            onSupplementPhoto = onSupplementPhoto
        )
    }

    analysis.ecommerceLinkInfo
        ?.takeIf { it.isSupportedPlatform }
        ?.let { linkInfo ->
            Spacer(Modifier.height(12.dp))
            ExpandableInfoCard(
                title = "商品来源",
                summary = "${linkInfo.platform.displayName} · ${analysis.ecommerceProduct?.parseStatus?.displayName ?: "链接已识别"}",
                expanded = showSourceDetails,
                onToggle = { showSourceDetails = !showSourceDetails }
            ) {
                ProductSourceCard(linkInfo, analysis.ecommerceProduct)
            }
            analysis.ecommerceProduct
                ?.takeIf { product ->
                    product.parseStatus != ProductDetailParseStatus.SUCCESS &&
                        product.parseStatus != ProductDetailParseStatus.PARTIAL
                }
                ?.let { product ->
                    Spacer(Modifier.height(12.dp))
                    ProductParseFallbackCard(
                        product = product,
                        onOpenProductPage = onOpenProductPage,
                        onSelectScreenshot = onSelectScreenshot
                    )
                }
        }

    Spacer(Modifier.height(12.dp))
    ExpandableInfoCard(
        title = "① 我识别到了什么？",
        summary = "已整理 ${analysis.productInfo.detectedCount}/${ProductInfo.FIELD_COUNT} 项包装信息",
        expanded = showProductDetails,
        onToggle = { showProductDetails = !showProductDetails }
    ) {
        ProductInfoSection(
            category = analysis.category,
            productInfo = analysis.productInfo
        )
    }

    Spacer(Modifier.height(12.dp))
    InfoCard("② 哪些地方需要警惕？") {
        RiskPreview(analysis)
        if (analysis.flags.size + analysis.claimConflicts.size > 2) {
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = { showRiskDetails = !showRiskDetails },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(if (showRiskDetails) "收起全部依据" else "查看全部风险依据")
            }
            if (showRiskDetails) {
                Spacer(Modifier.height(6.dp))
                RiskSignals(analysis, skip = 2)
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    VerificationCard(
        result = analysis.verification,
        onOpenOfficialSource = onOpenOfficialSource,
        onSelectOfficialScreenshot = onSelectOfficialScreenshot,
        isOfficialScreenshotOcrRunning = isOfficialScreenshotOcrRunning,
        officialScreenshotMessage = officialScreenshotMessage,
        onRecordSearchOutcome = onRecordSearchOutcome,
        onRecordFinding = onRecordFinding,
        onRetry = onSupplementPhoto
    )

    Spacer(Modifier.height(12.dp))
    InfoCard("④ 我现在应该怎么办？") {
        Text(purchaseAdvice(analysis), lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("重新查一个", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ResultSummary(analysis: RiskAnalysis) {
    val background = when (analysis.level) {
        RiskLevel.HIGH -> SoftRed
        RiskLevel.MEDIUM -> SoftAmber
        RiskLevel.LOW -> SoftGreen
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = background),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("综合结果 · ${analysis.category}", color = Muted, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = analysis.title,
                modifier = Modifier.semantics { heading() },
                fontSize = 27.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink
            )

            Spacer(Modifier.height(12.dp))
            Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    SummaryStatusRow("宣传风险", riskLevelText(analysis.level))
                    SummaryStatusRow("商品信息", analysis.productInfo.completenessLabel)
                    SummaryStatusRow("官方核验", analysis.verification.statusSummary)
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = riskSummary(analysis),
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 23.sp
            )
            Spacer(Modifier.height(8.dp))
            Text("辅助分数：${analysis.score}/100（只用于提示，不是真假结论）", color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun NextActionCard(
    analysis: RiskAnalysis,
    isSpeaking: Boolean,
    speechMessage: String?,
    onSpeakResult: () -> Unit,
    onStopSpeaking: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "现在先做什么",
                modifier = Modifier.semantics { heading() },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Spacer(Modifier.height(8.dp))
            Text(purchaseAdvice(analysis), lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = if (isSpeaking) onStopSpeaking else onSpeakResult,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (isSpeaking) "停止朗读" else "朗读结果给我听",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            speechMessage?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = Muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SupplementSuggestionCard(
    guidanceMessage: String,
    suggestedShots: List<String>,
    informationIsLimited: Boolean,
    onSupplementPhoto: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (informationIsLimited) SoftAmber else SoftGreen),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                if (informationIsLimited) "信息较少，请留意" else "可以补拍得更完整",
                modifier = Modifier.semantics { heading() },
                color = Ink,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(7.dp))
            Text(guidanceMessage, color = Ink, lineHeight = 23.sp)
            if (suggestedShots.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("建议拍：${suggestedShots.joinToString("、")}", color = Muted, lineHeight = 22.sp)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onSupplementPhoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("建议补拍包装（可以跳过）", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                "不补拍也能继续查看当前结果。",
                modifier = Modifier.padding(top = 6.dp),
                color = Muted,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ExpandableInfoCard(
    title: String,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                title,
                modifier = Modifier.semantics { heading() },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Spacer(Modifier.height(5.dp))
            Text(summary, color = Muted, lineHeight = 21.sp)
            TextButton(
                onClick = onToggle,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics { role = Role.Button }
            ) {
                Text(if (expanded) "收起详细信息" else "查看详细信息")
            }
            if (expanded) {
                Spacer(Modifier.height(4.dp))
                content()
            }
        }
    }
}

@Composable
private fun SummaryStatusRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Text(label, color = Muted, fontSize = 13.sp)
        Text(value, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp)
    }
}

@Composable
private fun ProductInfoSection(category: String, productInfo: ProductInfo) {
    ProductInfoRow("商品名称", productInfo.name)
    ProductInfoRow("品牌", productInfo.brand)
    ProductInfoRow("生产企业", productInfo.manufacturer)
    ProductInfoRow("型号", productInfo.model)
    ProductInfoRow("规格 / 净含量", productInfo.specification)
    ProductInfoRow("注册 / 备案号", productInfo.registrationNumber)
    ProductInfoRow("价格", productInfo.price)
    ProductInfoRow("初步分类", category)
    Spacer(Modifier.height(8.dp))
    Text(
        "以上内容由文字规则自动整理，请对照包装原文确认，不代表官方记录或资质核验结果。",
        color = Muted,
        fontSize = 13.sp,
        lineHeight = 20.sp
    )
}

@Composable
private fun ProductInfoRow(label: String, value: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Text(label, color = Muted, fontSize = 13.sp)
        Text(
            text = value ?: "未识别",
            color = if (value == null) Muted else Ink,
            fontWeight = if (value == null) FontWeight.Normal else FontWeight.SemiBold,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun RiskPreview(analysis: RiskAnalysis) {
    if (analysis.flags.isEmpty() && analysis.claimConflicts.isEmpty()) {
        Text(
            "当前规则未发现明显高风险话术。没有注册号的普通商品不会因此被直接判定为高风险。",
            color = Muted,
            lineHeight = 23.sp
        )
        return
    }
    val conflictsToShow = analysis.claimConflicts.take(2)
    conflictsToShow.forEach { conflict -> ConflictCard(conflict.title, conflict.matchedClaims.joinToString("、"), conflict.explanation) }
    val remaining = 2 - conflictsToShow.size
    analysis.flags.take(remaining).forEach { flag -> RiskFlagCard(flag.type, flag.matched, flag.explanation) }
}

@Composable
private fun RiskSignals(analysis: RiskAnalysis, skip: Int) {
    val combined = buildList<Pair<String, Triple<String, String, Boolean>>> {
        analysis.claimConflicts.forEach { conflict ->
            add(conflict.title to Triple(conflict.matchedClaims.joinToString("、"), conflict.explanation, true))
        }
        analysis.flags.forEach { flag ->
            add(flag.type to Triple(flag.matched, flag.explanation, false))
        }
    }.drop(skip)
    combined.forEach { (title, detail) ->
        if (detail.third) ConflictCard(title, detail.first, detail.second)
        else RiskFlagCard(title, detail.first, detail.second)
    }
}

@Composable
private fun ConflictCard(title: String, matched: String, explanation: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftRed),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.padding(bottom = 9.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("重点核验", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Ink, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
            Spacer(Modifier.height(5.dp))
            Text("涉及宣传：$matched", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(5.dp))
            Text(explanation, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun RiskFlagCard(title: String, matched: String, explanation: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftAmber),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("需要留意：$title", fontWeight = FontWeight.Bold, color = Ink)
            Text("命中：“$matched”", fontSize = 13.sp, color = Muted)
            Spacer(Modifier.height(5.dp))
            Text(explanation, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                title,
                modifier = Modifier.semantics { heading() },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

internal fun riskSummary(analysis: RiskAnalysis): String {
    val verification = analysis.verification
    if (verification.manualRecord?.hasMismatch == true) {
        return "人工核对发现包装信息与官方页面存在不一致。建议暂停付款，先请家人一起复核编号、企业和登记用途。"
    }
    if (verification.hasUnresolvedScreenshotMismatch) {
        return "官方查询截图辅助比对发现可能不一致。OCR 可能看错字，请暂停付款并在下方逐项人工确认。"
    }
    if (verification.status == VerificationStatus.NOT_FOUND) {
        return "人工查询暂未找到登记记录。请先检查编号和查询类别；暂时没找到不等于假货。"
    }
    if (verification.status == VerificationStatus.ERROR) {
        return "官方页面本次无法正常查询。页面异常不能说明商品有问题，建议稍后重试。"
    }
    if (analysis.claimConflicts.isNotEmpty()) {
        return "宣传内容与当前可识别的商品登记类型存在明显冲突风险。先不要只看广告结论，应核对官方登记用途。"
    }
    if (analysis.flags.isEmpty()) {
        return "没有命中明显风险词，但这不代表商品资质、功效和价格已经得到官方确认。"
    }
    val mainSignals = analysis.flags.take(2).joinToString("、") { it.type }
    return when (analysis.level) {
        RiskLevel.HIGH -> "同时发现“$mainSignals”等强风险信号，建议先暂停付款并完成官方人工核验。"
        RiskLevel.MEDIUM -> "发现“$mainSignals”等可疑宣传，需要先核对商品资质和完整信息。"
        RiskLevel.LOW -> "发现的风险信号较少，但当前结果还没有自动查询官方记录。"
    }
}

internal fun purchaseAdvice(analysis: RiskAnalysis): String {
    val verification = analysis.verification
    if (verification.manualRecord?.hasMismatch == true) {
        return "先暂停付款。保留商品页面和包装照片，请家人一起复核不一致的字段；不要把人工勾选当成官方认证。"
    }
    if (verification.hasUnresolvedScreenshotMismatch) {
        return "先暂停付款并人工核对截图中标记的字段。OCR 的“可能不一致”只是提醒，不能直接作为真假结论。"
    }
    if (verification.status == VerificationStatus.NOT_FOUND) {
        return "先检查编号有没有抄错、是否选错查询类别，再重新查询。仍找不到时，保存证据并向官方渠道或家人求助。"
    }
    if (verification.status == VerificationStatus.ERROR) {
        return "稍后重新打开官方页面，或把编号和包装照片发给家人协助查询。页面打不开时不要急着作出真假判断。"
    }
    return when (analysis.level) {
        RiskLevel.HIGH -> "先不要付款。保存商品名、生产企业、型号和注册 / 备案编号，再到官方平台逐项核对；涉及疾病治疗时，不要因为广告自行停药。"
        RiskLevel.MEDIUM -> "先核对资质、生产企业、型号规格和登记用途，再决定。不要只看“专家推荐、国家专利、用户案例”等宣传。"
        RiskLevel.LOW -> "目前没有触发明显高风险词，但仍建议核对企业、价格、抽检和召回信息。普通商品没有注册号并不等于有问题。"
    }
}

private fun riskLevelText(level: RiskLevel): String = when (level) {
    RiskLevel.HIGH -> "高"
    RiskLevel.MEDIUM -> "中"
    RiskLevel.LOW -> "低"
}
