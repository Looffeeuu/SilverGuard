package com.silverguard.app.engine

/** Normalises and masks a family member's phone number entered by hand. */
object PhoneNumbers {

    /** Mainland mobile numbers, landlines with area code, and short service numbers. */
    private val acceptable = Regex("\\+?\\d{5,15}")

    /** Returns digits only (keeping a leading +), or null if the input is not a plausible phone number. */
    fun normalize(raw: String): String? {
        val compact = raw.trim().replace(Regex("[\\s\\-()（）]"), "")
        return compact.takeIf { acceptable.matches(it) }
    }

    /** Hides the middle digits so the number is not read out in full on screen: 138****5678. */
    fun mask(number: String): String = when {
        number.length >= 11 -> number.take(3) + "****" + number.takeLast(4)
        number.length >= 7 -> number.take(2) + "***" + number.takeLast(2)
        else -> number
    }
}
