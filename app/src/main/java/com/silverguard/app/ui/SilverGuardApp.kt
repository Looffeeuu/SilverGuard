package com.silverguard.app.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.silverguard.app.model.AnalysisInputMethod
import kotlinx.coroutines.launch

private const val PREFS_NAME = "silverguard_accessibility"
private const val PREF_LARGE_TEXT = "large_text"
private const val PREF_HIGH_CONTRAST = "high_contrast"
private const val LARGE_TEXT_FACTOR = 1.18f
private const val MAX_NON_LINEAR_FONT_SCALE = 2f

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
    val appDensity = remember(currentDensity, largeText) {
        if (!largeText) {
            currentDensity
        } else {
            // Android's non-linear font scaling, which enlarges headings less than body text, only has
            // tables up to 2.0; beyond that text scales linearly and headings balloon. So the app's own
            // large-text step stops at 2.0 and adds nothing once the system font size is at its maximum.
            Density(
                density = currentDensity.density,
                fontScale = minOf(currentDensity.fontScale * LARGE_TEXT_FACTOR, MAX_NON_LINEAR_FONT_SCALE)
            )
        }
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

/**
 * Screen frame and the wiring that needs Compose: activity-result launchers, the back button,
 * lifecycle, focus and scrolling. The pages themselves are in HomeContent, ResultContent and HistoryScreen.
 */
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

    fun takePhoto(supplement: Boolean) {
        vm.prepareCapture(supplement)
        cameraLauncher.launch(null)
    }

    fun pickScreenshot(supplement: Boolean, cancelProductRead: Boolean = !supplement) {
        vm.prepareScreenshot(supplement, cancelProductRead)
        imagePicker.launch("image/*")
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

    AppDialogs(
        vm = vm,
        onConfirmReset = { returnHome() },
        onPickOfficialScreenshot = { officialScreenshotPicker.launch("image/*") }
    )

    Surface(modifier = Modifier.fillMaxSize(), color = Background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(Modifier.fillMaxSize()) {
                TopBar(
                    canReturnHome = vm.canReturnHome,
                    onReturnHome = { returnHome() },
                    onOpenHistory = {
                        vm.openHistory()
                        focusManager.clearFocus(force = true)
                        scrollToTop()
                    }
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(contentScroll)
                        .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 24.dp)
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
                        val current = vm.analysis
                        if (current == null) {
                            HomeContent(
                                vm = vm,
                                onTakePhoto = { takePhoto(supplement = false) },
                                onPickScreenshot = { pickScreenshot(supplement = false) },
                                onAnalyze = { text -> analyze(text) },
                                onExample = { sample ->
                                    focusManager.clearFocus(force = true)
                                    vm.analyzeExample(sample)
                                }
                            )
                            Spacer(Modifier.height(24.dp))
                            DisclaimerCard()
                        } else {
                            ResultContent(
                                vm = vm,
                                current = current,
                                onAnalyze = { text -> analyze(text) },
                                onApplyText = {
                                    focusManager.clearFocus(force = true)
                                    vm.applyResultText()
                                },
                                onTakePhoto = { takePhoto(supplement = true) },
                                onPickScreenshot = { cancelProductRead ->
                                    pickScreenshot(supplement = true, cancelProductRead = cancelProductRead)
                                },
                                onPickOfficialScreenshot = { officialScreenshotPicker.launch("image/*") }
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "银龄安心查 · Android MVP 0.5.0 · 本地分析版",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = Muted,
                            fontSize = 14.sp
                        )
                    }
                }

                vm.analysis?.takeIf { !vm.showHistory }?.let { current ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
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
private fun TopBar(canReturnHome: Boolean, onReturnHome: () -> Unit, onOpenHistory: () -> Unit) {
    Surface(color = Background, shadowElevation = 4.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onReturnHome,
                enabled = canReturnHome,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("return_home")
            ) { Text("返回首页", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            Button(
                onClick = onOpenHistory,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("open_history")
            ) { Text("历史记录", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Decorative logo: TalkBack would otherwise read a lone "安" before the app name.
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Brand, RoundedCornerShape(14.dp))
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center
        ) {
            // Sized in dp so it never outgrows the fixed 44dp box at large font settings.
            Text(
                "安",
                color = Color.White,
                fontSize = with(LocalDensity.current) { 21.dp.toSp() },
                fontWeight = FontWeight.Bold
            )
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
            Text("消费前，多一次核验", fontSize = 14.sp, color = Muted)
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
