package com.silverguard.app

import android.content.Context
import java.io.File

/**
 * Reads the history file directly to wait for the app's write. Polling through AnalysisHistoryRepository
 * would call AtomicFile.openRead(), which deletes an in-progress ".new" file and so can break the very
 * write the test is waiting for.
 */
internal fun historyFileContains(context: Context, text: String): Boolean =
    runCatching { File(context.noBackupFilesDir, "analysis-history-v1.json").readText(Charsets.UTF_8) }
        .getOrDefault("")
        .contains(text)
