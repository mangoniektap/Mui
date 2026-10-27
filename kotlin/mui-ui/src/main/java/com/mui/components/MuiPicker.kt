package com.mui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.glass.GlassBody
import com.mui.glass.GlassLens
import com.mui.glass.GlassShadowLevel
import com.mui.glass.LensShape
import com.mui.glass.paintPageBackdrop
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius
import com.mui.theme.muiElevation
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

/** 一档的高度；容器高 = ITEM_H × VISIBLE */
private const val PICKER_ITEM_H = 36f

/** 可见档数：中间那档 + 上下各 2 档，边缘靠渐隐遮罩 */
private const val PICKER_VISIBLE = 5

/** 转盘曲率半径（dp）：值越小越「弯」 */
private const val PICKER_RADIUS = 64f

/** 轮盘的固定宽度（dp） */
private const val PICKER_WHEEL_W = 160f

/** 中心档的顶部位置（相对容器，dp） */
private const val PICKER_CENTER = (PICKER_VISIBLE * PICKER_ITEM_H - PICKER_ITEM_H) / 2f

/** 吸附动画时长：缓出三次，滚动要「落定」的感觉 */
private const val PICKER_SNAP_MS = 260

/**
 * 转盘选择器：调用方只给数值范围 + 单位，组件自己铺出整条刻度。
 * 交互：按住上下拖，松手吸附到最近一档（滚轮桌面交互未移植）。
 * 选中条 = gray 档玻璃，只做高亮，指针穿透。
 * 数字沿前弧排布（sin 定位置、cos 定大小淡出、rotateX 倾斜），圆只是视觉：
 * 刻度列表是直的、两端不循环，转过侧面的档（|θ| ≥ π/2）不画。
 *
 * @param min 范围下限（闭区间，从 min 起每 step 一档）
 * @param max 范围上限（闭区间）
 * @param step 步长（默认 1；小数步长也支持）
 * @param unit 单位后缀（固定在数字右侧、不参与滚动）
 * @param value 受控值（null = 非受控）
 * @param defaultValue 初始值（非受控用，默认第一档）
 * @param onValueChange 吸附落定后的值回调
 * @param format 数字显示格式（null = String(v)）
 * @param disabled 禁用：半透明且不可展开
 */
