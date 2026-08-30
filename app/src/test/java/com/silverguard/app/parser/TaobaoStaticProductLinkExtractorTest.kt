package com.silverguard.app.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaobaoStaticProductLinkExtractorTest {

    @Test
    fun keepsOnlyProductIdFromPublicStaticTarget() {
        val result = TaobaoStaticProductLinkExtractor.extractCleanProductUrl(
            TaobaoHtmlFixtures.shortPageWithStaticProductUrl
        )

        assertEquals(
            "https://item.taobao.com/item.htm?id=1044769261323",
            result
        )
    }

    @Test
    fun ignoresUnknownStaticTargets() {
        val result = TaobaoStaticProductLinkExtractor.extractCleanProductUrl(
            "<script>var target='https://example.com/product?id=123'</script>"
        )

        assertNull(result)
    }
}
