package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.theme.muiElevation
import com.mui.token.MuiRadius

/** 弹出方位（hover-card.tsx 的 side） */
enum class HoverCardSide { Top, Right, Bottom, Left }

/**
 * 悬停卡片：悬停即时显示的锚点 Popup（bg-popover、border、md 阴影、rounded-md p-4、宽 256dp、
 * 文案 popover-foreground）。
 * 输入：触发槽、卡片内容槽；输出：悬停浮层。
 * 硬约束：classical 浮层配方（border + popover + md 阴影）；无 material3 依赖。
 *
 * TODO: web 的关闭延迟（closeDelay 300ms）与「指针从触发区移入卡片保持显示」未移植（离开即关）。
 *
 * @param trigger 触发内容（悬停其上显示卡片）
 * @param side 弹出方位（默认 Bottom）
 * @param offsetDp 卡片与触发区的间距（默认 4dp，对应 sideOffset=4）
 * @param modifier 触发区布局修饰
 * @param content 卡片内容（Column 作用域）
 */
@Composable
fun MuiHoverCard(
    trigger: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    side: HoverCardSide = HoverCardSide.Bottom,
    offsetDp: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hoverSource = remember { MutableInteractionSource() }
    val hovered by hoverSource.collectIsHoveredAsState()
    Box(modifier = modifier.hoverable(hoverSource)) { trigger() }

    if (hovered) {
        val density = LocalDensity.current
        val off = with(density) { offsetDp.roundToPx() }
        val (alignment, offset) = when (side) {
            HoverCardSide.Top -> Alignment.TopCenter to IntOffset(0, -off)
            HoverCardSide.Bottom -> Alignment.BottomCenter to IntOffset(0, off)
            HoverCardSide.Left -> Alignment.CenterStart to IntOffset(-off, 0)
            HoverCardSide.Right -> Alignment.CenterEnd to IntOffset(off, 0)
        }
        Popup(alignment = alignment, offset = offset) {
            val palette = LocalMuiPalette.current
            val shape = RoundedCornerShape(MuiRadius.md)
            CompositionLocalProvider(LocalMuiContentColor provides palette.popoverForeground) {
                Column(
                    modifier = Modifier
                        .width(256.dp)
                        .background(palette.popover, shape)
                        .border(1.dp, palette.border, shape)
                        .muiElevation("md", shape)
                        .padding(16.dp),
                    content = content,
                )
            }
        }
    }
}
