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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverguard.app.model.EcommerceFieldType
import com.silverguard.app.model.EcommerceLinkInfo
import com.silverguard.app.model.EcommerceProduct
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun ProductSourceCard(
    info: EcommerceLinkInfo,
    product: EcommerceProduct?
) {
    if (!info.isSupportedPlatform) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SoftGreen, contentColor = Ink),
        border = CardBorder,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("商品来源", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(10.dp))
            SourceRow("平台", info.platform.displayName)
            product?.title?.let { SourceRow("商品", it) }
            SourceRow("页面显示价格", product?.price ?: "暂未读取")
            SourceRow("店铺 / 卖家", product?.shopName ?: product?.sellerName ?: "暂未读取")
            SourceRow("商品 ID", product?.productId ?: info.productId ?: "暂未识别")
            SourceRow(
                "读取状态",
                product?.parseStatus?.displayName ?: if (info.isShortLink) {
                    "已识别短链接，尚未读取"
                } else "链接已识别，尚未读取"
            )
            product?.fetchedAt?.let { SourceRow("读取时间", formatTime(it)) }
            Spacer(Modifier.height(7.dp))
            val titleSource = product?.fields
                ?.firstOrNull { it.type == EcommerceFieldType.TITLE }
                ?.source
                ?.displayName
            if (titleSource != null) {
                Text(
                    "商品名称来源：$titleSource",
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
            product?.let {
                Text(
                    it.message,
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
            Text(
                "商品所在平台与监管部门的官方核验是两件事，请继续查看下方官方信息核验。",
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(timestamp))

@Composable
private fun SourceRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
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
