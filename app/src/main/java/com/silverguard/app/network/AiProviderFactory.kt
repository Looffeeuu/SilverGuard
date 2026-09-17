package com.silverguard.app.network

import com.silverguard.app.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object AiProviderFactory {
    fun create(): AiRiskAnalysisProvider? {
        if (!BuildConfig.SILVERGUARD_AI_ENABLED) return null
        val endpoint = BuildConfig.SILVERGUARD_AI_PROXY_URL.trim()
        val clientToken = BuildConfig.SILVERGUARD_AI_PROXY_TOKEN.trim()
        val parsed = endpoint.toHttpUrlOrNull() ?: return null
        if (parsed.scheme != "https" || clientToken.isBlank()) return null
        return GlmRiskAnalysisProvider(
            endpointUrl = endpoint,
            clientToken = clientToken
        )
    }
}
