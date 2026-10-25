package com.silverguard.app

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silverguard.app.data.AnalysisHistoryRepository
import com.silverguard.app.data.FamilyContactRepository
import org.junit.Assert.assertTrue
import com.silverguard.app.ui.PrivacyConsent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Walks every main screen and checks what older users and TalkBack depend on:
 * touch targets of at least 48dp, a readable label on everything tappable, no clipped text,
 * and no text smaller than 14sp. Run it again with the system font scale at 2.0 before a release.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityAuditTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val issues = mutableListOf<String>()

    @Before
    fun cleanState() {
        AnalysisHistoryRepository(composeRule.activity).apply { clear(); persist() }
        FamilyContactRepository(composeRule.activity).save(null)
        composeRule.activity.getSharedPreferences("silverguard_accessibility", 0).edit().clear().commit()
        // These tests exercise the app itself; PrivacyConsentUiTest covers the consent screen.
        composeRule.activity.let { PrivacyConsent.accept(it, PrivacyConsent.policyText(it)) }
        composeRule.activityRule.scenario.onActivity { it.viewModelStore.clear() }
        composeRule.activityRule.scenario.recreate()
    }

    @Test
    fun mainScreensMeetElderAccessibilityBasics() = auditAllScreens()

    @Test
    fun mainScreensStillWorkInLargeTextMode() {
        composeRule.onNodeWithTag("large_text_toggle").performClick()
        auditAllScreens()
    }

    private fun auditAllScreens() {
        audit("首页")

        composeRule.onNodeWithTag("manual_product_input").performScrollTo().performClick()
        composeRule.onNodeWithText("查看输入示例").performScrollTo().performClick()
        audit("首页 · 手动输入与示例")

        composeRule.onNodeWithTag("home_input_field").performScrollTo()
            .performTextInput("量子太赫兹理疗仪，根治高血压，不用吃药，仅限今天，加微信")
        composeRule.onNodeWithTag("start_analysis").performScrollTo().performClick()
        audit("结果页 · 高风险")

        composeRule.onNodeWithTag("result_details_toggle").performScrollTo().performClick()
        audit("结果页 · 详细信息")

        composeRule.onNodeWithTag("return_home").performClick()
        composeRule.onNodeWithTag("open_history").performClick()
        audit("历史记录")

        assertTrue("Accessibility issues:\n" + issues.distinct().joinToString("\n"), issues.isEmpty())
    }

    private fun audit(screen: String) {
        composeRule.waitForIdle()
        val density = composeRule.activity.resources.displayMetrics.density
        val minTouchPx = 48.dp.value * density - 1f
        composeRule.onAllNodes(isRoot()).fetchSemanticsNodes().forEach { root -> visit(root, screen, minTouchPx) }
    }

    private fun visit(node: SemanticsNode, screen: String, minTouchPx: Float) {
        val config = node.config
        val label = (config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
            config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
            config.getOrNull(SemanticsProperties.EditableText)?.let { listOf(it.text) }.orEmpty())
            .joinToString(" ").trim()
        val name = label.take(24).ifEmpty { config.getOrNull(SemanticsProperties.TestTag) ?: "#${node.id}" }

        val clickable = config.getOrNull(SemanticsActions.OnClick) != null &&
            config.getOrNull(SemanticsProperties.Disabled) == null
        if (clickable) {
            // Layout size, not on-screen bounds: a button half scrolled out of view is still big enough.
            val width = maxOf(node.size.width.toFloat(), node.touchBoundsInRoot.width)
            val height = maxOf(node.size.height.toFloat(), node.touchBoundsInRoot.height)
            if (width < minTouchPx || height < minTouchPx) {
                issues += "[$screen] 触控区域小于 48dp：「$name」"
            }
            if (label.isEmpty() && config.getOrNull(SemanticsProperties.EditableText) == null) {
                issues += "[$screen] 可点击但没有读屏文字：${config.getOrNull(SemanticsProperties.TestTag) ?: "#${node.id}"}"
            }
        }

        config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.let { getLayout ->
            val layouts = mutableListOf<TextLayoutResult>()
            if (getLayout(layouts)) {
                layouts.forEach { layout ->
                    // hasVisualOverflow also fires for unclipped text with a custom lineHeight, so only
                    // count text that really ends in an ellipsis; clipping is checked on screenshots.
                    if (layout.lineCount > 0 && layout.isLineEllipsized(layout.lineCount - 1)) {
                        issues += "[$screen] 文字被省略：「$name」"
                    }
                    val size = layout.layoutInput.style.fontSize
                    if (size.isSp && size.value < 14f) issues += "[$screen] 字号 ${size.value.toInt()}sp 偏小：「$name」"
                }
            }
        }

        node.children.forEach { visit(it, screen, minTouchPx) }
    }
}
