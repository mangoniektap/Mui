package com.mui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius

/**
 * 骨架屏：rounded-md 底色 --accent，透明度脉冲呼吸（animate-pulse 的对应物）。
 * 尺寸由调用方给（如 Modifier.size(120.dp, 16.dp)）。
 */
@Composable
fun MuiSkeleton(modifier: Modifier = Modifier) {
    val palette = LocalMuiPalette.current
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton-alpha",
    )
    androidx.compose.foundation.layout.Box(
        modifier
            .clip(RoundedCornerShape(MuiRadius.md))
            .background(palette.accent.copy(alpha = palette.accent.alpha * alpha)),
    )
}
