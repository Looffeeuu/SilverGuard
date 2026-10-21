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
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.silverguard.app.engine.EcommerceLinkParser
import com.silverguard.app.engine.ShareReportBuilder
import com.silverguard.app.model.AnalysisInputMethod
import com.silverguard.app.model.RiskAnalysis
import kotlinx.coroutines.launch

private const val PREFS_NAME = "silverguard_accessibility"
private const val PREF_LARGE_TEXT = "large_text"
private const val PREF_HIGH_CONTRAST = "high_contrast"

@Composable
fun SilverGuardApp(viewModel: SilverGuardViewModel) {
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
                vm = viewModel,
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
    vm: SilverGuardViewModel,
    largeText: Boolean,
    highContrast: Boolean,
    onLargeTextChange: (Boolean) -> Unit,
    onHighContrastChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val contentScroll = rememberScrollState()
    val speaker = vm.speaker
    val detectedLinkInfo = remember(vm.inputText) { EcommerceLinkParser.parse(vm.inputText) }

    fun scrollToTop() {
        coroutineScope.launch { contentScroll.scrollTo(0) }
    }

    fun analyze(text: String, inputMethod: AnalysisInputMethod? = null) {
        focusManager.clearFocus(force = true)
        vm.analyzeInput(text, inputMethod)
    }

    fun returnHome() {
        vm.returnHome()
        focusManager.clearFocus(force = true)
        scrollToTop()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.onAppResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = vm.canGoBack) {
        if (vm.showHistory) {
            vm.closeHistory()
            scrollToTop()
        } else returnHome()
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = vm::onScreenshotPicked
    )

    val officialScreenshotPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = vm::onOfficialScreenshotPicked
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = vm::onPhotoTaken
    )

    if (vm.showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { vm.showResetConfirmation = false },
            title = { Text("要重新查一个商品吗？", fontWeight = FontWeight.Bold) },
            text = { Text("当前分析会保留在本机历史记录中，然后回到首页。大字和高对比设置会保留。") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.showResetConfirmation = false
                        returnHome()
                    },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) { Text("确认重新查") }
            },
            dismissButton = {
                TextButton(
                    onClick = { vm.showResetConfirmation = false },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) { Text("继续看当前结果") }
            }
        )
    }

    if (vm.showAiConsentDialog) {
        AlertDialog(
            onDismissRequest = { vm.onAiConsent(granted = false) },
            title = { Text("使用 AI 深入分析？", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("将发送已经整理的宣传文字和商品字段给智谱 AI，帮助理解话术。")
                    Text("照片不会发送；手机号、邮箱、证件号和联系方式会先隐藏。")
                    Text("AI 可能理解错误，也不会改变本地风险分数或代替官方核验。", color = Muted)
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.onAiConsent(granted = true) },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) { Text("同意并开始分析") }
            },
            dismissButton = {
                TextButton(
                    onClick = { vm.onAiConsent(granted = false) },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) { Text("暂不使用") }
            }
        )
    }

    if (vm.showOfficialReturnDialog) {
        AlertDialog(
            onDismissRequest = { vm.showOfficialReturnDialog = false },
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
                            vm.showOfficialReturnDialog = false
                            officialScreenshotPicker.launch("image/*")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                    ) { Text("选择查询结果截图") }
                    OutlinedButton(
                        onClick = {
                            vm.showOfficialReturnDialog = false
                            Toast.makeText(context, "请在官方核验区逐项填写查询结果", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                    ) { Text("回核验区手动记录") }
                    TextButton(
                        onClick = { vm.showOfficialReturnDialog = false },
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
            Column(Modifier.fillMaxSize()) {
                Surface(color = Background, shadowElevation = 4.dp) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { returnHome() },
                            enabled = vm.canReturnHome,
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("return_home")
                        ) { Text("返回首页", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        Button(
                            onClick = {
                                vm.openHistory()
                                focusManager.clearFocus(force = true)
                                scrollToTop()
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("open_history")
                        ) { Text("历史记录", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(contentScroll)
                    .padding(
                        start = 18.dp,
                        top = 18.dp,
                        end = 18.dp,
                        bottom = 24.dp
                    )
            ) {
                if (vm.showHistory) {
                    HistoryScreen(
                        entries = vm.historyEntries,
                        storageMessage = vm.historyStorageMessage,
                        onOpen = { entry ->
                            vm.openHistoryEntry(entry)
                            focusManager.clearFocus(force = true)
                            scrollToTop()
                        },
                        onDelete = vm::deleteHistoryEntry,
                        onClear = {
                            vm.clearHistory()
                            scrollToTop()
                        }
                    )
                } else {
                BrandHeader()
                Spacer(Modifier.height(12.dp))
                AccessibilityControls(
                    largeText = largeText,
                    highContrast = highContrast,
                    onLargeTextChange = onLargeTextChange,
                    onHighContrastChange = onHighContrastChange
                )
                if (vm.analysis == null) {
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
                            vm.prepareCapture(supplement = false)
                            cameraLauncher.launch(null)
                        }
                    )
                    Spacer(Modifier.height(9.dp))
                    PrimaryEntryButton(
                        text = "选择商品截图",
                        testTag = "select_product_screenshot",
                        onClick = {
                            vm.prepareScreenshot(supplement = false)
                            imagePicker.launch("image/*")
                        }
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
                        ImagePreview(context = context, uri = selectedUri)
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
                        onClick = { if (vm.inputText.isNotBlank()) analyze(vm.inputText) },
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
                        ExampleButtons { sample ->
                            focusManager.clearFocus(force = true)
                            vm.analyzeExample(sample)
                        }
                    }
                }

                vm.analysis?.let { current ->
                    Spacer(Modifier.height(16.dp))
                    vm.historyStorageMessage?.let { Text(it, color = Ink, fontWeight = FontWeight.Bold) }
                    vm.restoredHistoryTime?.let { time ->
                        Card(colors = CardDefaults.cardColors(containerColor = SoftAmber, contentColor = Ink), border = CardBorder) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("历史结果 · ${historyTime(time)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("history_result_notice"))
                                Text("这是当时保存的分析，不是刚刚重新查询。可继续补充信息，或重新分析。原照片不保留，识别文字仍在下方。", lineHeight = 23.sp)
                                OutlinedButton(onClick = { analyze(vm.inputText) }, enabled = vm.inputText.isNotBlank() && !vm.isProductLoading && !vm.isOcrRunning, modifier = Modifier.heightIn(min = 52.dp).testTag("history_reanalyze")) { Text("用当前文字重新分析") }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    ResultInputComposer(
                        inputText = vm.inputText,
                        inputMethods = vm.analysisInputMethods,
                        hasPendingTextChanges = vm.hasPendingTextChanges,
                        isUpdating = vm.isOcrRunning || vm.isProductLoading,
                        updateMessage = when {
                            vm.isProductLoading -> "正在读取商品链接并更新综合结果…"
                            vm.isOcrRunning -> vm.ocrMessage
                            else -> null
                        },
                        onTextChange = vm::onResultInputChange,
                        onApplyText = {
                            focusManager.clearFocus(force = true)
                            vm.applyResultText()
                        },
                        onTakePhoto = {
                            vm.prepareCapture(supplement = true)
                            cameraLauncher.launch(null)
                        },
                        onSelectScreenshot = {
                            vm.prepareScreenshot(supplement = true)
                            imagePicker.launch("image/*")
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    ResultSection(
                        analysis = current,
                        isAiConfigured = vm.isAiConfigured,
                        isAiAnalyzing = vm.isAiAnalyzing,
                        onAnalyzeWithAi = vm::requestAiAnalysis,
                        onPriceReferenceChange = vm::updatePriceReference,
                        onOpenOfficialSource = { source ->
                            vm.onOfficialSourceOpening(source)?.let { registrationNumber ->
                                copyToClipboard(context, "注册 / 备案号", registrationNumber)
                            }
                            if (!openUrl(context, source.url)) vm.onOfficialSourceOpenFailed()
                        },
                        onSelectOfficialScreenshot = {
                            officialScreenshotPicker.launch("image/*")
                        },
                        isOfficialScreenshotOcrRunning = vm.isOfficialScreenshotOcrRunning,
                        officialScreenshotMessage = vm.officialScreenshotMessage,
                        onRecordSearchOutcome = vm::recordSearchOutcome,
                        onRecordFinding = vm::recordFinding,
                        onOpenProductPage = { openUrl(context, it) },
                        onSelectScreenshot = {
                            vm.prepareScreenshot(supplement = true, cancelProductRead = true)
                            imagePicker.launch("image/*")
                        },
                        inputMethods = vm.analysisInputMethods,
                        onSupplementPhoto = {
                            vm.prepareCapture(supplement = true)
                            cameraLauncher.launch(null)
                        },
                        onSupplementScreenshot = {
                            vm.prepareScreenshot(supplement = true)
                            imagePicker.launch("image/*")
                        },
                        isSupplementRunning = vm.isOcrRunning,
                        supplementMessage = vm.ocrMessage,
                        onSpeakResult = vm::speakResult,
                        onStopSpeaking = vm::stopSpeaking,
                        isSpeaking = speaker.isSpeaking,
                        speechMessage = speaker.unavailableMessage,
                        onReset = { vm.showResetConfirmation = true }
                    )
                }

                if (vm.analysis == null) {
                    Spacer(Modifier.height(24.dp))
                    DisclaimerCard()
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "银龄安心查 · Android MVP 0.5.0 · 本地分析版",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = Muted,
                    fontSize = 12.sp
                )
                }
            }

            vm.analysis?.takeIf { !vm.showHistory }?.let { current ->
                Surface(
                    modifier = Modifier
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
}

@Composable
private fun ResultInputComposer(
    inputText: String,
    inputMethods: Set<AnalysisInputMethod>,
    hasPendingTextChanges: Boolean,
    isUpdating: Boolean,
    updateMessage: String?,
    onTextChange: (String) -> Unit,
    onApplyText: () -> Unit,
    onTakePhoto: () -> Unit,
    onSelectScreenshot: () -> Unit
) {
    val textFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val sourceSummary = inputMethods
        .ifEmpty { setOf(AnalysisInputMethod.TEXT) }
        .joinToString("、") { it.displayName }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        border = CardBorder,
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "继续补充分析信息",
                modifier = Modifier.semantics { heading() },
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "当前已综合：$sourceSummary",
                color = Muted,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                OutlinedButton(
                    onClick = onTakePhoto,
                    enabled = !isUpdating,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(15.dp)
                ) { Text("拍照", fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = onSelectScreenshot,
                    enabled = !isUpdating,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(15.dp)
                ) { Text("截图", fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = {
                        textFocusRequester.requestFocus()
                        keyboardController?.show()
                    },
                    enabled = !isUpdating,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(15.dp)
                ) { Text("文字", fontWeight = FontWeight.Bold) }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChange,
                enabled = !isUpdating,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 132.dp)
                    .focusRequester(textFocusRequester)
                    .testTag("result_input_field"),
                label = { Text("当前用于分析的全部文字，可继续补充") },
                placeholder = { Text("可继续说明价格、商家说法、使用场景等") },
                shape = RoundedCornerShape(17.dp)
            )
            Spacer(Modifier.height(9.dp))
            Button(
                onClick = onApplyText,
                enabled = hasPendingTextChanges && inputText.isNotBlank() && !isUpdating,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (hasPendingTextChanges) "用补充文字更新分析" else "文字没有新修改",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            updateMessage?.let {
                Spacer(Modifier.height(7.dp))
                Text(it, color = Ink, fontWeight = FontWeight.Bold, lineHeight = 21.sp)
            }
            Text(
                "照片、截图和文字会合并到同一份分析中，不会互相覆盖。",
                modifier = Modifier.padding(top = 7.dp),
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
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
        colors = CardDefaults.cardColors(containerColor = SoftGreen, contentColor = Ink),
        border = CardBorder,
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
