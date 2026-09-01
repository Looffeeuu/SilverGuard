package com.silverguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverguard.app.model.VerificationResult
import com.silverguard.app.model.VerificationStatus
import com.silverguard.app.model.EvidenceMatchStatus
import com.silverguard.app.model.OfficialSearchOutcome
import com.silverguard.app.model.OfficialSource
import com.silverguard.app.model.VerificationEvidenceField

@Composable
internal fun VerificationCard(
    result: VerificationResult,
    onOpenOfficialSource: (OfficialSource) -> Unit,
    onSelectOfficialScreenshot: () -> Unit,
    isOfficialScreenshotOcrRunning: Boolean,
    officialScreenshotMessage: String,
    onRecordSearchOutcome: (OfficialSearchOutcome) -> Unit,
    onRecordFinding: (VerificationEvidenceField, EvidenceMatchStatus) -> Unit,
    onRetry: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "③ 官方信息核验",
                modifier = Modifier.semantics { heading() },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Spacer(Modifier.height(10.dp))

            Surface(
                color = verificationStatusColor(result),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = result.statusSummary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    color = Ink,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))
            if (result.readiness.ready) {
                ReadyVerificationContent(
                    result = result,
                    onOpenOfficialSource = onOpenOfficialSource,
                    onSelectOfficialScreenshot = onSelectOfficialScreenshot,
                    isOfficialScreenshotOcrRunning = isOfficialScreenshotOcrRunning,
                    officialScreenshotMessage = officialScreenshotMessage,
                    onRecordSearchOutcome = onRecordSearchOutcome,
                    onRecordFinding = onRecordFinding
                )
            } else {
                NotReadyVerificationContent(result, onRetry)
            }
        }
    }
}

