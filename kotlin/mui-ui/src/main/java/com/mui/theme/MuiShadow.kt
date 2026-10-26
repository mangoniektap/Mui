package com.mui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.mui.token.MuiElevation
import com.mui.token.MuiShadowSpec

/** 按档位取阴影层列表（亮 = 黑晕向下 / 暗 = 白晕零偏移） */
fun elevationSpecs(level: String, isDark: Boolean): List<MuiShadowSpec> = when (level) {
    "2xs" -> if (isDark) MuiElevation.dark2xs else MuiElevation.light2xs
    "xs" -> if (isDark) MuiElevation.darkXs else MuiElevation.lightXs
    "sm" -> if (isDark) MuiElevation.darkSm else MuiElevation.lightSm
    "md" -> if (isDark) MuiElevation.darkMd else MuiElevation.lightMd
    "lg" -> if (isDark) MuiElevation.darkLg else MuiElevation.lightLg
    "xl" -> if (isDark) MuiElevation.darkXl else MuiElevation.lightXl
    "2xl" -> if (isDark) MuiElevation.dark2xl else MuiElevation.light2xl
    else -> emptyList()
}

/**
 * 经典阴影（--elevation-* 的对应物）：按 shape 轮廓外扩绘制多层模糊投影。
 * 阴影画在节点内容之后面（drawBehind），超出节点边界的部分不裁剪（Compose 默认不 clip）。
 *
 * @param level 档位名：2xs / xs / sm / md / lg / xl / 2xl
 * @param shape 控件轮廓（仅支持圆角矩形 / 胶囊）
 */
fun Modifier.muiElevation(level: String, shape: Shape): Modifier = composed {
    val isDark = LocalIsDark.current
    val specs = remember(level, isDark) { elevationSpecs(level, isDark) }
    drawBehind {
        if (specs.isEmpty()) return@drawBehind
        val outline = shape.createOutline(size, layoutDirection, this)
        val rect = when (outline) {
            is Outline.Rounded -> outline.roundRect
            is Outline.Rectangle -> outline.rect
            else -> return@drawBehind
        }
        val radius = when (outline) {
            is Outline.Rounded -> outline.roundRect.cornerRadius.x
            else -> 0f
        }
        drawIntoCanvas { canvas ->
            val paint = Paint()
            for (spec in specs) {
                paint.color = spec.color
                paint.asFrameworkPaint().maskFilter = if (spec.blur > 0f) {
                    android.graphics.BlurMaskFilter(
                        spec.blur.toPx(),
                        android.graphics.BlurMaskFilter.Blur.NORMAL,
                    )
                } else null
                canvas.nativeCanvas.drawRoundRect(
                    rect.left + spec.dx.toPx(),
                    rect.top + spec.dy.toPx(),
                    rect.right + spec.dx.toPx(),
                    rect.bottom + spec.dy.toPx(),
                    radius,
                    radius,
                    paint.asFrameworkPaint(),
                )
            }
        }
    }
}

/** 胶囊/圆角矩形描边快捷：边框画在轮廓上（对应 web 的 border） */
fun Modifier.muiBorder(
    widthDp: Float,
    color: Color,
    cornerRadiusDp: Float,
): Modifier = composed {
    drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(cornerRadiusDp * density, cornerRadiusDp * density),
            style = Stroke(width = widthDp * density),
        )
    }
}
