package com.silverguard.app.data

import java.net.URI
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialSourcesTest {

    @Test
    fun everySourceUsesAnOfficialHttpsDomain() {
        val allowedDomains = listOf("nmpa.gov.cn", "samr.gov.cn", "gsxt.gov.cn")

        OfficialSources.all.forEach { source ->
            val uri = URI(source.url)
            assertTrue("${source.id} must use HTTPS", uri.scheme == "https")
            assertTrue(
                "${source.id} must use an official domain",
                allowedDomains.any { domain ->
                    uri.host == domain || uri.host.endsWith(".$domain")
                }
            )
        }
    }
}
