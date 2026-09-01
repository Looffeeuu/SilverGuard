package com.silverguard.app.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

internal class ResultSpeaker(context: Context) {
    private var engine: TextToSpeech? = null
    private var ready = false
    var isSpeaking by mutableStateOf(false)
        private set
    var unavailableMessage by mutableStateOf<String?>(null)
        private set

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val languageResult = engine?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                ready = languageResult != TextToSpeech.LANG_MISSING_DATA &&
                    languageResult != TextToSpeech.LANG_NOT_SUPPORTED
                unavailableMessage = if (ready) null else "手机尚未安装可用的中文朗读语音。"
                engine?.setSpeechRate(0.88f)
            } else {
                unavailableMessage = "手机朗读功能暂时不可用。"
            }
        }
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
            }

            @Deprecated("Deprecated in Android")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
                unavailableMessage = "本次朗读没有完成，可以重新尝试。"
            }
        })
    }

    fun speak(text: String) {
        if (!ready) {
            if (unavailableMessage == null) unavailableMessage = "朗读功能正在准备，请稍后再试。"
            return
        }
        unavailableMessage = null
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "silverguard-result")
    }

    fun stop() {
        engine?.stop()
        isSpeaking = false
    }

    fun release() {
        stop()
        engine?.shutdown()
        engine = null
    }
}

@Composable
internal fun rememberResultSpeaker(): ResultSpeaker {
    val context = LocalContext.current
    val speaker = remember(context) { ResultSpeaker(context) }
    DisposableEffect(speaker) {
        onDispose { speaker.release() }
    }
    return speaker
}
