package com.silverguard.app.engine

import java.text.Normalizer
import java.util.zip.CRC32

/**
 * Lookup key and shard for the offline device registration index.
 * Must stay identical to normalize_key and shard_of in tools/udi_index/build_udi_index.py;
 * DeviceRegistryKeyTest and the Python tests pin the same values.
 */
object DeviceRegistryKey {
    const val SHARD_COUNT = 64

    private val removedChars = Regex("[\\s:：()（）\\-—]")

    fun normalize(number: String): String {
        val text = Normalizer.normalize(number, Normalizer.Form.NFKC).uppercase().replace(removedChars, "")
        return text.removeSuffix("号")
    }

    fun shardOf(key: String): Int {
        val crc = CRC32().apply { update(key.toByteArray(Charsets.UTF_8)) }
        return (crc.value % SHARD_COUNT).toInt()
    }

    fun shardFileName(shard: Int): String = "shard-%02d.tsv.gz".format(shard)
}
