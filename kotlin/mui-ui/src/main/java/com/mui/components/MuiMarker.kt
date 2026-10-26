package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette

// TODO: web 的 [a] 链接下划线/hover 样式未移植（Compose 无富文本链接槽）。

/** 标记外观档位（marker.tsx 的 variant） */
enum class MarkerVariant { Default, Separator, Border }

/**
 * 行内标记：图标 + 内容的小行（text-sm muted-foreground）。
 * 输入 variant + 图标槽/内容槽；输出一行标记。
 * 硬约束：Separator 档两侧 1dp border 色线；Border 档底部 1dp 边线 + 底距 8dp。
 *
 * @param modifier 布局修饰
 * @param variant 外观档位（默认 Default）
 * @param icon 图标槽（16dp 方位；null = 无）
 * @param content 内容（文字用 MuiText，颜色自动取 muted-foreground）
 */
@Composable
fun MuiMarker(
    modifier: Modifier = Modifier,
    variant: MarkerVariant = MarkerVariant.Default,
    icon: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val palette = LocalMuiPalette.current
    val line = Modifier.height(1.dp).background(palette.border)
    CompositionLocalProvider(LocalMuiContentColor provides palette.mutedForeground) {
        when (variant) {
            MarkerVariant.Separator -> Row(
                modifier = modifier
                    .fillMaxWidth()
                    .defaultMinMarkerHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(line.weight(1f))
                if (icon != null) MuiMarkerIcon(content = icon)
                Box(Modifier) { content() }
                Box(line.weight(1f))
            }
            MarkerVariant.Border -> Row(
                modifier = modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = palette.border,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    .padding(bottom = 8.dp)
                    .defaultMinMarkerHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (icon != null) MuiMarkerIcon(content = icon)
                Box(Modifier.weight(1f)) { content() }
            }
            MarkerVariant.Default -> Row(
                modifier = modifier
                    .fillMaxWidth()
                    .defaultMinMarkerHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (icon != null) MuiMarkerIcon(content = icon)
                Box(Modifier.weight(1f)) { content() }
            }
        }
    }
}

/** 标记行最小高（min-h-4 = 16dp） */
private fun Modifier.defaultMinMarkerHeight(): Modifier =
    defaultMinSize(minHeight = 16.dp)

/**
 * 标记图标槽：16dp 固定方位（web 的 MarkerIcon）。
 *
 * @param modifier 布局修饰
 * @param content 图标内容（Canvas 自画或调用方组件）
 */
@Composable
fun MuiMarkerIcon(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.size(16.dp), contentAlignment = Alignment.Center) {
        content()
    }
}
