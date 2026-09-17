package com.silverguard.app.network

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.silverguard.app.engine.RiskAnalyzer
import com.silverguard.app.model.AiAnalysisStatus
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlmRiskAnalysisProviderTest {
    @Test
    fun sendsStructuredGlmRequestWithoutEmbeddingAuthorization() = runBlocking {
        val capturedRequest = AtomicReference<Request>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                capturedRequest.set(chain.request())
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(successResponse().toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        val provider = GlmRiskAnalysisProvider(
            endpointUrl = "http://localhost/v1/chat/completions",
            clientToken = "test-client-token",
            client = client,
            allowHttpForTests = true
        )

        val result = provider.analyze(
            RiskAnalyzer.analyze("七天降血糖，不用吃药，咨询电话13812345678")
        )

        assertEquals(AiAnalysisStatus.SUCCESS, result.status)
        assertEquals("宣传包含需要重点核对的疾病功效承诺。", result.insight?.summary)
        val request = capturedRequest.get()
        assertNotNull(request)
        assertEquals(null, request.header("Authorization"))
        assertEquals("test-client-token", request.header(GlmRiskAnalysisProvider.CLIENT_TOKEN_HEADER))
        val buffer = Buffer()
        request.body?.writeTo(buffer)
        val requestJson = buffer.readUtf8()
        assertTrue(requestJson.contains("\"model\":\"glm-4.7-flash\""))
        assertTrue(requestJson.contains("\"type\":\"json_object\""))
        assertFalse(requestJson.contains("13812345678"))
    }

    @Test
    fun invalidAiJsonFallsBackWithoutChangingLocalResult() {
        val provider = GlmRiskAnalysisProvider("https://example.com", "test-client-token")
        val response = JsonObject().apply {
            add("choices", JsonArray().apply {
                add(JsonObject().apply {
                    add("message", JsonObject().apply { addProperty("content", "not-json") })
                })
            })
        }.toString()

        val result = provider.parseResponse(response)

        assertEquals(AiAnalysisStatus.INVALID_RESPONSE, result.status)
        assertEquals(null, result.insight)
        assertTrue(result.message.contains("本地分析结果不受影响"))
    }

    private fun successResponse(): String {
        val content = JsonObject().apply {
            addProperty("summary", "宣传包含需要重点核对的疾病功效承诺。")
            add("implicitClaims", JsonArray().apply { add("短期内可改善疾病") })
            add("persuasionTactics", JsonArray().apply { add("用确定期限增强可信感") })
            add("verificationQuestions", JsonArray().apply { add("登记用途是否包含该疾病？") })
            addProperty("consumerAdvice", "不要自行停药，先核对官方登记用途。")
            addProperty("confidence", "HIGH")
        }.toString()
        return JsonObject().apply {
            add("choices", JsonArray().apply {
                add(JsonObject().apply {
                    add("message", JsonObject().apply { addProperty("content", content) })
                })
            })
        }.toString()
    }
}
