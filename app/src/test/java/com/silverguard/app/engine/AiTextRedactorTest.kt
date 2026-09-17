package com.silverguard.app.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiTextRedactorTest {
    @Test
    fun hidesContactDetailsAndRemovesUrlsBeforeAiRequest() {
        val redacted = AiTextRedactor.redact(
            "咨询电话 13812345678，邮箱 buyer@example.com，身份证 110101199001011234，" +
                "微信 abc_123，详情 https://item.taobao.com/item.htm?id=123"
        )

        assertFalse(redacted.contains("13812345678"))
        assertFalse(redacted.contains("buyer@example.com"))
        assertFalse(redacted.contains("110101199001011234"))
        assertFalse(redacted.contains("abc_123"))
        assertFalse(redacted.contains("https://"))
        assertTrue(redacted.contains("已隐藏"))
    }

    @Test
    fun limitsVeryLongText() {
        assertTrue(AiTextRedactor.redact("宣传语".repeat(3_000)).length <= 6_000)
    }
}
