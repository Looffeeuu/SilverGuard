package com.silverguard.app.data

import android.content.Context
import android.util.AtomicFile
import java.io.File

/** Stores one family phone number in app-private storage excluded from cloud backup, like the history. */
class FamilyContactRepository(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "family-contact.txt"))

    fun load(): String? = try {
        file.openRead().use { it.bufferedReader(Charsets.UTF_8).readText() }.trim().ifEmpty { null }
    } catch (_: Exception) {
        null
    }

    /** Call on an IO dispatcher. An empty value removes the saved number. */
    fun save(number: String?) {
        if (number.isNullOrBlank()) {
            file.delete()
            return
        }
        val output = file.startWrite()
        try {
            output.write(number.toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }
}
