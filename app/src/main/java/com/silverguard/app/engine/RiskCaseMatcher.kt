package com.silverguard.app.engine

import com.silverguard.app.data.RiskCaseRepository
import com.silverguard.app.model.RiskCaseMatch

object RiskCaseMatcher {
    fun match(text: String): List<RiskCaseMatch> {
        val material = UrlAnalysisSanitizer.stripUrlsForAnalysis(text)
        return RiskCaseRepository.cases.mapNotNull { case ->
            val terms = case.termGroups.map { group -> group.firstOrNull { material.contains(it, ignoreCase = true) } }
            if (terms.any { it == null }) null else RiskCaseMatch(case, terms.filterNotNull().distinct())
        }.take(3)
    }
}
