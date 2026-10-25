package com.silverguard.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PrivacyConsentTest {

    @Test
    fun checksumIgnoresLineEndingsFromTheBuildMachine() {
        assertEquals(PrivacyConsent.checksum("# 政策\n第一条\n"), PrivacyConsent.checksum("# 政策\r\n第一条\r\n"))
    }

    @Test
    fun anyWordingChangeAsksAgain() {
        assertNotEquals(PrivacyConsent.checksum("不出售你的信息"), PrivacyConsent.checksum("不出售你的资料"))
    }
}