@Composable
private fun ReadyVerificationContent(
    result: VerificationResult,
    onOpenOfficialSource: (OfficialSource) -> Unit,
    onSelectOfficialScreenshot: () -> Unit,
    isOfficialScreenshotOcrRunning: Boolean,
    officialScreenshotMessage: String,
    onRecordSearchOutcome: (OfficialSearchOutcome) -> Unit,
    onRecordFinding: (VerificationEvidenceField, EvidenceMatchStatus) -> Unit
) {
    VerificationDetailRow(
        "注册 / 备案号",
        result.registration.normalizedNumber.ifBlank { result.registration.rawNumber }
    )
    VerificationDetailRow("识别状态", "已识别到可查询编号")
    VerificationDetailRow("可能类型", result.registration.type.displayName)

    Spacer(Modifier.height(8.dp))
    Text(result.registration.reason, color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
    Spacer(Modifier.height(10.dp))
    Text(
        text = statusExplanation(result.status),
        color = Ink,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp
    )
    Spacer(Modifier.height(12.dp))
    VerificationSourceAndTime(result)
    Spacer(Modifier.height(12.dp))
    Text("第一步：复制编号并打开官方页面", fontWeight = FontWeight.Bold, color = Ink)
    Text(
        "按钮会先复制编号，再交给浏览器打开官方来源。",
        color = Muted,
        fontSize = 13.sp,
        lineHeight = 19.sp
    )
    Spacer(Modifier.height(8.dp))

    result.officialSources.forEach { source ->
        OutlinedButton(
            onClick = { onOpenOfficialSource(source) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text("复制编号并打开${source.name}", fontWeight = FontWeight.Bold)
                Text(
                    "${source.role.displayName} · ${source.organization}",
                    fontSize = 12.sp,
                    color = Muted
                )
                Text(source.description, fontSize = 12.sp, color = Muted)
                Text(source.queryHint, fontSize = 12.sp, color = Muted)
            }
        }
        Spacer(Modifier.height(7.dp))
    }

    result.manualRecord?.let { record ->
        Spacer(Modifier.height(8.dp))
        Text("第二步：导入官方查询结果截图", fontWeight = FontWeight.Bold, color = Ink)
        Text(
            "在官方页面查到结果后截图，返回这里选择该截图。文字只在手机本地识别。",
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onSelectOfficialScreenshot,
            enabled = !isOfficialScreenshotOcrRunning,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Brand),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isOfficialScreenshotOcrRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                if (isOfficialScreenshotOcrRunning) "正在识别截图…" else "选择官方查询结果截图",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (officialScreenshotMessage.isNotBlank()) {
            Spacer(Modifier.height(7.dp))
            Text(officialScreenshotMessage, color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
        }

        result.screenshotReview?.let { review ->
            Spacer(Modifier.height(10.dp))
            OfficialScreenshotReviewPanel(review)
        }

        Spacer(Modifier.height(14.dp))
        Text("第三步：确认并记录核对结果", fontWeight = FontWeight.Bold, color = Ink)
        Text(
            "请以你在官方页面看到的内容为准，修正截图 OCR 可能产生的错误。",
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
        Spacer(Modifier.height(8.dp))
        ManualVerificationPanel(
            record = record,
            onRecordOutcome = onRecordSearchOutcome,
            onRecordFinding = onRecordFinding
        )
        Spacer(Modifier.height(10.dp))
    }

    Text(
        "注意：打开入口或人工勾选都不等于 SilverGuard 已自动核验；查到登记信息也不代表广告中的所有功效得到认可。",
        color = Muted,
        fontSize = 13.sp,
        lineHeight = 20.sp
    )
}

@Composable
private fun NotReadyVerificationContent(
    result: VerificationResult,
    onRetry: () -> Unit
) {
    Text("暂时无法核验", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
    Spacer(Modifier.height(8.dp))
    Text("还缺少：", fontWeight = FontWeight.Bold, color = Ink)
    result.readiness.missingFields.forEach { field ->
        Text("○ ${field.displayName}", modifier = Modifier.padding(top = 4.dp), lineHeight = 21.sp)
    }
    Spacer(Modifier.height(9.dp))
    Text(result.readiness.suggestedAction, color = Muted, lineHeight = 22.sp)
    Spacer(Modifier.height(12.dp))
    VerificationSourceAndTime(result)
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = onRetry,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Brand),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text("重新拍照", fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VerificationSourceAndTime(result: VerificationResult) {
    val organizations = result.officialSources
        .map { it.organization }
        .distinct()
        .joinToString("、")
        .ifBlank { "尚未确定" }
    VerificationDetailRow("建议官方来源", organizations)
    VerificationDetailRow("自动核验时间", result.checkedAt ?: "尚未自动查询")
}

@Composable
private fun VerificationDetailRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(label, color = Muted, fontSize = 13.sp)
        Text(
            value,
            color = Ink,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 22.sp
        )
    }
}

private fun statusExplanation(status: VerificationStatus): String = when (status) {
    VerificationStatus.NOT_READY -> "当前信息不足，尚不能选择准确的官方查询入口。"
    VerificationStatus.READY -> "已具备核验信息，但尚未查询官方数据库。"
    VerificationStatus.MANUAL_REQUIRED -> "当前版本尚未自动完成官方数据库匹配，需要你或家人前往官方平台人工核对。"
    VerificationStatus.MANUAL_REVIEWED -> "已保存你根据官方页面填写的人工核对结果，但这不是自动数据库认证。"
    VerificationStatus.VERIFIED -> "已取得官方数据结果，仍需核对匹配字段和登记用途。"
    VerificationStatus.NOT_FOUND -> "你记录为官方页面暂未找到匹配记录；请先排除编号或查询类别错误，不能据此认定假货。"
    VerificationStatus.ERROR -> "你记录为官方页面暂时无法访问，可以稍后重试或请家人协助。"
}

@Composable
private fun verificationStatusColor(result: VerificationResult): Color = when {
    result.manualRecord?.hasMismatch == true -> SoftRed
    result.hasUnresolvedScreenshotMismatch -> SoftRed
    result.status == VerificationStatus.MANUAL_REVIEWED -> SoftGreen
    else -> SoftAmber
}
