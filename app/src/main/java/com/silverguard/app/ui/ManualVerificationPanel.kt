package com.silverguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverguard.app.model.EvidenceMatchStatus
import com.silverguard.app.model.ManualVerificationRecord
import com.silverguard.app.model.OfficialSearchOutcome
import com.silverguard.app.model.VerificationEvidenceField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun ManualVerificationPanel(
    record: ManualVerificationRecord,
    onRecordOutcome: (OfficialSearchOutcome) -> Unit,
    onRecordFinding: (VerificationEvidenceField, EvidenceMatchStatus) -> Unit
) {
    val panelColor = when {
        record.hasMismatch -> SoftRed
        record.searchOutcome == OfficialSearchOutcome.RECORD_NOT_FOUND -> SoftAmber
        record.searchOutcome == OfficialSearchOutcome.PAGE_UNAVAILABLE -> SoftAmber
        record.searchOutcome == OfficialSearchOutcome.RECORD_FOUND -> SoftGreen
        else -> Color(0xFFF8FAF8)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = panelColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(15.dp)) {
            Text("把官方页面结果记下来", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(5.dp))
            Text("本次来源：${record.source.organization}", color = Muted, fontSize = 13.sp)
            Text("打开时间：${formatVerificationTime(record.openedAt)}", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Text(record.summary, color = Ink, fontWeight = FontWeight.Bold, lineHeight = 21.sp)

            Spacer(Modifier.height(12.dp))
            Text("你在官方页面看到了什么？", fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                OutcomeChoice(
                    text = "找到了记录",
                    selected = record.searchOutcome == OfficialSearchOutcome.RECORD_FOUND,
                    onClick = { onRecordOutcome(OfficialSearchOutcome.RECORD_FOUND) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutcomeChoice(
                    text = "暂时没找到",
                    selected = record.searchOutcome == OfficialSearchOutcome.RECORD_NOT_FOUND,
                    onClick = { onRecordOutcome(OfficialSearchOutcome.RECORD_NOT_FOUND) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(7.dp))
            OutcomeChoice(
                text = "页面打不开或查询异常",
                selected = record.searchOutcome == OfficialSearchOutcome.PAGE_UNAVAILABLE,
                onClick = { onRecordOutcome(OfficialSearchOutcome.PAGE_UNAVAILABLE) },
                modifier = Modifier.fillMaxWidth()
            )

            when (record.searchOutcome) {
                OfficialSearchOutcome.RECORD_FOUND -> {
                    Spacer(Modifier.height(15.dp))
                    Text("逐项对照包装和官方页面", fontWeight = FontWeight.Bold, color = Ink)
                    Text(
                        "不确定的项目可以暂时不选，不要凭印象判断。",
                        color = Muted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    record.findings.forEach { finding ->
                        FindingRow(
                            field = finding.item.field,
                            expectedValue = finding.item.expectedValue,
                            guidance = finding.item.guidance,
                            status = finding.status,
                            onRecordFinding = onRecordFinding
                        )
                    }
                }
                OfficialSearchOutcome.RECORD_NOT_FOUND -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "先检查编号是否抄错、是否选错国产/进口或注册/备案类别。暂时没找到不等于假货。",
                        color = Ink,
                        lineHeight = 21.sp
                    )
                }
                OfficialSearchOutcome.PAGE_UNAVAILABLE -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "可以稍后重试，或把编号和商品包装发给家人一起核对。页面异常不能作为商品有问题的证据。",
                        color = Ink,
                        lineHeight = 21.sp
                    )
                }
                OfficialSearchOutcome.NOT_RECORDED -> Unit
            }

            record.reviewedAt?.let { reviewedAt ->
                Spacer(Modifier.height(10.dp))
                Text("人工记录时间：${formatVerificationTime(reviewedAt)}", color = Muted, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "以上内容由你根据官方页面手动记录，SilverGuard 尚未自动连接官方数据库。",
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun FindingRow(
    field: VerificationEvidenceField,
    expectedValue: String?,
    guidance: String,
    status: EvidenceMatchStatus,
    onRecordFinding: (VerificationEvidenceField, EvidenceMatchStatus) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(13.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(field.displayName, fontWeight = FontWeight.Bold, color = Ink)
            expectedValue?.let {
                Text("包装识别：$it", color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
            }
            Text(guidance, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                FindingChoice(
                    text = field.matchedLabel,
                    selected = status == EvidenceMatchStatus.MATCHED,
                    onClick = {
                        onRecordFinding(
                            field,
                            if (status == EvidenceMatchStatus.MATCHED) {
                                EvidenceMatchStatus.NOT_CHECKED
                            } else {
                                EvidenceMatchStatus.MATCHED
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                FindingChoice(
                    text = field.mismatchedLabel,
                    selected = status == EvidenceMatchStatus.MISMATCHED,
                    onClick = {
                        onRecordFinding(
                            field,
                            if (status == EvidenceMatchStatus.MISMATCHED) {
                                EvidenceMatchStatus.NOT_CHECKED
                            } else {
                                EvidenceMatchStatus.MISMATCHED
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OutcomeChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Brand),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(text, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(text, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FindingChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) = OutcomeChoice(text, selected, onClick, modifier)

private fun formatVerificationTime(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(timestamp))
