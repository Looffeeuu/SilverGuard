package com.silverguard.app.ui

/**
 * Parses the small Markdown subset used by the privacy policy (assets/privacy_policy.md):
 * headings, one paragraph per line, "-" and "1." lists (indented ones too), tables,
 * **bold** and [links](https://…). Kept dependency-free so it can be unit tested.
 */
internal object PolicyMarkdown {

    data class Span(val text: String, val bold: Boolean = false, val url: String? = null)

    sealed interface Block {
        data class Heading(val level: Int, val spans: List<Span>) : Block
        data class Paragraph(val spans: List<Span>, val indent: Int = 0) : Block
        data class ListItem(val marker: String, val spans: List<Span>, val indent: Int = 0) : Block
        data class Table(val header: List<List<Span>>, val rows: List<List<List<Span>>>) : Block
    }

    private val heading = Regex("^(#{1,6})\\s+(.*)$")
    private val bullet = Regex("^(\\s*)-\\s+(.*)$")
    private val numbered = Regex("^(\\s*)(\\d+)\\.\\s+(.*)$")
    private val tableSeparator = Regex("^\\|?\\s*:?-{3,}")
    private val inline = Regex("\\*\\*(.+?)\\*\\*|\\[([^\\]]+)]\\((https?://[^)\\s]+)\\)")

    fun parse(markdown: String): List<Block> {
        val blocks = mutableListOf<Block>()
        val lines = markdown.replace("\r\n", "\n").lines()
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            when {
                line.isBlank() -> Unit
                line.trimStart().startsWith("|") -> {
                    val tableLines = mutableListOf<String>()
                    while (index < lines.size && lines[index].trimStart().startsWith("|")) {
                        tableLines += lines[index].trim()
                        index++
                    }
                    index--
                    val cells = tableLines
                        .filterNot { tableSeparator.containsMatchIn(it) }
                        .map { row -> row.trim('|').split("|").map { inlineSpans(it.trim()) } }
                    if (cells.isNotEmpty()) blocks += Block.Table(cells.first(), cells.drop(1))
                }
                heading.matches(line) -> heading.find(line)!!.destructured.let { (marks, text) ->
                    blocks += Block.Heading(marks.length, inlineSpans(text))
                }
                bullet.matches(line) -> bullet.find(line)!!.destructured.let { (spaces, text) ->
                    blocks += Block.ListItem("•", inlineSpans(text), indentOf(spaces))
                }
                numbered.matches(line) -> numbered.find(line)!!.destructured.let { (spaces, number, text) ->
                    blocks += Block.ListItem("$number.", inlineSpans(text), indentOf(spaces))
                }
                else -> blocks += Block.Paragraph(inlineSpans(line.trim()), indentOf(line.takeWhile { it == ' ' }))
            }
            index++
        }
        return blocks
    }

    fun inlineSpans(text: String): List<Span> {
        val spans = mutableListOf<Span>()
        var position = 0
        for (match in inline.findAll(text)) {
            if (match.range.first > position) spans += Span(text.substring(position, match.range.first))
            val (bold, linkText, url) = match.destructured
            spans += if (bold.isNotEmpty()) Span(bold, bold = true) else Span(linkText, url = url)
            position = match.range.last + 1
        }
        if (position < text.length) spans += Span(text.substring(position))
        return spans
    }

    private fun indentOf(spaces: String): Int = if (spaces.length >= 2) 1 else 0
}
