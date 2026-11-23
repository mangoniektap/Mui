package com.mui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mui.glass.GlassBody
import com.mui.glass.GlassLens
import com.mui.glass.glassCanvasPx
import com.mui.glass.GlassShadowLevel
import com.mui.glass.LensShape
import com.mui.glass.glassPageColor
import com.mui.theme.LocalMuiPalette
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

/** 拇指件尺寸（dp）：胶囊，宽 > 高、圆角 = 半高 */
private const val THUMB_W = 24f
private const val THUMB_H = 16f

/** 透镜画布比拇指件外扩的边距：容纳按压外鼓 */
/** 画布外扩：外鼓 0.3×短边 + 惯性应变放大都要装得下 */
private const val LENS_MARGIN = 10f

/**
 * 滑杆：轨道 h-2 胶囊（bg-muted）+ 已填充段（bg-primary）+ 玻璃拇指件。
 * 支持单值和区间，拖动中连续回调 onValueChange。
 *
 * @param values 受控值列表（一个值表示单值滑杆，null 表示非受控）
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
    MuiSlider(
        orientation = Orientation.Horizontal, values = values, onValueChange = onValueChange, modifier = modifier,
        defaultValues = defaultValues, min = min, max = max, disabled = disabled,
    )
}

/** 支持横向/纵向、区间、步进、键盘调整和每个拇指的无障碍操作。 */
@Composable
fun MuiSlider(
    orientation: Orientation,
    values: List<Float>? = null,
    onValueChange: ((List<Float>) -> Unit)? = null,
    modifier: Modifier = Modifier,
    defaultValues: List<Float> = listOf(0f),
    min: Float = 0f,
    max: Float = 100f,
    disabled: Boolean = false,
    step: Float = 1f,
) {
    val palette = LocalMuiPalette.current
    val density = LocalDensity.current
    val pageColor = glassPageColor()
    val vertical = orientation == Orientation.Vertical
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val reversed = vertical || rtl
    val lower = min.takeIf { it.isFinite() } ?: 0f
    val upper = max.takeIf { it.isFinite() && it >= lower } ?: lower
    val increment = step.takeIf { it.isFinite() && it > 0f } ?: 1f
    val span = upper.toDouble() - lower
    fun fraction(value: Float) = if (span > 0.0) ((value - lower) / span).toFloat().coerceIn(0f, 1f) else 0f
    fun normalized(value: Float): Float {
        val offset = value.toDouble() - lower
        return (lower + round(offset / increment) * increment).toFloat().coerceIn(lower, upper)
    }
    fun nextValues(previous: List<Float>, index: Int, value: Float): List<Float> =
        previous.toMutableList().apply { this[index] = value }.sorted()

    var inner by remember { mutableStateOf(defaultValues) }
    val current = (values ?: inner).map { it.takeIf(Float::isFinite)?.coerceIn(lower, upper) ?: lower }.sorted()
    val latestCurrent by rememberUpdatedState(current)
    val latestCommit by rememberUpdatedState<(List<Float>) -> Unit> { next ->
        if (values == null) inner = next
        onValueChange?.invoke(next)
    }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var pressedIndex by remember { mutableStateOf(-1) }

    val fillStart = if (current.size > 1) fraction(current.first()) else 0f
    val fillEnd = current.lastOrNull()?.let { fraction(it) } ?: 0f
    val trackThickness = if (vertical) 6.dp else 8.dp
    val thumbWidth = THUMB_W.dp
    val thumbHeight = THUMB_H.dp
    val physicalStart = if (reversed) 1f - fillEnd else fillStart
    val physicalEnd = if (reversed) 1f - fillStart else fillEnd
    val dimensions = if (vertical) Modifier.width(thumbWidth).heightIn(min = 176.dp)
        else Modifier.fillMaxWidth().height(thumbHeight)
    Box(
        modifier.then(dimensions)
            .alpha(if (disabled) 0.5f else 1f)
            .onSizeChanged { rootSize = it }
            .drawBehind {
                drawSliderTrack(Offset.Zero, size, trackThickness.toPx(), vertical,
                    physicalStart, physicalEnd, palette.muted, palette.primary)
            }
            .pointerInput(disabled, vertical, reversed, lower, upper, increment) {
                if (disabled || span <= 0.0) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var gestureValues = latestCurrent
                    val length = if (vertical) size.height else size.width
                    if (gestureValues.isEmpty() || length <= 0) return@awaitEachGesture
                    fun valueAt(position: Offset): Float {
                        val axis = if (vertical) position.y else position.x
                        val ratio = (axis / length).coerceIn(0f, 1f)
                        return normalized((lower + (if (reversed) 1f - ratio else ratio) * span).toFloat())
                    }
                    val startValue = valueAt(down.position)
                    var index = gestureValues.indices.minBy { abs(gestureValues[it] - startValue) }
                    fun move(value: Float) {
                        if (value == gestureValues[index]) return
                        gestureValues = nextValues(gestureValues, index, value)
                        index = gestureValues.indexOf(value)
                        pressedIndex = index
                        latestCommit(gestureValues)
                    }
                    pressedIndex = index
                    down.consume()
                    move(startValue)
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            move(valueAt(change.position))
                            change.consume()
                            if (!change.pressed) break
                        }
                    } finally {
                        pressedIndex = -1
                    }
                }
            },
        contentAlignment = androidx.compose.ui.AbsoluteAlignment.TopLeft,
    ) {
        if (rootSize.width > 0 && rootSize.height > 0) {
            current.forEachIndexed { index, value ->
                val position = fraction(value)
                val physical = if (reversed) 1f - position else position
                val center = Offset(
                    if (vertical) rootSize.width / 2f else rootSize.width * physical,
                    if (vertical) rootSize.height * physical else rootSize.height / 2f,
                )
                val d = density.density
                val left = (center.x - THUMB_W * d / 2f).roundToInt()
                val top = (center.y - THUMB_H * d / 2f).roundToInt()
                val margin = (LENS_MARGIN * d).roundToInt()
                val origin = Offset((left - margin).toFloat(), (top - margin).toFloat())
                val interaction = rememberMuiInteractionSource()
                val shape = RoundedCornerShape(50)
                val enabled = !disabled && span > 0.0
                fun setThumb(next: Float) {
                    val safe = normalized(next)
                    if (safe != value) latestCommit(nextValues(current, index, safe))
                }
                val changeSteps = (span / increment).toInt().coerceAtLeast(1) - 1
                Box(
                    Modifier.absoluteOffset { IntOffset(left, top) }
                        .size(thumbWidth, thumbHeight)
                        .muiFocusRing(interaction, shape)
                        .semantics {
                            progressBarRangeInfo = ProgressBarRangeInfo(value, lower..upper, changeSteps)
                            if (enabled) setProgress { next -> setThumb(next); true }
                        }
                        .onKeyEvent { event ->
                            if (!enabled || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                            val shift = when (event.key) {
                                Key.DirectionUp -> increment
                                Key.DirectionDown -> -increment
                                Key.DirectionRight -> if (!vertical && rtl) -increment else increment
                                Key.DirectionLeft -> if (!vertical && rtl) increment else -increment
                                Key.PageUp -> increment * 10f
                                Key.PageDown -> -increment * 10f
                                Key.MoveHome -> lower - value
                                Key.MoveEnd -> upper - value
                                else -> return@onKeyEvent false
                            }
                            setThumb(value + shift)
                            true
                        }
                        .focusable(enabled, interaction),
                    contentAlignment = androidx.compose.ui.AbsoluteAlignment.TopLeft,
                ) {
                    val pressed = pressedIndex == index
                    GlassLens(
                        pageColor = pageColor,
                        modifier = Modifier.glassCanvasPx(
                            widthPx = (THUMB_W * d).roundToInt() + margin * 2,
                            heightPx = (THUMB_H * d).roundToInt() + margin * 2,
                            offsetXPx = -margin,
                            offsetYPx = -margin,
                        ),
                        shapes = listOf(
                            LensShape(
                                cx = (center.x - origin.x) / d,
                                cy = (center.y - origin.y) / d,
                                hw = THUMB_W / 2f,
                                hh = THUMB_H / 2f,
                                radius = THUMB_H / 2f,
                                swell = 1f,
                            ),
                        ),
                        live = true,
                        motion = 5,
                        body = if (pressed) GlassBody.TRANSPARENT else GlassBody.JADE,
                        pressed = pressed,
                        lensBand = 0.3f,
                        lensCompress = 0.20f,
                    ) {
                        drawSliderTrack(
                            -origin, Size(rootSize.width.toFloat(), rootSize.height.toFloat()),
                            trackThickness.toPx(), vertical, physicalStart, physicalEnd, palette.muted, palette.primary,
                        )
                    }
                }
            }
        }
    }
}

