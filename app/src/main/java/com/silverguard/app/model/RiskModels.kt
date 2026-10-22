package com.silverguard.app.model

enum class RiskLevel {
    LOW, MEDIUM, HIGH
}

data class RiskFlag(
    val type: String,
    val matched: String,
    val explanation: String,
    val weight: Int
)

data class RiskAnalysis(
    val rawText: String,
    val category: String,
    val score: Int,
    val level: RiskLevel,
    val title: String,
    val flags: List<RiskFlag>,
    val productInfo: ProductInfo = ProductInfo(),
    val verification: VerificationResult,
    val claimConflicts: List<ClaimRegistrationConflict> = emptyList(),
    val ecommerceLinkInfo: EcommerceLinkInfo? = null,
    val ecommerceProduct: EcommerceProduct? = null,
    val aiAnalysis: AiAnalysisResult = AiAnalysisResult(),
    val priceReference: PriceReferenceResult = PriceReferenceResult(),
    val relatedCases: List<RiskCaseMatch> = emptyList(),
    /** Local UDI registry lookup; null when there is no device number or no index on this phone. */
    val deviceRegistry: DeviceRegistryCheck? = null
) {
    /** A manual check or the local registry found that the packaging does not match the registered record. */
    val hasVerificationMismatch: Boolean
        get() = verification.manualRecord?.hasMismatch == true || deviceRegistry?.hasMismatch == true

    /**
     * The headline shown and read aloud. [title] only reflects the advertising wording, so a mismatch
     * with the registered record must not be headed "暂未发现明显高风险".
     */
    val headline: String
        get() = if (hasVerificationMismatch) "信息对不上，建议暂缓付款" else title
}
