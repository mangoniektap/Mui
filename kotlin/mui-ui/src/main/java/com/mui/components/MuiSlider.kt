package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mui.glass.GlassBody
import com.mui.glass.GlassLens
import com.mui.glass.GlassShadowLevel
import com.mui.glass.LensShape
import com.mui.glass.paintPageBackdrop
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiPageColor
import com.mui.theme.LocalMuiPalette
import kotlin.math.abs
import kotlin.math.roundToInt

/** 拇指件尺寸（dp）：胶囊，宽 > 高、圆角 = 半高 */
private const val THUMB_W = 24f
private const val THUMB_H = 16f

/** 透镜画布比拇指件外扩的边距：容纳按压外鼓 */
private const val LENS_MARGIN = 6f

/**
 * 滑杆：轨道 h-2 胶囊（bg-muted）+ 已填充段（bg-primary）+ 玻璃拇指件。
 * 支持单值与双值（区间）；拖动中连续回调 onValueChange。
 *
 * @param values 受控值列表（1 个 = 单值滑杆，2 个 = 区间滑杆；null = 非受控）
 * @param defaultValues 初始值（非受控用）
 * @param onValueChange 值变化回调（拖动中连续触发）
 * @param min 范围下限（默认 0f）
 * @param max 范围上限（默认 100f）
 * @param disabled 禁用：半透明且不响应
 */
