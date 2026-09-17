package com.silverguard.app.engine

import com.silverguard.app.model.PriceQuoteInput
import com.silverguard.app.model.PriceReferenceInput
import com.silverguard.app.model.PriceReferenceStatus
import org.junit.Assert.*
import org.junit.Test

class PriceReferenceEvaluatorTest {
    private fun ready(price: String = "120") = PriceReferenceInput(
        askingPrice = price,
        quotes = listOf(PriceQuoteInput("甲店", "100"), PriceQuoteInput("乙店", "120"), PriceQuoteInput("丙店", "140")),
        sameProductConfirmed = true
    )

    @Test fun parsesExactRenminbiWithoutFloatingPointRounding() {
        assertEquals(123450L, PriceReferenceEvaluator.parseCents("￥1,234.50元"))
        assertEquals(1999L, PriceReferenceEvaluator.parseCents("１９．９９"))
        assertEquals(1L, PriceReferenceEvaluator.parseCents("0.01"))
        assertEquals("¥19.99", PriceReferenceEvaluator.formatPrice(1999))
    }

    @Test fun rejectsAmbiguousAndInvalidAmounts() {
        listOf("", "0", "-1", "100-200", "100起", "定金20", "每月50", "USD100", "1,23", "1.001", "NaN", "1e2", "999999999999999").forEach {
            assertNull("Must reject $it", PriceReferenceEvaluator.parseCents(it))
        }
    }

    @Test fun missingInformationNeverInventsRange() {
        val result = PriceReferenceEvaluator.evaluate(PriceReferenceInput())
        assertEquals(PriceReferenceStatus.NOT_READY, result.status)
        assertFalse(result.hasRange)
        assertNull(result.recordedAt)
    }

    @Test fun requiresAtLeastThreeQuotes() {
        assertEquals(PriceReferenceStatus.NOT_READY, PriceReferenceEvaluator.evaluate(ready().copy(quotes = ready().quotes.take(2))).status)
    }

    @Test fun requiresExplicitConfirmationOfComparableProducts() {
        val result = PriceReferenceEvaluator.evaluate(ready().copy(sameProductConfirmed = false))
        assertEquals(PriceReferenceStatus.NEED_CONFIRMATION, result.status)
        assertFalse(result.hasRange)
    }

    @Test fun detectsHighPriceAgainstOnlyRecordedSamples() {
        val result = PriceReferenceEvaluator.evaluate(ready("150"), now = 1234L)
        assertEquals(PriceReferenceStatus.ABOVE_RANGE, result.status)
        assertEquals(10000L, result.minCents)
        assertEquals(14000L, result.maxCents)
        assertEquals(1234L, result.recordedAt)
    }

    @Test fun lowPriceIsNotCalledCounterfeit() {
        val result = PriceReferenceEvaluator.evaluate(ready("80"))
        assertEquals(PriceReferenceStatus.BELOW_RANGE, result.status)
        assertTrue(result.message.contains("低价不等于假货"))
    }

    @Test fun includesBothRangeBoundaries() {
        listOf("100", "120", "140").forEach {
            assertEquals(PriceReferenceStatus.WITHIN_RANGE, PriceReferenceEvaluator.evaluate(ready(it)).status)
        }
    }

    @Test fun repeatedStoreDoesNotCountAsIndependentQuotes() {
        val input = ready().copy(quotes = listOf(PriceQuoteInput("Ａ 店", "100"), PriceQuoteInput("A店", "120"), PriceQuoteInput("丙店", "140")))
        assertEquals(PriceReferenceStatus.INVALID_INPUT, PriceReferenceEvaluator.evaluate(input).status)
    }

    @Test fun partiallyEnteredQuoteIsNotSilentlyDiscarded() {
        val input = ready().copy(quotes = ready().quotes + PriceQuoteInput("丁店", ""))
        assertEquals(PriceReferenceStatus.INVALID_INPUT, PriceReferenceEvaluator.evaluate(input).status)
    }

    @Test fun blankExtraRowDoesNotChangeResult() {
        val input = ready().copy(quotes = ready().quotes + PriceQuoteInput())
        assertEquals(PriceReferenceStatus.WITHIN_RANGE, PriceReferenceEvaluator.evaluate(input).status)
    }

    @Test fun sourceIsRequiredAndQuoteCountIsBounded() {
        val missingSource = ready().copy(quotes = listOf(PriceQuoteInput("", "100")) + ready().quotes.drop(1))
        assertEquals(PriceReferenceStatus.INVALID_INPUT, PriceReferenceEvaluator.evaluate(missingSource).status)
        assertEquals(PriceReferenceStatus.INVALID_INPUT, PriceReferenceEvaluator.evaluate(ready().copy(quotes = List(6) { PriceQuoteInput() })).status)
    }

    @Test fun referenceDoesNotChangeLocalRiskScore() {
        val analysis = RiskAnalyzer.analyze("商品名称：毛巾\n价格：150元")
        val updated = analysis.copy(priceReference = PriceReferenceEvaluator.evaluate(ready("150")))
        assertEquals(0, updated.score)
        assertEquals(analysis.level, updated.level)
    }

    @Test fun referenceReportRecordsBasisSourcesAndTime() {
        val analysis = RiskAnalyzer.analyze("普通日用品").copy(priceReference = PriceReferenceEvaluator.evaluate(ready(), 1234L))
        val report = ShareReportBuilder.build(analysis)
        assertTrue(report.contains("【价格参考】"))
        assertTrue(report.contains("¥100 ～ ¥140"))
        assertTrue(report.contains("报价来源：甲店"))
        assertTrue(report.contains("录入时间："))
        assertTrue(report.contains("不是实时市场价"))
    }

    @Test fun newAnalysisDoesNotReuseUnconfirmedOldQuotes() {
        val initial = RiskAnalyzer.analyze("毛巾150元").copy(priceReference = PriceReferenceEvaluator.evaluate(ready()))
        val updated = RiskAnalyzer.analyze(initial.rawText + "\n这是两条装")
        assertFalse(updated.priceReference.hasRange)
        assertTrue(updated.priceReference.input.quotes.all { it.price.isEmpty() })
    }
}
