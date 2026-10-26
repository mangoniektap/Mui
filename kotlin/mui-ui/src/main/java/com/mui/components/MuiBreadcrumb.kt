package com.mui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.theme.rememberMuiInteractionSource

/**
 * 面包屑：横向文本链（text-sm muted-foreground，gap-1.5）+ 分隔符槽 + 椭圆省略项。
 * 输入 = 各级链接 / 当前页 / 分隔符；输出 = 链接点击。
 * 硬约束：分隔符与省略号用 Canvas 画；无 ripple。
 *
 * @param modifier 布局修饰
 * @param content 面包屑内容（Row 作用域：排布 Link / Page / Separator / Ellipsis）
 */
@Composable
fun MuiBreadcrumb(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalMuiContentColor provides LocalMuiPalette.current.mutedForeground,
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

/**
 * 面包屑单项容器：inline-flex items-center gap-1.5。
 *
 * @param modifier 布局修饰
 * @param content 单项内容（Row 作用域）
 */
@Composable
fun MuiBreadcrumbItem(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/**
 * 面包屑链接：muted-foreground，悬停转 foreground。
 *
 * @param text 链接文字
 * @param onClick 点击回调
 * @param modifier 布局修饰
 * @param interactionSource 交互源
 */
@Composable
fun MuiBreadcrumbLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
) {
    val palette = LocalMuiPalette.current
    val hovered by interactionSource.collectIsHoveredAsState()
    MuiText(
        text = text,
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        ),
        color = if (hovered) palette.foreground else Color.Unspecified,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
}

/**
 * 面包屑当前页：font-normal text-foreground（不可点）。
 *
 * @param text 页面文字
 * @param modifier 布局修饰
 */
@Composable
fun MuiBreadcrumbPage(
    text: String,
    modifier: Modifier = Modifier,
) {
    MuiText(
        text = text,
        modifier = modifier,
        color = LocalMuiPalette.current.foreground,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
}

/**
 * 面包屑分隔符：默认 chevron-right（14dp，随内容色）。
 *
 * @param modifier 布局修饰
 * @param icon 自定义分隔符槽（null = 默认 chevron）
 */
@Composable
fun MuiBreadcrumbSeparator(
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
) {
    val contentColor = LocalMuiContentColor.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (icon != null) {
            icon()
        } else {
            Canvas(modifier = Modifier.size(14.dp)) {
                drawChevron(MenuIconDirection.Right, contentColor.takeOrDefault(LocalMuiPalette.current.mutedForeground))
            }
        }
    }
}

/**
 * 面包屑省略项：36dp 方块居中 more-horizontal 三点图标。
 *
 * @param modifier 布局修饰
 */
@Composable
fun MuiBreadcrumbEllipsis(
    modifier: Modifier = Modifier,
) {
    val contentColor = LocalMuiContentColor.current
    Box(
        modifier = modifier.size(36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(16.dp)) {
            drawDots(contentColor.takeOrDefault(LocalMuiPalette.current.mutedForeground))
        }
    }
}

/** DrawScope 上的 more-horizontal 三点路径（面包屑 / 分页共用画法） */
internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDots(color: Color) {
    val r = size.width / 24f
    val cy = size.height / 2f
    val xs = listOf(size.width * 5f / 24f, size.width / 2f, size.width * 19f / 24f)
    for (x in xs) {
        drawCircle(color = color, radius = r, center = Offset(x, cy))
    }
}
