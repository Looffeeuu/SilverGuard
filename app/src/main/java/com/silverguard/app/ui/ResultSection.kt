package com.silverguard.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverguard.app.model.ProductInfo
import com.silverguard.app.model.ProductDetailParseStatus
import com.silverguard.app.model.RiskAnalysis
import com.silverguard.app.model.RiskLevel

@Composable
internal fun ResultSection(
    analysis: RiskAnalysis,
    onOpenOfficialSource: (String) -> Unit,
    onOpenProductPage: (String) -> Unit,
    onSelectScreenshot: () -> Unit,
    onReset: () -> Unit
) {
    ResultSummary(analysis)

    analysis.ecommerceLinkInfo
        ?.takeIf { it.isSupportedPlatform }
        ?.let { linkInfo ->
            Spacer(Modifier.height(12.dp))
            ProductSourceCard(linkInfo, analysis.ecommerceProduct)
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
    InfoCard("① 我识别到了什么？") {
        ProductInfoSection(
            category = analysis.category,
            productInfo = analysis.productInfo
        )
    }

    Spacer(Modifier.height(12.dp))
    InfoCard("② 哪些地方需要警惕？") {
        RiskSignals(analysis)
    }

    Spacer(Modifier.height(12.dp))
    VerificationCard(
        result = analysis.verification,
        onOpenOfficialSource = onOpenOfficialSource,
        onRetry = onReset
    )

    Spacer(Modifier.height(12.dp))
    InfoCard("④ 我现在应该怎么办？") {
        Text(purchaseAdvice(analysis), lineHeight = 23.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
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
            Text("综合结果 · ${analysis.category}", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = analysis.title,
                    modifier = Modifier.weight(1f),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink
                )
                ScoreBadge(analysis.score)
            }

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
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun SummaryStatusRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.width(94.dp), color = Muted, fontSize = 14.sp)
        Text(value, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun ProductInfoSection(
    category: String,
    productInfo: ProductInfo
) {
    Surface(color = SoftGreen, shape = RoundedCornerShape(12.dp)) {
        Text(
            text = "已整理 ${productInfo.detectedCount}/${ProductInfo.FIELD_COUNT} 项包装信息",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = Ink,
            fontWeight = FontWeight.Bold
        )
    }
    Spacer(Modifier.height(10.dp))

    ProductInfoRow("商品名称", productInfo.name)
    ProductInfoRow("品牌", productInfo.brand)
    ProductInfoRow("生产企业", productInfo.manufacturer)
    ProductInfoRow("型号", productInfo.model)
    ProductInfoRow("规格/净含量", productInfo.specification)
    ProductInfoRow("注册/备案号", productInfo.registrationNumber)
    ProductInfoRow("价格", productInfo.price)
    ProductInfoRow("初步分类", category)

    val missingKeyFields = buildList {
        if (productInfo.name == null) add("商品名称")
        if (productInfo.manufacturer == null) add("生产企业")
        if (productInfo.registrationNumber == null) add("注册/备案号")
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = if (missingKeyFields.isEmpty()) {
            "关键字段已找到，请对照商品包装原文再次确认。"
        } else {
            "建议补拍包装正反面，继续寻找：${missingKeyFields.joinToString("、")}。"
        },
        color = Muted,
        fontSize = 13.sp,
        lineHeight = 20.sp
    )
    Text(
        "以上内容由文字规则自动整理，不代表官方记录或资质核验结果。",
        color = Muted,
        fontSize = 13.sp,
        lineHeight = 20.sp
    )
}

@Composable
private fun ProductInfoRow(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, modifier = Modifier.width(108.dp), color = Muted, fontSize = 14.sp)
        Text(
            text = value ?: "未识别",
            modifier = Modifier.weight(1f),
            color = if (value == null) Muted else Ink,
            fontWeight = if (value == null) FontWeight.Normal else FontWeight.SemiBold,
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun RiskSignals(analysis: RiskAnalysis) {
    analysis.claimConflicts.forEach { conflict ->
        Card(
            colors = CardDefaults.cardColors(containerColor = SoftRed),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.padding(bottom = 9.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("重点核验", color = Color(0xFF9A2E22), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(conflict.title, color = Ink, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                Spacer(Modifier.height(5.dp))
                Text("涉及宣传：${conflict.matchedClaims.joinToString("、")}", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(5.dp))
                Text(conflict.explanation, lineHeight = 21.sp)
            }
        }
    }

    if (analysis.flags.isEmpty() && analysis.claimConflicts.isEmpty()) {
        Text(
            "当前规则未发现明显高风险话术。没有注册号的普通商品也不会因此被直接判定为高风险。",
            color = Muted,
            lineHeight = 22.sp
        )
        return
    }

    analysis.flags.forEach { flag ->
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAF8)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("⚠ ${flag.type}", fontWeight = FontWeight.Bold, color = Ink)
                Text("命中：“${flag.matched}”", fontSize = 13.sp, color = Muted)
                Spacer(Modifier.height(5.dp))
                Text(flag.explanation, lineHeight = 21.sp)
            }
        }
    }
}

@Composable
private fun ScoreBadge(score: Int) {
    Box(
        modifier = Modifier
            .size(74.dp)
            .background(Color.White, RoundedCornerShape(37.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(score.toString(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text("/100", fontSize = 11.sp, color = Muted)
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

private fun riskSummary(analysis: RiskAnalysis): String {
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

private fun purchaseAdvice(analysis: RiskAnalysis): String = when (analysis.level) {
    RiskLevel.HIGH ->
        "先不要付款。保存商品名、生产企业、型号和注册 / 备案编号，再到官方平台逐项核对；涉及疾病治疗时，不要因为广告自行停药。"
    RiskLevel.MEDIUM ->
        "先核对资质、生产企业、型号规格和登记用途，再决定。不要只看“专家推荐、国家专利、用户案例”等宣传。"
    RiskLevel.LOW ->
        "目前没有触发明显高风险词，但仍建议核对企业、价格、抽检和召回信息。普通商品没有注册号并不等于有问题。"
}

private fun riskLevelText(level: RiskLevel): String = when (level) {
    RiskLevel.HIGH -> "高"
    RiskLevel.MEDIUM -> "中"
    RiskLevel.LOW -> "低"
}
