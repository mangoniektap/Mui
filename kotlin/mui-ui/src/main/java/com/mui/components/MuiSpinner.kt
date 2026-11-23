package com.mui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mui.theme.LocalMuiPalette

/**
 * 加载圈：Loader2Icon 的对应物——四分之三圆弧、16dp 见方（size 可调）、匀速旋转。
 *
 * @param size 圈的边长（默认 16dp = size-4）
 * @param strokeWidth 圈线粗（默认 2dp）
 */
@Composable
fun MuiSpinner(modifier: Modifier = Modifier, size: Dp = 16.dp, strokeWidth: Dp = 2.dp) {
    val palette = LocalMuiPalette.current
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "spinnerRotation",
    )
    Canvas(modifier = modifier.size(size)) {
        val stroke = strokeWidth.toPx().coerceAtMost(this.size.minDimension)
        if (stroke <= 0f) return@Canvas
        val inset = stroke / 2f
        val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
        rotate(degrees = angle) {
            drawArc(
                color = palette.foreground,
                startAngle = -90f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}
