package com.silverguard.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.silverguard.app.engine.CaptureGuidanceEvaluator
import com.silverguard.app.engine.EcommerceLinkParser
import com.silverguard.app.engine.ManualVerificationRecorder
import com.silverguard.app.engine.OfficialScreenshotAnalyzer
import com.silverguard.app.engine.RiskAnalyzer
import com.silverguard.app.engine.ShareReportBuilder
import com.silverguard.app.engine.SpeechSummaryBuilder
import com.silverguard.app.model.RiskAnalysis
import com.silverguard.app.network.TaobaoTmallProductResolver
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val PREFS_NAME = "silverguard_accessibility"
private const val PREF_LARGE_TEXT = "large_text"
private const val PREF_HIGH_CONTRAST = "high_contrast"

@Composable
fun SilverGuardApp() {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    var largeText by rememberSaveable {
        mutableStateOf(preferences.getBoolean(PREF_LARGE_TEXT, false))
    }
    var highContrast by rememberSaveable {
        mutableStateOf(preferences.getBoolean(PREF_HIGH_CONTRAST, false))
    }
    val currentDensity = LocalDensity.current
    val appDensity = remember(currentDensity.density, currentDensity.fontScale, largeText) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * if (largeText) 1.18f else 1f
        )
    }
    val palette = if (highContrast) HighContrastSilverGuardColors else StandardSilverGuardColors

    CompositionLocalProvider(LocalDensity provides appDensity) {
        SilverGuardPaletteTheme(palette) {
            SilverGuardContent(
                largeText = largeText,
                highContrast = highContrast,
                onLargeTextChange = { enabled ->
                    largeText = enabled
                    preferences.edit().putBoolean(PREF_LARGE_TEXT, enabled).apply()
                },
                onHighContrastChange = { enabled ->
                    highContrast = enabled
                    preferences.edit().putBoolean(PREF_HIGH_CONTRAST, enabled).apply()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SilverGuardContent(
    largeText: Boolean,
    highContrast: Boolean,
    onLargeTextChange: (Boolean) -> Unit,
    onHighContrastChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val productResolver = remember { TaobaoTmallProductResolver() }
    val speaker = rememberResultSpeaker()
    val lifecycleOwner = LocalLifecycleOwner.current

    var inputText by rememberSaveable { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var analysis by remember { mutableStateOf<RiskAnalysis?>(null) }
    var isOcrRunning by remember { mutableStateOf(false) }
    var isProductLoading by remember { mutableStateOf(false) }
    var analysisJob by remember { mutableStateOf<Job?>(null) }
    var analysisRequestVersion by remember { mutableStateOf(0) }
    var ocrMessage by remember { mutableStateOf("拍商品或选择截图后，可在手机本地识别文字") }
    var isOfficialScreenshotOcrRunning by remember { mutableStateOf(false) }
    var officialScreenshotMessage by remember { mutableStateOf("") }
    var showManualInput by rememberSaveable { mutableStateOf(false) }
    var showExamples by rememberSaveable { mutableStateOf(false) }
    var isSupplementCapture by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var waitingForOfficialReturn by remember { mutableStateOf(false) }
    var officialPageWasOpened by remember { mutableStateOf(false) }
    var showOfficialReturnDialog by remember { mutableStateOf(false) }
    val detectedLinkInfo = remember(inputText) { EcommerceLinkParser.parse(inputText) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (
                event == Lifecycle.Event.ON_RESUME &&
                waitingForOfficialReturn &&
                officialPageWasOpened
            ) {
                showOfficialReturnDialog = true
                waitingForOfficialReturn = false
                officialPageWasOpened = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun cancelProductRead() {
        analysisRequestVersion += 1
        analysisJob?.cancel()
        analysisJob = null
        isProductLoading = false
    }

    fun analyzeInput(text: String) {
        val requestText = text.trim()
        if (requestText.isBlank()) return
        cancelProductRead()
        speaker.stop()
        isOfficialScreenshotOcrRunning = false
        officialScreenshotMessage = ""
        focusManager.clearFocus(force = true)
        val requestId = analysisRequestVersion
        val linkInfo = EcommerceLinkParser.parse(requestText)

        if (!productResolver.canResolve(linkInfo)) {
            analysis = RiskAnalyzer.analyze(requestText)
            return
        }

        isProductLoading = true
        analysis = null
        analysisJob = coroutineScope.launch {
            val product = productResolver.resolve(linkInfo)
            if (analysisRequestVersion == requestId) {
                analysis = RiskAnalyzer.analyze(requestText, product)
                isProductLoading = false
                analysisJob = null
            }
        }
    }

    fun resetAll() {
        cancelProductRead()
        speaker.stop()
        analysis = null
        inputText = ""
        selectedUri = null
        capturedBitmap = null
        isOfficialScreenshotOcrRunning = false
        officialScreenshotMessage = ""
        ocrMessage = "拍商品或选择截图后，可在手机本地识别文字"
        showManualInput = false
        isSupplementCapture = false
    }

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
                    showManualInput = true
                    if (text.isNotBlank()) analyzeInput(text)
                    isOcrRunning = false
                    ocrMessage = if (text.isBlank()) {
                        "没有识别到文字，可以手动输入商品名或宣传语"
                    } else {
                        "识别完成，可以修改文字后再分析"
                    }
                },
                onError = {
                    isOcrRunning = false
                    ocrMessage = "识别失败：${it.message ?: "未知错误"}。原有文字没有被清除。"
                }
            )
        }
    }

    val officialScreenshotPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val current = analysis
        if (uri != null && current?.verification?.manualRecord != null) {
            isOfficialScreenshotOcrRunning = true
            officialScreenshotMessage = "正在本地识别官方查询截图…"
            val requestedRegistration = current.verification.registration
                .normalizedNumber
                .ifBlank { current.verification.registration.rawNumber }
            runChineseOcr(
                context = context,
                uri = uri,
                onSuccess = { text ->
                    val latest = analysis
                    val latestRegistration = latest?.let { latestAnalysis ->
                        latestAnalysis.verification.registration.normalizedNumber.ifBlank {
                            latestAnalysis.verification.registration.rawNumber
                        }
                    }
                    if (
                        latest != null &&
                        latest.verification.manualRecord != null &&
                        latestRegistration == requestedRegistration
                    ) {
                        val review = OfficialScreenshotAnalyzer.analyze(text, latest.verification)
                        analysis = latest.copy(
                            verification = ManualVerificationRecorder.attachScreenshotReview(
                                latest.verification,
                                review
                            )
                        )
                        officialScreenshotMessage = review.summary
                    } else {
                        officialScreenshotMessage = "商品信息已经变化，请重新选择官方查询截图。"
                    }
                    isOfficialScreenshotOcrRunning = false
                },
                onError = { error ->
                    isOfficialScreenshotOcrRunning = false
                    officialScreenshotMessage = "截图识别失败：${error.message ?: "未知错误"}。当前分析仍然保留。"
                }
            )
        } else if (uri != null) {
            officialScreenshotMessage = "请先打开一个官方查询入口，再导入查询结果截图。"
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val supplement = isSupplementCapture
            capturedBitmap = bitmap
            selectedUri = null
            isOcrRunning = true
            ocrMessage = if (supplement) "正在把补拍文字合并到当前商品…" else "正在识别拍到的商品文字…"
            runChineseOcr(
                bitmap = bitmap,
                onSuccess = { text ->
                    val combined = if (supplement) {
                        CaptureGuidanceEvaluator.mergeRecognizedText(inputText, text)
                    } else {
                        text.trim()
                    }
                    if (combined.isNotBlank()) {
                        inputText = combined
                        showManualInput = true
                        analyzeInput(combined)
                    }
                    isOcrRunning = false
                    isSupplementCapture = false
                    ocrMessage = when {
                        text.isBlank() && supplement -> "补拍照片没有识别到新文字，当前分析仍然保留"
                        text.isBlank() -> "没有识别到文字，可以重新拍照或手动输入"
                        supplement -> "补拍信息已合并，并重新生成分析结果"
                        else -> "识别完成，已整理商品信息并生成核验建议"
                    }
                },
                onError = {
                    isOcrRunning = false
                    isSupplementCapture = false
                    ocrMessage = "识别失败：${it.message ?: "未知错误"}。当前文字和结果没有被清除。"
                }
            )
        } else {
            isSupplementCapture = false
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("要重新查一个商品吗？", fontWeight = FontWeight.Bold) },
            text = { Text("当前分析和人工核对记录会被清空。大字和高对比设置会保留。") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmation = false
                        resetAll()
                    },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) { Text("确认重新查") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmation = false },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) { Text("继续看当前结果") }
            }
        )
    }

    if (showOfficialReturnDialog) {
        AlertDialog(
            onDismissRequest = { showOfficialReturnDialog = false },
            title = { Text("已经完成官方查询了吗？", fontWeight = FontWeight.Bold) },
            text = {
                Text("如果已经查到结果，建议先截图，再回到银龄安心查继续核对。")
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showOfficialReturnDialog = false
                            officialScreenshotPicker.launch("image/*")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                    ) { Text("选择查询结果截图") }
                    OutlinedButton(
                        onClick = {
                            showOfficialReturnDialog = false
                            Toast.makeText(context, "请在官方核验区逐项填写查询结果", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                    ) { Text("回核验区手动记录") }
                    TextButton(
                        onClick = { showOfficialReturnDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                    ) { Text("还没有查完") }
                }
            }
        )
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
                        bottom = if (analysis == null) 24.dp else 116.dp
                    )
            ) {
                BrandHeader()
                Spacer(Modifier.height(12.dp))
                AccessibilityControls(
                    largeText = largeText,
                    highContrast = highContrast,
                    onLargeTextChange = onLargeTextChange,
                    onHighContrastChange = onHighContrastChange
                )
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
                    onClick = {
                        cancelProductRead()
                        isSupplementCapture = false
                        cameraLauncher.launch(null)
                    }
                )
                Spacer(Modifier.height(9.dp))
                PrimaryEntryButton(
                    text = "选择商品截图",
                    testTag = "select_product_screenshot",
                    onClick = {
                        cancelProductRead()
                        imagePicker.launch("image/*")
                    }
                )
                Spacer(Modifier.height(9.dp))
                PrimaryEntryButton(
                    text = "粘贴商品链接 / 手动输入",
                    testTag = "manual_product_input",
                    onClick = { showManualInput = true }
                )

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
                            modifier = Modifier.size(20.dp),
                            color = Brand,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(ocrMessage, color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
                }

                if (showManualInput || inputText.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            cancelProductRead()
                            speaker.stop()
                            analysis = null
                            inputText = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 190.dp),
                        label = { Text("商品信息、宣传文字或商品链接") },
                        placeholder = {
                            Text("例如：太赫兹理疗仪 改善鼻炎 2980元\n\n也可以粘贴淘宝、天猫、拼多多、京东或抖音商品链接")
                        },
                        shape = RoundedCornerShape(18.dp)
                    )

                    Spacer(Modifier.height(10.dp))
                    EcommerceLinkCard(detectedLinkInfo)
                    if (detectedLinkInfo.extractedUrl != null) Spacer(Modifier.height(10.dp))
                    if (isProductLoading) {
                        ProductLoadingCard()
                        Spacer(Modifier.height(10.dp))
                    }
                    Button(
                        onClick = { if (inputText.isNotBlank()) analyzeInput(inputText) },
                        enabled = inputText.isNotBlank() && !isOcrRunning && !isProductLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            if (isProductLoading) "正在读取商品信息…" else "开始分析",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { showExamples = !showExamples },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(if (showExamples) "收起示例" else "查看输入示例")
                }
                if (showExamples) {
                    ExampleButtons { sample ->
                        inputText = sample
                        showManualInput = true
                        analyzeInput(sample)
                    }
                }

                analysis?.let { current ->
                    Spacer(Modifier.height(24.dp))
                    ResultSection(
                        analysis = current,
                        onOpenOfficialSource = { source ->
                            val latest = analysis ?: current
                            analysis = latest.copy(
                                verification = ManualVerificationRecorder.markSourceOpened(
                                    latest.verification,
                                    source
                                )
                            )
                            officialScreenshotMessage = "查询完成并截图后，请返回这里继续。"
                            val registrationNumber = latest.verification.registration
                                .normalizedNumber
                                .ifBlank { latest.verification.registration.rawNumber }
                            if (registrationNumber.isNotBlank()) {
                                copyToClipboard(context, "注册 / 备案号", registrationNumber)
                            }
                            waitingForOfficialReturn = true
                            officialPageWasOpened = true
                            if (!openUrl(context, source.url)) {
                                waitingForOfficialReturn = false
                                officialPageWasOpened = false
                            }
                        },
                        onSelectOfficialScreenshot = {
                            officialScreenshotPicker.launch("image/*")
                        },
                        isOfficialScreenshotOcrRunning = isOfficialScreenshotOcrRunning,
                        officialScreenshotMessage = officialScreenshotMessage,
                        onRecordSearchOutcome = { outcome ->
                            val latest = analysis ?: current
                            analysis = latest.copy(
                                verification = ManualVerificationRecorder.recordSearchOutcome(
                                    latest.verification,
                                    outcome
                                )
                            )
                        },
                        onRecordFinding = { field, status ->
                            val latest = analysis ?: current
                            analysis = latest.copy(
                                verification = ManualVerificationRecorder.recordFinding(
                                    latest.verification,
                                    field,
                                    status
                                )
                            )
                        },
                        onOpenProductPage = { openUrl(context, it) },
                        onSelectScreenshot = {
                            cancelProductRead()
                            imagePicker.launch("image/*")
                        },
                        onSupplementPhoto = {
                            isSupplementCapture = true
                            cameraLauncher.launch(null)
                        },
                        onSpeakResult = {
                            speaker.speak(SpeechSummaryBuilder.build(analysis ?: current))
                        },
                        onStopSpeaking = speaker::stop,
                        isSpeaking = speaker.isSpeaking,
                        speechMessage = speaker.unavailableMessage,
                        onReset = { showResetConfirmation = true }
                    )
                }

                Spacer(Modifier.height(24.dp))
                DisclaimerCard()
                Spacer(Modifier.height(24.dp))
                Text(
                    "银龄安心查 · Android MVP 0.3.5",
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
                            .heightIn(min = 60.dp),
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
private fun AccessibilityControls(
    largeText: Boolean,
    highContrast: Boolean,
    onLargeTextChange: (Boolean) -> Unit,
    onHighContrastChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { onLargeTextChange(!largeText) },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 52.dp)
                .testTag("large_text_toggle")
                .semantics {
                    stateDescription = if (largeText) "大字模式已开启" else "大字模式未开启"
                },
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (largeText) "大字：开" else "大字：关", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = { onHighContrastChange(!highContrast) },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 52.dp)
                .testTag("high_contrast_toggle")
                .semantics {
                    stateDescription = if (highContrast) "高对比模式已开启" else "高对比模式未开启"
                },
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (highContrast) "高对比：开" else "高对比：关", fontWeight = FontWeight.Bold)
        }
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
            Text(
                "银龄安心查",
                modifier = Modifier.semantics { heading() },
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
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
        Text("点一个示例体验：", color = Muted, fontSize = 13.sp)
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
private fun DisclaimerCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftGreen),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            "重要说明：分析用于消费风险辅助判断，不代表行政认定、医学诊断或官方认证。图片和官方查询截图只在手机本地识别；本版不会伪装成已经自动查询国家数据库。",
            modifier = Modifier.padding(16.dp),
            color = Ink,
            lineHeight = 22.sp,
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
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
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
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
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

private fun openUrl(context: Context, url: String): Boolean = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    true
} catch (_: Exception) {
    Toast.makeText(context, "没有找到可以打开该页面的应用", Toast.LENGTH_LONG).show()
    false
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "编号已复制，可粘贴到官方查询框", Toast.LENGTH_SHORT).show()
}

private fun shareAnalysis(context: Context, analysis: RiskAnalysis) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, ShareReportBuilder.build(analysis))
    }
    context.startActivity(Intent.createChooser(intent, "发给家人"))
}
