package com.silverguard.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal data class SilverGuardColors(
    val brand: Color,
    val background: Color,
    val softGreen: Color,
    val softRed: Color,
    val softAmber: Color,
    val ink: Color,
    val muted: Color
)

internal val StandardSilverGuardColors = SilverGuardColors(
    brand = Color(0xFF176B4D),
    background = Color(0xFFF6F8F5),
    softGreen = Color(0xFFE8F3ED),
    softRed = Color(0xFFFFF0EE),
    softAmber = Color(0xFFFFF7DB),
    ink = Color(0xFF16231D),
    muted = Color(0xFF68756E)
)

internal val HighContrastSilverGuardColors = SilverGuardColors(
    brand = Color(0xFF005B3B),
    background = Color.White,
    softGreen = Color(0xFFD9F2E5),
    softRed = Color(0xFFFFDDD8),
    softAmber = Color(0xFFFFE9A8),
    ink = Color.Black,
    muted = Color(0xFF34443C)
)

internal val LocalSilverGuardColors = staticCompositionLocalOf {
    StandardSilverGuardColors
}

internal val Brand: Color
    @Composable get() = LocalSilverGuardColors.current.brand
internal val Background: Color
    @Composable get() = LocalSilverGuardColors.current.background
internal val SoftGreen: Color
    @Composable get() = LocalSilverGuardColors.current.softGreen
internal val SoftRed: Color
    @Composable get() = LocalSilverGuardColors.current.softRed
internal val SoftAmber: Color
    @Composable get() = LocalSilverGuardColors.current.softAmber
internal val Ink: Color
    @Composable get() = LocalSilverGuardColors.current.ink
internal val Muted: Color
    @Composable get() = LocalSilverGuardColors.current.muted

@Composable
internal fun SilverGuardPaletteTheme(
    colors: SilverGuardColors,
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary = colors.brand,
        onPrimary = Color.White,
        background = colors.background,
        onBackground = colors.ink,
        surface = Color.White,
        onSurface = colors.ink,
        outline = colors.muted
    )
    CompositionLocalProvider(LocalSilverGuardColors provides colors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

@Composable
fun SilverGuardTheme(content: @Composable () -> Unit) {
    SilverGuardPaletteTheme(StandardSilverGuardColors, content)
}
