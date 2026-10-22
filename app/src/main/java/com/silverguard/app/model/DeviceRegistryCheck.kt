package com.silverguard.app.model

enum class DeviceRegistryStatus {
    /** The number is in the local copy of the NMPA UDI database. */
    FOUND,

    /** The local copy has no such number. This alone says nothing about whether the product is genuine. */
    NOT_FOUND
}

/** Whether a field read from the packaging agrees with the registered record. */
enum class FieldAgreement {
    MATCHES,
    DIFFERS,
    NOT_COMPARED
}

data class DeviceRegistryRecord(
    val number: String,
    val holder: String,
    val productNames: List<String>,
    val category: String
)

/**
 * A lookup against the offline index built from the NMPA UDI database. It is a local snapshot dated
 * [dataDate], not a live official query, and never changes the risk score on its own.
 */
data class DeviceRegistryCheck(
    val queriedNumber: String,
    val status: DeviceRegistryStatus,
    val dataDate: String,
    val record: DeviceRegistryRecord? = null,
    val nameAgreement: FieldAgreement = FieldAgreement.NOT_COMPARED,
    val holderAgreement: FieldAgreement = FieldAgreement.NOT_COMPARED
) {
    val hasMismatch: Boolean
        get() = nameAgreement == FieldAgreement.DIFFERS || holderAgreement == FieldAgreement.DIFFERS
}
