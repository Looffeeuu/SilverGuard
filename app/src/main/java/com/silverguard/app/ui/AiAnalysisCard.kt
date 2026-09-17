package com.silverguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverguard.app.model.AiAnalysisStatus
import com.silverguard.app.model.AiRiskInsight
import com.silverguard.app.model.RiskAnalysis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun AiAnalysisCard(
    analysis: RiskAnalysis,
    isConfigured: Boolean,
    isAnalyzing: Boolean,
    onAnalyze: () -> Unit
) {
    val result = analysis.aiAnalysis
    val insight = result.insight
    var showDetails by remember(analysis.rawText, insight?.analyzedAt) { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (insight != null) SoftGreen else androidx.compose.ui.graphics.Color.White,
            contentColor = Ink
        ),
        border = CardBorder,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.testTag("ai_analysis_card")
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "AI 深入分析（可选）",
                modifier = Modifier.semantics { heading() },
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(7.dp))

            when {
                isAnalyzing -> AnalyzingContent()
                insight != null -> SuccessContent(insight, showDetails) {
                    showDetails = !showDetails
                }
                !isConfigured -> Text(
                    "AI 深入分析暂未启用。本地规则分析、OCR 和官方核验辅助仍可正常使用。",
                    color = Muted,
                    lineHeight = 23.sp
                )
                else -> {
                    Text(
                        if (result.status == AiAnalysisStatus.NOT_REQUESTED) {
                            "帮助看懂宣传话术中的暗示、劝购方式和需要核对的问题，不会改变上方的本地风险结论。"
                        } else {
                            result.message
                        },
                        color = if (result.status == AiAnalysisStatus.NOT_REQUESTED) Ink else Muted,
                        lineHeight = 23.sp
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "点击后只发送整理过的文字和商品字段，不发送照片；手机号、邮箱等会先隐藏。",
                        color = Muted,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onAnalyze,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 58.dp)
                            .testTag("start_ai_analysis"),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            if (result.status == AiAnalysisStatus.NOT_REQUESTED) "使用 AI 深入分析" else "重新尝试 AI 分析",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyzingContent() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = Brand,
            strokeWidth = 2.dp
        )
        Spacer(Modifier.size(10.dp))
        Text("正在理解宣传话术，请稍候…", color = Ink, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(6.dp))
    Text("本地分析结果会一直保留。", color = Muted, fontSize = 13.sp)
}

@Composable
private fun SuccessContent(
    insight: AiRiskInsight,
    showDetails: Boolean,
    onToggleDetails: () -> Unit
) {
    Text("AI 补充结论", color = Muted, fontSize = 13.sp)
    Text(
        insight.summary,
        color = Ink,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(10.dp))
    Text("建议：${insight.consumerAdvice}", color = Ink, lineHeight = 23.sp)
    Spacer(Modifier.height(10.dp))
    OutlinedButton(
        onClick = onToggleDetails,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(if (showDetails) "收起 AI 详细分析" else "查看 AI 详细分析", fontWeight = FontWeight.Bold)
    }

    if (showDetails) {
        AiList("话术中的隐含承诺", insight.implicitClaims)
        AiList("常见劝购方式", insight.persuasionTactics)
        AiList("购买前建议核对", insight.verificationQuestions)
        Spacer(Modifier.height(8.dp))
        Text(
            "AI 判断把握：${insight.confidence.displayName} · 来源：${insight.providerName} ${insight.modelName}",
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
        Text(
            "分析时间：${formatAiTime(insight.analyzedAt)}",
            color = Muted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "AI 可能理解错误。此结果不代表商品真假、医学诊断、行政认定或官方认证。",
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun AiList(title: String, items: List<String>) {
    if (items.isEmpty()) return
    Spacer(Modifier.height(10.dp))
    Text(title, color = Ink, fontWeight = FontWeight.Bold)
    items.forEach { item ->
        Text("• $item", color = Ink, lineHeight = 22.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

private fun formatAiTime(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(timestamp))
