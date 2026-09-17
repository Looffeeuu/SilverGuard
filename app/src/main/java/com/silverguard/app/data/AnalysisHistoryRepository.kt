package com.silverguard.app.data

import android.content.Context
import android.util.AtomicFile
import com.silverguard.app.model.AnalysisHistoryEntry
import java.io.File

/** App-private storage excluded from Android cloud backup and device migration. No images saved. */
class AnalysisHistoryRepository(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "analysis-history-v1.json"))
    private var entries = emptyList<AnalysisHistoryEntry>()
    var loadFailed: Boolean = false
        private set

    init {
        try {
            file.openRead().use { stream ->
                require(stream.channel.size() <= 16 * 1024 * 1024) { "History file too large" }
                entries = AnalysisHistoryCodec.decode(stream.bufferedReader(Charsets.UTF_8).readText())
            }
        } catch (_: java.io.FileNotFoundException) {
            // First launch has no history yet.
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    @Synchronized fun list(): List<AnalysisHistoryEntry> = entries.toList()

    @Synchronized fun upsert(entry: AnalysisHistoryEntry): List<AnalysisHistoryEntry> {
        entries = AnalysisHistoryCodec.upsert(entries, entry)
        return list()
    }

    @Synchronized fun delete(id: String): List<AnalysisHistoryEntry> {
        entries = entries.filterNot { it.id == id }
        return list()
    }

    @Synchronized fun clear(): List<AnalysisHistoryEntry> {
        entries = emptyList()
        loadFailed = false
        return list()
    }

    /** Call on IO dispatcher. The synchronized snapshot prevents older writes winning a race. */
    @Synchronized fun persist() {
        check(!loadFailed) { "Existing history could not be read; preserved without overwrite" }
        val output = file.startWrite()
        try {
            output.write(AnalysisHistoryCodec.encode(entries).toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }
}
