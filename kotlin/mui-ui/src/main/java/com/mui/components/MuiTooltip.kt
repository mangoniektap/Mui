package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius
import kotlinx.coroutines.delay

/** 弹出方位（tooltip.tsx 的 side） */
enum class TooltipSide { Top, Right, Bottom, Left }

/**
 * 在触发器悬停或聚焦时显示提示，默认立即显示。箭头随浮层翻转。
 *
 * @param side 浮层朝向（默认 Top）。
 * @param offsetDp 箭头尖端与触发器之间的间距。
 * @param delayMillis 悬停显示延迟，键盘聚焦时立即显示。
 */
@Composable
fun MuiTooltip(
    text: String,
    modifier: Modifier = Modifier,
    side: TooltipSide = TooltipSide.Top,
    offsetDp: Dp = 0.dp,
    delayMillis: Long = 0,
    content: @Composable () -> Unit,
) {
    val hoverSource = remember { MutableInteractionSource() }
    val hovered by hoverSource.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(hovered, focused, delayMillis) {
        if (focused || hovered) {
            if (!focused) delay(delayMillis.coerceAtLeast(0))
            shown = true
        } else {
            shown = false
        }
    }

    val preferredSide = MuiPopupSide.valueOf(side.name)
    var arrow by remember(preferredSide) { mutableStateOf(preferredSide to 0) }
    val updateArrow: (MuiPopupSide, Int) -> Unit = remember {
        { placedSide, offset -> arrow = placedSide to offset }
    }
    Box(modifier) {
        Box(Modifier.onFocusChanged { focused = it.hasFocus }.focusGroup().hoverable(hoverSource)) {
            content()
        }
        if (shown) {
            Popup(popupPositionProvider = rememberMuiPopupPosition(preferredSide, offsetDp, onPlaced = updateArrow)) {
                val palette = LocalMuiPalette.current
                val (placedSide, arrowOffset) = arrow
                val arrowHeight = 5.dp
                val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                val arrowPadding = when (placedSide) {
                    MuiPopupSide.Top -> PaddingValues(bottom = arrowHeight)
                    MuiPopupSide.Right -> if (rtl) PaddingValues(end = arrowHeight) else PaddingValues(start = arrowHeight)
                    MuiPopupSide.Bottom -> PaddingValues(top = arrowHeight)
                    MuiPopupSide.Left -> if (rtl) PaddingValues(start = arrowHeight) else PaddingValues(end = arrowHeight)
                }
                Box(
                    Modifier.drawBehind {
                        val depth = arrowHeight.toPx()
                        val halfWidth = 5.dp.toPx()
                        val vertical = placedSide == MuiPopupSide.Top || placedSide == MuiPopupSide.Bottom
                        val crossSize = if (vertical) size.width else size.height
                        val inset = halfWidth.coerceAtMost(crossSize / 2)
                        val center = arrowOffset.toFloat().coerceIn(inset, crossSize - inset)
                        val pointsOut = placedSide == MuiPopupSide.Top || placedSide == MuiPopupSide.Left
                        val axisSize = if (vertical) size.height else size.width
                        val tip = if (pointsOut) axisSize else 0f
                        val base = if (pointsOut) axisSize - depth else depth
                        val path = Path().apply {
                            if (vertical) {
                                moveTo(center, tip)
                                lineTo(center - inset, base)
                                lineTo(center + inset, base)
                            } else {
                                moveTo(tip, center)
                                lineTo(base, center - inset)
                                lineTo(base, center + inset)
                            }
                            close()
                        }
                        drawPath(path, palette.foreground)
                    }.padding(arrowPadding),
                ) {
                    Box(
                        modifier = Modifier
                            .background(palette.foreground, RoundedCornerShape(MuiRadius.md))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        CompositionLocalProvider(LocalMuiContentColor provides palette.background) {
                            MuiText(text = text, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
