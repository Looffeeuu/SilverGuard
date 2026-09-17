package com.silverguard.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp

internal data class SilverGuardColors(
    val brand: Color,
    val background: Color,
    val softGreen: Color,
    val softRed: Color,
    val softAmber: Color,
    val ink: Color,
    val muted: Color,
    val outline: Color
)

internal val StandardSilverGuardColors = SilverGuardColors(
    brand = Color(0xFF176B4D),
    background = Color(0xFFE5EEE8),
    softGreen = Color(0xFFD4EADD),
    softRed = Color(0xFFFFE0DA),
    softAmber = Color(0xFFFFE9AB),
    ink = Color(0xFF16231D),
    muted = Color(0xFF40574B),
    outline = Color(0xFF587666)
)

internal val HighContrastSilverGuardColors = SilverGuardColors(
    brand = Color(0xFF005B3B),
    background = Color.White,
    softGreen = Color(0xFFD9F2E5),
    softRed = Color(0xFFFFDDD8),
    softAmber = Color(0xFFFFE9A8),
    ink = Color.Black,
    muted = Color(0xFF243C2E),
    outline = Color(0xFF1F4332)
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
internal val CardBorder: BorderStroke
    @Composable get() = BorderStroke(1.dp, LocalSilverGuardColors.current.outline)

@Composable
internal fun SilverGuardPaletteTheme(
    colors: SilverGuardColors,
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary = colors.brand,
        onPrimary = Color.White,
        primaryContainer = colors.softGreen,
        onPrimaryContainer = colors.ink,
        secondary = colors.brand,
        onSecondary = Color.White,
        secondaryContainer = colors.softGreen,
        onSecondaryContainer = colors.ink,
        background = colors.background,
        onBackground = colors.ink,
        surface = Color.White,
        onSurface = colors.ink,
        surfaceVariant = colors.softGreen,
        onSurfaceVariant = colors.muted,
        surfaceContainer = colors.background,
        surfaceContainerHigh = colors.softGreen,
        outline = colors.outline,
        outlineVariant = colors.outline
    )
    CompositionLocalProvider(LocalSilverGuardColors provides colors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

@Composable
fun SilverGuardTheme(content: @Composable () -> Unit) {
    SilverGuardPaletteTheme(StandardSilverGuardColors, content)
}
