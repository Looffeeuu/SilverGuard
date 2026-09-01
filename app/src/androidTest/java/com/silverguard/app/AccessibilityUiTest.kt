package com.silverguard.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun threePrimaryEntryPointsAreReachable() {
        listOf(
            "take_product_photo",
            "select_product_screenshot",
            "manual_product_input"
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag)
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun largeTextAndHighContrastControlsCanBeOperated() {
        composeRule.onNodeWithTag("large_text_toggle")
            .assertIsDisplayed()
            .performClick()
            .assertIsDisplayed()

        composeRule.onNodeWithTag("high_contrast_toggle")
            .assertIsDisplayed()
            .performClick()
            .assertIsDisplayed()
    }

    @Test
    fun resultKeepsInputAndHidesDetailsUntilRequested() {
        composeRule.onNodeWithTag("manual_product_input").performClick()
        composeRule.onNodeWithTag("home_input_field")
            .performScrollTo()
            .performTextInput("普通日用品 价格二十元")
        composeRule.onNodeWithTag("start_analysis")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithTag("result_input_field")
            .performScrollTo()
            .assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("① 我识别到了什么？")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        )

        composeRule.onNodeWithTag("result_details_toggle")
            .performScrollTo()
            .performClick()
        assertTrue(
            composeRule.onAllNodesWithText("① 我识别到了什么？")
                .fetchSemanticsNodes()
                .isNotEmpty()
        )
    }
}
