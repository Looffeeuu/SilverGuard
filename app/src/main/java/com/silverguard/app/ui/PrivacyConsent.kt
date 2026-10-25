package com.silverguard.app.ui

import android.content.Context
import java.util.zip.CRC32

/**
 * Remembers which version of the privacy policy the user agreed to. The version is a checksum of
 * the policy text, so any change to assets/privacy_policy.md asks for consent again.
 */
internal object PrivacyConsent {
    const val POLICY_ASSET = "privacy_policy.md"
    private const val PREFS_NAME = "silverguard_privacy"
    private const val KEY_ACCEPTED = "accepted_policy_checksum"

    fun policyText(context: Context): String =
        context.assets.open(POLICY_ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }

    fun checksum(policy: String): Long = CRC32().apply { update(policy.toByteArray(Charsets.UTF_8)) }.value

    fun isAccepted(context: Context, policy: String): Boolean =
        prefs(context).getLong(KEY_ACCEPTED, -1L) == checksum(policy)

    fun accept(context: Context, policy: String) {
        prefs(context).edit().putLong(KEY_ACCEPTED, checksum(policy)).apply()
    }

    fun withdraw(context: Context) {
        prefs(context).edit().remove(KEY_ACCEPTED).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
