/** @file 按触发器边缘定位浮层，并在窗口边缘处理翻转和偏移。 */
package com.mui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider

internal enum class MuiPopupSide { Top, Right, Bottom, Left }

/** 为浮层计算朝向、距锚点间距和窗口内的最终位置。alignStart 按布局方向对齐。 */
@Composable
internal fun rememberMuiPopupPosition(
    side: MuiPopupSide,
    gap: Dp,
    alignStart: Boolean = false,
    alignOffset: Dp = 0.dp,
    onPlaced: ((MuiPopupSide, Int) -> Unit)? = null,
): PopupPositionProvider {
    val density = LocalDensity.current
    return remember(density, side, gap, alignStart, alignOffset, onPlaced) {
        with(density) {
            MuiPopupPosition(side, gap.roundToPx(), alignStart, alignOffset.roundToPx(), onPlaced)
        }
    }
}

private class MuiPopupPosition(
    private val side: MuiPopupSide,
    private val gap: Int,
    private val alignStart: Boolean,
    private val alignOffset: Int,
    private val onPlaced: ((MuiPopupSide, Int) -> Unit)?,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val anchor = anchorBounds
        val popup = popupContentSize
        val vertical = side == MuiPopupSide.Top || side == MuiPopupSide.Bottom
        val opposite = when (side) {
            MuiPopupSide.Top -> MuiPopupSide.Bottom
            MuiPopupSide.Right -> MuiPopupSide.Left
            MuiPopupSide.Bottom -> MuiPopupSide.Top
            MuiPopupSide.Left -> MuiPopupSide.Right
        }
        fun available(candidate: MuiPopupSide) = when (candidate) {
            MuiPopupSide.Top -> anchor.top
            MuiPopupSide.Right -> windowSize.width - anchor.right
            MuiPopupSide.Bottom -> windowSize.height - anchor.bottom
            MuiPopupSide.Left -> anchor.left
        } - gap

        val required = if (vertical) popup.height else popup.width
        val actualSide = if (available(side) < required && available(opposite) > available(side)) opposite else side
        val x = when (actualSide) {
            MuiPopupSide.Left -> anchor.left - gap - popup.width
            MuiPopupSide.Right -> anchor.right + gap
            else -> when {
                !alignStart -> anchor.center.x - popup.width / 2
                layoutDirection == LayoutDirection.Rtl -> anchor.right - popup.width - alignOffset
                else -> anchor.left + alignOffset
            }
        }.coerceIn(0, (windowSize.width - popup.width).coerceAtLeast(0))
        val y = when (actualSide) {
            MuiPopupSide.Top -> anchor.top - gap - popup.height
            MuiPopupSide.Bottom -> anchor.bottom + gap
            else -> if (alignStart) anchor.top + alignOffset else anchor.center.y - popup.height / 2
        }.coerceIn(0, (windowSize.height - popup.height).coerceAtLeast(0))
        onPlaced?.invoke(actualSide, if (vertical) anchor.center.x - x else anchor.center.y - y)
        return IntOffset(x, y)
    }
}
