package com.silverguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverguard.app.model.EcommerceLinkInfo
import com.silverguard.app.model.EcommerceLinkStatus

@Composable
internal fun EcommerceLinkCard(info: EcommerceLinkInfo) {
    if (info.status == EcommerceLinkStatus.NO_LINK) return

    val isSupported = info.isSupportedPlatform
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSupported) SoftGreen else SoftAmber
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = if (isSupported) "检测到商品链接" else "检测到网页链接",
                color = Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            LinkInfoRow("平台", info.platform.displayName)
            info.extractedUrl?.let { LinkInfoRow("原始链接", it) }
            info.normalizedUrl?.let { LinkInfoRow("整理后", it) }
            if (isSupported) {
                LinkInfoRow("商品 ID", info.productId ?: "暂未识别")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = info.message,
                color = Ink,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (isSupported) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "识别到平台来源，不代表商品真假、资质或宣传已经通过官方核验。",
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun LinkInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.width(82.dp),
            color = Muted,
            fontSize = 14.sp
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = Color(0xFF24332B),
            fontWeight = FontWeight.Medium,
            lineHeight = 20.sp
        )
    }
}
