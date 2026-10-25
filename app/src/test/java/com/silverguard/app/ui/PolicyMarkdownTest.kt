package com.silverguard.app.ui

import com.silverguard.app.ui.PolicyMarkdown.Block
import com.silverguard.app.ui.PolicyMarkdown.Span
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyMarkdownTest {

    @Test
    fun parsesHeadingsParagraphsAndLists() {
        val blocks = PolicyMarkdown.parse(
            """
            # 标题
            生效日期：【待填写】

            ## 一、小节
            - 第一条
            1. **读取商品链接**：说明
               - 缩进条目
               补充说明
            """.trimIndent()
        )

        assertEquals(Block.Heading(1, listOf(Span("标题"))), blocks[0])
        assertEquals(Block.Paragraph(listOf(Span("生效日期：【待填写】"))), blocks[1])
        assertEquals(Block.Heading(2, listOf(Span("一、小节"))), blocks[2])
        assertEquals(Block.ListItem("•", listOf(Span("第一条"))), blocks[3])
        assertEquals(Block.ListItem("1.", listOf(Span("读取商品链接", bold = true), Span("：说明"))), blocks[4])
        assertEquals(Block.ListItem("•", listOf(Span("缩进条目")), indent = 1), blocks[5])
        assertEquals(Block.Paragraph(listOf(Span("补充说明")), indent = 1), blocks[6])
    }

    @Test
    fun parsesTablesWithoutTheSeparatorRow() {
        val table = PolicyMarkdown.parse(
            """
            | 信息 | 用途 |
            | --- | --- |
            | 家人电话 | **不参与**云备份 |
            """.trimIndent()
        ).single() as Block.Table

        assertEquals(listOf(listOf(Span("信息")), listOf(Span("用途"))), table.header)
        assertEquals(1, table.rows.size)
        assertEquals(listOf(Span("不参与", bold = true), Span("云备份")), table.rows.single()[1])
    }

    @Test
    fun parsesLinksAndBoldInsideText() {
        assertEquals(
            listOf(Span("详见 "), Span("ML Kit 条款", url = "https://developers.google.com/ml-kit/terms"), Span("。")),
            PolicyMarkdown.inlineSpans("详见 [ML Kit 条款](https://developers.google.com/ml-kit/terms)。")
        )
    }

    @Test
    fun bundledPolicyParsesIntoReadableBlocks() {
        val blocks = PolicyMarkdown.parse(File("src/main/assets/privacy_policy.md").readText(Charsets.UTF_8))

        assertTrue(blocks.first() == Block.Heading(1, listOf(Span("银龄安心查隐私政策"))))
        assertEquals(7, blocks.count { it is Block.Heading && it.level == 2 })
        assertTrue(blocks.any { it is Block.Table })
        // No Markdown syntax should reach the screen.
        val shownText = blocks.flatMap { block ->
            when (block) {
                is Block.Heading -> block.spans
                is Block.Paragraph -> block.spans
                is Block.ListItem -> block.spans
                is Block.Table -> (block.header + block.rows.flatten()).flatten()
            }
        }.joinToString("") { it.text }
        assertTrue(shownText, listOf("**", "](", "| ---").none { it in shownText })
    }
}
