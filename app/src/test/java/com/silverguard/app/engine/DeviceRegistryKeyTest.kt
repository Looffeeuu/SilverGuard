package com.silverguard.app.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pins the same values as tools/udi_index/test_build_udi_index.py, so the app and the index builder agree. */
class DeviceRegistryKeyTest {

    @Test
    fun normalizeMatchesIndexBuilder() {
        assertEquals("京大械备20260001", DeviceRegistryKey.normalize("京大械备20260001号"))
        assertEquals("国械注准20263090001", DeviceRegistryKey.normalize(" 国械注准 2026-3090001 "))
        assertEquals("国械注准20263090001", DeviceRegistryKey.normalize("国械注准２０２６３０９０００１"))
    }

    @Test
    fun shardIsCrc32Mod64LikeIndexBuilder() {
        assertEquals(31, DeviceRegistryKey.shardOf("国械注准20263090001"))
        assertEquals(45, DeviceRegistryKey.shardOf("京大械备20260001"))
        assertEquals("shard-07.tsv.gz", DeviceRegistryKey.shardFileName(7))
    }
}
