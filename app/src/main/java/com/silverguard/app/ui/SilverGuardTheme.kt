package com.silverguard.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val Brand = Color(0xFF176B4D)
internal val Background = Color(0xFFF6F8F5)
internal val SoftGreen = Color(0xFFE8F3ED)
internal val SoftRed = Color(0xFFFFF0EE)
internal val SoftAmber = Color(0xFFFFF7DB)
internal val Ink = Color(0xFF16231D)
internal val Muted = Color(0xFF68756E)

@Composable
fun SilverGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
