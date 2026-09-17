package com.silverguard.app.data

import com.silverguard.app.engine.RiskAnalyzer
import com.silverguard.app.engine.PriceReferenceEvaluator
import com.silverguard.app.model.AnalysisHistoryEntry
import com.silverguard.app.model.AnalysisInputMethod
import com.silverguard.app.model.PriceReferenceInput
import com.silverguard.app.model.PriceQuoteInput
import org.junit.Assert.*
import org.junit.Test

class AnalysisHistoryCodecTest {
    private fun entry(id: String = "one", time: Long = 1234) = AnalysisHistoryEntry(
        id, time, RiskAnalyzer.analyze("保健品 七天降血糖 不用吃药"),
        setOf(AnalysisInputMethod.TEXT, AnalysisInputMethod.PHOTO, AnalysisInputMethod.SCREENSHOT)
    )

    @Test fun fullAnalysisAndAllInputSourcesSurviveRoundTrip() {
        val original = entry().copy(draftText = "保健品 七天降血糖 不用吃药\n还有尚未分析的说明")
        assertEquals(listOf(original), AnalysisHistoryCodec.decode(AnalysisHistoryCodec.encode(listOf(original))))
    }

    @Test fun pricesAndRecordedTimeArePreservedWithoutRequery() {
        val original = entry()
        val price = PriceReferenceEvaluator.evaluate(PriceReferenceInput("150", listOf(PriceQuoteInput("甲", "100"), PriceQuoteInput("乙", "120"), PriceQuoteInput("丙", "140")), true))
        val updated = original.copy(analysis = original.analysis.copy(priceReference = price))
        val restored = AnalysisHistoryCodec.decode(AnalysisHistoryCodec.encode(listOf(updated))).single()
        assertEquals(updated, restored)
        assertEquals(price.recordedAt, restored.analysis.priceReference.recordedAt)
    }

    @Test fun sameSessionUpdatesInsteadOfAddingDuplicates() {
        val updated = entry(time = 9999)
        val list = AnalysisHistoryCodec.upsert(listOf(entry()), updated)
        assertEquals(listOf(updated), list)
    }

    @Test fun newerSearchesAreFirstAndOnlyLatestFiftyAreKept() {
        val many = (1..65).map { entry("$it", it.toLong()) }
        val restored = AnalysisHistoryCodec.decode(AnalysisHistoryCodec.encode(many))
        assertEquals(50, restored.size)
        assertEquals("65", restored.first().id)
        assertEquals("16", restored.last().id)
    }

    @Test fun emptyHistoryIsValid() {
        assertTrue(AnalysisHistoryCodec.decode(AnalysisHistoryCodec.encode(emptyList())).isEmpty())
    }

    @Test(expected = Exception::class) fun damagedHistoryIsRejectedInsteadOfSilentlyErased() {
        AnalysisHistoryCodec.decode("{broken")
    }

    @Test(expected = IllegalArgumentException::class) fun unsupportedVersionIsPreservedForFutureMigration() {
        AnalysisHistoryCodec.decode("{\"version\":2,\"entries\":[]}")
    }
}
