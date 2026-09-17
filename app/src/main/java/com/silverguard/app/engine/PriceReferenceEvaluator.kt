package com.silverguard.app.engine

import com.silverguard.app.model.PriceReferenceInput
import com.silverguard.app.model.PriceReferenceResult
import com.silverguard.app.model.PriceReferenceStatus
import java.math.BigDecimal
import java.text.Normalizer

object PriceReferenceEvaluator {
    const val MAX_QUOTES = 5
    private const val MAX_CENTS = 999_999_999L
    private val moneyPattern = Regex("^(?:[¥￥]\\s*)?((?:[0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:\\.[0-9]{1,2})?)\\s*(?:元)?$")

    fun parseCents(text: String): Long? {
        if (text.length > 40) return null
        val normalized = Normalizer.normalize(text.trim(), Normalizer.Form.NFKC)
        val number = moneyPattern.matchEntire(normalized)?.groupValues?.get(1)?.replace(",", "") ?: return null
        return runCatching { BigDecimal(number).movePointRight(2).longValueExact() }
            .getOrNull()?.takeIf { it in 1..MAX_CENTS }
    }

    fun formatPrice(cents: Long): String = "¥${BigDecimal.valueOf(cents, 2).stripTrailingZeros().toPlainString()}"

    fun evaluate(input: PriceReferenceInput, now: Long = System.currentTimeMillis()): PriceReferenceResult {
        fun pending(status: PriceReferenceStatus, message: String) =
            PriceReferenceResult(input = input, status = status, message = message)

        val quotes = input.quotes.filter { it.source.isNotBlank() || it.price.isNotBlank() }
        if (input.quotes.size > MAX_QUOTES) {
            return pending(PriceReferenceStatus.INVALID_INPUT, "本版最多比较五家店铺的报价。")
        }
        val asking = parseCents(input.askingPrice)
        if (input.askingPrice.isNotBlank() && asking == null) {
            return pending(PriceReferenceStatus.INVALID_INPUT, "请填写一个人民币到手价，如 298.50；区间、起售价、定金和分期金额不能直接比较。")
        }
        if (quotes.any { it.source.isBlank() || it.source.length > 80 || parseCents(it.price) == null }) {
            return pending(PriceReferenceStatus.INVALID_INPUT, "每条报价都要填写店铺或来源，以及大于零的人民币到手价（最多两位小数）。")
        }
        val uniqueSources = quotes.map {
            Normalizer.normalize(it.source, Normalizer.Form.NFKC).filterNot(Char::isWhitespace).lowercase()
        }.distinct()
        if (uniqueSources.size != quotes.size) {
            return pending(PriceReferenceStatus.INVALID_INPUT, "同一家店铺不要重复计入，请补充不同店铺的报价。")
        }
        if (asking == null || quotes.size < 3) {
            return pending(PriceReferenceStatus.NOT_READY, "先填写当前到手价和至少三家店铺的报价。数据不足时，暂不判断价格高低。")
        }
        if (!input.sameProductConfirmed) {
            return pending(PriceReferenceStatus.NEED_CONFIRMATION, "请确认品牌、型号、规格、数量和购买条件相同；不同商品不能直接比较。")
        }
        val prices = quotes.map { requireNotNull(parseCents(it.price)) }
        val min = prices.min()
        val max = prices.max()
        val status = when {
            asking > max -> PriceReferenceStatus.ABOVE_RANGE
            asking < min -> PriceReferenceStatus.BELOW_RANGE
            else -> PriceReferenceStatus.WITHIN_RANGE
        }
        val message = when (status) {
            PriceReferenceStatus.ABOVE_RANGE -> "当前价格高于你录入的最高报价，先核对配置、服务和优惠条件，再决定。"
            PriceReferenceStatus.BELOW_RANGE -> "当前价格低于你录入的最低报价，先核对规格、运费和优惠条件；低价不等于假货。"
            else -> "当前价格在你录入的报价范围内，这不能证明商品安全、功效可靠或一定划算。"
        }
        return PriceReferenceResult(input, status, message, min, max, asking, now)
    }
}
