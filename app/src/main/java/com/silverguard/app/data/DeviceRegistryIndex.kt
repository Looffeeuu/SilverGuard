package com.silverguard.app.data

import android.content.res.AssetManager
import com.google.gson.JsonParser
import com.silverguard.app.engine.DeviceRegistryKey
import com.silverguard.app.model.DeviceRegistryRecord
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream

/**
 * Reads the offline device registration index written by tools/udi_index/build_udi_index.py.
 * A lookup decompresses one of 64 small shards, so it stays fast and light on low-end phones.
 * No index ships with the app yet (see docs/DEVICE_REGISTRY.md); [isAvailable] is then false.
 */
class DeviceRegistryIndex(private val source: Source) {

    /** Where the index files live: bundled assets, or a directory the app downloaded them to. */
    fun interface Source {
        /** Returns null when the file does not exist. */
        fun open(name: String): InputStream?
    }

    class Manifest(val dataDate: String, val records: Int)

    val manifest: Manifest? by lazy { readManifest() }

    val isAvailable: Boolean get() = manifest != null

    /** Call on an IO dispatcher. Returns null when the number is not in the index or there is no index. */
    fun lookup(number: String): DeviceRegistryRecord? {
        if (!isAvailable) return null
        val key = DeviceRegistryKey.normalize(number)
        if (key.isEmpty()) return null
        val stream = source.open(DeviceRegistryKey.shardFileName(DeviceRegistryKey.shardOf(key))) ?: return null
        GZIPInputStream(stream).bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (line in lines) {
                if (!line.startsWith(key + "\t")) continue
                val fields = line.split('\t')
                if (fields.size < 5) return null
                return DeviceRegistryRecord(
                    number = fields[1],
                    holder = fields[2],
                    productNames = fields[3].split('；').filter { it.isNotBlank() },
                    category = fields[4]
                )
            }
        }
        return null
    }

    private fun readManifest(): Manifest? = try {
        source.open(MANIFEST)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
            val json = JsonParser.parseReader(reader).asJsonObject
            if (json["format"].asInt != FORMAT || json["shards"].asInt != DeviceRegistryKey.SHARD_COUNT) {
                null
            } else {
                Manifest(dataDate = json["dataDate"].asString, records = json["records"].asInt)
            }
        }
    } catch (_: Exception) {
        null
    }

    companion object {
        const val FORMAT = 1
        const val MANIFEST = "manifest.json"
        const val DIRECTORY = "udi-index"

        fun directorySource(directory: File) = Source { name ->
            File(directory, name).takeIf { it.isFile }?.inputStream()
        }

        fun assetSource(assets: AssetManager, directory: String = DIRECTORY) = Source { name ->
            try {
                assets.open("$directory/$name")
            } catch (_: IOException) {
                null
            }
        }

        /** Prefers a downloaded index in app storage over one bundled in assets. */
        fun firstAvailable(vararg sources: Source): DeviceRegistryIndex =
            sources.map(::DeviceRegistryIndex).firstOrNull { it.isAvailable } ?: DeviceRegistryIndex { null }
    }
}
