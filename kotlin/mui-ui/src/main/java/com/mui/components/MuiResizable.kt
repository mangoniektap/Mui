package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mui.theme.LocalMuiPalette
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource

// TODO: web 的多面板 Group/Panel 协作（Collapsible、持久化）未移植，只做双栏。

/**
 * 双栏可拖分隔容器：第一栏占比可拖动调整（web 的 ResizablePanelGroup 简化）。
 * 输入方向 + 初始占比 + 两栏槽；输出可拖拽的双栏布局。
 * 分隔线为 1dp，触控区为 12dp。占比钳制在 [minRatio, maxRatio]。
 *
 * @param modifier 布局修饰
 * @param orientation 分隔走向（Horizontal = 左右双栏 / Vertical = 上下双栏）
 * @param initialRatio 第一栏初始占比（0..1，默认 0.5f）
 * @param minRatio 第一栏最小占比（默认 0.05f）
 * @param maxRatio 第一栏最大占比（默认 0.95f）
 * @param first 第一栏内容
 * @param second 第二栏内容
 */
@Composable
fun MuiResizable(
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
    initialRatio: Float = 0.5f,
    minRatio: Float = 0.05f,
    maxRatio: Float = 0.95f,
    first: @Composable BoxScope.() -> Unit,
    second: @Composable BoxScope.() -> Unit,
) {
    MuiResizable(modifier, orientation, initialRatio, minRatio, maxRatio, false, first, second)
}

/** 可选握把，支持触控拖动、键盘方向键和无障碍尺寸调整。 */
@Composable
fun MuiResizable(
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
    initialRatio: Float = 0.5f,
    minRatio: Float = 0.05f,
    maxRatio: Float = 0.95f,
    withHandle: Boolean,
    first: @Composable BoxScope.() -> Unit,
    second: @Composable BoxScope.() -> Unit,
) {
    val palette = LocalMuiPalette.current
    val lower = minOf(minRatio, maxRatio).coerceIn(0f, 1f)
    val upper = maxOf(minRatio, maxRatio).coerceIn(0f, 1f)
    var ratio by rememberSaveable { mutableFloatStateOf(initialRatio.coerceIn(lower, upper)) }
    val current = ratio.coerceIn(lower, upper)
    val horizontal = orientation == Orientation.Horizontal
    val direction = if (horizontal && LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val hitArea = 12.dp
    val density = LocalDensity.current
    val interaction = rememberMuiInteractionSource()
    val handleShape = RoundedCornerShape(2.dp)

    BoxWithConstraints(modifier.fillMaxSize()) {
        val total = if (horizontal) maxWidth else maxHeight
        val available = (total - hitArea).coerceAtLeast(0.dp)
        val availablePx = with(density) { available.toPx() }
        val enabled = availablePx.isFinite() && availablePx > 0f && upper > lower
        fun resize(next: Float) {
            ratio = next.coerceIn(lower, upper)
        }
        val handleModifier = Modifier
            .muiFocusRing(interaction, handleShape)
            .drawBehind {
                val line = 1.dp.toPx()
                if (horizontal) drawLine(palette.border, Offset(center.x, 0f), Offset(center.x, size.height), line)
                else drawLine(palette.border, Offset(0f, center.y), Offset(size.width, center.y), line)
            }
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(current, lower..upper)
                if (enabled) setProgress { resize(it); true }
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || !enabled) return@onKeyEvent false
                val decreaseKey = if (horizontal) Key.DirectionLeft else Key.DirectionUp
                val increaseKey = if (horizontal) Key.DirectionRight else Key.DirectionDown
                when (event.key) {
                    decreaseKey -> resize(ratio - 0.05f * direction)
                    increaseKey -> resize(ratio + 0.05f * direction)
                    Key.MoveHome -> resize(lower)
                    Key.MoveEnd -> resize(upper)
                    else -> return@onKeyEvent false
                }
                true
            }
            .focusable(enabled, interaction)
            .pointerInput(horizontal, direction, lower, upper, availablePx) {
                if (!enabled) return@pointerInput
                detectDragGestures { change, delta ->
                    val distance = if (horizontal) delta.x else delta.y
                    resize(ratio + distance * direction / availablePx)
                    change.consume()
                }
            }
        val handle: @Composable (Modifier) -> Unit = { bounds ->
            Box(bounds.then(handleModifier), contentAlignment = Alignment.Center) {
                if (withHandle) {
                    val width = if (horizontal) 12.dp else 16.dp
                    val height = if (horizontal) 16.dp else 12.dp
                    Box(
                        Modifier.size(width, height)
                            .background(palette.background, handleShape)
                            .border(1.dp, palette.border, handleShape),
                    )
                }
            }
        }

        if (horizontal) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.width(available * current).fillMaxHeight(), content = first)
                handle(Modifier.width(hitArea).fillMaxHeight())
                Box(Modifier.weight(1f).fillMaxHeight(), content = second)
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.height(available * current).fillMaxWidth(), content = first)
                handle(Modifier.height(hitArea).fillMaxWidth())
                Box(Modifier.weight(1f).fillMaxWidth(), content = second)
            }
        }
    }
}
