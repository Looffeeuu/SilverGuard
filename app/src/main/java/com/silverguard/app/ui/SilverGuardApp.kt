package com.silverguard.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.silverguard.app.engine.RiskAnalyzer
import com.silverguard.app.engine.ShareReportBuilder
import com.silverguard.app.model.RiskAnalysis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SilverGuardApp() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var inputText by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var analysis by remember { mutableStateOf<RiskAnalysis?>(null) }
    var isOcrRunning by remember { mutableStateOf(false) }
    var ocrMessage by remember { mutableStateOf("选择截图后可在手机本地识别文字") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedUri = uri
        capturedBitmap = null
        if (uri != null) {
            isOcrRunning = true
            ocrMessage = "正在识别截图文字…"
            runChineseOcr(
                context = context,
                uri = uri,
                onSuccess = { text ->
                    inputText = text.trim()
                    if (text.isNotBlank()) {
                        focusManager.clearFocus(force = true)
                        analysis = RiskAnalyzer.analyze(text)
                    }
                    isOcrRunning = false
                    ocrMessage = if (text.isBlank()) {
                        "没有识别到文字，可以手动输入商品名或宣传语"
                    } else {
                        "识别完成，可以修改文字后再分析"
                    }
                },
                onError = {
                    isOcrRunning = false
                    ocrMessage = "识别失败：${it.message ?: "未知错误"}"
                }
            )
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            selectedUri = null
            isOcrRunning = true
            ocrMessage = "正在识别拍到的商品文字…"
            runChineseOcr(
                bitmap = bitmap,
                onSuccess = { text ->
                    inputText = text.trim()
                    if (text.isNotBlank()) {
                        focusManager.clearFocus(force = true)
                        analysis = RiskAnalyzer.analyze(text)
                    }
                    isOcrRunning = false
                    ocrMessage = if (text.isBlank()) {
                        "没有识别到文字，可以重新拍照或手动输入"
                    } else {
                        "识别完成，已整理商品信息并生成核验建议"
                    }
                },
                onError = {
                    isOcrRunning = false
                    ocrMessage = "识别失败：${it.message ?: "未知错误"}"
                }
            )
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 18.dp,
                        top = 18.dp,
                        end = 18.dp,
                        bottom = if (analysis == null) 24.dp else 104.dp
                    )
            ) {
                BrandHeader()
                Spacer(Modifier.height(18.dp))
                HeroCard()
                Spacer(Modifier.height(18.dp))

                Text("怎么查？", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("拍商品查", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("选截图查", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(10.dp))
                when {
                    capturedBitmap != null -> {
                        BitmapPreview(bitmap = capturedBitmap!!)
                        Spacer(Modifier.height(8.dp))
                    }
                    selectedUri != null -> {
                        ImagePreview(context = context, uri = selectedUri!!)
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isOcrRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Brand,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(ocrMessage, color = Muted, fontSize = 14.sp)
                }

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    label = { Text("把包装信息或广告里的话写在这里") },
                    placeholder = { Text("例如：商品名、生产企业、注册号和宣传内容") },
                    shape = RoundedCornerShape(18.dp)
                )

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            focusManager.clearFocus(force = true)
                            analysis = RiskAnalyzer.analyze(inputText)
                        }
                    },
                    enabled = inputText.isNotBlank() && !isOcrRunning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("开始分析", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(12.dp))
                ExampleButtons { sample ->
                    focusManager.clearFocus(force = true)
                    inputText = sample
                    analysis = RiskAnalyzer.analyze(sample)
                }

                analysis?.let { current ->
                    Spacer(Modifier.height(24.dp))
                    ResultSection(
                        analysis = current,
                        onOpenOfficialSource = { openUrl(context, it) },
                        onReset = {
                            analysis = null
                            inputText = ""
                            selectedUri = null
                            capturedBitmap = null
                            ocrMessage = "拍商品或选择截图后，可在手机本地识别文字"
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))
                DisclaimerCard()
                Spacer(Modifier.height(24.dp))
                Text(
                    "银龄安心查 · Android MVP 0.3.0",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = Muted,
                    fontSize = 12.sp
                )
            }

            analysis?.let { current ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = Background,
                    shadowElevation = 12.dp
                ) {
                    Button(
                        onClick = { shareAnalysis(context, current) },
                        modifier = Modifier
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("发给家人一起看看", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Brand, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("安", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("银龄安心查", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("消费前，多一次核验", fontSize = 12.sp, color = Muted)
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
                color = Color.White,
                fontSize = 31.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "先看宣传风险，再整理包装信息，并告诉你应该去哪个官方平台人工核对。",
                color = Color(0xFFE8F4ED),
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
            contentDescription = "拍照预览",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.8f)
        )
    }
}

@Composable
private fun ImagePreview(context: Context, uri: Uri) {
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
                contentDescription = "商品截图预览",
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
        Text("可以先点示例体验：", color = Muted, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onExample("太赫兹理疗仪 疏通血管 改善鼻炎 专家推荐 现价￥2980 型号 TD-01") },
                modifier = Modifier.weight(1f)
            ) {
                Text("理疗仪")
            }
            OutlinedButton(
                onClick = { onExample("智能节电器 插上就省电40% 国家专利 仅限今天") },
                modifier = Modifier.weight(1f)
            ) {
                Text("节电器")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onExample("保健品 七天降血糖 不用吃药 教授推荐") },
                modifier = Modifier.weight(1f)
            ) {
                Text("保健品")
            }
            OutlinedButton(
                onClick = { onExample("退休赚钱秘籍 零风险 月入过万 加微信进群") },
                modifier = Modifier.weight(1f)
            ) {
                Text("赚钱秘籍")
            }
        }
    }
}

@Composable
private fun DisclaimerCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F3F0)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            "重要说明：当前版本提供消费风险辅助判断、编号分类和官方人工查询入口，尚未自动匹配国家数据库，不是行政认定、医学诊断或官方认证。",
            modifier = Modifier.padding(16.dp),
            color = Muted,
            lineHeight = 21.sp,
            fontSize = 13.sp
        )
    }
}

private fun runChineseOcr(
    bitmap: Bitmap,
    onSuccess: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    try {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(
            ChineseTextRecognizerOptions.Builder().build()
        )
        recognizer.process(image)
            .addOnSuccessListener { result ->
                onSuccess(result.text)
                recognizer.close()
            }
            .addOnFailureListener { error ->
                onError(error)
                recognizer.close()
            }
    } catch (error: Exception) {
        onError(error)
    }
}

private fun runChineseOcr(
    context: Context,
    uri: Uri,
    onSuccess: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    try {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(
            ChineseTextRecognizerOptions.Builder().build()
        )
        recognizer.process(image)
            .addOnSuccessListener { result ->
                onSuccess(result.text)
                recognizer.close()
            }
            .addOnFailureListener { error ->
                onError(error)
                recognizer.close()
            }
    } catch (error: Exception) {
        onError(error)
    }
}

private fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

private fun shareAnalysis(context: Context, analysis: RiskAnalysis) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, ShareReportBuilder.build(analysis))
    }
    context.startActivity(Intent.createChooser(intent, "发给家人"))
}
