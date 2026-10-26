package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.theme.muiElevation
import com.mui.theme.rememberMuiInteractionSource
import com.mui.token.MuiRadius

/** 弹出方位（popover.tsx 的 side） */
enum class PopoverSide { Top, Right, Bottom, Left }

/**
 * 弹出层：受控 open 的锚点 Popup（bg-popover、border、md 阴影、rounded-md p-4、宽 288dp、
 * 文案 popover-foreground），点触发区开合、点外部/返回键关闭（focusable Popup）。
 * 输入：受控/非受控 open、触发槽、内容槽；输出：弹出浮层。
 * 硬约束：classical 浮层配方（border + popover + md 阴影）；无 ripple、无 material3 依赖。
 *
 * @param open 受控开关（null = 非受控，走 defaultOpen）
 * @param defaultOpen 初始开关状态（非受控时用）
 * @param onOpenChange 开关变化回调（外部关闭也会触发 false）
 * @param side 弹出方位（默认 Bottom）
 * @param offsetDp 浮层与触发区的间距（默认 4dp，对应 sideOffset=4）
 * @param modifier 触发区布局修饰
 * @param trigger 触发内容（点击切换 open）
 * @param content 浮层内容（Column 作用域）
 */
@Composable
fun MuiPopover(
    open: Boolean? = null,
    defaultOpen: Boolean = false,
    onOpenChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    side: PopoverSide = PopoverSide.Bottom,
    offsetDp: Dp = 4.dp,
    trigger: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var inner by remember { mutableStateOf(defaultOpen) }
    val isOpen = open ?: inner
    fun setOpen(value: Boolean) {
        if (open == null) inner = value
        onOpenChange(value)
    }

    val clickSource = rememberMuiInteractionSource()
    Box(
        modifier = modifier.clickable(
            interactionSource = clickSource,
            indication = null,
        ) { setOpen(!isOpen) },
    ) { trigger() }

    if (isOpen) {
        val density = LocalDensity.current
        val off = with(density) { offsetDp.roundToPx() }
        val (alignment, offset) = when (side) {
            PopoverSide.Top -> Alignment.TopCenter to IntOffset(0, -off)
            PopoverSide.Bottom -> Alignment.BottomCenter to IntOffset(0, off)
            PopoverSide.Left -> Alignment.CenterStart to IntOffset(-off, 0)
            PopoverSide.Right -> Alignment.CenterEnd to IntOffset(off, 0)
        }
        Popup(
            alignment = alignment,
            offset = offset,
            properties = PopupProperties(focusable = true),
            onDismissRequest = { setOpen(false) },
        ) {
            val palette = LocalMuiPalette.current
            val shape = RoundedCornerShape(MuiRadius.md)
            CompositionLocalProvider(LocalMuiContentColor provides palette.popoverForeground) {
                Column(
                    modifier = Modifier
                        .width(288.dp)
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

/** 浮层头区：纵向排布（gap-1） */
@Composable
fun MuiPopoverHeader(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

/** 浮层标题：font-medium */
@Composable
fun MuiPopoverTitle(text: String, modifier: Modifier = Modifier) {
    MuiText(text = text, modifier = modifier, fontWeight = FontWeight.Medium)
}

/** 浮层描述：muted-foreground */
@Composable
fun MuiPopoverDescription(text: String, modifier: Modifier = Modifier) {
    val palette = LocalMuiPalette.current
    CompositionLocalProvider(LocalMuiContentColor provides palette.mutedForeground) {
        MuiText(text = text, modifier = modifier)
    }
}
