package com.silverguard.app.data

import com.silverguard.app.model.OfficialSourceRole
import com.silverguard.app.model.RegistrationType
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
            assertTrue("${source.id} must explain how to query", source.queryHint.isNotBlank())
        }
    }

    @Test
    fun everyKnownRegistrationTypeStartsWithAPrimaryOfficialSource() {
        RegistrationType.entries
            .filter { it != RegistrationType.UNKNOWN }
            .forEach { type ->
                val sources = OfficialSources.forType(type)
                assertTrue("$type must have an official source", sources.isNotEmpty())
                assertTrue(
                    "$type must start with its primary lookup",
                    sources.first().role == OfficialSourceRole.PRIMARY
                )
            }
    }
}
