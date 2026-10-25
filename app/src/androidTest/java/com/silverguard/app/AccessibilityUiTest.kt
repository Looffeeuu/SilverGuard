package com.silverguard.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import com.silverguard.app.data.AnalysisHistoryRepository
import com.silverguard.app.data.FamilyContactRepository
import com.silverguard.app.ui.PrivacyConsent
import org.junit.Before

@RunWith(AndroidJUnit4::class)
class AccessibilityUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun cleanTestHistory() {
        AnalysisHistoryRepository(composeRule.activity).apply { clear(); persist() }
        FamilyContactRepository(composeRule.activity).save(null)
        composeRule.activity.getSharedPreferences("silverguard_accessibility", 0).edit().clear().commit()
        // The analysis session lives in a ViewModel that survives recreation; drop it so it reloads the cleared history.
        // These tests exercise the app itself; PrivacyConsentUiTest covers the consent screen.
        composeRule.activity.let { PrivacyConsent.accept(it, PrivacyConsent.policyText(it)) }
        composeRule.activityRule.scenario.onActivity { it.viewModelStore.clear() }
        composeRule.activityRule.scenario.recreate()
    }

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
        composeRule.onNodeWithTag("manual_product_input").performScrollTo().performClick()
        composeRule.onNodeWithTag("home_input_field")
            .performScrollTo()
            .performTextInput("普通日用品 价格二十元")
        composeRule.onNodeWithTag("start_analysis")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithTag("result_input_field")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("ai_analysis_card").assertDoesNotExist()
        composeRule.onNodeWithTag("price_reference_card").assertDoesNotExist()
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

    @Test
    fun localPricesCanBeEnteredAndComparedInsideDetails() {
        composeRule.onNodeWithTag("manual_product_input").performScrollTo().performClick()
        composeRule.onNodeWithTag("home_input_field").performScrollTo().performTextInput("普通毛巾")
        composeRule.onNodeWithTag("start_analysis").performScrollTo().performClick()
        composeRule.onNodeWithTag("result_details_toggle").performScrollTo().performClick()
        composeRule.onNodeWithTag("price_reference_edit").performScrollTo().performClick()
        composeRule.onNodeWithTag("price_current").performScrollTo().performTextInput("150")
        listOf("100", "120", "140").forEachIndexed { index, price ->
            composeRule.onNodeWithTag("price_source_$index").performScrollTo().performTextInput("店铺$index")
            composeRule.onNodeWithTag("price_quote_$index").performScrollTo().performTextInput(price)
        }
        composeRule.onNodeWithTag("price_same_product").performScrollTo().performClick()
        composeRule.onNodeWithTag("price_reference_range").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("related_cases_card").performScrollTo().assertIsDisplayed()
    }

    private fun analyze(text: String) {
        composeRule.onNodeWithTag("manual_product_input").performScrollTo().performClick()
        composeRule.onNodeWithTag("home_input_field").performScrollTo().performTextInput(text)
        composeRule.onNodeWithTag("start_analysis").performScrollTo().performClick()
    }

    @Test fun homeRestoresInitialScreenAndHistorySurvivesAppRecreation() {
        analyze("普通毛巾 历史测试")
        composeRule.onNodeWithTag("result_input_field").performScrollTo().performTextInput(" 待补充说明")
        composeRule.onNodeWithTag("return_home").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("home_input_field").assertDoesNotExist()
        composeRule.onNodeWithTag("result_input_field").assertDoesNotExist()
        composeRule.onNodeWithTag("take_product_photo").performScrollTo().assertIsDisplayed()
        composeRule.waitUntil(5000) { historyFileContains(composeRule.activity, "历史测试") }
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("open_history").performClick()
        composeRule.onNodeWithTag("history_search").performTextInput("毛巾")
        composeRule.onNodeWithText("打开记录").performScrollTo().performClick()
        composeRule.onNodeWithTag("history_result_notice").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("result_input_field").performScrollTo().assertTextContains("待补充说明", substring = true)
    }

    @Test fun historyCanDeleteOneRecordAndClearAllWithConfirmation() {
        analyze("普通毛巾")
        composeRule.onNodeWithTag("return_home").performClick()
        analyze("普通茶杯")
        composeRule.onNodeWithTag("return_home").performClick()
        composeRule.onNodeWithTag("open_history").performClick()
        composeRule.onNodeWithTag("history_search").performTextInput("毛巾")
        composeRule.onNodeWithText("删除").performScrollTo().performClick()
        composeRule.onNodeWithTag("history_confirm_delete").performClick()
        composeRule.onNodeWithText("打开记录").assertDoesNotExist()
        composeRule.onNodeWithTag("history_search").performScrollTo().performTextReplacement("")
        composeRule.onNodeWithText("打开记录").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("history_clear").performScrollTo().performClick()
        composeRule.onNodeWithTag("history_confirm_delete").performClick()
        composeRule.onNodeWithText("打开记录").assertDoesNotExist()
    }

    @Test fun resultSurvivesRotationAndOtherConfigurationChanges() {
        analyze("普通茶杯 转屏测试")
        composeRule.onNodeWithTag("result_input_field").performScrollTo().performTextInput(" 未提交的补充")
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("result_input_field")
            .performScrollTo()
            .assertTextContains("转屏测试", substring = true)
            .assertTextContains("未提交的补充", substring = true)
        composeRule.onNodeWithTag("take_product_photo").assertDoesNotExist()
    }

    @Test fun helpContactsAppearOnlyForRiskyResults() {
        analyze("普通茶杯")
        composeRule.onNodeWithTag("help_contacts_card").assertDoesNotExist()
        composeRule.onNodeWithTag("return_home").performClick()

        analyze("量子理疗仪，根治高血压，不用吃药")
        composeRule.onNodeWithTag("help_contacts_card").performScrollTo().assertIsDisplayed()
        listOf("help_call_12315", "help_call_96110", "help_call_110").forEach { tag ->
            composeRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
    }

    @Test fun familyPhoneIsValidatedMaskedAndKeptAfterRecreation() {
        analyze("量子理疗仪，根治高血压，不用吃药")
        composeRule.onNodeWithTag("help_family_input").performScrollTo().performTextInput("妈妈")
        composeRule.onNodeWithTag("help_family_save").performScrollTo().performClick()
        composeRule.onNodeWithTag("help_call_family").assertDoesNotExist()

        composeRule.onNodeWithTag("help_family_input").performScrollTo().performTextReplacement("138 1234 5678")
        composeRule.onNodeWithTag("help_family_save").performScrollTo().performClick()
        composeRule.onNodeWithTag("help_call_family")
            .performScrollTo()
            .assertTextContains("138****5678", substring = true)

        composeRule.waitUntil(5000) { FamilyContactRepository(composeRule.activity).load() == "13812345678" }
        // A fresh ViewModel reloads the number from disk and the open analysis from saved state.
        composeRule.activityRule.scenario.onActivity { it.viewModelStore.clear() }
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("help_call_family").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("help_family_clear").performScrollTo().performClick()
        composeRule.onNodeWithTag("help_family_input").performScrollTo().assertIsDisplayed()
    }

    @Test fun systemBackFromResultReturnsHomeWithoutExiting() {
        analyze("普通茶杯")
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithTag("take_product_photo").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("open_history").assertIsDisplayed()
    }
}
