package com.silverguard.app.engine

import com.silverguard.app.model.EcommercePlatform
import com.silverguard.app.model.EcommerceProduct
import com.silverguard.app.model.ProductDetailParseStatus
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EcommerceProductToProductInfoMapperTest {

    @Test
    fun shopNameIsNeverMappedToManufacturer() {
        val product = EcommerceProduct(
            platform = EcommercePlatform.TAOBAO,
            productId = "123",
            sourceUrl = "https://item.taobao.com/item.htm?id=123",
            title = "测试理疗仪",
            shopName = "某某健康旗舰店",
            parseStatus = ProductDetailParseStatus.PARTIAL
        )

        val info = EcommerceProductToProductInfoMapper.toProductInfo(product)

        assertNull(info.manufacturer)
    }

    @Test
    fun riskyProductTitleParticipatesInRiskAnalysis() {
        val product = EcommerceProduct(
            platform = EcommercePlatform.TAOBAO,
            productId = "123",
            sourceUrl = "https://item.taobao.com/item.htm?id=123",
            title = "七天降血糖不用吃药保健品",
            parseStatus = ProductDetailParseStatus.PARTIAL
        )

        val analysis = RiskAnalyzer.analyze(product.sourceUrl, product)

        assertTrue(analysis.flags.any { it.matched == "降血糖" })
        assertTrue(analysis.flags.any { it.matched == "不用吃药" })
    }

    @Test
    fun emptyEcommerceResultDoesNotBreakManualAnalysis() {
        val product = EcommerceProduct(
            platform = EcommercePlatform.TAOBAO,
            productId = "123",
            sourceUrl = "https://item.taobao.com/item.htm?id=123",
            parseStatus = ProductDetailParseStatus.PARSE_FAILED
        )

        val analysis = RiskAnalyzer.analyze("太赫兹理疗仪 改善鼻炎", product)

        assertTrue(analysis.flags.any { it.matched == "改善鼻炎" })
        assertTrue(analysis.productInfo.name?.contains("理疗仪") == true)
    }
}
