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
import androidx.compose.material3.MaterialTheme
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
import com.silverguard.app.data.OfficialSources
import com.silverguard.app.engine.RiskAnalyzer
import com.silverguard.app.model.ProductInfo
import com.silverguard.app.model.RiskAnalysis
import com.silverguard.app.model.RiskLevel

private val Brand = Color(0xFF176B4D)
private val Background = Color(0xFFF6F8F5)
private val SoftGreen = Color(0xFFE8F3ED)
private val SoftRed = Color(0xFFFFF0EE)
private val SoftAmber = Color(0xFFFFF7DB)
private val Ink = Color(0xFF16231D)
private val Muted = Color(0xFF68756E)

@Composable
fun SilverGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

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
                        "识别完成，已经自动做了第一轮风险分析"
                    }
                },
                onError = {
                    isOcrRunning = false
                    ocrMessage = "识别失败：${it.message ?: "未知错误"}"
                }
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Background
    ) {
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

                Text(
                    text = "怎么查？",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
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

                if (capturedBitmap != null) {
                    BitmapPreview(bitmap = capturedBitmap!!)
                    Spacer(Modifier.height(8.dp))
                } else if (selectedUri != null) {
                    ImagePreview(context = context, uri = selectedUri!!)
                    Spacer(Modifier.height(8.dp))
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
                    Text(
                        text = ocrMessage,
                        color = Muted,
                        fontSize = 14.sp
                    )
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    label = { Text("把商品名字或广告里的话写在这里") },
                    placeholder = {
                        Text("例如：七天降血糖，不用吃药，专家推荐")
                    },
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

                analysis?.let {
                    Spacer(Modifier.height(24.dp))
                    ResultSection(
                        context = context,
                        analysis = it,
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
                    text = "银龄安心查 · Android MVP 0.2.2",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = Muted,
                    fontSize = 12.sp
                )
            }

            analysis?.let { currentAnalysis ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = Background,
                    shadowElevation = 12.dp
                ) {
                    Button(
                        onClick = { shareAnalysis(context, currentAnalysis) },
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
                text = "这个东西，\n买之前先查一下。",
                color = Color.White,
                fontSize = 31.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "拍商品截图，或者输入商品名称。先找出夸大宣传和高风险承诺，再决定要不要付款。",
                color = Color(0xFFE8F4ED),
                fontSize = 16.sp,
                lineHeight = 25.sp
            )
        }
    }
}

@Composable
private fun BitmapPreview(bitmap: Bitmap) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
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
private fun ResultSection(
    context: Context,
    analysis: RiskAnalysis,
    onReset: () -> Unit
) {
    val bg = when (analysis.level) {
        RiskLevel.HIGH -> SoftRed
        RiskLevel.MEDIUM -> SoftAmber
        RiskLevel.LOW -> SoftGreen
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("初步分类 · ${analysis.category}", color = Muted, fontSize = 13.sp)
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
            Spacer(Modifier.height(8.dp))
            Text(
                if (analysis.flags.isEmpty()) {
                    "当前规则库没有发现明显高风险话术，但这不等于商品一定安全或值得购买。"
                } else {
                    "发现 ${analysis.flags.size} 类需要注意的宣传信号。"
                },
                color = Ink,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(10.dp))
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = riskSummary(analysis),
                    modifier = Modifier.padding(13.dp),
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.sp
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    InfoCard("① 商品信息（自动整理）") {
        ProductInfoSection(
            category = analysis.category,
            productInfo = analysis.productInfo
        )
    }

    Spacer(Modifier.height(12.dp))
    InfoCard("② 哪些地方需要警惕？") {
        if (analysis.flags.isEmpty()) {
            Text(
                "没有命中当前规则。下一版还需要检查企业、注册/备案、抽检、召回、处罚和价格异常。",
                color = Muted
            )
        } else {
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
    }

    Spacer(Modifier.height(12.dp))
    InfoCard("③ 国家有没有记录？") {
        Surface(
            color = SoftAmber,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "尚未自动核验",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = Ink,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(9.dp))
        Text(
            "当前版本还不会自动查询国家数据库。请点击下面的真实官方入口，由你或家人继续核对；下一版会把这里升级成自动匹配。",
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(8.dp))
        OfficialSources.forCategory(analysis.category).forEach { source ->
            OutlinedButton(
                onClick = { openUrl(context, source.url) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(source.name, fontWeight = FontWeight.Bold)
                    Text(source.description, fontSize = 12.sp, color = Muted)
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        Text(
            "注意：查到备案/注册 ≠ 广告里所有功效都得到国家认可。",
            fontSize = 13.sp,
            color = Muted
        )
    }

    Spacer(Modifier.height(12.dp))
    InfoCard("④ 我现在应该怎么办？") {
        val advice = when (analysis.level) {
            RiskLevel.HIGH ->
                "先不要付款。保存商品名、生产企业、型号和注册/备案编号，再核对官方记录；涉及疾病治疗时，不要因为广告自行停药或替代正规治疗。"
            RiskLevel.MEDIUM ->
                "先核对资质和完整商品信息，再决定。不要只看“专家推荐、国家专利、用户案例”等宣传。"
            RiskLevel.LOW ->
                "目前没有触发明显高风险词，但仍建议核对资质、价格、抽检、召回等信息。"
        }
        Text(advice, lineHeight = 23.sp)

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("重新查一个")
        }
    }
}

@Composable
private fun ProductInfoSection(
    category: String,
    productInfo: ProductInfo
) {
    Surface(
        color = SoftGreen,
        shape = RoundedCornerShape(12.dp)
    ) {
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
        text = "以上内容由文字规则自动整理，不代表官方记录或资质核验结果。",
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
        Text(
            text = label,
            modifier = Modifier.width(108.dp),
            color = Muted,
            fontSize = 14.sp
        )
        Text(
            text = value ?: "未识别",
            modifier = Modifier.weight(1f),
            color = if (value == null) Muted else Ink,
            fontWeight = if (value == null) FontWeight.Normal else FontWeight.SemiBold,
            lineHeight = 21.sp
        )
    }
}

private fun riskSummary(analysis: RiskAnalysis): String {
    if (analysis.flags.isEmpty()) {
        return "为什么仍要核验：没有命中明显风险词，不代表商品资质、功效和价格已经得到官方确认。"
    }

    val mainSignals = analysis.flags
        .take(2)
        .joinToString("、") { it.type }

    return when (analysis.level) {
        RiskLevel.HIGH ->
            "为什么风险高：同时发现“$mainSignals”等强风险信号。建议先暂停付款，不要因广告自行停药或替代正规治疗。"
        RiskLevel.MEDIUM ->
            "为什么需要谨慎：发现“$mainSignals”等可疑宣传，需要先核对商品资质和完整信息。"
        RiskLevel.LOW ->
            "为什么仍要核验：发现的风险信号较少，但当前结果还没有自动查询官方记录。"
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

@Composable
private fun DisclaimerCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F3F0)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = "重要说明：当前版本是消费风险初筛，不是行政认定、医学诊断或商品鉴定。正式版本应显示数据来源、查询时间、产品/企业匹配依据和置信度。",
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
    } catch (e: Exception) {
        onError(e)
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
    } catch (e: Exception) {
        onError(e)
    }
}

private fun openUrl(context: Context, url: String) {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url))
    )
}

private fun shareAnalysis(context: Context, analysis: RiskAnalysis) {
    val flags = if (analysis.flags.isEmpty()) {
        "当前规则未发现明显高风险词"
    } else {
        analysis.flags.joinToString("、") { "${it.type}（${it.matched}）" }
    }

    val info = analysis.productInfo
    val productDetails = listOfNotNull(
        info.name?.let { "商品名称：$it" },
        info.brand?.let { "品牌：$it" },
        info.manufacturer?.let { "生产企业：$it" },
        info.model?.let { "型号：$it" },
        info.specification?.let { "规格/净含量：$it" },
        info.registrationNumber?.let { "注册/备案号：$it" },
        info.price?.let { "价格：$it" }
    ).ifEmpty { listOf("暂未提取到明确的包装字段") }
        .joinToString("\n")

    val report = """
        【银龄安心查 · 消费风险初筛】
        ${analysis.title}
        风险分：${analysis.score}/100
        初步分类：${analysis.category}

        【自动整理的商品信息】
        $productDetails

        风险信号：$flags

        建议：付款前再到国家药监局、市场监管总局等官方平台核验具体资质。
        注：商品信息来自文字自动提取，请对照包装核对；风险结果不是行政认定或医学诊断。
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, report)
    }
    context.startActivity(Intent.createChooser(intent, "发给家人"))
}
