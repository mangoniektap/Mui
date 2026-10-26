package com.mui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mui.theme.LocalMuiPalette

/**
 * 进度条：h-2 胶囊轨道（primary/20）+ bg-primary 填充指示器，宽度变化 150ms 过渡。
 * 输入：进度值；输出：静态指示条（无交互）。
 * 硬约束：颜色走 LocalMuiPalette（primary 20% 透明轨道 / primary 填充）；无 material3 依赖。
 *
 * @param value 进度 0..100（null = 无进度，呈现空轨道，对应 web 的 value||0）
 * @param modifier 布局修饰
 */
@Composable
fun MuiProgress(
    value: Float?,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMuiPalette.current
    val fraction = ((value ?: 0f) / 100f).coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 150),
    )
    val trackShape = CircleShape
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(trackShape)
            .background(palette.primary.copy(alpha = palette.primary.alpha * 0.20f)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(8.dp)
                .background(palette.primary, trackShape),
        )
    }
}
