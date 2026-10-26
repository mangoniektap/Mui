package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius

// TODO: web 的 tooltip 内特殊配色（bg-background/20）依赖宿主上下文，未移植。

/**
 * 键帽：单键提示小块（h-5 rounded-sm bg-muted text-xs font-medium muted-foreground）。
 * 输入文本或内容槽；输出 20dp 高的键帽。
 * 硬约束：px-1（4dp）、最小宽 20dp、font-sans。
 *
 * @param modifier 布局修饰
 * @param text 键名（如 "⌘"、"B"；null = 用 content 槽）
 * @param content 自定义内容（Row 作用域，可放 Canvas 图标）
 */
@Composable
fun MuiKbd(
    modifier: Modifier = Modifier,
    text: String? = null,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    val palette = LocalMuiPalette.current
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .background(palette.muted, shape)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CompositionLocalProvider(LocalMuiContentColor provides palette.mutedForeground) {
            if (text != null) {
                MuiText(text = text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            content?.invoke(this)
        }
    }
}

/**
 * 键帽组：多枚键帽横排（gap-1）。
 *
 * @param modifier 布局修饰
 * @param content 组内容（若干 MuiKbd）
 */
@Composable
fun MuiKbdGroup(
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
