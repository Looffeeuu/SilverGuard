package com.silverguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.silverguard.app.model.VerificationResult
import com.silverguard.app.model.VerificationStatus

@Composable
internal fun VerificationCard(
    result: VerificationResult,
    onOpenOfficialSource: (String) -> Unit,
    onRetry: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("③ 官方信息核验", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(10.dp))

            Surface(
                color = if (result.readiness.ready) SoftGreen else SoftAmber,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = result.status.displayName,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    color = Ink,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))
            if (result.readiness.ready) {
                ReadyVerificationContent(result, onOpenOfficialSource)
            } else {
                NotReadyVerificationContent(result, onRetry)
            }
        }
    }
}

@Composable
private fun ReadyVerificationContent(
    result: VerificationResult,
    onOpenOfficialSource: (String) -> Unit
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
    Spacer(Modifier.height(8.dp))
    Text("前往官方平台后，请逐项核对：", fontWeight = FontWeight.Bold, color = Ink)
    listOf("产品名称", "注册人 / 备案人", "型号规格", "适用范围", "登记状态").forEach {
        Text("• $it", modifier = Modifier.padding(top = 3.dp), lineHeight = 20.sp)
    }

    Spacer(Modifier.height(12.dp))
    VerificationSourceAndTime(result)
    Spacer(Modifier.height(10.dp))

    result.officialSources.forEach { source ->
        OutlinedButton(
            onClick = { onOpenOfficialSource(source.url) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text("前往${source.name}", fontWeight = FontWeight.Bold)
                Text("官方来源 · ${source.organization}", fontSize = 12.sp, color = Muted)
                Text(source.description, fontSize = 12.sp, color = Muted)
            }
        }
        Spacer(Modifier.height(7.dp))
    }

    Text(
        "注意：打开官方入口不等于已经完成核验；查到登记信息也不代表广告中的所有功效得到认可。",
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
            .height(58.dp),
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
    VerificationDetailRow("数据来源", organizations)
    VerificationDetailRow("核验时间", result.checkedAt ?: "尚未查询")
}

@Composable
private fun VerificationDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, modifier = Modifier.width(104.dp), color = Muted, fontSize = 14.sp)
        Text(
            value,
            modifier = Modifier.weight(1f),
            color = Ink,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 21.sp
        )
    }
}

private fun statusExplanation(status: VerificationStatus): String = when (status) {
    VerificationStatus.NOT_READY -> "当前信息不足，尚不能选择准确的官方查询入口。"
    VerificationStatus.READY -> "已具备核验信息，但尚未查询官方数据库。"
    VerificationStatus.MANUAL_REQUIRED -> "当前版本尚未自动完成官方数据库匹配，需要你或家人前往官方平台人工核对。"
    VerificationStatus.VERIFIED -> "已取得官方数据结果，仍需核对匹配字段和登记用途。"
    VerificationStatus.NOT_FOUND -> "官方平台暂未找到匹配记录，不能仅凭未找到就认定商品有问题。"
    VerificationStatus.ERROR -> "查询过程发生异常，请稍后重试或直接打开官方平台。"
}
