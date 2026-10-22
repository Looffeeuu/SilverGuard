package com.silverguard.app.engine

import com.silverguard.app.model.DeviceRegistryRecord
import com.silverguard.app.model.DeviceRegistryStatus
import com.silverguard.app.model.FieldAgreement
import com.silverguard.app.model.ProductInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceRegistryComparatorTest {

    private val bloodPressureMonitor = DeviceRegistryRecord(
        number = "国械注准20263090001",
        holder = "安心医疗科技有限公司",
        productNames = listOf("电子血压计"),
        category = "器械"
    )

    @Test
    fun matchingPackagingAgreesWithRecord() {
        val check = DeviceRegistryComparator.check(
            "国械注准20263090001",
            bloodPressureMonitor,
            ProductInfo(name = "安心牌电子血压计 U10", manufacturer = "安心医疗科技有限公司"),
            "2026-10-05"
        )

        assertEquals(DeviceRegistryStatus.FOUND, check.status)
        assertEquals(FieldAgreement.MATCHES, check.nameAgreement)
        assertEquals(FieldAgreement.MATCHES, check.holderAgreement)
        assertFalse(check.hasMismatch)
    }

    @Test
    fun borrowedNumberOnADifferentProductIsAMismatch() {
        val check = DeviceRegistryComparator.check(
            "国械注准20263090001",
            bloodPressureMonitor,
            ProductInfo(name = "量子太赫兹理疗仪", manufacturer = "深圳某某健康科技有限公司"),
            "2026-10-05"
        )

        assertEquals(FieldAgreement.DIFFERS, check.nameAgreement)
        assertEquals(FieldAgreement.DIFFERS, check.holderAgreement)
        assertTrue(check.hasMismatch)
    }

    @Test
    fun companySuffixesAndShortNamesStillMatch() {
        assertEquals(FieldAgreement.MATCHES, DeviceRegistryComparator.compareHolder("安心医疗科技", "安心医疗科技有限公司"))
        assertEquals(FieldAgreement.MATCHES, DeviceRegistryComparator.compareHolder("安心医疗科技股份有限公司", "安心医疗科技有限公司"))
        assertEquals(FieldAgreement.MATCHES, DeviceRegistryComparator.compareName("电子血压计（上臂式）", listOf("电子血压计")))
    }

    @Test
    fun missingPackagingFieldsAreNotCompared() {
        val check = DeviceRegistryComparator.check("国械注准20263090001", bloodPressureMonitor, ProductInfo(), "2026-10-05")

        assertEquals(FieldAgreement.NOT_COMPARED, check.nameAgreement)
        assertEquals(FieldAgreement.NOT_COMPARED, check.holderAgreement)
        assertFalse(check.hasMismatch)
    }

    @Test
    fun missingRecordIsNotFound() {
        val check = DeviceRegistryComparator.check("国械注准20269999999", null, ProductInfo(name = "电子血压计"), "2026-10-05")

        assertEquals(DeviceRegistryStatus.NOT_FOUND, check.status)
        assertEquals(null, check.record)
        assertFalse(check.hasMismatch)
    }
}
