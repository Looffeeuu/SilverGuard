package com.silverguard.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class SilverGuardContrastTest {
    private fun ratio(a: Color, b: Color): Float {
        val x = a.luminance()
        val y = b.luminance()
        return (maxOf(x, y) + .05f) / (minOf(x, y) + .05f)
    }
    @Test fun normalModeTextIsReadableOnAllPanelColors() = checkText(StandardSilverGuardColors)
    @Test fun highContrastTextIsReadableOnAllPanelColors() = checkText(HighContrastSilverGuardColors)
    private fun checkText(colors: SilverGuardColors) {
        listOf(Color.White, colors.background, colors.softGreen, colors.softRed, colors.softAmber).forEach { background ->
            assertTrue("Primary text contrast", ratio(colors.ink, background) >= 4.5f)
            assertTrue("Secondary text contrast", ratio(colors.muted, background) >= 4.5f)
        }
        assertTrue("Button text contrast", ratio(Color.White, colors.brand) >= 4.5f)
    }
    @Test fun normalModeCardAndControlBoundariesAreVisible() {
        val colors = StandardSilverGuardColors
        listOf(Color.White, colors.background, colors.softGreen, colors.softRed, colors.softAmber).forEach {
            assertTrue("Panel boundary contrast", ratio(colors.outline, it) >= 3f)
        }
    }
}
