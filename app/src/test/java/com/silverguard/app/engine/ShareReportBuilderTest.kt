package com.silverguard.app.engine

import org.junit.Assert.assertTrue
import org.junit.Test

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
}
