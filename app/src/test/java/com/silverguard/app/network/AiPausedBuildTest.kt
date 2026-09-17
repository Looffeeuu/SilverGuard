package com.silverguard.app.network

import com.silverguard.app.BuildConfig
import org.junit.Assert.*
import org.junit.Test

class AiPausedBuildTest {
    @Test fun offlineReleaseCannotCreateRemoteAiProviderOrPackageProxyCredentials() {
        assertFalse(BuildConfig.SILVERGUARD_AI_ENABLED)
        assertEquals("", BuildConfig.SILVERGUARD_AI_PROXY_URL)
        assertEquals("", BuildConfig.SILVERGUARD_AI_PROXY_TOKEN)
        assertNull(AiProviderFactory.create())
    }
}
