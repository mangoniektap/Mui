package com.mui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette

// TODO: web 的 Previous/Next 覆盖 px-2.5（按钮默认 px-4）未移植，沿用 MuiButton 默认内边距
// TODO: web 的 sm: 断点文字显隐（小屏隐藏 Previous/Next 文字）未移植

/**
 * 分页：居中的页码按钮组（gap-1），页码钮 = 当前页 outline / 其它 ghost（icon 方钮）。
 * 输入 = 页码钮 / 上页 / 下页 / 省略号组合；输出 = 各钮点击。
 * 硬约束：外观复用 MuiButton variant；chevron 与省略号 Canvas 画。
 *
 * @param modifier 布局修饰
 * @param content 分页内容（Row 作用域：排布 Pagination 部件）
 */
@Composable
fun MuiPagination(
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
 * 页码钮：当前页 outline、其它 ghost，36dp 方钮（web size=icon）。
 *
 * @param page 页码文字（如 "1"）
 * @param onClick 点击回调
 * @param modifier 布局修饰
 * @param isActive 是否当前页
 */
@Composable
fun MuiPaginationLink(
    page: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
) {
    MuiButton(
        onClick = onClick,
        modifier = modifier,
        variant = if (isActive) ButtonVariant.Outline else ButtonVariant.Ghost,
        size = ButtonSize.Icon,
    ) {
        MuiText(text = page)
    }
}

/**
 * 上一页钮：ghost/outline + 左 chevron + "Previous" 文字。
 *
 * @param onClick 点击回调
 * @param modifier 布局修饰
 * @param isActive 是否当前页
 */
@Composable
fun MuiPaginationPrevious(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
) {
    MuiButton(
        onClick = onClick,
        modifier = modifier,
        variant = if (isActive) ButtonVariant.Outline else ButtonVariant.Ghost,
        size = ButtonSize.Default,
    ) {
        PaginationChevron(direction = MenuIconDirection.Left)
        MuiText(text = "Previous")
    }
}

/**
 * 下一页钮：ghost/outline + "Next" 文字 + 右 chevron。
 *
 * @param onClick 点击回调
 * @param modifier 布局修饰
 * @param isActive 是否当前页
 */
@Composable
fun MuiPaginationNext(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
) {
    MuiButton(
        onClick = onClick,
        modifier = modifier,
        variant = if (isActive) ButtonVariant.Outline else ButtonVariant.Ghost,
        size = ButtonSize.Default,
    ) {
        MuiText(text = "Next")
        PaginationChevron(direction = MenuIconDirection.Right)
    }
}

/**
 * 省略号：36dp 方块居中 more-horizontal 三点图标（不可点）。
 *
 * @param modifier 布局修饰
 */
@Composable
fun MuiPaginationEllipsis(
    modifier: Modifier = Modifier,
) {
    val contentColor = LocalMuiContentColor.current
    Box(
        modifier = modifier.size(36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(16.dp)) {
            drawDots(contentColor.takeOrDefault(LocalMuiPalette.current.foreground))
        }
    }
}

/** 按钮内容色 chevron（取 LocalMuiContentColor，兜底 foreground） */
@Composable
private fun PaginationChevron(direction: MenuIconDirection) {
    val contentColor = LocalMuiContentColor.current
    Canvas(modifier = Modifier.size(16.dp)) {
        drawChevron(direction, contentColor.takeOrDefault(LocalMuiPalette.current.foreground))
    }
}
