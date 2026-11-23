package com.mui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.Orientation as DragOrientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mui.theme.LocalMuiPalette

/**
 * 滚动容器：单方向滚动包装（web 的 radix ScrollArea）。
 * 输入方向 + 内容槽；输出可滚动区域。
 * 硬约束：纵向 = verticalScroll、横向 = horizontalScroll；尺寸约束由调用方给。
 *
 * @param modifier 布局修饰（高度/宽度约束由调用方给）
 * @param orientation 滚动方向（默认 Vertical）
 * @param content 滚动内容
 */
@Composable
fun MuiScrollArea(
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Vertical,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberScrollState()
    val vertical = orientation == Orientation.Vertical
    val rtl = !vertical && LocalLayoutDirection.current == LayoutDirection.Rtl
    val hoverSource = remember { MutableInteractionSource() }
    val hovered by hoverSource.collectIsHoveredAsState()
    var dragging by remember { mutableStateOf(false) }
    val overflow = state.maxValue in 1 until Int.MAX_VALUE && state.viewportSize > 0
    val visible = overflow && (hovered || state.isScrollInProgress || dragging)
    val alpha by animateFloatAsState(if (visible) 1f else 0f, label = "scrollbarAlpha")
    val color = LocalMuiPalette.current.border
    val scroll = if (vertical) Modifier.verticalScroll(state) else Modifier.horizontalScroll(state)

    Box(modifier.hoverable(hoverSource)) {
        Box(modifier = scroll, content = content)
        if (overflow) {
            val direction = if (rtl) -1f else 1f
            val dragState = rememberDraggableState { delta ->
                val viewport = state.viewportSize.toFloat()
                val thumb = scrollThumbSize(viewport, state.maxValue)
                if (viewport > thumb) {
                    state.dispatchRawDelta(delta * direction * state.maxValue / (viewport - thumb))
                }
            }
            val barSize = if (vertical) {
                Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(10.dp)
            } else {
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(10.dp)
            }
            Box(barSize.drawBehind {
                val length = if (vertical) size.height else size.width
                val thumb = scrollThumbSize(length, state.maxValue)
                val fraction = state.value.toFloat() / state.maxValue
                val offset = (length - thumb) * if (rtl) 1f - fraction else fraction
                val thickness = 7.dp.toPx()
                val inset = 1.dp.toPx()
                drawRoundRect(
                    color = color.copy(alpha = color.alpha * alpha),
                    topLeft = if (vertical) Offset(size.width - thickness - inset, offset) else Offset(offset, inset),
                    size = if (vertical) Size(thickness, thumb) else Size(thumb, thickness),
                    cornerRadius = CornerRadius(thickness / 2),
                )
            }.draggable(
                state = dragState,
                orientation = if (vertical) DragOrientation.Vertical else DragOrientation.Horizontal,
                enabled = visible,
                onDragStarted = { dragging = true },
                onDragStopped = { dragging = false },
            ))
        }
    }
}

private fun scrollThumbSize(viewport: Float, overflow: Int): Float =
    if (viewport <= 0f) 0f else (viewport * viewport / (viewport + overflow)).coerceIn(minOf(18f, viewport), viewport)

/** 横向滚动区快捷（占满宽） */
@Composable
fun MuiHorizontalScrollArea(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    MuiScrollArea(
        modifier = modifier.fillMaxWidth(),
        orientation = Orientation.Horizontal,
        content = content,
    )
}
