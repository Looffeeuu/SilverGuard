package com.silverguard.app.model

enum class OfficialSearchOutcome(val displayName: String) {
    NOT_RECORDED("尚未记录查询结果"),
    RECORD_FOUND("在官方页面找到了记录"),
    RECORD_NOT_FOUND("在官方页面暂未找到记录"),
    PAGE_UNAVAILABLE("官方页面暂时无法访问")
}

enum class EvidenceMatchStatus {
    NOT_CHECKED,
    MATCHED,
    MISMATCHED
}

enum class VerificationEvidenceField(
    val displayName: String,
    val matchedLabel: String,
    val mismatchedLabel: String
) {
    REGISTRATION_NUMBER("注册 / 备案号", "编号一致", "编号不一致"),
    PRODUCT_NAME("产品名称", "名称一致", "名称不一致"),
    REGISTRANT("注册人 / 备案人", "企业一致", "企业不一致"),
    MODEL_OR_SPECIFICATION("型号规格", "型号一致", "型号不一致"),
    REGISTERED_SCOPE("登记用途 / 适用范围", "未发现冲突", "与宣传有冲突"),
    REGISTRATION_STATUS("登记状态", "状态正常", "状态异常")
}

data class VerificationChecklistItem(
    val field: VerificationEvidenceField,
    val expectedValue: String? = null,
    val guidance: String,
    val required: Boolean
)

data class VerificationFinding(
    val item: VerificationChecklistItem,
    val status: EvidenceMatchStatus = EvidenceMatchStatus.NOT_CHECKED
)

data class ManualVerificationRecord(
    val source: OfficialSource,
    val openedAt: Long,
    val reviewedAt: Long? = null,
    val searchOutcome: OfficialSearchOutcome = OfficialSearchOutcome.NOT_RECORDED,
    val findings: List<VerificationFinding>
) {
    val checkedCount: Int
        get() = findings.count { it.status != EvidenceMatchStatus.NOT_CHECKED }

    val mismatchCount: Int
        get() = findings.count { it.status == EvidenceMatchStatus.MISMATCHED }

    val hasMismatch: Boolean
        get() = mismatchCount > 0

    val requiredChecksComplete: Boolean
        get() = findings
            .filter { it.item.required }
            .all { it.status != EvidenceMatchStatus.NOT_CHECKED }

    val summary: String
        get() = when {
            searchOutcome == OfficialSearchOutcome.RECORD_NOT_FOUND ->
                "人工查询暂未找到记录"
            searchOutcome == OfficialSearchOutcome.PAGE_UNAVAILABLE ->
                "官方页面暂时无法访问"
            searchOutcome == OfficialSearchOutcome.NOT_RECORDED ->
                "已打开官方入口，等待记录结果"
            hasMismatch ->
                "人工核对发现不一致"
            requiredChecksComplete ->
                "人工核对暂未发现不一致"
            checkedCount > 0 ->
                "已记录部分人工核对"
            else ->
                "已找到记录，等待逐项核对"
        }
}
