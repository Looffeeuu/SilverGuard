package com.silverguard.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silverguard.app.data.AnalysisHistoryRepository
import com.silverguard.app.data.DeviceRegistryIndex
import com.silverguard.app.engine.DeviceRegistryKey
import java.io.File
import java.util.zip.GZIPOutputStream
import org.junit.After
import com.silverguard.app.ui.PrivacyConsent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Puts a one-record index where a downloaded index would live and checks what the result page shows. */
@RunWith(AndroidJUnit4::class)
class DeviceRegistryUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val indexDir: File get() = File(composeRule.activity.filesDir, DeviceRegistryIndex.DIRECTORY)

    @Before
    fun installTinyIndex() {
        AnalysisHistoryRepository(composeRule.activity).apply { clear(); persist() }
        indexDir.deleteRecursively()
        indexDir.mkdirs()
        File(indexDir, "manifest.json").writeText(
            """{"format": 1, "shards": 64, "dataDate": "2026-10-05", "records": 1}"""
        )
        val key = DeviceRegistryKey.normalize("国械注准20263090001")
        GZIPOutputStream(File(indexDir, DeviceRegistryKey.shardFileName(DeviceRegistryKey.shardOf(key))).outputStream()).use {
            it.write("$key\t国械注准20263090001\t安心医疗科技有限公司\t电子血压计\t器械\n".toByteArray())
        }
        // The ViewModel opens the index once; start from a fresh one that sees this index.
        // These tests exercise the app itself; PrivacyConsentUiTest covers the consent screen.
        composeRule.activity.let { PrivacyConsent.accept(it, PrivacyConsent.policyText(it)) }
        composeRule.activityRule.scenario.onActivity { it.viewModelStore.clear() }
        composeRule.activityRule.scenario.recreate()
    }

    @After
    fun removeIndex() {
        indexDir.deleteRecursively()
    }

    private fun analyze(text: String) {
        composeRule.onNodeWithTag("manual_product_input").performScrollTo().performClick()
        composeRule.onNodeWithTag("home_input_field").performScrollTo().performTextInput(text)
        composeRule.onNodeWithTag("start_analysis").performScrollTo().performClick()
    }

    @Test
    fun borrowedNumberIsShownOnTheResultPage() {
        analyze("商品名称：量子太赫兹理疗仪\n医疗器械注册证编号：国械注准20263090001\n生产企业：深圳某某健康科技有限公司")

        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithTagExists("device_registry_card")
        }
        composeRule.onNodeWithText("这个编号登记的是别的产品或企业").performScrollTo().assertIsDisplayed()
        // The headline must not say "暂未发现明显高风险" above a mismatch.
        composeRule.onNodeWithText("信息对不上，建议暂缓付款").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("help_contacts_card").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun matchingNumberIsListedInDetails() {
        analyze("商品名称：电子血压计\n医疗器械注册证编号：国械注准20263090001\n生产企业：安心医疗科技有限公司")

        composeRule.onNodeWithTag("result_details_toggle").performScrollTo().performClick()
        composeRule.waitUntil(5000) { composeRule.onAllNodesWithTagExists("device_registry_card") }
        composeRule.onNodeWithText("本机登记数据找到了这个编号").performScrollTo().assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.onAllNodesWithTagExists(tag: String): Boolean =
        onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
}
