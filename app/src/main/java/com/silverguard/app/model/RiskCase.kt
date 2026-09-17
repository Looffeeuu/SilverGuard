package com.silverguard.app.model

enum class RiskCaseKind(val displayName: String) {
    PUBLISHED_CASE("公开案例摘要"),
    CONSUMER_NOTICE("公开风险提示")
}

data class RiskCase(
    val id: String,
    val title: String,
    val kind: RiskCaseKind,
    val summary: String,
    val action: String,
    val sourceName: String,
    val sourceTitle: String,
    val sourceUrl: String,
    val publishedOn: String,
    // Every group must match; a group accepts any one of its terms.
    val termGroups: List<List<String>>
)

data class RiskCaseMatch(val case: RiskCase, val matchedTerms: List<String>)
