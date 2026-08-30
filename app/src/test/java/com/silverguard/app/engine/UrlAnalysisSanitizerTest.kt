package com.silverguard.app.engine

import com.silverguard.app.model.EcommercePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlAnalysisSanitizerTest {

    @Test
    fun riskAnalysisKeepsClaimsButExcludesUrl() {
        val analysis = RiskAnalyzer.analyze(
            "七天降血糖 不用吃药\nhttps://item.taobao.com/item.htm?id=123"
        )

        assertTrue(analysis.flags.any { it.matched == "降血糖" })
        assertTrue(analysis.flags.any { it.matched == "不用吃药" })
        assertFalse(analysis.rawText.contains("http"))
        assertFalse(analysis.rawText.contains("id=123"))
        assertEquals(EcommercePlatform.TAOBAO, analysis.ecommerceLinkInfo?.platform)
    }

    @Test
    fun sanitizerRemovesEveryUrlAndPreservesSurroundingText() {
        val result = UrlAnalysisSanitizer.stripUrlsForAnalysis(
            "前文 https://example.com/a?price=999 后文 https://item.jd.com/12345.html"
        )

        assertEquals("前文 后文", result)
    }
}
