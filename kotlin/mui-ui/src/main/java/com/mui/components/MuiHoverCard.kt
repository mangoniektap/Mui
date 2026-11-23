package com.mui.components

import kotlinx.coroutines.delay
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.theme.muiElevation
import com.mui.token.MuiRadius

/** 弹出方位（hover-card.tsx 的 side） */
enum class HoverCardSide { Top, Right, Bottom, Left }

/**
 * 悬停卡片：悬停延迟显示的锚点 Popup（bg-popover、border、md 阴影、rounded-md p-4、宽 256dp、
 * 文案 popover-foreground）。
 * 输入：触发槽、卡片内容槽；输出：悬停浮层。
 * 硬约束：classical 浮层配方（border + popover + md 阴影）；无 material3 依赖。
 *
 * 悬停 700ms 后打开，离开触发器和内容 300ms 后关闭。
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
    val triggerSource = remember { MutableInteractionSource() }
    val cardSource = remember { MutableInteractionSource() }
    val hoveredTrigger by triggerSource.collectIsHoveredAsState()
    val hoveredCard by cardSource.collectIsHoveredAsState()
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(hoveredTrigger, hoveredCard) {
        if (hoveredTrigger || hoveredCard) {
            if (!shown) {
                delay(700)
                shown = true
            }
        } else if (shown) {
            delay(300)
            shown = false
        }
    }
    Box(modifier) {
        Box(Modifier.hoverable(triggerSource)) { trigger() }
        if (shown) {
            Popup(popupPositionProvider = rememberMuiPopupPosition(MuiPopupSide.valueOf(side.name), offsetDp)) {
                val palette = LocalMuiPalette.current
                val shape = RoundedCornerShape(MuiRadius.md)
                CompositionLocalProvider(LocalMuiContentColor provides palette.popoverForeground) {
                    Column(
                        modifier = Modifier
                            .hoverable(cardSource)
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
}
