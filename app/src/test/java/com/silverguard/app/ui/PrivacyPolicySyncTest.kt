package com.silverguard.app.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The policy shown in the app must be the reviewed draft minus its leading "> 草稿" note.
 * After editing docs/store/PRIVACY_POLICY_DRAFT.md, copy the body into app/src/main/assets/privacy_policy.md.
 */
class PrivacyPolicySyncTest {

    @Test
    fun inAppPolicyMatchesTheDraftBody() {
        val draft = File("../docs/store/PRIVACY_POLICY_DRAFT.md").readText(Charsets.UTF_8).replace("\r\n", "\n")
        val body = draft.lines()
            .dropWhile { it.startsWith(">") || it.isBlank() }
            .joinToString("\n")
            .trimEnd() + "\n"
        val asset = File("src/main/assets/privacy_policy.md").readText(Charsets.UTF_8).replace("\r\n", "\n")

        assertEquals("app/src/main/assets/privacy_policy.md is out of sync with the policy draft", body, asset)
    }
}
