/** @file 视口边缘的渐变磨砂表面。 */
package com.mui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mui.glass.GlassBackdrop
import com.mui.glass.LocalGlassBackdrop
import com.mui.glass.glassPageColor
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.roundToInt

private const val FROST_DECAY = 3f
private const val FROST_STOPS = 24

/**
 * 将磨砂栏覆盖在视口容器边缘。用 [GlassBackdrop] 提供能被模糊的实际页面内容。
 *
 * @param bottom true 时贴底，透明侧在上。false 时贴顶，透明侧在下。
 * @param blurRadius 磨砂的模糊半径，对应 Web 的 `--bar-frost-blur`。
 * @param wideBottomInset 宽视口中底栏为左侧栏留出的距离。
 * @param content 磨砂层上方的内容。栏本身可以为空。
 */
@Composable
fun BoxScope.MuiBarSurface(
    modifier: Modifier = Modifier,
    bottom: Boolean = false,
    blurRadius: Dp = 12.dp,
    wideBottomInset: Dp = 240.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val source = LocalGlassBackdrop.current
    val pageColor = glassPageColor()
    var origin by remember { mutableStateOf<Offset?>(null) }
    val blurred = rememberGraphicsLayer()
    val masked = rememberGraphicsLayer()

    BoxWithConstraints(
        Modifier.align(if (bottom) Alignment.BottomCenter else Alignment.TopCenter)
            .zIndex(30f)
            .then(modifier)
            .fillMaxWidth()
            .height(56.dp),
    ) {
        val leftInset = if (bottom && maxWidth >= 768.dp) wideBottomInset else 0.dp
        Box(
            Modifier.absolutePadding(left = leftInset)
                .fillMaxSize()
                .onGloballyPositioned { origin = it.localToRoot(Offset.Zero) }
                .drawWithCache {
                    val width = size.width.roundToInt()
                    val height = size.height.roundToInt()
                    val radius = blurRadius.toPx().coerceAtLeast(0f)
                    val margin = ceil(radius * 3f).toInt()
                    val sampleTop = if (bottom) -margin.toFloat() else 0f
                    val tail = exp(-FROST_DECAY)
                    val stops = Array(FROST_STOPS + 1) { index ->
                        val position = index.toFloat() / FROST_STOPS
                        val distance = if (bottom) position else 1f - position
                        val alpha = 1f - (exp(-FROST_DECAY * distance) - tail) / (1f - tail)
                        position to Color.Black.copy(alpha = alpha)
                    }
                    val gradient = Brush.verticalGradient(*stops, startY = 0f, endY = height.toFloat())
                    blurred.renderEffect = if (radius > 0f) BlurEffect(radius, radius, TileMode.Clamp) else null
                    masked.compositingStrategy = CompositingStrategy.Offscreen

                    onDrawBehind {
                        val backgroundOrigin = source?.origin
                        val barOrigin = origin
                        if (source != null && backgroundOrigin != null && barOrigin != null && width > 0 && height > 0) {
                            val offset = backgroundOrigin - barOrigin
                            blurred.record(this, layoutDirection, IntSize(width, height + margin)) {
                                drawRect(pageColor)
                                translate(offset.x, offset.y - sampleTop) { drawLayer(source.layer) }
                            }
                            masked.record(this, layoutDirection, IntSize(width, height)) {
                                clipRect {
                                    translate(top = sampleTop) { drawLayer(blurred) }
                                    drawRect(gradient, blendMode = BlendMode.DstIn)
                                }
                            }
                            drawLayer(masked)
                        }
                    }
                },
            content = content,
        )
    }
}