/** 控件和透镜背面用同一组轨道边界和填充位置，填充段受轨道圆角裁切。 */
private fun DrawScope.drawSliderTrack(
    origin: Offset,
    bounds: Size,
    thickness: Float,
    vertical: Boolean,
    fillStart: Float,
    fillEnd: Float,
    trackColor: Color,
    fillColor: Color,
) {
    if (bounds.width <= 0f || bounds.height <= 0f) return
    val length = if (vertical) bounds.height else bounds.width
    val trackSize = if (vertical) Size(thickness, length) else Size(length, thickness)
    val trackOrigin = origin + Offset((bounds.width - trackSize.width) / 2f, (bounds.height - trackSize.height) / 2f)
    val radius = CornerRadius(minOf(length, thickness) / 2f)
    drawRoundRect(trackColor, trackOrigin, trackSize, radius)
    if (fillEnd > fillStart) {
        val amount = length * (fillEnd - fillStart)
        val clip = Path().apply { addRoundRect(RoundRect(Rect(trackOrigin, trackSize), radius)) }
        clipPath(clip) {
            val distance = length * fillStart
            drawRoundRect(
                fillColor,
                trackOrigin + if (vertical) Offset(0f, distance) else Offset(distance, 0f),
                if (vertical) Size(thickness, amount) else Size(amount, thickness),
                CornerRadius(minOf(amount, thickness) / 2f),
            )
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
