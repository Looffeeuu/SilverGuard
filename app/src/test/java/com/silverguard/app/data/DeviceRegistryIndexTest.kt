package com.silverguard.app.data

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reads udi-index-fixture, which tools/udi_index/build_udi_index.py built from its synthetic test data,
 * so this also checks that the Python output and the Kotlin reader agree.
 */
class DeviceRegistryIndexTest {

    private val fixture = File(requireNotNull(javaClass.classLoader?.getResource("udi-index-fixture/manifest.json")).toURI()).parentFile
    private val index = DeviceRegistryIndex(DeviceRegistryIndex.directorySource(fixture))

    @Test
    fun manifestIsRead() {
        assertTrue(index.isAvailable)
        assertEquals("2026-10-05", index.manifest?.dataDate)
        assertEquals(4, index.manifest?.records)
    }

    @Test
    fun findsNumbersWrittenAsOnPackaging() {
        val record = requireNotNull(index.lookup("国械注准 20263090001"))
        assertEquals("国械注准20263090001", record.number)
        assertEquals("安心医疗科技有限公司", record.holder)
        assertEquals(listOf("电子血压计"), record.productNames)
        assertEquals("器械", record.category)

        assertEquals("北京安康卫生材料有限公司", index.lookup("京大械备20260001号")?.holder)
    }

    @Test
    fun appliesLaterUpdates() {
        assertEquals("苏州康复器械股份有限公司", index.lookup("苏械注准20262090002号")?.holder)
        assertNull("deleted in the update package", index.lookup("粤械注准20262400003"))
    }

    @Test
    fun unknownNumberIsNotFound() {
        assertNull(index.lookup("国械注准20269999999"))
        assertNull(index.lookup(""))
    }

    @Test
    fun missingOrForeignIndexIsUnavailable() {
        val empty = DeviceRegistryIndex.directorySource(Files.createTempDirectory("no-index").toFile())
        assertFalse(DeviceRegistryIndex(empty).isAvailable)
        assertNull(DeviceRegistryIndex(empty).lookup("国械注准20263090001"))

        val wrongFormat = Files.createTempDirectory("old-index").toFile().apply {
            File(this, "manifest.json").writeText("""{"format": 99, "shards": 64, "dataDate": "2020-01-01", "records": 1}""")
        }
        assertFalse(DeviceRegistryIndex(DeviceRegistryIndex.directorySource(wrongFormat)).isAvailable)
    }

    @Test
    fun firstAvailablePrefersADownloadedIndex() {
        val missing = DeviceRegistryIndex.directorySource(Files.createTempDirectory("none").toFile())
        val chosen = DeviceRegistryIndex.firstAvailable(missing, DeviceRegistryIndex.directorySource(fixture))
        assertTrue(chosen.isAvailable)
        assertFalse(DeviceRegistryIndex.firstAvailable(missing).isAvailable)
    }
}
