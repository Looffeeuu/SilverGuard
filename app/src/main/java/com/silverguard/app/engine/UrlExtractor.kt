package com.silverguard.app.engine

object UrlExtractor {

    private val httpUrlRegex = Regex(
        pattern = """(?i)https?://[^\s<>\"'，。；、）》】]+"""
    )

    private val trailingPunctuation = charArrayOf(
        ',', ';', '!', ')', ']', '}', '>',
        '，', '；', '！', '？', '）', '】', '》'
    )

    fun extractAll(text: String): List<String> = httpUrlRegex
        .findAll(text)
        .map { it.value.trimEnd(*trailingPunctuation) }
        .filter { it.isNotBlank() }
        .distinct()
        .toList()

    fun removeAll(text: String): String = httpUrlRegex
        .replace(text, " ")
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex(" *\\n *"), "\n")
}
