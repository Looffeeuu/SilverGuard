package com.silverguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverguard.app.model.EcommerceProduct

@Composable
internal fun ProductParseFallbackCard(
    product: EcommerceProduct,
    onOpenProductPage: (String) -> Unit,
    onSelectScreenshot: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SoftAmber),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "暂时无法自动读取商品详情",
                color = Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(product.message, color = Ink, lineHeight = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "这不代表商品存在风险。你可以打开商品页，或选择截图继续分析。",
                color = Muted,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
            Spacer(Modifier.height(12.dp))
            val openUrl = product.canonicalUrl ?: product.sourceUrl
            if (openUrl.isNotBlank()) {
                Button(
                    onClick = { onOpenProductPage(openUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("打开商品页面", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(9.dp))
            }
            OutlinedButton(
                onClick = onSelectScreenshot,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("选择商品截图", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "建议截图：① 商品标题和价格 ② 详情页宣传 ③ 商品包装参数",
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}
