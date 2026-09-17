package com.silverguard.app.model

data class PriceQuoteInput(val source: String = "", val price: String = "")

data class PriceReferenceInput(
    val askingPrice: String = "",
    val quotes: List<PriceQuoteInput> = List(3) { PriceQuoteInput() },
    val sameProductConfirmed: Boolean = false
)

enum class PriceReferenceStatus(val displayName: String) {
    NOT_READY("还缺少可比报价"),
    INVALID_INPUT("请检查填写的价格"),
    NEED_CONFIRMATION("请先确认报价可比较"),
    ABOVE_RANGE("高于已录报价区间"),
    BELOW_RANGE("低于已录报价区间"),
    WITHIN_RANGE("在已录报价区间内")
}

data class PriceReferenceResult(
    val input: PriceReferenceInput = PriceReferenceInput(),
    val status: PriceReferenceStatus = PriceReferenceStatus.NOT_READY,
    val message: String = "至少填写三家店铺的同款、同规格到手价，再比较。",
    val minCents: Long? = null,
    val maxCents: Long? = null,
    val askingCents: Long? = null,
    val recordedAt: Long? = null
) {
    val hasRange: Boolean get() = minCents != null && maxCents != null
}
