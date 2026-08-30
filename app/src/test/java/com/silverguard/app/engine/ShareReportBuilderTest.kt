package com.silverguard.app.engine

import org.junit.Assert.assertTrue
import org.junit.Test
import com.silverguard.app.model.EcommerceFieldSource
import com.silverguard.app.model.EcommerceFieldType
import com.silverguard.app.model.EcommercePlatform
import com.silverguard.app.model.EcommerceProduct
import com.silverguard.app.model.EcommerceProductField
import com.silverguard.app.model.ProductDetailParseStatus

class ShareReportBuilderTest {

    @Test
    fun reportContainsProductAndVerificationSections() {
        val analysis = RiskAnalyzer.analyze(
            "商品名称：示例理疗仪\n生产企业：示例医疗科技有限公司\n注册证号：国械注准XXXXXXXX\n型号：SG-01"
        )

        val report = ShareReportBuilder.build(analysis)

        assertTrue(report.contains("【商品信息】"))
        assertTrue(report.contains("注册/备案号：国械注准XXXXXXXX"))
        assertTrue(report.contains("【官方核验】"))
        assertTrue(report.contains("当前状态：需要前往官方平台人工核验"))
        assertTrue(report.contains("不代表行政认定、医学诊断或官方认证"))
    }

    @Test
    fun reportContainsCleanEcommerceSourceWhenLinkWasDetected() {
        val analysis = RiskAnalyzer.analyze(
            "七天降血糖 不用吃药\nhttps://item.taobao.com/item.htm?id=123&spm=abc&utm_source=test"
        )

        val report = ShareReportBuilder.build(analysis)

        assertTrue(report.contains("【商品来源】"))
        assertTrue(report.contains("平台：淘宝"))
        assertTrue(report.contains("商品 ID：123"))
        assertTrue(report.contains("https://item.taobao.com/item.htm?id=123"))
        assertTrue(!report.contains("spm=abc"))
        assertTrue(report.contains("尚未自动读取完整商品详情"))
    }

    @Test
    fun reportContainsResolvedPublicProductDetailsAndSource() {
        val link = "https://item.taobao.com/item.htm?id=123"
        val product = EcommerceProduct(
            platform = EcommercePlatform.TAOBAO,
            productId = "123",
            sourceUrl = link,
            canonicalUrl = link,
            title = "测试理疗仪",
            price = "¥2980",
            sellerName = "安心旗舰店",
            parseStatus = ProductDetailParseStatus.SUCCESS,
            fields = listOf(
                EcommerceProductField(
                    EcommerceFieldType.TITLE,
                    "测试理疗仪",
                    EcommerceFieldSource.OPEN_GRAPH,
                    0.92f
                )
            ),
            fetchedAt = 123L,
            message = "已读取商品基础信息"
        )
        val report = ShareReportBuilder.build(RiskAnalyzer.analyze(link, product))

        assertTrue(report.contains("【银龄安心查 · v0.3.2】"))
        assertTrue(report.contains("商品：测试理疗仪"))
        assertTrue(report.contains("页面显示价格：¥2980"))
        assertTrue(report.contains("店铺/卖家：安心旗舰店"))
        assertTrue(report.contains("解析状态：已读取商品信息"))
        assertTrue(report.contains("商品名称来源：商品页面公开信息"))
    }
}
