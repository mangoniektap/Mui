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
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mui.glass.GlassBody
import com.mui.glass.GlassLens
import com.mui.glass.glassCanvasPx
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

/** 玻璃画布的几何账：原点（相对球盒左上角，px）、球心（画布坐标，px）、外扩量（px） */
private data class SwitchGlassGeo(
    val originX: Float,
    val originY: Float,
    val cx: Float,
    val cy: Float,
    val marginPx: Int,
)

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
            .muiElevation("xs", trackShape)
            .background(trackColor, trackShape)
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
        contentAlignment = Alignment.TopStart,
    ) {
        // 与 web 的 switch 结构一一对应：外壳 = thumb 元素（thumbW×thumbH）。
        // 球的落点全部显式给（对齐不参与）：x = TRACK_INSET + 位移，y = TRACK_INSET
        // （web 里 items-center 的居中值恰好就是 TRACK_INSET，这里直接写死同一个语义）。
        Box(
            modifier = Modifier
                .offset { IntOffset((inset + clampedX.dp).roundToPx(), inset.roundToPx()) }
                .size(thumbW, thumbH),
        ) {
        // 球心与背面轨道的画布坐标。摆放层只能落在整数像素（offset/layout 的 roundToPx），
        // dp→px 的余数会在那里被吃掉 —— 按「取整后的实际画布原点」反推球心与轨道位置，
        // 把余数补回浮点 uniform，亚像素居中才成立（web 全程浮点，没有这一层损失）。
        val d = density.density
        val geo = with(density) {
            val boxX = (inset + clampedX.dp).roundToPx()
            val boxY = inset.roundToPx()
            // 外扩量只取整**一次**，记账与摆放共用同一个 px 值。
            // 之前记账用 margin.roundToPx()=+14、摆放用 (-margin).roundToPx()=-13
            // （负数 roundToPx 向零舍），球心因此比轨道中心低 1px。
            val m = margin.toPx().roundToInt()
            val originX = boxX - m
            val originY = boxY - m
            // 球心取**未取整**的理想位置（inset = 1.5dp）：盒子被 offset 夹到整像素，
            // 但轨道（背面）是按浮点画的 —— 球必须跟轨道对齐，不能跟自己被夹过的盒子对齐。
            SwitchGlassGeo(
                originX = originX.toFloat(),
                originY = originY.toFloat(),
                cx = (inset + clampedX.dp).toPx() + size.thumbW * d / 2f - originX,
                cy = inset.toPx() + size.thumbH * d / 2f - originY,
                marginPx = m,
            )
        }
        GlassLens(
            modifier = Modifier
                // 画布 = 球外扩四周 margin（= web 的 inset:-6px）：glassCanvas 显式摆放，
                // 不吃父对齐也不被父约束夹（requiredSize/offset/绘制位移三条路都叠过错偏移）。
                .glassCanvasPx(
                    widthPx = (thumbW.value * d).roundToInt() + geo.marginPx * 2,
                    heightPx = (thumbH.value * d).roundToInt() + geo.marginPx * 2,
                    offsetXPx = -geo.marginPx,
                    offsetYPx = -geo.marginPx,
                ),
            shapes = listOf(
                LensShape(
                    cx = geo.cx / d,
                    cy = geo.cy / d,
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
            paintPageBackdrop(resolvedPage)
            // 轨道在画布里的位置 = 画布原点取整后的负值（与球心同一套账，见上方 geo）
            val trackX = -geo.originX / d
            val trackY = -geo.originY / d
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(trackX * d, trackY * d),
                size = Size(trackW.toPx(), trackH.toPx()),
                cornerRadius = CornerRadius(trackH.toPx() / 2f),
            )
        }
        }
    }
}
