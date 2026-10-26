package com.mui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 宽高比容器：内容按固定宽高比排布（web 的 radix AspectRatio）。
 * 输入 ratio + 内容槽；输出按比例约束尺寸的容器。
 * 硬约束：宽度由调用方给，高度按 ratio 推出。
 *
 * @param ratio 宽高比（width / height，如 16/9 = 1.7778f）
 * @param modifier 布局修饰（宽度等由调用方给）
 * @param content 容器内容
 */
@Composable
fun MuiAspectRatio(
    ratio: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.aspectRatio(ratio), content = content)
}
