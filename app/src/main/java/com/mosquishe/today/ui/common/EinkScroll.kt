package com.mosquishe.today.ui.common

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * E-ink scroll containers, per Mudita's rules: no momentum/fling, a swipe steps exactly one page,
 * and an always-visible scrollbar you can drag. Built in-app on purpose — MMD 1.0.0's own lazy-list
 * scrollbar throws in the draw phase when its canvas height is 0, which force-closed the app
 * (issue #1). The thumb math here is the same idea but clamps the degenerate cases instead of
 * crashing.
 */

private val BAR_TOUCH = 16.dp   // width of the draggable vertical scrollbar strip
private val ROW_BAR = 10.dp     // height reserved for the horizontal scrollbar (slimmer under chips)
private val BAR_LINE = 5.dp     // visible track/thumb thickness
private val MIN_THUMB = 28.dp
private val SWIPE_STEP = 20.dp  // a drag past this counts as a page step
private const val PAGE_KEEP = 0.9f // page jump keeps ~10% of the previous screen for context

private val TRACK_COLOR = Color(0xFFCCCCCC)
private val THUMB_COLOR = Color.Black

@Composable
fun EinkLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyListScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    Box(modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = BAR_TOUCH)
                .verticalPageSwipe(state, scope),
            state = state,
            contentPadding = contentPadding,
            userScrollEnabled = false,
            content = content,
        )
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(BAR_TOUCH)
                .verticalScrollbarDrag(state, scope)
                .drawVerticalScrollbar(state),
        )
    }
}

@Composable
fun EinkLazyRow(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyListScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    Box(modifier) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = ROW_BAR)
                .horizontalPageSwipe(state, scope),
            state = state,
            contentPadding = contentPadding,
            userScrollEnabled = false,
            content = content,
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(ROW_BAR)
                .horizontalScrollbarDrag(state, scope)
                .drawHorizontalScrollbar(state),
        )
    }
}

// ---- gestures ----------------------------------------------------------------

private fun Modifier.verticalPageSwipe(state: LazyListState, scope: CoroutineScope) =
    pointerInput(state) {
        val step = SWIPE_STEP.toPx()
        var acc = 0f
        detectVerticalDragGestures(
            onDragStart = { acc = 0f },
            onDragCancel = { acc = 0f },
            onVerticalDrag = { change, dy -> acc += dy; change.consume() },
            onDragEnd = {
                val dir = if (acc <= -step) 1 else if (acc >= step) -1 else 0
                if (dir != 0) scope.launch { pageBy(state, dir) }
            },
        )
    }

private fun Modifier.horizontalPageSwipe(state: LazyListState, scope: CoroutineScope) =
    pointerInput(state) {
        val step = SWIPE_STEP.toPx()
        var acc = 0f
        detectHorizontalDragGestures(
            onDragStart = { acc = 0f },
            onDragCancel = { acc = 0f },
            onHorizontalDrag = { change, dx -> acc += dx; change.consume() },
            onDragEnd = {
                val dir = if (acc <= -step) 1 else if (acc >= step) -1 else 0
                if (dir != 0) scope.launch { pageBy(state, dir) }
            },
        )
    }

/** Jump one page (a viewport) in [dir]; instant, no animation (one e-ink refresh). */
private suspend fun pageBy(state: LazyListState, dir: Int) {
    val info = state.layoutInfo
    val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
    if (viewport <= 0f) return
    state.scrollBy(dir * viewport * PAGE_KEEP)
}

private fun Modifier.verticalScrollbarDrag(state: LazyListState, scope: CoroutineScope) =
    pointerInput(state) {
        detectVerticalDragGestures { change, dy ->
            change.consume()
            val delta = dragToContentDelta(state, dy, size.height.toFloat())
            if (delta != 0f) scope.launch { state.scrollBy(delta) }
        }
    }

private fun Modifier.horizontalScrollbarDrag(state: LazyListState, scope: CoroutineScope) =
    pointerInput(state) {
        detectHorizontalDragGestures { change, dx ->
            change.consume()
            val delta = dragToContentDelta(state, dx, size.width.toFloat())
            if (delta != 0f) scope.launch { state.scrollBy(delta) }
        }
    }

/** Map a drag along the scrollbar track to a content scroll delta (approximate, uniform-size). */
private fun dragToContentDelta(state: LazyListState, drag: Float, trackLen: Float): Float {
    if (trackLen <= 0f) return 0f
    val info = state.layoutInfo
    val visible = info.visibleItemsInfo.size.coerceAtLeast(1)
    val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
    if (viewport <= 0f) return 0f
    val contentLen = (viewport / visible) * info.totalItemsCount.coerceAtLeast(1)
    return (drag / trackLen) * contentLen
}

// ---- drawing -----------------------------------------------------------------

private fun Modifier.drawVerticalScrollbar(state: LazyListState) = drawBehind {
    val canvas = size.height
    if (canvas <= 0f) return@drawBehind
    val line = BAR_LINE.toPx()
    val x = size.width - line
    drawRoundRect(TRACK_COLOR, Offset(x, 0f), Size(line, canvas), CornerRadius(line / 2))
    val (thumb, offset) = thumbMetrics(state, canvas, MIN_THUMB.toPx()) ?: return@drawBehind
    drawRoundRect(THUMB_COLOR, Offset(x, offset), Size(line, thumb), CornerRadius(line / 2))
}

private fun Modifier.drawHorizontalScrollbar(state: LazyListState) = drawBehind {
    val canvas = size.width
    if (canvas <= 0f) return@drawBehind
    val info = state.layoutInfo
    // A chip row that already fits gets no bar — a full-width thumb would just read as a heavy rule.
    if (info.totalItemsCount == 0 || info.visibleItemsInfo.size >= info.totalItemsCount) return@drawBehind
    val line = BAR_LINE.toPx()
    val y = size.height - line
    drawRoundRect(TRACK_COLOR, Offset(0f, y), Size(canvas, line), CornerRadius(line / 2))
    val (thumb, offset) = thumbMetrics(state, canvas, MIN_THUMB.toPx()) ?: return@drawBehind
    drawRoundRect(THUMB_COLOR, Offset(offset, y), Size(thumb, line), CornerRadius(line / 2))
}

/**
 * Thumb length and offset along a track of [canvas] px. Returns null when there's nothing to draw.
 * Clamps every step so a zero/short viewport can never produce a negative range (the MMD crash).
 */
private fun thumbMetrics(state: LazyListState, canvas: Float, minThumbPx: Float): Pair<Float, Float>? {
    val info = state.layoutInfo
    val total = info.totalItemsCount
    if (total <= 0 || canvas <= 0f) return null
    val visible = info.visibleItemsInfo.size.coerceAtLeast(1)
    val first = info.visibleItemsInfo.firstOrNull()?.index ?: 0
    val thumbFrac = (visible.toFloat() / total).coerceIn(0f, 1f)
    val offsetFrac = (first.toFloat() / total).coerceIn(0f, 1f)
    val thumb = max(canvas * thumbFrac, minThumbPx).coerceAtMost(canvas)
    val maxOffset = (canvas - thumb).coerceAtLeast(0f)
    val offset = (offsetFrac * canvas).coerceIn(0f, maxOffset)
    return thumb to offset
}
