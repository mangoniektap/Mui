package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mui.theme.LocalMuiPalette

/**
 * 分隔线：1dp 实线，颜色 --border。
 *
 * @param orientation 方向（Horizontal = 横线占满宽 / Vertical = 竖线占满高）
 * @param thickness 线粗（默认 1dp）
 */
@Composable
fun MuiSeparator(
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
    thickness: Dp = 1.dp,
) {
    val color = LocalMuiPalette.current.border
    val m = modifier.background(color)
    when (orientation) {
        Orientation.Horizontal -> m.height(thickness).fillMaxWidth()
        Orientation.Vertical -> m.width(thickness)
    }
}

/** 方向枚举（分隔线 / 表单布局共用） */
enum class Orientation { Horizontal, Vertical }
