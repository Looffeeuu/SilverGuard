package com.silverguard.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silverguard.app.ui.PrivacyConsent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrivacyConsentUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun startWithoutConsent() {
        PrivacyConsent.withdraw(composeRule.activity)
        relaunch()
    }

    private fun relaunch() {
        composeRule.activityRule.scenario.onActivity { it.viewModelStore.clear() }
        composeRule.activityRule.scenario.recreate()
    }

    @Test
    fun firstLaunchShowsConsentBeforeTheApp() {
        composeRule.onNodeWithTag("consent_accept").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("take_product_photo").assertDoesNotExist()
    }

    @Test
    fun fullPolicyCanBeReadAndClosed() {
        composeRule.onNodeWithTag("consent_show_policy").performScrollTo().performClick()
        composeRule.onNodeWithText("银龄安心查隐私政策").assertIsDisplayed()
        composeRule.onNodeWithText("三、会离开你手机的信息").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("policy_withdraw").assertDoesNotExist()

        composeRule.onNodeWithTag("policy_back").performScrollTo().performClick()
        composeRule.onNodeWithTag("consent_accept").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun refusingExplainsWhyAndAllowsReconsidering() {
        composeRule.onNodeWithTag("consent_refuse").performScrollTo().performClick()
        composeRule.onNodeWithText("不同意就无法使用分析功能").assertIsDisplayed()
        composeRule.onNodeWithTag("consent_exit").assertIsDisplayed()

        composeRule.onNodeWithTag("consent_reconsider").performClick()
        composeRule.onNodeWithTag("consent_accept").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("take_product_photo").assertDoesNotExist()
    }

    @Test
    fun acceptingOpensTheAppAndIsRemembered() {
        composeRule.onNodeWithTag("consent_accept").performScrollTo().performClick()
        composeRule.onNodeWithTag("take_product_photo").performScrollTo().assertIsDisplayed()

        relaunch()
        composeRule.onNodeWithTag("take_product_photo").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("consent_accept").assertDoesNotExist()
    }

    @Test
    fun consentCanBeWithdrawnFromThePolicyLink() {
        composeRule.onNodeWithTag("consent_accept").performScrollTo().performClick()
        composeRule.onNodeWithTag("privacy_policy_link").performScrollTo().performClick()
        composeRule.onNodeWithTag("policy_withdraw").performScrollTo().performClick()
        composeRule.onNodeWithTag("policy_withdraw_confirm").performClick()
        composeRule.onNodeWithTag("consent_accept").performScrollTo().assertIsDisplayed()

        relaunch()
        composeRule.onNodeWithTag("consent_accept").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun changedPolicyAsksForConsentAgain() {
        PrivacyConsent.accept(composeRule.activity, "上一版隐私政策")
        relaunch()
        composeRule.onNodeWithTag("consent_accept").performScrollTo().assertIsDisplayed()
    }
}
