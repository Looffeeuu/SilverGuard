package com.silverguard.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverguard.app.model.AnalysisInputMethod
import com.silverguard.app.model.RiskAnalysis

/** Everything below the header once there is an analysis: history notice, input composer and results. */
@Composable
internal fun ResultContent(
    vm: SilverGuardViewModel,
    current: RiskAnalysis,
    onAnalyze: (String) -> Unit,
    onApplyText: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickScreenshot: (cancelProductRead: Boolean) -> Unit,
    onPickOfficialScreenshot: () -> Unit
) {
    val context = LocalContext.current
    val speaker = vm.speaker

    Spacer(Modifier.height(16.dp))
    vm.historyStorageMessage?.let { Text(it, color = Ink, fontWeight = FontWeight.Bold) }
    vm.restoredHistoryTime?.let { time ->
        Card(colors = CardDefaults.cardColors(containerColor = SoftAmber, contentColor = Ink), border = CardBorder) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("历史结果 · ${historyTime(time)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("history_result_notice"))
                Text("这是当时保存的分析，不是刚刚重新查询。可继续补充信息，或重新分析。原照片不保留，识别文字仍在下方。", lineHeight = 23.sp)
                OutlinedButton(onClick = { onAnalyze(vm.inputText) }, enabled = vm.inputText.isNotBlank() && !vm.isProductLoading && !vm.isOcrRunning, modifier = Modifier.heightIn(min = 52.dp).testTag("history_reanalyze")) { Text("用当前文字重新分析") }
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
        onApplyText = onApplyText,
        onTakePhoto = onTakePhoto,
        onSelectScreenshot = { onPickScreenshot(false) }
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
        onSelectOfficialScreenshot = onPickOfficialScreenshot,
        isOfficialScreenshotOcrRunning = vm.isOfficialScreenshotOcrRunning,
        officialScreenshotMessage = vm.officialScreenshotMessage,
        onRecordSearchOutcome = vm::recordSearchOutcome,
        onRecordFinding = vm::recordFinding,
        onOpenProductPage = { openUrl(context, it) },
        // A screenshot replacing an unreadable product link also stops the pending link read.
        onSelectScreenshot = { onPickScreenshot(true) },
        inputMethods = vm.analysisInputMethods,
        onSupplementPhoto = onTakePhoto,
        onSupplementScreenshot = { onPickScreenshot(false) },
        isSupplementRunning = vm.isOcrRunning,
        supplementMessage = vm.ocrMessage,
        onSpeakResult = vm::speakResult,
        onStopSpeaking = vm::stopSpeaking,
        isSpeaking = speaker.isSpeaking,
        speechMessage = speaker.unavailableMessage,
        familyPhone = vm.familyPhone,
        onSaveFamilyPhone = vm::saveFamilyPhone,
        onClearFamilyPhone = vm::clearFamilyPhone,
        onDial = { number -> openDialer(context, number) },
        onReset = { vm.showResetConfirmation = true }
    )
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
                fontSize = 14.sp,
                lineHeight = 19.sp
            )
        }
    }
}
