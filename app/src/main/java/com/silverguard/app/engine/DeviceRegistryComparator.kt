package com.silverguard.app.engine

import com.silverguard.app.model.DeviceRegistryCheck
import com.silverguard.app.model.DeviceRegistryRecord
import com.silverguard.app.model.DeviceRegistryStatus
import com.silverguard.app.model.FieldAgreement
import com.silverguard.app.model.ProductInfo

/**
 * Compares the packaging with the registered record. A scam may print a real registration number
 * that belongs to a different product or company, so a mismatch is worth pointing out; the wording
 * stays cautious because OCR and short brand names can differ from the registered names.
 */
object DeviceRegistryComparator {

    private val companySuffixes = Regex("股份有限公司|有限责任公司|有限公司|集团|股份|公司|\\s")
    private val punctuation = Regex("[\\s()（）【】\\[\\]·\\-—/、，,。]")

    fun check(
        queriedNumber: String,
        record: DeviceRegistryRecord?,
        productInfo: ProductInfo,
        dataDate: String
    ): DeviceRegistryCheck {
        if (record == null) {
            return DeviceRegistryCheck(queriedNumber, DeviceRegistryStatus.NOT_FOUND, dataDate)
        }
        return DeviceRegistryCheck(
            queriedNumber = queriedNumber,
            status = DeviceRegistryStatus.FOUND,
            dataDate = dataDate,
            record = record,
            nameAgreement = compareName(productInfo.name, record.productNames),
            holderAgreement = compareHolder(productInfo.manufacturer, record.holder)
        )
    }

    fun compareName(seen: String?, registered: List<String>): FieldAgreement {
        val packaging = seen?.replace(punctuation, "").orEmpty()
        if (packaging.length < 2 || registered.isEmpty()) return FieldAgreement.NOT_COMPARED
        val matches = registered.any { name ->
            val official = name.replace(punctuation, "")
            official.isNotEmpty() &&
                (packaging.contains(official) || official.contains(packaging) || bigramOverlap(packaging, official) >= 0.5)
        }
        return if (matches) FieldAgreement.MATCHES else FieldAgreement.DIFFERS
    }

    fun compareHolder(seen: String?, registered: String): FieldAgreement {
        val packaging = seen?.replace(companySuffixes, "").orEmpty()
        val official = registered.replace(companySuffixes, "")
        if (packaging.length < 2 || official.isEmpty()) return FieldAgreement.NOT_COMPARED
        val matches = packaging.contains(official) || official.contains(packaging) ||
            bigramOverlap(packaging, official) >= 0.6
        return if (matches) FieldAgreement.MATCHES else FieldAgreement.DIFFERS
    }

    /** Share of the shorter text's character pairs that also appear in the longer one. */
    private fun bigramOverlap(a: String, b: String): Double {
        fun bigrams(text: String) = text.windowed(2).toSet()
        val first = bigrams(a)
        val second = bigrams(b)
        val smaller = minOf(first.size, second.size)
        if (smaller == 0) return 0.0
        return first.intersect(second).size.toDouble() / smaller
    }
}
