package com.silverguard.app.network

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.silverguard.app.engine.AiRiskPromptBuilder
import com.silverguard.app.model.AiAnalysisResult
import com.silverguard.app.model.AiAnalysisStatus
import com.silverguard.app.model.AiConfidence
import com.silverguard.app.model.AiRiskInsight
import com.silverguard.app.model.RiskAnalysis
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class GlmRiskAnalysisProvider(
    private val endpointUrl: String,
    private val clientToken: String,
    private val client: OkHttpClient = defaultClient(),
    private val allowHttpForTests: Boolean = false
) : AiRiskAnalysisProvider {

    override suspend fun analyze(analysis: RiskAnalysis): AiAnalysisResult = withContext(Dispatchers.IO) {
        val endpoint = endpointUrl.toHttpUrlOrNull()
            ?: return@withContext configurationError()
        if (endpoint.scheme != "https" && !allowHttpForTests) {
            return@withContext configurationError()
        }

        val prompt = AiRiskPromptBuilder.build(analysis)
        val body = JsonObject().apply {
            addProperty("model", MODEL_NAME)
            add("messages", com.google.gson.JsonArray().apply {
                add(message("system", prompt.systemMessage))
                add(message("user", prompt.userMessage))
            })
            addProperty("temperature", 0.2)
            addProperty("max_tokens", 900)
            addProperty("stream", false)
            add("response_format", JsonObject().apply { addProperty("type", "json_object") })
        }
        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "application/json")
            .header(CLIENT_TOKEN_HEADER, clientToken)

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return@use httpError(response.code)
                val responseText = response.body.string()
                parseResponse(responseText)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            AiAnalysisResult(
                status = AiAnalysisStatus.UNAVAILABLE,
                message = "AI 服务暂时连接不上，本地分析结果不受影响，可以稍后重试。"
            )
        } catch (_: Exception) {
            AiAnalysisResult(
                status = AiAnalysisStatus.ERROR,
                message = "AI 深入分析本次没有完成，本地分析结果仍可正常使用。"
            )
        }
    }

    internal fun parseResponse(responseText: String): AiAnalysisResult = try {
        val root = JsonParser.parseString(responseText).asJsonObject
        val content = root.getAsJsonArray("choices")
            ?.firstOrNull()
            ?.asJsonObject
            ?.getAsJsonObject("message")
            ?.get("content")
            ?.asString
            ?.removeSurrounding("```json", "```")
            ?.removeSurrounding("```", "```")
            ?.trim()
            .orEmpty()
        val result = JsonParser.parseString(content).asJsonObject
        val summary = result.requiredText("summary", 240)
        val advice = result.requiredText("consumerAdvice", 240)
        val confidence = runCatching {
            AiConfidence.valueOf(result.requiredText("confidence", 12).uppercase())
        }.getOrNull() ?: throw IllegalArgumentException("Invalid confidence")

        AiAnalysisResult(
            status = AiAnalysisStatus.SUCCESS,
            insight = AiRiskInsight(
                summary = summary,
                implicitClaims = result.stringList("implicitClaims", 3),
                persuasionTactics = result.stringList("persuasionTactics", 3),
                verificationQuestions = result.stringList("verificationQuestions", 4),
                consumerAdvice = advice,
                confidence = confidence
            ),
            message = "AI 深入分析已完成"
        )
    } catch (_: Exception) {
        AiAnalysisResult(
            status = AiAnalysisStatus.INVALID_RESPONSE,
            message = "AI 返回内容无法安全读取，本地分析结果不受影响，可以稍后重试。"
        )
    }

    private fun httpError(code: Int): AiAnalysisResult = when (code) {
        401, 403 -> AiAnalysisResult(
            status = AiAnalysisStatus.NOT_CONFIGURED,
            message = "AI 服务授权尚未配置好，本地分析结果仍可正常使用。"
        )
        408, 429, in 500..599 -> AiAnalysisResult(
            status = AiAnalysisStatus.UNAVAILABLE,
            message = "AI 服务现在比较忙，本地分析结果不受影响，可以稍后重试。"
        )
        else -> AiAnalysisResult(
            status = AiAnalysisStatus.ERROR,
            message = "AI 深入分析本次没有完成，本地分析结果仍可正常使用。"
        )
    }

    private fun configurationError() = AiAnalysisResult(
        status = AiAnalysisStatus.NOT_CONFIGURED,
        message = "AI 深入分析尚未启用，本地分析结果仍可正常使用。"
    )

    private fun message(role: String, content: String) = JsonObject().apply {
        addProperty("role", role)
        addProperty("content", content)
    }

    private fun JsonObject.requiredText(name: String, maxLength: Int): String =
        get(name)?.takeIf { it.isJsonPrimitive }?.asString?.trim()?.take(maxLength)
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing $name")

    private fun JsonObject.stringList(name: String, maxItems: Int): List<String> =
        getAsJsonArray(name)
            ?.mapNotNull { item ->
                item.takeIf { it.isJsonPrimitive }?.asString?.trim()?.take(180)?.takeIf(String::isNotBlank)
            }
            ?.take(maxItems)
            .orEmpty()

    companion object {
        const val MODEL_NAME = "glm-4.7-flash"
        const val CLIENT_TOKEN_HEADER = "X-SilverGuard-Client-Token"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private fun defaultClient() = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(50, TimeUnit.SECONDS)
            .build()
    }
}
