package com.silverguard.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.silverguard.app.data.AnalysisHistoryRepository
import com.silverguard.app.engine.CaptureGuidanceEvaluator
import com.silverguard.app.engine.EcommerceLinkParser
import com.silverguard.app.engine.ManualVerificationRecorder
import com.silverguard.app.engine.OfficialScreenshotAnalyzer
import com.silverguard.app.engine.PriceReferenceEvaluator
import com.silverguard.app.engine.RiskAnalyzer
import com.silverguard.app.engine.SpeechSummaryBuilder
import com.silverguard.app.model.AiAnalysisResult
import com.silverguard.app.model.AiAnalysisStatus
import com.silverguard.app.model.AnalysisHistoryEntry
import com.silverguard.app.model.AnalysisInputMethod
import com.silverguard.app.model.EvidenceMatchStatus
import com.silverguard.app.model.OfficialSearchOutcome
import com.silverguard.app.model.OfficialSource
import com.silverguard.app.model.PriceReferenceInput
import com.silverguard.app.model.RiskAnalysis
import com.silverguard.app.model.VerificationEvidenceField
import com.silverguard.app.network.AiProviderFactory
import com.silverguard.app.network.TaobaoTmallProductResolver
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val HOME_OCR_MESSAGE = "拍商品或选择截图后，可在手机本地识别文字"

/**
 * Holds the analysis session so a screen rotation, font or dark-mode change does not drop the result,
 * a running OCR or link read, or the open dialogs. UI-only effects (focus, scrolling, intents) stay in Compose.
 */
class SilverGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val productResolver = TaobaoTmallProductResolver()
    private val aiProvider = AiProviderFactory.create()
    private val historyRepository = AnalysisHistoryRepository(application)
    internal val speaker = ResultSpeaker(application)

    val isAiConfigured: Boolean get() = aiProvider != null

    var historyEntries by mutableStateOf(historyRepository.list())
        private set
    var historyStorageMessage by mutableStateOf(
        if (historyRepository.loadFailed) "历史文件暂时无法读取，原文件已保留。可重启应用再试，或确认清空后重新记录。" else null
    )
        private set
    var showHistory by mutableStateOf(false)
        private set
    private var activeHistoryId: String? = null
    var restoredHistoryTime by mutableStateOf<Long?>(null)
        private set

    var inputText by mutableStateOf("")
        private set
    var selectedUri by mutableStateOf<Uri?>(null)
        private set
    var capturedBitmap by mutableStateOf<Bitmap?>(null)
        private set
    var analysis by mutableStateOf<RiskAnalysis?>(null)
        private set
    var analysisInputMethods by mutableStateOf<Set<AnalysisInputMethod>>(emptySet())
        private set
    var hasPendingTextChanges by mutableStateOf(false)
        private set

    var isOcrRunning by mutableStateOf(false)
        private set
    var ocrMessage by mutableStateOf(HOME_OCR_MESSAGE)
        private set
    private var ocrRequestVersion = 0
    private var isSupplementCapture = false
    private var isSupplementScreenshot = false

    var isProductLoading by mutableStateOf(false)
        private set
    private var analysisJob: Job? = null
    private var analysisRequestVersion = 0

    var isAiAnalyzing by mutableStateOf(false)
        private set
    private var aiJob: Job? = null
    private var aiRequestVersion = 0
    private var aiConsentGranted = false
    var showAiConsentDialog by mutableStateOf(false)
        private set
    private var pendingAiAnalysis: RiskAnalysis? = null

    var isOfficialScreenshotOcrRunning by mutableStateOf(false)
        private set
    var officialScreenshotMessage by mutableStateOf("")
        private set
    private var waitingForOfficialReturn = false
    private var officialPageWasOpened = false
    var showOfficialReturnDialog by mutableStateOf(false)

    var showManualInput by mutableStateOf(false)
    var showExamples by mutableStateOf(false)
    var showResetConfirmation by mutableStateOf(false)

    /** Whether the system back button should return home instead of leaving the app. */
    val canGoBack: Boolean
        get() = showHistory || analysis != null || showManualInput || isOcrRunning || isProductLoading

    val canReturnHome: Boolean
        get() = canGoBack || inputText.isNotBlank()

    // ---- History ----

    private fun updateAnalysis(value: RiskAnalysis?) {
        analysis = value
        saveCurrentHistory()
    }

    private fun persistHistory() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { historyRepository.persist() }
                historyStorageMessage = null
            } catch (_: Exception) {
                historyStorageMessage = "历史记录暂未写入手机，请检查存储空间；关闭应用后可能丢失本次记录。"
            }
        }
    }

    private fun saveCurrentHistory() {
        val current = analysis ?: return
        val id = activeHistoryId ?: return
        val previous = historyEntries.firstOrNull { it.id == id }
        val methods = analysisInputMethods.ifEmpty { setOf(AnalysisInputMethod.TEXT) }
        if (previous?.analysis == current && previous.draftText == inputText && previous.inputMethods == methods) return
        historyEntries = historyRepository.upsert(AnalysisHistoryEntry(id, System.currentTimeMillis(), current, methods, inputText))
        persistHistory()
    }

    fun openHistory() {
        saveCurrentHistory()
        cancelProductRead()
        cancelAiAnalysis()
        speaker.stop()
        ocrRequestVersion += 1
        isOcrRunning = false
        isOfficialScreenshotOcrRunning = false
        waitingForOfficialReturn = false
        officialPageWasOpened = false
        showOfficialReturnDialog = false
        showHistory = true
    }

    fun closeHistory() {
        showHistory = false
    }

    fun openHistoryEntry(entry: AnalysisHistoryEntry) {
        saveCurrentHistory()
        resetAll()
        activeHistoryId = entry.id
        restoredHistoryTime = entry.savedAt
        inputText = entry.draftText
        analysisInputMethods = entry.inputMethods
        hasPendingTextChanges = entry.draftText != entry.analysis.rawText
        updateAnalysis(entry.analysis)
    }

    fun deleteHistoryEntry(id: String) {
        historyEntries = historyRepository.delete(id)
        if (activeHistoryId == id) {
            resetAll()
            showHistory = true
        }
        persistHistory()
    }

    fun clearHistory() {
        historyEntries = historyRepository.clear()
        resetAll()
        showHistory = true
        persistHistory()
    }

    // ---- Analysis ----

    fun cancelProductRead() {
        analysisRequestVersion += 1
        analysisJob?.cancel()
        analysisJob = null
        isProductLoading = false
    }

    private fun cancelAiAnalysis() {
        aiRequestVersion += 1
        aiJob?.cancel()
        aiJob = null
        isAiAnalyzing = false
        showAiConsentDialog = false
        pendingAiAnalysis = null
    }

    fun analyzeInput(text: String, inputMethod: AnalysisInputMethod? = null) {
        val requestText = text.trim()
        if (requestText.isBlank()) return
        saveCurrentHistory()
        if (activeHistoryId == null || restoredHistoryTime != null) activeHistoryId = UUID.randomUUID().toString()
        restoredHistoryTime = null
        val linkInfo = EcommerceLinkParser.parse(requestText)
        val resolvedInputMethod = inputMethod ?: if (linkInfo.extractedUrl != null) {
            AnalysisInputMethod.PRODUCT_LINK
        } else {
            AnalysisInputMethod.TEXT
        }
        analysisInputMethods = analysisInputMethods + resolvedInputMethod
        hasPendingTextChanges = false
        cancelProductRead()
        cancelAiAnalysis()
        speaker.stop()
        isOfficialScreenshotOcrRunning = false
        officialScreenshotMessage = ""
        val requestId = analysisRequestVersion

        if (!productResolver.canResolve(linkInfo)) {
            updateAnalysis(RiskAnalyzer.analyze(requestText))
            return
        }

        isProductLoading = true
        analysisJob = viewModelScope.launch {
            val product = productResolver.resolve(linkInfo)
            if (analysisRequestVersion == requestId) {
                updateAnalysis(RiskAnalyzer.analyze(requestText, product))
                isProductLoading = false
                analysisJob = null
            }
        }
    }

    fun onHomeInputChange(text: String) {
        cancelProductRead()
        cancelAiAnalysis()
        speaker.stop()
        updateAnalysis(null)
        inputText = text
    }

    fun onResultInputChange(text: String) {
        inputText = text
        hasPendingTextChanges = true
    }

    fun applyResultText() {
        if (EcommerceLinkParser.parse(inputText).extractedUrl != null) {
            analysisInputMethods = analysisInputMethods + AnalysisInputMethod.PRODUCT_LINK
        }
        analyzeInput(inputText, AnalysisInputMethod.TEXT)
    }

    fun analyzeExample(sample: String) {
        inputText = sample
        showManualInput = true
        analyzeInput(sample, AnalysisInputMethod.TEXT)
    }

    fun resetAll() {
        cancelProductRead()
        cancelAiAnalysis()
        speaker.stop()
        analysis = null
        activeHistoryId = null
        restoredHistoryTime = null
        showHistory = false
        ocrRequestVersion += 1
        isOcrRunning = false
        inputText = ""
        selectedUri = null
        capturedBitmap = null
        isOfficialScreenshotOcrRunning = false
        officialScreenshotMessage = ""
        ocrMessage = HOME_OCR_MESSAGE
        showManualInput = false
        showExamples = false
        showOfficialReturnDialog = false
        waitingForOfficialReturn = false
        officialPageWasOpened = false
        isSupplementCapture = false
        isSupplementScreenshot = false
        analysisInputMethods = emptySet()
        hasPendingTextChanges = false
    }

    fun returnHome() {
        saveCurrentHistory()
        resetAll()
    }

    // ---- Photo and screenshot OCR ----

    /** Call right before launching the camera; a supplement photo is merged into the current analysis. */
    fun prepareCapture(supplement: Boolean) {
        if (!supplement) cancelProductRead()
        isSupplementCapture = supplement
    }

    /** Call right before launching the image picker; a supplement screenshot is merged into the current analysis. */
    fun prepareScreenshot(supplement: Boolean, cancelProductRead: Boolean = !supplement) {
        if (cancelProductRead) cancelProductRead()
        isSupplementScreenshot = supplement
    }

    fun onPhotoTaken(bitmap: Bitmap?) {
        if (bitmap == null) {
            isSupplementCapture = false
            return
        }
        val ocrVersion = ++ocrRequestVersion
        val supplement = isSupplementCapture
        capturedBitmap = bitmap
        selectedUri = null
        isOcrRunning = true
        ocrMessage = if (supplement) "正在把补拍文字合并到当前商品…" else "正在识别拍到的商品文字…"
        runChineseOcr(
            image = { InputImage.fromBitmap(bitmap, 0) },
            onSuccess = { text ->
                if (ocrVersion != ocrRequestVersion) return@runChineseOcr
                mergeRecognizedText(text, supplement, AnalysisInputMethod.PHOTO)
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
                if (ocrVersion != ocrRequestVersion) return@runChineseOcr
                isOcrRunning = false
                isSupplementCapture = false
                ocrMessage = "识别失败：${it.message ?: "未知错误"}。当前文字和结果没有被清除。"
            }
        )
    }

    fun onScreenshotPicked(uri: Uri?) {
        val supplement = isSupplementScreenshot
        if (uri == null) {
            isSupplementScreenshot = false
            return
        }
        val ocrVersion = ++ocrRequestVersion
        selectedUri = uri
        capturedBitmap = null
        isOcrRunning = true
        ocrMessage = if (supplement) "正在把补充截图文字合并到当前商品…" else "正在识别截图文字…"
        runChineseOcr(
            image = { InputImage.fromFilePath(getApplication(), uri) },
            onSuccess = { text ->
                if (ocrVersion != ocrRequestVersion) return@runChineseOcr
                mergeRecognizedText(text, supplement, AnalysisInputMethod.SCREENSHOT)
                isOcrRunning = false
                isSupplementScreenshot = false
                ocrMessage = when {
                    text.isBlank() && supplement -> "补充截图没有识别到新文字，当前分析仍然保留"
                    text.isBlank() -> "没有识别到文字，可以手动输入商品名或宣传语"
                    supplement -> "补充截图信息已合并，并重新生成分析结果"
                    else -> "识别完成，可以修改文字后再分析"
                }
            },
            onError = {
                if (ocrVersion != ocrRequestVersion) return@runChineseOcr
                isOcrRunning = false
                isSupplementScreenshot = false
                ocrMessage = "识别失败：${it.message ?: "未知错误"}。原有文字没有被清除。"
            }
        )
    }

    private fun mergeRecognizedText(text: String, supplement: Boolean, method: AnalysisInputMethod) {
        val combined = if (supplement) {
            CaptureGuidanceEvaluator.mergeRecognizedText(inputText, text)
        } else {
            analysisInputMethods = emptySet()
            text.trim()
        }
        if (combined.isNotBlank()) {
            inputText = combined
            showManualInput = true
            analyzeInput(combined, method)
        }
    }

    // ---- Official verification ----

    /** Records the opened source and returns the registration number the UI should copy, if any. */
    fun onOfficialSourceOpening(source: OfficialSource): String? {
        val latest = analysis ?: return null
        updateAnalysis(
            latest.copy(verification = ManualVerificationRecorder.markSourceOpened(latest.verification, source))
        )
        officialScreenshotMessage = "查询完成并截图后，请返回这里继续。"
        waitingForOfficialReturn = true
        officialPageWasOpened = true
        return latest.verification.registration.normalizedNumber
            .ifBlank { latest.verification.registration.rawNumber }
            .takeIf { it.isNotBlank() }
    }

    fun onOfficialSourceOpenFailed() {
        waitingForOfficialReturn = false
        officialPageWasOpened = false
    }

    /** Asks whether the official search is done once the user comes back from the browser. */
    fun onAppResumed() {
        if (waitingForOfficialReturn && officialPageWasOpened) {
            showOfficialReturnDialog = true
            waitingForOfficialReturn = false
            officialPageWasOpened = false
        }
    }

    fun onOfficialScreenshotPicked(uri: Uri?) {
        val current = analysis
        if (uri != null && current?.verification?.manualRecord != null) {
            val ocrVersion = ocrRequestVersion
            isOfficialScreenshotOcrRunning = true
            officialScreenshotMessage = "正在本地识别官方查询截图…"
            val requestedRegistration = current.verification.registration
                .normalizedNumber
                .ifBlank { current.verification.registration.rawNumber }
            runChineseOcr(
                image = { InputImage.fromFilePath(getApplication(), uri) },
                onSuccess = { text ->
                    if (ocrVersion != ocrRequestVersion) return@runChineseOcr
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
                        updateAnalysis(
                            latest.copy(
                                verification = ManualVerificationRecorder.attachScreenshotReview(latest.verification, review)
                            )
                        )
                        officialScreenshotMessage = review.summary
                    } else {
                        officialScreenshotMessage = "商品信息已经变化，请重新选择官方查询截图。"
                    }
                    isOfficialScreenshotOcrRunning = false
                },
                onError = { error ->
                    if (ocrVersion != ocrRequestVersion) return@runChineseOcr
                    isOfficialScreenshotOcrRunning = false
                    officialScreenshotMessage = "截图识别失败：${error.message ?: "未知错误"}。当前分析仍然保留。"
                }
            )
        } else if (uri != null) {
            officialScreenshotMessage = "请先打开一个官方查询入口，再导入查询结果截图。"
        }
    }

    fun recordSearchOutcome(outcome: OfficialSearchOutcome) {
        val latest = analysis ?: return
        updateAnalysis(
            latest.copy(verification = ManualVerificationRecorder.recordSearchOutcome(latest.verification, outcome))
        )
    }

    fun recordFinding(field: VerificationEvidenceField, status: EvidenceMatchStatus) {
        val latest = analysis ?: return
        updateAnalysis(
            latest.copy(verification = ManualVerificationRecorder.recordFinding(latest.verification, field, status))
        )
    }

    fun updatePriceReference(input: PriceReferenceInput) {
        val latest = analysis ?: return
        speaker.stop()
        updateAnalysis(latest.copy(priceReference = PriceReferenceEvaluator.evaluate(input)))
    }

    // ---- Speech ----

    fun speakResult() {
        analysis?.let { speaker.speak(SpeechSummaryBuilder.build(it)) }
    }

    fun stopSpeaking() = speaker.stop()

    // ---- Optional AI ----

    fun requestAiAnalysis() {
        val current = analysis ?: return
        if (aiProvider == null || aiConsentGranted) {
            startAiAnalysis(current)
        } else {
            pendingAiAnalysis = current
            showAiConsentDialog = true
        }
    }

    fun onAiConsent(granted: Boolean) {
        val pending = pendingAiAnalysis
        showAiConsentDialog = false
        pendingAiAnalysis = null
        if (granted) {
            aiConsentGranted = true
            pending?.let(::startAiAnalysis)
        }
    }

    private fun startAiAnalysis(current: RiskAnalysis) {
        val provider = aiProvider
        if (provider == null) {
            updateAnalysis(
                (analysis ?: current).copy(
                    aiAnalysis = AiAnalysisResult(
                        status = AiAnalysisStatus.NOT_CONFIGURED,
                        message = "AI 深入分析尚未启用，本地分析结果仍可正常使用。"
                    )
                )
            )
            return
        }
        cancelAiAnalysis()
        val requestId = aiRequestVersion
        isAiAnalyzing = true
        aiJob = viewModelScope.launch {
            val result = provider.analyze(current)
            val latest = analysis
            if (aiRequestVersion == requestId && latest != null && latest.rawText == current.rawText) {
                updateAnalysis(latest.copy(aiAnalysis = result))
                isAiAnalyzing = false
                aiJob = null
            }
        }
    }

    override fun onCleared() {
        speaker.release()
    }
}

/** ML Kit delivers its callbacks on the main thread, so they can update Compose state directly. */
private fun runChineseOcr(
    image: () -> InputImage,
    onSuccess: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    try {
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        recognizer.process(image())
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
