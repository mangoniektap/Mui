package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
 * 文字提示：悬停触发（延迟默认 700ms 显示、离开即关），气泡 = bg-foreground、text-background、
 * rounded-md、px-3 py-1.5、12sp；锚点 Popup（无箭头）。
 * 输入：提示文本、方位与间距、触发内容槽；输出：气泡浮层。
 * 硬约束：气泡颜色 = foreground/background 反转对（对齐 web bg-foreground text-background）；无 material3 依赖。
 *
 * TODO: web 的触摸设备触发未移植（仅悬停；触摸设备上等效按压前态不可用时无提示）。
 *
 * @param text 提示文本
 * @param side 弹出方位（默认 Top）
 * @param offsetDp 气泡与触发区的间距（默认 8dp，对应 web slide-in-from-2）
 * @param delayMillis 悬停到显示的延迟（默认 700ms）
 * @param modifier 触发区布局修饰
 * @param content 触发内容
 */
@Composable
fun MuiTooltip(
    text: String,
    modifier: Modifier = Modifier,
    side: TooltipSide = TooltipSide.Top,
    offsetDp: Dp = 8.dp,
    delayMillis: Long = 700,
    content: @Composable () -> Unit,
) {
    val hoverSource = remember { MutableInteractionSource() }
    val hovered by hoverSource.collectIsHoveredAsState()
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(hovered) {
        if (hovered) {
            delay(delayMillis)
            shown = true
        } else {
            shown = false
        }
    }
    Box(modifier = modifier.hoverable(hoverSource)) { content() }

    if (shown) {
        val density = LocalDensity.current
        val off = with(density) { offsetDp.roundToPx() }
        val (alignment, offset) = when (side) {
            TooltipSide.Top -> Alignment.TopCenter to IntOffset(0, -off)
            TooltipSide.Bottom -> Alignment.BottomCenter to IntOffset(0, off)
            TooltipSide.Left -> Alignment.CenterStart to IntOffset(-off, 0)
            TooltipSide.Right -> Alignment.CenterEnd to IntOffset(off, 0)
        }
        Popup(alignment = alignment, offset = offset) {
            val palette = LocalMuiPalette.current
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
