package com.mui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * 聚焦环配方（classicalFocusRing 的对应物）：聚焦时在控件外画 3dp 描边环。
 *
 * web 版 = focus-visible:border-ring + ring-[3px] ring-ring/50；
 * Compose 侧把环画在轮廓上（描边中心贴边），颜色随 [invalid] 切 destructive。
 *
 * @param interactionSource 组件自己的 interactionSource（读取聚焦态）
 * @param shape 控件轮廓
 * @param invalid 校验失败态：环转 destructive（亮 20% / 暗 40%）
 */
fun Modifier.muiFocusRing(
    interactionSource: MutableInteractionSource,
    shape: Shape,
    invalid: Boolean = false,
): Modifier = composed {
    val focused by interactionSource.collectIsFocusedAsState()
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    val ringColor = if (invalid) {
        palette.destructive.copy(alpha = if (isDark) 0.40f else 0.20f)
    } else {
        palette.ring.copy(alpha = 0.50f)
    }
    val ringAlpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
    )
    drawBehind {
        if (ringAlpha <= 0f) return@drawBehind
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(
            outline = outline,
            color = ringColor.copy(alpha = ringColor.alpha * ringAlpha),
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

/** 无交互源的变体：由调用方直接给聚焦布尔（如 has-focus 的日历） */
fun Modifier.muiFocusRing(
    focused: Boolean,
    shape: Shape,
    invalid: Boolean = false,
): Modifier = composed {
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    val ringColor = if (invalid) {
        palette.destructive.copy(alpha = if (isDark) 0.40f else 0.20f)
    } else {
        palette.ring.copy(alpha = 0.50f)
    }
    drawBehind {
        if (!focused) return@drawBehind
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(outline = outline, color = ringColor, style = Stroke(width = 3.dp.toPx()))
    }
}

/** 经典聚焦环的交互源便捷创建（组件内部用） */
@Composable
fun rememberMuiInteractionSource(): MutableInteractionSource =
    remember { MutableInteractionSource() }

/** 前景色低透明度快捷（web 的 foreground/6 之类） */
fun Color.withAlphaFrac(frac: Float): Color = copy(alpha = frac)
