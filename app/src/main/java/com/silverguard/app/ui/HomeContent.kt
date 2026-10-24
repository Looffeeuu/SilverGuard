package com.silverguard.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverguard.app.engine.EcommerceLinkParser

/** The start page before any analysis: entry points, the OCR preview and the manual input box. */
@Composable
internal fun HomeContent(
    vm: SilverGuardViewModel,
    onTakePhoto: () -> Unit,
    onPickScreenshot: () -> Unit,
    onAnalyze: (String) -> Unit,
    onExample: (String) -> Unit
) {
    val detectedLinkInfo = remember(vm.inputText) { EcommerceLinkParser.parse(vm.inputText) }

    Spacer(Modifier.height(16.dp))
    HeroCard()
    Spacer(Modifier.height(18.dp))

    Text(
        "选择一种查询方式",
        modifier = Modifier.semantics { heading() },
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = Ink
    )
    Spacer(Modifier.height(10.dp))
    PrimaryEntryButton(
        text = "拍商品包装",
        testTag = "take_product_photo",
        filled = true,
        onClick = onTakePhoto
    )
    Spacer(Modifier.height(9.dp))
    PrimaryEntryButton(
        text = "选择商品截图",
        testTag = "select_product_screenshot",
        onClick = onPickScreenshot
    )
    Spacer(Modifier.height(9.dp))
    PrimaryEntryButton(
        text = "粘贴商品链接 / 手动输入",
        testTag = "manual_product_input",
        onClick = { vm.showManualInput = true }
    )

    Spacer(Modifier.height(10.dp))
    val capturedBitmap = vm.capturedBitmap
    val selectedUri = vm.selectedUri
    when {
        capturedBitmap != null -> {
            BitmapPreview(bitmap = capturedBitmap)
            Spacer(Modifier.height(8.dp))
        }
        selectedUri != null -> {
            ImagePreview(uri = selectedUri)
            Spacer(Modifier.height(8.dp))
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (vm.isOcrRunning) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Brand,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(vm.ocrMessage, color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
    }

    if (vm.showManualInput || vm.inputText.isNotBlank()) {
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = vm.inputText,
            onValueChange = vm::onHomeInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 190.dp)
                .testTag("home_input_field"),
            label = { Text("商品信息、宣传文字或商品链接") },
            placeholder = {
                Text("例如：太赫兹理疗仪 改善鼻炎 2980元\n\n也可以粘贴淘宝、天猫、拼多多、京东或抖音商品链接")
            },
            shape = RoundedCornerShape(18.dp)
        )

        Spacer(Modifier.height(10.dp))
        EcommerceLinkCard(detectedLinkInfo)
        if (detectedLinkInfo.extractedUrl != null) Spacer(Modifier.height(10.dp))
        if (vm.isProductLoading) {
            ProductLoadingCard()
            Spacer(Modifier.height(10.dp))
        }
        Button(
            onClick = { if (vm.inputText.isNotBlank()) onAnalyze(vm.inputText) },
            enabled = vm.inputText.isNotBlank() && !vm.isOcrRunning && !vm.isProductLoading,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .testTag("start_analysis"),
            colors = ButtonDefaults.buttonColors(containerColor = Brand),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                if (vm.isProductLoading) "正在读取商品信息…" else "开始分析",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    TextButton(
        onClick = { vm.showExamples = !vm.showExamples },
        modifier = Modifier.heightIn(min = 48.dp)
    ) {
        Text(if (vm.showExamples) "收起示例" else "查看输入示例")
    }
    if (vm.showExamples) {
        ExampleButtons(onExample)
    }
}

@Composable
private fun PrimaryEntryButton(
    text: String,
    testTag: String,
    filled: Boolean = false,
    onClick: () -> Unit
) {
    if (filled) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .testTag(testTag),
            colors = ButtonDefaults.buttonColors(containerColor = Brand),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .testTag(testTag),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HeroCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Brand),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(
                "这个东西，\n买之前先查一下。",
                modifier = Modifier.semantics { heading() },
                color = Color.White,
                fontSize = 31.sp,
                lineHeight = 39.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "拍包装、选截图或粘贴链接。先看宣传风险，再告诉你如何去官方平台核对。",
                color = Color(0xFFF2FFF7),
                fontSize = 16.sp,
                lineHeight = 25.sp
            )
        }
    }
}

@Composable
private fun BitmapPreview(bitmap: Bitmap) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "刚拍摄的商品包装预览",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.8f)
        )
    }
}

@Composable
private fun ImagePreview(uri: Uri) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }
    }
    bitmap?.let {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "已选择的商品截图预览",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.8f)
            )
        }
    }
}

@Composable
private fun ExampleButtons(onExample: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("点一个示例体验：", color = Muted, fontSize = 14.sp)
        listOf(
            "理疗仪" to "太赫兹理疗仪 疏通血管 改善鼻炎 专家推荐 现价￥2980 型号 TD-01",
            "节电器" to "智能节电器 插上就省电40% 国家专利 仅限今天",
            "保健品" to "保健品 七天降血糖 不用吃药 教授推荐",
            "赚钱秘籍" to "退休赚钱秘籍 零风险 月入过万 加微信进群"
        ).chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { (label, sample) ->
                    OutlinedButton(
                        onClick = { onExample(sample) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                    ) { Text(label) }
                }
            }
        }
    }
}

@Composable
internal fun DisclaimerCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftGreen, contentColor = Ink),
        border = CardBorder,
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            "重要说明：分析用于消费风险辅助判断，不代表行政认定、医学诊断或官方认证。图片和官方查询截图只在手机本地识别；本版不会伪装成已经自动查询国家数据库。",
            modifier = Modifier.padding(16.dp),
            color = Ink,
            lineHeight = 22.sp,
            fontSize = 14.sp
        )
    }
}
