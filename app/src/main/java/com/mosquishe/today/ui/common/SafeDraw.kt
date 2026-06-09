package com.mosquishe.today.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent

/**
 * Guards against a draw-phase crash inside MMD 1.0.0's lazy-list scrollbar.
 *
 * When a [com.mudita.mmd.components.lazy.LazyColumnMMD] / LazyRowMMD is drawn with a
 * near-zero viewport — a transient frame during navigation or while the keyboard is
 * animating in or out — its internal VerticalScrollbar computes a negative track length
 * and throws `IllegalArgumentException` from `coerceIn` (maximum < minimum). That happens
 * in the draw phase, on the main thread, so it takes down the whole app. On the Mudita
 * Kompakt (slow e-ink redraw) the bad frame is common enough that the app force-closes
 * constantly.
 *
 * The scrollbar lives in the MMD binary, so we can't fix it there. Instead we wrap the
 * lazy list's draw and drop the one transient frame that fails; the next frame, with a
 * real viewport, draws fine. Apply to the modifier passed to the MMD lazy list (MMD puts
 * it on the box that parents both the list and the scrollbar, so the scrollbar draw is
 * covered).
 */
fun Modifier.guardMmdScrollbarDraw(): Modifier = drawWithContent {
    try {
        drawContent()
    } catch (_: IllegalArgumentException) {
        // MMD scrollbar hit a zero-height viewport this frame. Skip it; the next frame redraws.
    }
}
