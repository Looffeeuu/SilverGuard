package com.silverguard.app.engine

object UrlAnalysisSanitizer {

    fun stripUrlsForAnalysis(text: String): String = UrlExtractor
        .removeAll(text)
        .trim()
}
