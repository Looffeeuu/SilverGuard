package com.silverguard.app.network

import com.silverguard.app.model.EcommercePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AllowedEcommerceHostsTest {

    @Test
    fun allowsOnlyExactTaobaoAndTmallHttpsHosts() {
        assertTrue(
            AllowedEcommerceHosts.isAllowedHttpsUrl(
                "https://item.taobao.com/item.htm?id=123"
            )
        )
        assertTrue(
            AllowedEcommerceHosts.isAllowedHttpsUrl(
                "https://detail.tmall.com/item.htm?id=123"
            )
        )
        assertFalse(
            AllowedEcommerceHosts.isAllowedHttpsUrl(
                "https://fake-taobao.com/item.htm?id=123"
            )
        )
        assertFalse(AllowedEcommerceHosts.isAllowedHttpsUrl("https://127.0.0.1/test"))
        assertFalse(AllowedEcommerceHosts.isAllowedHttpsUrl("http://item.taobao.com/item.htm?id=123"))
        assertFalse(AllowedEcommerceHosts.isAllowedHttpsUrl("https://item.taobao.com:8443/test"))
    }

    @Test
    fun identifiesPlatformWithoutContainsMatching() {
        assertEquals(
            EcommercePlatform.TAOBAO,
            AllowedEcommerceHosts.platformForUrl("https://e.tb.cn/h.test")
        )
        assertEquals(
            EcommercePlatform.TMALL,
            AllowedEcommerceHosts.platformForUrl("https://m.tmall.com/item.htm?id=123")
        )
        assertEquals(
            EcommercePlatform.UNKNOWN,
            AllowedEcommerceHosts.platformForUrl("https://taobao.example.com/test")
        )
    }
}
