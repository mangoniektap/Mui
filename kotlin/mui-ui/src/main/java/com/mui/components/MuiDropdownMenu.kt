package com.mui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius
import com.mui.theme.muiElevation
import com.mui.theme.rememberMuiInteractionSource

/**
 * 下拉菜单：触发器槽 + Popup 菜单表面（bg-popover + border + shadow-md + rounded-md）。
 * 菜单内容用条目 lambda 组合（MuiDropdownMenuItem / Label / Separator / CheckboxItem / RadioItem / SubMenu），
 * 条目点击后经 onDismiss 关闭菜单。
 *
 * @param open 菜单是否展开
 * @param onOpenChange 展开态变化回调（点外部 / 选中条目都会回调 false）
 * @param trigger 触发器槽（任意可点内容，由调用方负责点击切换 open）
 * @param modifier 触发器容器的布局修饰
 * @param content 菜单内容（Column 作用域：组合各条目）
 */
@Composable
fun MuiDropdownMenu(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    trigger: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    var triggerHeight by remember { mutableIntStateOf(0) }
    Box(modifier.onSizeChanged { triggerHeight = it.height }) {
        trigger()
    }
    if (open) {
        Popup(
            alignment = Alignment.TopStart,
            offset = IntOffset(0, triggerHeight + with(density) { 4.dp.roundToPx() }),
            onDismissRequest = { onOpenChange(false) },
            properties = PopupProperties(focusable = true),
        ) {
            MuiMenuSurface(content = content)
        }
    }
}

/**
 * 菜单表面容器：min-w-[8rem] p-1、bg-popover、border、shadow-md、rounded-md，
 * 内部提供 popoverForeground 内容色（条目文字默认取它）。
 *
 * @param content 菜单内容（Column 作用域）
 * @param modifier 布局修饰
 */
@Composable
fun MuiMenuSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalMuiPalette.current
    val shape = RoundedCornerShape(MuiRadius.md)
    CompositionLocalProvider(LocalMuiContentColor provides palette.popoverForeground) {
        Column(
            modifier = modifier
                .widthIn(min = 128.dp)
                .background(palette.popover, shape)
                .border(1.dp, palette.border, shape)
                .muiElevation("md", shape)
                .padding(4.dp),
            content = content,
        )
    }
}

/**
 * 菜单条目：rounded-sm px-2 py-1.5 text-sm gap-2；悬停 = bg-accent text-accent-foreground。
 * destructive 条目 = text-destructive，悬停底 = destructive/10（暗色 /20）。
 *
 * @param text 条目文字
 * @param onClick 点击回调
 * @param onDismiss 点击后关闭菜单的回调（可为 null = 不关闭）
 * @param modifier 布局修饰
 * @param enabled 可用性（false = 半透明且不可点）
 * @param destructive 危险项样式
 * @param inset 缩进排版（左侧预留指示器位 pl-8）
 * @param leading 前置图标槽（16dp，默认 muted-foreground 色）
 * @param trailing 后置槽位（放 Shortcut 等）
 * @param interactionSource 交互源
 */
@Composable
fun MuiDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    inset: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
) {
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    val hovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(MuiRadius.sm)

    val background = when {
        destructive && hovered -> palette.destructive.copy(alpha = if (isDark) 0.20f else 0.10f)
        hovered -> palette.accent
        else -> Color.Transparent
    }
    val contentColor = when {
        destructive -> palette.destructive
        hovered -> palette.accentForeground
        else -> Color.Unspecified
    }

    Row(
        modifier = modifier
            .clip(shape)
            .background(background, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
            ) {
                onClick()
                onDismiss?.invoke()
            }
            .alpha(if (enabled) 1f else 0.5f)
            .then(if (inset) Modifier.padding(start = 32.dp) else Modifier)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompositionLocalProvider(LocalMuiContentColor provides contentColor) {
            if (leading != null) leading()
            MuiText(text = text, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f, fill = false))
            if (trailing != null) trailing()
        }
    }
}

/**
 * 菜单分组标题：px-2 py-1.5 text-sm font-medium。
 *
 * @param text 标题文字
 * @param modifier 布局修饰
 * @param inset 缩进排版（pl-8）
 */
@Composable
fun MuiDropdownMenuLabel(
    text: String,
    modifier: Modifier = Modifier,
    inset: Boolean = false,
) {
    MuiText(
        text = text,
        modifier = modifier
            .then(if (inset) Modifier.padding(start = 32.dp) else Modifier)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    )
}

/** 菜单分隔线：-mx-1 my-1 h-px bg-border（负水平外边距由两侧各 4dp 补偿实现） */
@Composable
fun MuiDropdownMenuSeparator(modifier: Modifier = Modifier) {
    val palette = LocalMuiPalette.current
    Box(
        modifier = modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .height(1.dp)
            .background(palette.border),
    )
}

/**
 * 菜单快捷键文字：ml-auto text-xs tracking-widest text-muted-foreground。
 *
 * @param text 快捷键文本（如 ⌘C）
 * @param modifier 布局修饰
 */
@Composable
fun MuiDropdownMenuShortcut(
    text: String,
    modifier: Modifier = Modifier,
) {
    MuiText(
        text = text,
        modifier = modifier,
        color = LocalMuiPalette.current.mutedForeground,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.em,
    )
}

/**
 * 菜单复选条目：pl-8，左侧 14dp 指示位画对勾（选中时）。
 *
 * @param text 条目文字
 * @param checked 是否勾选
 * @param onCheckedChange 勾选态变化回调
 * @param onDismiss 点击后关闭菜单的回调（可为 null = 不关闭）
 * @param modifier 布局修饰
 * @param enabled 可用性
 * @param interactionSource 交互源
 */
