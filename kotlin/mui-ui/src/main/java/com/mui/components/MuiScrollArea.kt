package com.mui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// TODO: web 的细滚动条（bg-border 圆头 thumb）未实现，依赖系统默认滚动条。

/**
 * 滚动容器：单方向滚动包装（web 的 radix ScrollArea）。
 * 输入方向 + 内容槽；输出可滚动区域。
 * 硬约束：纵向 = verticalScroll、横向 = horizontalScroll；尺寸约束由调用方给。
 *
 * @param modifier 布局修饰（高度/宽度约束由调用方给）
 * @param orientation 滚动方向（默认 Vertical）
 * @param content 滚动内容
 */
@Composable
fun MuiScrollArea(
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Vertical,
    content: @Composable BoxScope.() -> Unit,
) {
    val scrollModifier = when (orientation) {
        Orientation.Vertical -> Modifier.verticalScroll(rememberScrollState())
        Orientation.Horizontal -> Modifier.horizontalScroll(rememberScrollState())
    }
    Box(modifier = modifier.then(scrollModifier), content = content)
}

/** 横向滚动区快捷（占满宽） */
@Composable
fun MuiHorizontalScrollArea(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    MuiScrollArea(
        modifier = modifier.fillMaxWidth(),
        orientation = Orientation.Horizontal,
        content = content,
    )
}
