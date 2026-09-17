package com.silverguard.app.data

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.silverguard.app.model.AnalysisHistoryEntry

/** Versioned, bounded snapshots. Reopening a record must not call OCR, a shop, or an AI service. */
object AnalysisHistoryCodec {
    const val MAX_ENTRIES = 50
    private val gson = Gson()

    fun encode(entries: List<AnalysisHistoryEntry>): String = gson.toJson(
        Envelope(1, entries.sortedByDescending { it.savedAt }.distinctBy { it.id }.take(MAX_ENTRIES))
    )

    fun decode(text: String): List<AnalysisHistoryEntry> {
        if (text.isBlank()) return emptyList()
        val root = JsonParser.parseString(text).asJsonObject
        require(root["version"].asInt == 1) { "Unsupported history version" }
        return root["entries"].asJsonArray.map { json ->
            val entry = gson.fromJson(json, AnalysisHistoryEntry::class.java)
            // Reject damaged files instead of silently overwriting them with an empty history.
            require(entry.id.isNotBlank() && entry.savedAt > 0)
            require(entry.analysis.rawText.isNotBlank())
            require(entry.analysis.level.name.isNotBlank())
            require(entry.analysis.verification.status.name.isNotBlank())
            require(entry.inputMethods.isNotEmpty())
            require(entry.draftText.length <= 200_000)
            entry
        }.sortedByDescending { it.savedAt }.distinctBy { it.id }.take(MAX_ENTRIES)
    }

    fun upsert(entries: List<AnalysisHistoryEntry>, entry: AnalysisHistoryEntry): List<AnalysisHistoryEntry> =
        (listOf(entry) + entries.filterNot { it.id == entry.id })
            .sortedByDescending { it.savedAt }.take(MAX_ENTRIES)

    private data class Envelope(val version: Int, val entries: List<AnalysisHistoryEntry>)
}
