package com.silverguard.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
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
}