@Composable
fun MuiPicker(
    min: Float,
    max: Float,
    modifier: Modifier = Modifier,
    step: Float = 1f,
    unit: String? = null,
    value: Float? = null,
    defaultValue: Float? = null,
    onValueChange: ((Float) -> Unit)? = null,
    format: ((Float) -> String)? = null,
    disabled: Boolean = false,
) {
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    val scope = rememberCoroutineScope()
    val interactionSource = rememberMuiInteractionSource()

    val values = remember(min, max, step) {
        val n = ((max - min) / step + 1e-9f).toInt() + 1
        (0 until n.coerceAtLeast(0)).map { i -> ((min + i * step) * 1e6f).roundToInt() / 1e6f }
    }
    var inner by remember { mutableStateOf(defaultValue ?: values.firstOrNull() ?: min) }
    val current = value ?: inner
    val index = values.indexOfFirst { abs(it - current) < step / 2f }.coerceAtLeast(0)

    var open by remember { mutableStateOf(false) }
    /** 触发器实测尺寸与位置：轮盘要按它居中，并夹在窗口内（浮层坐标相对触发器左上角） */
    var triggerSize by remember { mutableStateOf(IntSize.Zero) }
    var triggerPos by remember { mutableStateOf(Offset.Zero) }
    /** 展开那一刻触发器在 root 里的位置：页面一滚，浮层就跟丢了，直接收起来 */
    var openAtPos by remember { mutableStateOf(Offset.Zero) }
    val scroll = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }

    val maxScroll = ((values.size - 1) * PICKER_ITEM_H).coerceAtLeast(0f)
    val label: (Float) -> String = { v -> format?.invoke(v) ?: trimNum(v) }

    // 展开时把轮盘定位到当前档
    LaunchedEffect(open, index, maxScroll) {
        if (open && !dragging) {
            scroll.snapTo((index * PICKER_ITEM_H).coerceIn(0f, maxScroll))
        }
    }

    fun snap() {
        val i = (scroll.value / PICKER_ITEM_H).roundToInt().coerceIn(0, values.size - 1)
        val next = values.getOrNull(i) ?: return
        scope.launch {
            scroll.animateTo(i * PICKER_ITEM_H, tween(PICKER_SNAP_MS, easing = EaseOutCubic))
        }
        if (next != current) {
            if (value == null) inner = next
            onValueChange?.invoke(next)
        }
    }

    Box(modifier = modifier) {
        // 触发器：h-9 圆角 md 描边；展开时隐形但保留占位（轮盘中心对准它）
        Box(
            Modifier
                .alpha(if (open) 0f else 1f)
                .clip(RoundedCornerShape(MuiRadius.md))
                .background(
                    if (isDark) {
                        palette.input.copy(alpha = palette.input.alpha * 0.30f)
                    } else Color.Transparent
                )
                .border(1.dp, palette.input, RoundedCornerShape(MuiRadius.md))
                .muiElevation("xs", RoundedCornerShape(MuiRadius.md))
                .muiFocusRing(interactionSource, RoundedCornerShape(MuiRadius.md))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = !disabled,
                ) {
                    if (!open) openAtPos = triggerPos
                    open = !open
                }
                .alpha(if (disabled) 0.5f else 1f)
                .padding(horizontal = 12.dp)
                .height(36.dp)
                .onSizeChanged { triggerSize = it }
                .onGloballyPositioned {
                    // 浮层是独立窗口，不跟页面滚动。触发器一移动（滚页面 / 弹键盘），
                    // 位置就对不上了 —— 按 web 的 tooltip 同款处理：直接收起，不要悬在半空。
                    val p = it.positionInRoot()
                    if (open && (p - openAtPos).getDistance() > 1f) open = false
                    triggerPos = p
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MuiText(text = if (values.isNotEmpty()) label(current) else "—")
                if (unit != null) {
                    MuiText(
                        text = unit,
                        modifier = Modifier.padding(start = 4.dp),
                        color = palette.mutedForeground,
                    )
                }
            }
        }

        if (open) {
            // 轮盘：浮层挂在触发器上（与 Select/Dialog 同一条路；web 侧是 absolute + portal）。
            // 早先它只是 Box 里的普通子项 —— 一展开就把整行撑到 180dp 高，
            // 并且会被任何 overflow-hidden 的祖先裁掉。
            val wheelShape = RoundedCornerShape(16.dp)
            val wheelH = (PICKER_VISIBLE * PICKER_ITEM_H).dp
            val ld = LocalDensity.current
            Popup(
                alignment = Alignment.TopStart,
                offset = with(ld) {
                    // 轮盘在触发器上居中；贴屏幕边时往里收（Radix 的 collision 一样的意思），
                    // 否则设置页右槽里的 Picker 一展开就有一半在屏幕外。
                    val pad = 8.dp.toPx()
                    val win = LocalWindowInfo.current.containerSize
                    val wPx = PICKER_WHEEL_W.dp.toPx()
                    val hPx = wheelH.toPx()
                    val wantX = (triggerSize.width - wPx) / 2f
                    val wantY = (triggerSize.height - hPx) / 2f
                    val minX = pad - triggerPos.x
                    val maxX = win.width - pad - wPx - triggerPos.x
                    val minY = pad - triggerPos.y
                    val maxY = win.height - pad - hPx - triggerPos.y
                    IntOffset(
                        (if (maxX >= minX) wantX.coerceIn(minX, maxX) else wantX).roundToInt(),
                        (if (maxY >= minY) wantY.coerceIn(minY, maxY) else wantY).roundToInt(),
                    )
                },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                ),
                onDismissRequest = { open = false },
            ) {
            Box(
                Modifier
                    .size(PICKER_WHEEL_W.dp, wheelH)
                    .clip(wheelShape)
                    .background(palette.popover)
                    .border(1.dp, palette.border, wheelShape)
                    .muiElevation("lg", wheelShape)
                    .pointerInput(values.size, maxScroll) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            dragging = true
                            val y0 = down.position.y
                            val s0 = scroll.value
                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    val target = (s0 - (change.position.y - y0))
                                        .coerceIn(-PICKER_ITEM_H, maxScroll + PICKER_ITEM_H)
                                    scope.launch { scroll.snapTo(target) }
                                    change.consume()
                                    if (!change.pressed) break
                                }
                            } finally {
                                dragging = false
                            }
                            snap()
                        }
                    },
            ) {
                // 选中条：gray 档玻璃，背面取转盘自己的 popover 底色
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(y = PICKER_CENTER.dp)
                        .size(PICKER_WHEEL_W.dp, PICKER_ITEM_H.dp)
                ) {
                    GlassLens(
                        modifier = Modifier.matchParentSize(),
                        body = GlassBody.GRAY,
                        shadow = GlassShadowLevel.NONE,
                        shapes = listOf(
                            LensShape(
                                cx = PICKER_WHEEL_W / 2f,
                                cy = PICKER_ITEM_H / 2f,
                                hw = PICKER_WHEEL_W / 2f,
                                hh = PICKER_ITEM_H / 2f,
                                radius = PICKER_ITEM_H / 2f,
                            )
                        ),
                    ) {
                        paintPageBackdrop(palette.popover)
                    }
                }

                // 数字槽（只有数字在滚）+ 单位钉在右侧不动
                Row(
                    Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val scrollPx = scroll.value
                    Box(
                        Modifier
                            .width(20.dp)
                            .height(wheelH)
                    ) {
                        values.forEachIndexed { i, v ->
                            val theta = (i * PICKER_ITEM_H - scrollPx) / PICKER_RADIUS
                            // 只画前弧：θ 未取模，转过侧面的档直接不画。
                            // 只判 cos 不够 —— 差一整圈（≈11.17 档）的档会转回正面、叠在同一行
                            if (abs(theta) >= (Math.PI / 2).toFloat()) return@forEachIndexed
                            val c = cos(theta)
                            val y = sin(theta) * PICKER_RADIUS
                            val dist = abs(theta)
                            val scale = 0.7f + 0.3f * c
                            val opacity = 1f.coerceAtMost(c.pow(1.4f) * 1.5f)
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(PICKER_ITEM_H.dp)
                                    .graphicsLayer {
                                        translationY = (PICKER_CENTER + y).dp.toPx()
                                        rotationX = -Math.toDegrees(theta.toDouble()).toFloat()
                                        cameraDistance = 600f * density
                                        scaleX = scale
                                        scaleY = scale
                                        alpha = opacity
                                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                MuiText(
                                    text = label(v),
                                    fontSize = 14.sp,
                                    color = if (dist < 0.2f) palette.foreground else palette.mutedForeground,
                                    fontWeight = if (dist < 0.2f) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                    if (unit != null) {
                        MuiText(text = unit, color = palette.mutedForeground, fontSize = 14.sp)
                    }
                }

                // 边缘渐隐遮罩（上下各 20%）
                val fadeH = (PICKER_VISIBLE * PICKER_ITEM_H * 0.2f).dp
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .size(PICKER_WHEEL_W.dp, fadeH)
                        .background(Brush.verticalGradient(listOf(palette.popover, Color.Transparent))),
                )
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .size(PICKER_WHEEL_W.dp, fadeH)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, palette.popover))),
                )

                // 关闭钮：mask 之外（不会被渐隐一起淡掉）
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { open = false },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(14.dp)) {
                        val s = size.width
                        drawLine(
                            palette.mutedForeground,
                            Offset(0f, 0f),
                            Offset(s, s),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            palette.mutedForeground,
                            Offset(s, 0f),
                            Offset(0f, s),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
            }
        }
    }
}

/** 数字显示：整数不带小数点，小数保留原样 */
private fun trimNum(v: Float): String {
    val r = (v * 1e6f).roundToInt() / 1e6f
    return if (r == r.toLong().toFloat()) r.toLong().toString() else r.toString()
}