@Composable
fun MuiDropdownMenuCheckboxItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
) {
    val contentColor = LocalMuiContentColor.current
    val palette = LocalMuiPalette.current
    MuiDropdownMenuItem(
        text = text,
        onClick = { onCheckedChange(!checked) },
        onDismiss = onDismiss,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        leading = {
            Box(modifier = Modifier.size(14.dp), contentAlignment = Alignment.Center) {
                if (checked) MenuCheckIcon(color = contentColor.takeOrDefault(palette.popoverForeground))
            }
        },
    )
}

/**
 * 菜单单选条目：pl-8，左侧 14dp 指示位画实心圆点（选中时）。
 *
 * @param text 条目文字
 * @param selected 是否选中
 * @param onClick 选中回调
 * @param onDismiss 点击后关闭菜单的回调（可为 null = 不关闭）
 * @param modifier 布局修饰
 * @param enabled 可用性
 * @param interactionSource 交互源
 */
@Composable
fun MuiDropdownMenuRadioItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
) {
    val contentColor = LocalMuiContentColor.current
    val palette = LocalMuiPalette.current
    MuiDropdownMenuItem(
        text = text,
        onClick = onClick,
        onDismiss = onDismiss,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        leading = {
            Box(modifier = Modifier.size(14.dp), contentAlignment = Alignment.Center) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(contentColor.takeOrDefault(palette.popoverForeground), RoundedCornerShape(50)),
                    )
                }
            }
        },
    )
}

/**
 * 子菜单：条目样式 + 右侧 chevron，点击在其右侧弹出嵌套菜单表面。
 *
 * @param text 子菜单标题
 * @param onDismiss 关闭顶层菜单的回调（子项选中时由子条目再触发）
 * @param modifier 布局修饰
 * @param enabled 可用性
 * @param content 子菜单内容（Column 作用域：组合子条目）
 */
@Composable
fun MuiDropdownMenuSubMenu(
    text: String,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalMuiPalette.current
    val contentColor = LocalMuiContentColor.current
    val hovered = remember { MutableInteractionSource() }
    var open by remember { mutableStateOf(false) }
    var itemWidth by remember { mutableIntStateOf(0) }

    Box(modifier.onSizeChanged { itemWidth = it.width }) {
        MuiDropdownMenuItem(
            text = text,
            onClick = { open = !open },
            enabled = enabled,
            interactionSource = hovered,
            leading = null,
            trailing = {
                MenuChevronIcon(
                    direction = MenuIconDirection.Right,
                    color = contentColor.takeOrDefault(palette.popoverForeground),
                )
            },
        )
        if (open) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(itemWidth, 0),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                MuiMenuSurface(content = content)
            }
        }
    }
}

/** 图标方向（菜单内 Canvas 小图标共用） */
internal enum class MenuIconDirection { Left, Right, Down, Up }

/** 对勾图标：两段线（默认 16dp，线宽 2dp） */
@Composable
internal fun MenuCheckIcon(color: Color, modifier: Modifier = Modifier, iconSize: androidx.compose.ui.unit.Dp = 16.dp) {
    androidx.compose.foundation.Canvas(modifier.size(iconSize)) {
        drawCheck(color)
    }
}

/** chevron 小箭头：两段线（默认 16dp，线宽 2dp） */
@Composable
internal fun MenuChevronIcon(
    direction: MenuIconDirection,
    color: Color,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 16.dp,
) {
    androidx.compose.foundation.Canvas(modifier.size(iconSize)) {
        drawChevron(direction, color)
    }
}

/** DrawScope 上的对勾路径（菜单 / 命令面板共用画法） */
internal fun DrawScope.drawCheck(color: Color) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.20f, h * 0.52f), Offset(w * 0.40f, h * 0.72f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.40f, h * 0.72f), Offset(w * 0.80f, h * 0.30f), strokeWidth = stroke.width, cap = StrokeCap.Round)
}

/** DrawScope 上的 chevron 路径（方向枚举控制开口朝向） */
internal fun DrawScope.drawChevron(direction: MenuIconDirection, color: Color) {
    val w = size.width
    val h = size.height
    val stroke = 2.dp.toPx()
    val (start, mid, end) = when (direction) {
        MenuIconDirection.Right -> Triple(Offset(w * 0.38f, h * 0.25f), Offset(w * 0.62f, h * 0.50f), Offset(w * 0.38f, h * 0.75f))
        MenuIconDirection.Left -> Triple(Offset(w * 0.62f, h * 0.25f), Offset(w * 0.38f, h * 0.50f), Offset(w * 0.62f, h * 0.75f))
        MenuIconDirection.Down -> Triple(Offset(w * 0.25f, h * 0.38f), Offset(w * 0.50f, h * 0.62f), Offset(w * 0.75f, h * 0.38f))
        MenuIconDirection.Up -> Triple(Offset(w * 0.25f, h * 0.62f), Offset(w * 0.50f, h * 0.38f), Offset(w * 0.75f, h * 0.62f))
    }
    drawLine(color, start, mid, strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, mid, end, strokeWidth = stroke, cap = StrokeCap.Round)
}

/** Unspecified 内容色兜底（图标槽在 CompositionLocal 外读取时用） */
internal fun Color.takeOrDefault(fallback: Color): Color =
    if (this == Color.Unspecified) fallback else this
