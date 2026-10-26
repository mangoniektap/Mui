package com.mui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius
import com.mui.theme.muiElevation
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource

// TODO: web 的 viewport 共享面板（多个 trigger 共用一个视口动画）未移植，这里每个 trigger 独立 Popup
// TODO: web 的悬停移出自动收起未移植（点外部 / 再点触发器关闭）

/**
 * 导航菜单：横向一排顶级触发器（h-9 rounded-md px-4 text-sm font-medium），
 * 悬停 / 点击展开 Popup 面板（bg-popover + border + shadow-md + rounded-md）。
 * 输入 = 若干 Trigger（带面板槽）与 Link；输出 = 链接点击。
 * 硬约束：无 ripple；chevron 12dp 展开旋转 180°。
 *
 * @param modifier 布局修饰
 * @param content 导航菜单内容（Row 作用域：排布 MuiNavigationMenuTrigger / Link）
 */
@Composable
fun MuiNavigationMenu(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

/**
 * 导航菜单触发器：悬停或点击展开下方面板；展开时底色 accent/50、chevron 旋转。
 *
 * @param text 触发器文字
 * @param panel 面板内容（Column 作用域：放 MuiNavigationMenuLink 等）
 * @param modifier 布局修饰
 * @param enabled 可用性
 */
@Composable
fun MuiNavigationMenuTrigger(
    text: String,
    panel: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalMuiPalette.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val interactionSource = rememberMuiInteractionSource()
    val hovered by interactionSource.collectIsHoveredAsState()
    var open by remember { mutableStateOf(false) }
    var triggerHeight by remember { mutableStateOf(0) }
    val shape = RoundedCornerShape(MuiRadius.md)

    val chevronRotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
    )

    androidx.compose.runtime.LaunchedEffect(hovered) {
        if (hovered) open = true
    }

    val background = when {
        open -> palette.accent.copy(alpha = palette.accent.alpha * 0.50f)
        hovered -> palette.accent
        else -> palette.background
    }
    val contentColor = if (open || hovered) palette.accentForeground else Color.Unspecified

    Box(modifier.onSizeChanged { triggerHeight = it.height }) {
        Row(
            modifier = Modifier
                .height(36.dp)
                .clip(shape)
                .background(background, shape)
                .muiFocusRing(interactionSource, shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                ) { open = !open }
                .alpha(if (enabled) 1f else 0.5f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CompositionLocalProvider(LocalMuiContentColor provides contentColor) {
                MuiText(text = text, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .rotate(chevronRotation),
                ) {
                    MenuChevronIcon(
                        direction = MenuIconDirection.Down,
                        color = contentColor.takeOrDefault(palette.foreground),
                        iconSize = 12.dp,
                    )
                }
            }
        }
        if (open) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, triggerHeight + with(density) { 6.dp.roundToPx() }),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                MuiNavigationMenuPanel(content = panel)
            }
        }
    }
}

/**
 * 导航面板容器：bg-popover + border + shadow-md + rounded-md，p-2（右 2.5 格）。
 *
 * @param modifier 布局修饰
 * @param content 面板内容（Column 作用域）
 */
@Composable
fun MuiNavigationMenuPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalMuiPalette.current
    val shape = RoundedCornerShape(MuiRadius.md)
    CompositionLocalProvider(LocalMuiContentColor provides palette.popoverForeground) {
        Column(
            modifier = modifier
                .widthIn(min = 192.dp)
                .background(palette.popover, shape)
                .border(1.dp, palette.border, shape)
                .muiElevation("md", shape)
                .padding(start = 8.dp, top = 8.dp, end = 10.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}

/**
 * 导航链接：纵向 gap-1 rounded-sm p-2 text-sm，悬停/激活 bg-accent。
 *
 * @param title 链接标题
 * @param onClick 点击回调
 * @param modifier 布局修饰
 * @param description 次级描述文字（null = 无）
 * @param active 激活态（bg-accent/50）
 * @param enabled 可用性
 * @param interactionSource 交互源
 */
@Composable
fun MuiNavigationMenuLink(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    active: Boolean = false,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
) {
    val palette = LocalMuiPalette.current
    val hovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(MuiRadius.sm)
    val background = when {
        hovered -> palette.accent
        active -> palette.accent.copy(alpha = palette.accent.alpha * 0.50f)
        else -> Color.Transparent
    }

    Column(
        modifier = modifier
            .clip(shape)
            .background(background, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .alpha(if (enabled) 1f else 0.5f)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MuiText(text = title, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
        if (description != null) {
            MuiText(
                text = description,
                color = palette.mutedForeground,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}
