package com.silverguard.app

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.silverguard.app.data.AnalysisHistoryRepository
import com.silverguard.app.ui.SilverGuardViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Simulates Android killing the app in the background: a new ViewModel is built only from the values
 * that were written to SavedStateHandle, as a freshly started process would get them back.
 */
@RunWith(AndroidJUnit4::class)
class ProcessDeathRestoreTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext.applicationContext as Application

    @Before
    fun clearHistory() {
        AnalysisHistoryRepository(app).apply { clear(); persist() }
    }

    @Test
    fun openAnalysisAndPendingSupplementPhotoSurviveProcessDeath() {
        val handle = SavedStateHandle()
        instrumentation.runOnMainSync {
            val before = SilverGuardViewModel(app, handle)
            before.analyzeInput("普通茶杯 进程回收测试")
            before.onResultInputChange("普通茶杯 进程回收测试 补充说明")
            before.prepareCapture(supplement = true)
        }
        waitUntil { AnalysisHistoryRepository(app).list().isNotEmpty() }

        val restoredHandle = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })
        lateinit var after: SilverGuardViewModel
        instrumentation.runOnMainSync {
            after = SilverGuardViewModel(app, restoredHandle)
            assertTrue(after.analysis?.rawText.orEmpty().contains("进程回收测试"))
            assertEquals("普通茶杯 进程回收测试 补充说明", after.inputText)
            assertTrue(after.hasPendingTextChanges)
            // The camera result arrives in the new process; a blank photo has no text to merge.
            after.onPhotoTaken(Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888))
        }
        waitUntil { !after.isOcrRunning }

        assertTrue(after.ocrMessage, after.ocrMessage.startsWith("补拍照片"))
        assertFalse(after.analysis == null)
    }

    private fun waitUntil(timeoutMs: Long = 10_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "Timed out waiting for condition" }
            Thread.sleep(50)
        }
    }
}
