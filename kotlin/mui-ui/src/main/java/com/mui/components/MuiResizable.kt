package com.mui.components

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mui.theme.LocalMuiPalette

// TODO: web 的 withHandle 握把图形与键盘调整（focus ring + 方向键）未移植。
// TODO: web 的多面板 Group/Panel 协作（Collapsible、持久化）未移植，只做双栏。

/**
 * 双栏可拖分隔容器：第一栏占比可拖动调整（web 的 ResizablePanelGroup 简化）。
 * 输入方向 + 初始占比 + 两栏槽；输出可拖拽的双栏布局。
 * 硬约束：手柄为 1.5dp 实线（--border 色），热区 12dp；占比钳制在 [minRatio, maxRatio]。
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
    val palette = LocalMuiPalette.current
    val ratio = remember { mutableFloatStateOf(initialRatio) }
    val handleThickness = 1.5.dp
    val dragArea = 12.dp
    val density = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxSize()) {
        val total = if (orientation == Orientation.Horizontal) maxWidth else maxHeight
        val totalPx = with(density) { total.toPx() }

        if (orientation == Orientation.Horizontal) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .width(total * ratio.floatValue)
                        .fillMaxHeight(),
                    content = first,
                )
                Box(
                    modifier = Modifier
                        .width(dragArea)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(handleThickness)
                            .fillMaxHeight()
                            .background(palette.border)
                            .pointerInput(orientation, minRatio, maxRatio, totalPx) {
                                detectDragGestures { change, amount ->
                                    change.consume()
                                    ratio.floatValue = (ratio.floatValue + amount.x / totalPx)
                                        .coerceIn(minRatio, maxRatio)
                                }
                            },
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    content = second,
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .height(total * ratio.floatValue)
                        .fillMaxWidth(),
                    content = first,
                )
                Box(
                    modifier = Modifier
                        .height(dragArea)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .height(handleThickness)
                            .fillMaxWidth()
                            .background(palette.border)
                            .pointerInput(orientation, minRatio, maxRatio, totalPx) {
                                detectDragGestures { change, amount ->
                                    change.consume()
                                    ratio.floatValue = (ratio.floatValue + amount.y / totalPx)
                                        .coerceIn(minRatio, maxRatio)
                                }
                            },
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    content = second,
                )
            }
        }
    }
}
