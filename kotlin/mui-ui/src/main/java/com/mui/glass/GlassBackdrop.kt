/** @file 将 Compose 页面背景录制到可由玻璃采样的图层。 */
package com.mui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import com.mui.theme.LocalMuiPageColor
import com.mui.theme.LocalMuiPalette

internal class GlassBackdropLayer(val layer: GraphicsLayer) {
    var origin by mutableStateOf<Offset?>(null)
}

internal val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdropLayer?> { null }

/**
 * 提供开关、滑杆和分段控件可折射的页面背景。两层内容应位于同一个 Compose 根节点。
 *
 * @param background 随容器铺满的背景内容，例如图片、渐变或滚动内容。
 * @param content 前景内容，由它决定容器尺寸，玻璃会采样其下方的 background。
 */
@Composable
fun GlassBackdrop(
    modifier: Modifier = Modifier,
    background: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val source = remember(layer) { GlassBackdropLayer(layer) }
    Box(modifier) {
        Box(
            Modifier.matchParentSize()
                .onGloballyPositioned { source.origin = it.localToRoot(Offset.Zero) }
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                },
            content = background,
        )
        CompositionLocalProvider(LocalGlassBackdrop provides source) {
            content()
        }
    }
}

@Composable
internal fun glassPageColor() = LocalMuiPageColor.current.takeIf { it != Color.Unspecified }
    ?: LocalMuiPalette.current.background
