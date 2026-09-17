package com.silverguard.app.engine

import com.silverguard.app.data.RiskCaseRepository
import com.silverguard.app.model.RiskCaseKind
import java.net.URI
import org.junit.Assert.*
import org.junit.Test

class RiskCaseMatcherTest {
    @Test fun healthClaimMatchesPublishedCaseWithExplanation() {
        val match = RiskCaseMatcher.match("保健品七天降血糖，不用吃药").first()
        assertEquals("health-disease-claims", match.case.id)
        assertEquals(RiskCaseKind.PUBLISHED_CASE, match.case.kind)
        assertTrue(match.matchedTerms.contains("保健品"))
    }

    @Test fun ordinaryGoodsDoNotGetUnrelatedHealthCases() {
        assertTrue(RiskCaseMatcher.match("普通毛巾 纯棉 20元").isEmpty())
        assertTrue(RiskCaseMatcher.match("药品可用于治疗疾病").isEmpty())
    }

    @Test fun giftsAloneDoNotImplyPrivateSales() {
        assertTrue(RiskCaseMatcher.match("超市鸡蛋10元").isEmpty())
        assertEquals("gift-private-sales", RiskCaseMatcher.match("免费送鸡蛋，加微信进群").single().case.id)
    }

    @Test fun linksAreNotInterpretedAsCaseClaims() {
        assertTrue(RiskCaseMatcher.match("https://example.com/保健品降血糖").isEmpty())
    }

    @Test fun investmentNoticeIsDistinctFromAdjudicatedCase() {
        val match = RiskCaseMatcher.match("退休赚钱 投资项目 保本高收益").single()
        assertEquals(RiskCaseKind.CONSUMER_NOTICE, match.case.kind)
    }

    @Test fun repositoryHasTraceableOfficialSourcesAndUniqueIds() {
        val cases = RiskCaseRepository.cases
        assertEquals(cases.size, cases.map { it.id }.distinct().size)
        cases.forEach {
            val uri = URI(it.sourceUrl)
            assertEquals("https", uri.scheme)
            assertTrue(uri.host in setOf("www.samr.gov.cn", "www.nfra.gov.cn"))
            assertTrue(it.publishedOn.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
            assertTrue(it.sourceTitle.isNotBlank())
            assertTrue(it.termGroups.isNotEmpty() && it.termGroups.all(List<String>::isNotEmpty))
        }
    }

    @Test fun reportSeparatesSimilarWordingFromCurrentProductIdentity() {
        val report = ShareReportBuilder.build(RiskAnalyzer.analyze("保健品 七天降血糖"))
        assertTrue(report.contains("【类似案例与消费提示】"))
        assertTrue(report.contains("相似话术："))
        assertTrue(report.contains("https://www.samr.gov.cn/"))
        assertTrue(report.contains("不代表当前商品或商家属于案例对象"))
        assertFalse(report.contains("【AI 深入分析】"))
    }
}
