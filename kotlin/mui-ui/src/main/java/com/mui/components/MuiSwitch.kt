package com.mui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mui.glass.GlassBody
import com.mui.glass.GlassLens
import com.mui.glass.GlassShadowLevel
import com.mui.glass.LensShape
import com.mui.glass.paintPageBackdrop
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiPageColor
import com.mui.theme.LocalMuiPalette
import com.mui.theme.muiElevation
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** 三档尺寸：轨道 w×h；thumb 高 = 轨道高 − 2×缝，四边等粗 */
enum class SwitchSize(val trackW: Float, val trackH: Float, val thumbW: Float, val thumbH: Float) {
    Sm(24f, 14f, 16f, 11f),
    Md(32f, 16f, 20f, 13f),
    Lg(40f, 20f, 24f, 17f),
}

/** 拖动判定阈值：位移超过它才算拖动，否则当点击（toggle） */
private const val DRAG_THRESHOLD_DP = 3f

/** 透镜画布比球外扩的边距：容纳按压外鼓，否则鼓出部分被画布裁掉 */
private const val LENS_MARGIN_DP = 6f

/** thumb 与轨道四周各留的缝：露出一道淡边，四边等粗 */
private const val TRACK_INSET_DP = 1.5f

/**
 * 开关：点击切换；按住可沿轨道左右拖动，松手按落点是否过半吸附。
 * 玻璃球走 GlassLens（按住 = 透明档、平时 = 玉白档），背面解析绘制（页面色 + 轨道胶囊）。
 *
 * @param checked 受控状态（null = 非受控，走 defaultChecked）
 * @param defaultChecked 初始状态（非受控时用）
 * @param onCheckedChange 状态变化回调（拖动提交与点击都会走这里）
 * @param size 三档尺寸（默认 Md）
 * @param disabled 禁用：半透明且不响应
 * @param invalid 校验失败态：聚焦环转 destructive
 * @param interactionSource 交互源（聚焦环出口）
 */
@Composable
fun MuiSwitch(
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    defaultChecked: Boolean = false,
    size: SwitchSize = SwitchSize.Md,
    disabled: Boolean = false,
    invalid: Boolean = false,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
) {
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    val pageColor = LocalMuiPageColor.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var inner by remember { mutableStateOf(defaultChecked) }
    val isChecked = checked ?: inner

    var pressed by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }

    val inset = TRACK_INSET_DP.dp
    val margin = LENS_MARGIN_DP.dp
    val trackW = size.trackW.dp
    val trackH = size.trackH.dp
    val thumbW = size.thumbW.dp
    val thumbH = size.thumbH.dp
    val travel = size.trackW - size.thumbW - TRACK_INSET_DP * 2

    // 球位移动画：静止/切档走 150ms ease-out；拖动中直接跟手
    val animX = remember { Animatable(0f) }
    LaunchedEffect(isChecked, travel, dragging) {
        val target = if (isChecked) travel else 0f
        if (!dragging) {
            animX.animateTo(target, tween(durationMillis = 150, easing = EaseOut))
        }
    }
    val displayX = if (dragging) (if (isChecked) travel else 0f) + dragX else animX.value
    val clampedX = displayX.coerceIn(0f, travel)

    val trackShape = RoundedCornerShape(trackH / 2)
    val trackColor = if (isChecked) {
        palette.primary
    } else if (isDark) {
        palette.input.copy(alpha = palette.input.alpha * 0.80f)
    } else {
        palette.input
    }
    val resolvedPage = if (pageColor != androidx.compose.ui.graphics.Color.Unspecified) {
        pageColor
    } else {
        palette.background
    }

    fun commit(next: Boolean) {
        if (checked == null) inner = next
        onCheckedChange?.invoke(next)
    }

    val capsuleShape = RoundedCornerShape(thumbH / 2)
    Box(
        modifier = modifier
            .size(trackW, trackH)
            .alpha(if (disabled) 0.5f else 1f)
            .clip(trackShape)
            .background(trackColor, trackShape)
            .muiElevation("xs", trackShape)
            .muiFocusRing(interactionSource, trackShape, invalid)
            .focusable(enabled = !disabled, interactionSource = interactionSource)
            .pointerInput(disabled) {
                if (disabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    dragging = false
                    dragX = 0f
                    var moved = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val dx = change.position.x - down.position.x
                            if (!moved && abs(dx) > DRAG_THRESHOLD_DP.dp.toPx()) {
                                moved = true
                                dragging = true
                                scope.launch { animX.snapTo(clampedX) }
                            }
                            if (moved) {
                                dragX = dx
                                change.consume()
                            }
                            if (!change.pressed) break
                        }
                    } finally {
                        val dx = dragX
                        pressed = false
                        dragging = false
                        dragX = 0f
                        if (moved) {
                            commit((if (isChecked) travel else 0f) + dx > travel / 2f)
                        } else {
                            commit(!isChecked)
                        }
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset((inset + clampedX.dp).roundToPx(), inset.roundToPx()) }
                .size(thumbW, thumbH),
        ) {
            GlassLens(
                modifier = Modifier
                    .offset { IntOffset(-margin.roundToPx(), -margin.roundToPx()) }
                    .size(thumbW + margin * 2, thumbH + margin * 2),
                shapes = listOf(
                    LensShape(
                        cx = LENS_MARGIN_DP + size.thumbW / 2f,
                        cy = LENS_MARGIN_DP + size.thumbH / 2f,
                        hw = size.thumbW / 2f,
                        hh = size.thumbH / 2f,
                        radius = size.thumbH / 2f,
                        swell = 1f,
                    )
                ),
                live = true,
                motion = 5,
                body = if (pressed) GlassBody.TRANSPARENT else GlassBody.JADE,
                pressed = pressed,
                lensBand = 0.3f,
                shadow = GlassShadowLevel.SM,
            ) {
                // 背面：页面色 + 轨道胶囊（坐标按画布原点 = 球左上角 − margin）
                paintPageBackdrop(resolvedPage)
                val trackX = LENS_MARGIN_DP - TRACK_INSET_DP - clampedX
                val trackY = LENS_MARGIN_DP - TRACK_INSET_DP
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(trackX.dp.toPx(), trackY.dp.toPx()),
                    size = Size(trackW.toPx(), trackH.toPx()),
                    cornerRadius = CornerRadius(trackH.toPx() / 2f),
                )
            }
        }
    }
}