@Composable
fun MuiSlider(
    values: List<Float>? = null,
    onValueChange: ((List<Float>) -> Unit)? = null,
    modifier: Modifier = Modifier,
    defaultValues: List<Float> = listOf(0f),
    min: Float = 0f,
    max: Float = 100f,
    disabled: Boolean = false,
) {
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    val density = LocalDensity.current
    val pageColor = LocalMuiPageColor.current
    val resolvedPage = if (pageColor != androidx.compose.ui.graphics.Color.Unspecified) pageColor else palette.background

    var inner by remember { mutableStateOf(defaultValues) }
    val current = values ?: inner
    val thumbCount = current.size.coerceIn(1, 2)

    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var pressedIndex by remember { mutableStateOf(-1) }

    fun commit(next: List<Float>) {
        if (values == null) inner = next
        onValueChange?.invoke(next)
    }

    val trackH = 8.dp
    val thumbWDp = THUMB_W.dp
    val thumbHDp = THUMB_H.dp
    val marginDp = LENS_MARGIN.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thumbHDp)
            .alpha(if (disabled) 0.5f else 1f)
            .onSizeChanged { rootSize = it }
            .pointerInput(disabled, thumbCount) {
                if (disabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // 落点最近的那颗 thumb 接管
                    val trackW = size.width.toFloat()
                    val frac = (down.position.x / trackW).coerceIn(0f, 1f)
                    val downVal = min + frac * (max - min)
                    var idx = 0
                    var best = Float.MAX_VALUE
                    current.forEachIndexed { i, v ->
                        val d = abs(v - downVal)
                        if (d < best) {
                            best = d
                            idx = i
                        }
                    }
                    pressedIndex = idx
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val f = (change.position.x / trackW).coerceIn(0f, 1f)
                            var v = min + f * (max - min)
                            // 双 thumb 时互相推挤，不越过对方
                            val nextList = current.toMutableList()
                            if (thumbCount == 2) {
                                if (idx == 0) v = v.coerceAtMost(nextList[1])
                                else v = v.coerceAtLeast(nextList[0])
                            }
                            nextList[idx] = v
                            commit(nextList)
                            change.consume()
                            if (!change.pressed) break
                        }
                    } finally {
                        pressedIndex = -1
                    }
                }
            },
        contentAlignment = androidx.compose.ui.Alignment.CenterStart,
    ) {
        // 轨道
        val trackShape = RoundedCornerShape(trackH / 2)
        Box(
            Modifier
                .fillMaxWidth()
                .height(trackH)
                .background(palette.muted, trackShape)
        )
        // 已填充段：最低 thumb 到最高 thumb
        val trackW = rootSize.width
        if (trackW > 0 && current.isNotEmpty()) {
            val lo = ((current.minOrNull() ?: min) - min) / (max - min)
            val hi = ((current.maxOrNull() ?: max) - min) / (max - min)
            if (hi > lo) {
                Box(
                    Modifier
                        .offset { IntOffset((lo * trackW).roundToInt(), 0) }
                        .size(with(density) { ((hi - lo) * trackW).toDp() }, 8.dp)
                        .background(palette.primary, RoundedCornerShape(4.dp))
                )
            }
        }
        // 玻璃拇指件
        if (trackW > 0) {
            current.forEachIndexed { i, v ->
                val frac = ((v - min) / (max - min)).coerceIn(0f, 1f)
                val thumbCenterX = frac * trackW
                val isPressed = pressedIndex == i
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                (thumbCenterX - thumbWDp.toPx() / 2f).roundToInt(),
                                0,
                            )
                        }
                        .size(thumbWDp, thumbHDp)
                ) {
                    GlassLens(
                        modifier = Modifier
                            .offset {
                                IntOffset(-marginDp.roundToPx(), -marginDp.roundToPx())
                            }
                            .size(thumbWDp + marginDp * 2, thumbHDp + marginDp * 2),
                        shapes = listOf(
                            LensShape(
                                cx = LENS_MARGIN + THUMB_W / 2f,
                                cy = LENS_MARGIN + THUMB_H / 2f,
                                hw = THUMB_W / 2f,
                                hh = THUMB_H / 2f,
                                radius = THUMB_H / 2f,
                                swell = 1f,
                            )
                        ),
                        live = true,
                        motion = 5,
                        body = if (isPressed) GlassBody.TRANSPARENT else GlassBody.JADE,
                        pressed = isPressed,
                        lensBand = 0.3f,
                        lensCompress = 0.20f,
                        shadow = GlassShadowLevel.SM,
                    ) {
                        // 背面：页面 + 轨道胶囊 + 已填充段（弧头，裁进轨道圆角内）
                        paintPageBackdrop(resolvedPage)
                        val trackTop = (thumbHDp.toPx() - trackH.toPx()) / 2f
                        val canvasLeft = thumbCenterX - thumbWDp.toPx() / 2f - marginDp.toPx()
                        val trackX = -canvasLeft
                        val trackColor = palette.muted
                        val fillStart = (((current.minOrNull() ?: min) - min) / (max - min)).coerceIn(0f, 1f) * trackW
                        val fillEnd = (((current.maxOrNull() ?: max) - min) / (max - min)).coerceIn(0f, 1f) * trackW
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(trackX, trackTop),
                            size = Size(trackW.toFloat(), trackH.toPx()),
                            cornerRadius = CornerRadius(trackH.toPx() / 2f),
                        )
                        if (fillEnd > fillStart) {
                            drawRoundRect(
                                color = palette.primary,
                                topLeft = Offset(trackX + fillStart, trackTop),
                                size = Size(fillEnd - fillStart, trackH.toPx()),
                                cornerRadius = CornerRadius(trackH.toPx() / 2f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 单值便捷重载：内部走 List 通道 */
@Composable
fun MuiSlider(
    value: Float? = null,
    onValueChange: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
    defaultValue: Float = 0f,
    min: Float = 0f,
    max: Float = 100f,
    disabled: Boolean = false,
) {
    var inner by remember { mutableStateOf(defaultValue) }
    val listValue = value?.let { listOf(it) }
    MuiSlider(
        values = listValue,
        onValueChange = if (onValueChange != null) {
            { list ->
                val v = list.first()
                if (value == null) inner = v
                onValueChange(v)
            }
        } else null,
        modifier = modifier,
        defaultValues = listOf(if (value == null) inner else defaultValue),
        min = min,
        max = max,
        disabled = disabled,
    )
}
