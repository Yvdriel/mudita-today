package com.mosquishe.today.ui.theme

import androidx.compose.runtime.Composable
import com.mudita.mmd.ThemeMMD
import com.mudita.mmd.eInkColorScheme
import com.mudita.mmd.eInkTypography

/**
 * Root theme. All-black/white e-ink scheme + typography from MMD. Every screen sits inside this so
 * colors are drawn from the theme only (Law 2 — never hardcode greys/alpha).
 */
@Composable
fun TodayTheme(content: @Composable () -> Unit) {
    ThemeMMD(
        colorScheme = eInkColorScheme,
        typography = eInkTypography,
        content = content,
    )
}
