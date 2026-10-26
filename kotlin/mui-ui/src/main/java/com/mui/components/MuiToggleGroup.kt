package com.mui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.VectorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.glass.GlassBody
import com.mui.glass.GlassLens
import com.mui.glass.GlassShadowLevel
import com.mui.glass.LensShape
import com.mui.glass.paintPageBackdrop
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiPageColor
import com.mui.theme.LocalMuiPalette
import com.mui.theme.MuiRadius
import com.mui.theme.muiElevation
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** 单选组的一个栏目：label 必给；给 icon 变成「图标在上、文字在下」的竖排形态 */
data class SegmentedItem(
    /** 选中值（提交给 onValueChange 的那个） */
    val value: String,
    /** 栏目文字（同一组内请保持等长，等长才等宽、玻璃只滑位置） */
    val label: String,
    /** 图标（null = 纯文字段） */
    val icon: ImageVector? = null,
)

/** 组的模式：Single = 白胶囊底座 + 玻璃滑块分段控件；Multiple = 原按钮组 */
enum class ToggleGroupType { Single, Multiple }

/** 玻璃滑动的缓动（与开关 thumb 的吸附同一条手感） */
private val GLIDE_EASING = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

/** 画布外扩边距 = 段高 × 这个比例：装下按压外鼓与惯性纵向膨胀 */
private const val LENS_MARGIN_RATIO = 0.32f

/** 段：纯文字（高 32dp）；段：图标 + 文字（竖排，11sp） */
private val SEG_TEXT_H = 32.dp
private val SEG_STACK_VPAD = 8.dp

/**
 * 开关组：Single（items 给齐）= 白胶囊底座 + 平灰玻璃滑块（按住切透明档、可拖动吸附）；
 * Multiple = 原按钮组（default/outline 变体，spacing 0 时首尾圆角相连）。
 *
 * @param type 组模式（Single / Multiple）
 * @param items 栏目清单；Single 必给（分段控件数据源），Multiple 也可用（按钮组数据源）
 * @param value 单选受控值（Multiple 时忽略）
 * @param defaultSingle 单选初始值（非受控用）
 * @param onValueChange 单选值回调（按下即切 + 松手吸附提交）
 * @param values 多选受控值集合（Single 时忽略）
 * @param defaultValues 多选初始集合
 * @param onValuesChange 多选值回调
 * @param variant 按钮组变体（default / outline，仅 Multiple 生效）
 * @param spacing 按钮间距（0 = 连成一体的按钮组外观）
 * @param activeColor 被玻璃盖住的文字/图标染色（按几何位置算：玻璃滑到哪里染到哪里；null = foreground）
 * @param disabled 禁用整组
 */
@Composable
fun MuiToggleGroup(
    type: ToggleGroupType,
    items: List<SegmentedItem>,
    modifier: Modifier = Modifier,
    value: String? = null,
    defaultSingle: String? = null,
    onValueChange: ((String) -> Unit)? = null,
    values: Set<String> = emptySet(),
    defaultValues: Set<String> = emptySet(),
    onValuesChange: ((Set<String>) -> Unit)? = null,
    variant: ToggleVariant = ToggleVariant.Default,
    spacing: Int = 0,
    activeColor: Color? = null,
    disabled: Boolean = false,
) {
    if (type == ToggleGroupType.Single) {
        SegmentedToggleGroup(
            items = items,
            modifier = modifier,
            value = value,
            defaultSingle = defaultSingle,
            onValueChange = onValueChange,
            activeColor = activeColor,
            disabled = disabled,
        )
    } else {
        MultipleToggleGroup(
            items = items,
            modifier = modifier,
            values = values,
            defaultValues = defaultValues,
            onValuesChange = onValuesChange,
            variant = variant,
            spacing = spacing,
            disabled = disabled,
        )
    }
}

/** 单选组内部状态包：受控/非受控的当前值与提交 */
@Composable
private fun SegmentedToggleGroup(
    items: List<SegmentedItem>,
    modifier: Modifier,
    value: String?,
    defaultSingle: String?,
    onValueChange: ((String) -> Unit)?,
    activeColor: Color?,
    disabled: Boolean,
) {
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    val pageColor = LocalMuiPageColor.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    var inner by remember { mutableStateOf(defaultSingle ?: items.firstOrNull()?.value) }
    val current = value ?: inner
    val activeIndex = items.indexOfFirst { it.value == current }

    var pressed by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var dragX by remember { mutableStateOf(0f) }
    /** 拖动基准：按下的那一段（x 与宽），与刚提交的选中段同源 */
    var baseX by remember { mutableStateOf(0f) }
    var baseW by remember { mutableStateOf(0f) }

    /** 每个段相对 Root 的盒子（px） */
    var segs by remember { mutableStateOf<List<Rect>>(emptyList()) }

    fun commit(next: String) {
        if (value == null) inner = next
        onValueChange?.invoke(next)
    }

    // 段盒子位置：逐段 onGloballyPositioned 量（相对 root）
    val rootOrigin = remember { mutableStateOf(Offset.Zero) }

    val glassAnim = remember { Animatable(0f) }
    val targetSeg = segs.getOrNull(activeIndex)
    LaunchedEffect(targetSeg?.left, dragging) {
        val t = targetSeg ?: return@LaunchedEffect
        if (!dragging) {
            glassAnim.animateTo(t.left, tween(280, easing = GLIDE_EASING))
        }
    }

    fun commitAt(index: Int) {
        items.getOrNull(index)?.let { commit(it.value) }
    }

    val segH = SEG_TEXT_H

    val baseShape = RoundedCornerShape(50)
    val resolvedPage = if (pageColor != Color.Unspecified) pageColor else palette.background
    val tint = activeColor ?: palette.foreground

    Box(
        modifier = modifier
            .onGloballyPositioned { rootOrigin.value = it.boundsInRoot().topLeft }
            .alpha(if (disabled) 0.5f else 1f)
            .clip(baseShape)
            .background(palette.card, baseShape)
            .border(1.dp, palette.input, baseShape)
            .muiElevation("sm", baseShape)
            .pointerInput(items.size, disabled, segs.size) {
                if (disabled || segs.isEmpty()) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val rootLeft = rootOrigin.value.x
                    val px = down.position.x
                    // 按下即锚定：按到的那段不是选中段就立刻切过去
                    val hit = segs.indexOfFirst { px >= it.left && px <= it.right }
                    val segIdx = if (hit >= 0) hit else 0
                    val seg = segs[segIdx]
                    baseX = seg.left
                    baseW = seg.width
                    pressed = true
                    dragging = false
                    dragX = 0f
                    var moved = false
                    if (items.getOrNull(segIdx)?.value != current) {
                        commitAt(segIdx)
                    }
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val dx = change.position.x - down.position.x
                            if (!moved && abs(dx) > 3.dp.toPx()) {
                                moved = true
                                dragging = true
                                scope.launch { glassAnim.snapTo(baseX) }
                            }
                            if (moved) {
                                dragX = dx
                                change.consume()
                            }
                            if (!change.pressed) break
                        }
                    } finally {
                        val dxF = dragX
                        pressed = false
                        dragging = false
                        dragX = 0f
                        if (moved) {
                            // 玻璃中心落在哪一段就吸到哪一段
                            val center = (baseX + dxF).coerceIn(segs.first().left, segs.last().left) + baseW / 2f
                            var to = 0
                            var best = Float.MAX_VALUE
                            segs.forEachIndexed { i, s ->
                                val d = abs(s.center.x - center)
                                if (d < best) {
                                    best = d
                                    to = i
                                }
                            }
                            commitAt(to)
                        }
                    }
                }
            }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        // 段行
        Row(verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { i, item ->
                val on = item.value == current
                val segInteraction = rememberMuiInteractionSource()
                Box(
                    Modifier
                        .onGloballyPositioned { coords ->
                            val b = coords.boundsInRoot()
                            val rel = b.translate(-rootOrigin.value)
                            segs = segs.toMutableList().also { list ->
                                while (list.size <= i) list.add(Rect.Zero)
                                list[i] = rel
                            }
                        }
                        .clip(CircleShape)
                        .height(if (item.icon != null) segH + SEG_STACK_VPAD else segH)
                        .clickable(
                            interactionSource = segInteraction,
                            indication = null,
                            enabled = !disabled,
                        ) {
                            // 单选组不允许取消选中：点已选中的那栏什么都不做
                            if (!on) commitAt(i)
                        }
                        .muiFocusRing(segInteraction, CircleShape)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item.icon != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(item.icon),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                colorFilter = ColorFilter.tint(
                                    when {
                                        on -> tint
                                        else -> palette.mutedForeground
                                    }
                                ),
                            )
                            MuiText(
                                text = item.label,
                                fontSize = 11.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    on -> tint
                                    else -> palette.mutedForeground
                                },
                            )
                        }
                    } else {
                        MuiText(
                            text = item.label,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                on -> tint
                                else -> palette.mutedForeground
                            },
                        )
                    }
                }
            }
        }

        // 玻璃滑块（最顶层）：按住外鼓 + 换透明档；平时平灰档不投影
        val activeBox = segs.getOrNull(activeIndex)
        if (activeBox != null && activeBox.width > 0) {
            val segW = activeBox.width
            val segHpx = activeBox.height
            val marginPx = ceil(segHpx * LENS_MARGIN_RATIO)
            val marginDp = with(density) { marginPx.toDp() }
            val glassLeft = if (dragging) {
                (baseX + dragX).coerceIn(segs.first().left, segs.last().left)
            } else {
                glassAnim.value
            }
            val painters: List<VectorPainter> = items.map { item ->
                key(item.value) { androidx.compose.ui.graphics.vector.rememberVectorPainter(item.icon ?: emptyIcon()) }
            }
            Box(
                Modifier
                    .offset {
                        IntOffset(glassLeft.roundToInt() - marginPx.roundToInt(), activeBox.top.roundToInt() - marginPx.roundToInt())
                    }
                    .size(
                        with(density) { segW.toDp() } + marginDp * 2,
                        with(density) { segHpx.toDp() } + marginDp * 2,
                    )
            ) {
                GlassLens(
                    modifier = Modifier.matchParentSize(),
                    live = true,
                    motion = 3,
                    body = if (pressed) GlassBody.TRANSPARENT else GlassBody.GRAY,
                    pressed = pressed,
                    lensCompress = 0f,
                    shadow = if (pressed) GlassShadowLevel.SM else GlassShadowLevel.NONE,
                    shapes = listOf(
                        LensShape(
                            cx = marginPx / density.density + segW / density.density / 2f,
                            cy = marginPx / density.density + segHpx / density.density / 2f,
                            hw = segW / density.density / 2f,
                            hh = segHpx / density.density / 2f,
                            radius = segHpx / density.density / 2f,
                            swell = 1f,
                        )
                    ),
                ) {
                    // 背面 = 玻璃压着的那层：页面 + 底座（带投影与描边）+ 每段的字与图标
                    paintPageBackdrop(resolvedPage)
                    val rootW = size.width - marginPx * 2
                    val rootH = size.height - marginPx * 2
                    // 底座：实色胶囊 + 投影（白底座压白页面，靠影子给出边界）
                    drawIntoCanvas { canvas ->
                        val fw = androidx.compose.ui.graphics.Paint().asFrameworkPaint()
                        fw.color = palette.card.toArgb()
                        fw.setShadowLayer(
                            4f * density.density,
                            0f,
                            1f * density.density,
                            android.graphics.Color.argb((0.12f * 255).toInt(), 0, 0, 0),
                        )
                        canvas.nativeCanvas.drawRoundRect(
                            marginPx,
                            marginPx,
                            marginPx + rootW,
                            marginPx + rootH,
                            rootH / 2f,
                            rootH / 2f,
                            fw,
                        )
                        fw.clearShadowLayer()
                        // 1dp 描边（玻璃盖住的那段边框由这里补）
                        fw.style = android.graphics.Paint.Style.STROKE
                        fw.strokeWidth = 1f * density.density
                        fw.color = palette.border.toArgb()
                        canvas.nativeCanvas.drawRoundRect(
                            marginPx + 0.5f * density.density,
                            marginPx + 0.5f * density.density,
                            marginPx + rootW - 0.5f * density.density,
                            marginPx + rootH - 0.5f * density.density,
                            rootH / 2f - 0.5f * density.density,
                            rootH / 2f - 0.5f * density.density,
                            fw,
                        )
                        fw.style = android.graphics.Paint.Style.FILL
                    }
                    // 每段的字与图标重绘（玻璃盖住的区域能透出折射后的内容）
                    segs.forEachIndexed { i, seg ->
                        val item = items.getOrNull(i) ?: return@forEachIndexed
                        val on = item.value == current
                        val color = tint
                        val cx = marginPx + seg.center.x
                        if (item.icon != null) {
                            val painter = painters.getOrNull(i) ?: return@forEachIndexed
                            val iconSize = 16.dp.toPx()
                            // 竖排：图标在上、文字在下
                            drawIntoCanvas { canvas ->
                                canvas.save()
                                canvas.translate(cx - iconSize / 2f, marginPx + seg.top + seg.height / 2f - iconSize / 2f - 10.dp.toPx())
                                with(painter) {
                                    draw(
                                        Size(iconSize, iconSize),
                                        colorFilter = ColorFilter.tint(color),
                                    )
                                }
                                canvas.restore()
                            }
                            val labelStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color)
                            val lr = textMeasurer.measure(item.label, labelStyle)
                            drawText(
                                lr,
                                topLeft = Offset(cx - lr.size.width / 2f, marginPx + seg.center.y + 10.dp.toPx() - lr.size.height / 2f),
                            )
                        } else {
                            val labelStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = color)
                            val lr = textMeasurer.measure(item.label, labelStyle)
                            drawText(
                                lr,
                                topLeft = Offset(cx - lr.size.width / 2f, marginPx + seg.center.y - lr.size.height / 2f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 多选组：原按钮组（default/outline；spacing 0 时首尾圆角相连） */
@Composable
private fun MultipleToggleGroup(
    items: List<SegmentedItem>,
    modifier: Modifier,
    values: Set<String>,
    defaultValues: Set<String>,
    onValuesChange: ((Set<String>) -> Unit)?,
    variant: ToggleVariant,
    spacing: Int,
    disabled: Boolean,
) {
    val palette = LocalMuiPalette.current
    var inner by remember { mutableStateOf(defaultValues) }
    val current = if (values.isEmpty() && onValuesChange == null) inner else values

    fun toggle(v: String) {
        val next = if (v in current) current - v else current + v
        if (onValuesChange == null) inner = next else onValuesChange(next)
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { i, item ->
            val on = item.value in current
            val first = i == 0
            val last = i == items.lastIndex
            val connected = spacing == 0
            val shape = when {
                !connected -> RoundedCornerShape(MuiRadius.md)
                first -> RoundedCornerShape(topStart = MuiRadius.md, bottomStart = MuiRadius.md)
                last -> RoundedCornerShape(topEnd = MuiRadius.md, bottomEnd = MuiRadius.md)
                else -> RoundedCornerShape(0.dp)
            }
            val bg = when {
                on -> palette.accent
                variant == ToggleVariant.Outline -> Color.Transparent
                else -> Color.Transparent
            }
            val fg = if (on) palette.accentForeground else palette.foreground
            val interaction = rememberMuiInteractionSource()
            Box(
                Modifier
                    .alpha(if (disabled) 0.5f else 1f)
                    .clip(shape)
                    .background(bg, shape)
                    .then(
                        if (variant == ToggleVariant.Outline) {
                            Modifier.border(
                                1.dp,
                                if (on) palette.accent else palette.input,
                                shape,
                            )
                        } else Modifier
                    )
                    .then(
                        if (variant == ToggleVariant.Outline && connected && !first) {
                            Modifier.offset(x = (-1).dp)
                        } else Modifier
                    )
                    .muiFocusRing(interaction, shape)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = !disabled,
                    ) { toggle(item.value) }
                    .padding(horizontal = 12.dp)
                    .height(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.icon?.let {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(it),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            colorFilter = ColorFilter.tint(fg),
                        )
                        Box(Modifier.size(4.dp))
                    }
                    MuiText(text = item.label, color = fg)
                }
            }
            if (!connected && spacing > 0) {
                Box(Modifier.size(spacing.dp))
            }
        }
    }
}

/** 多选组的变体（default = 无框 / outline = 描边） */
enum class ToggleVariant { Default, Outline }

/** 空图标占位（无 icon 的段在玻璃纹理里跳过绘制） */
private fun emptyIcon(): ImageVector = EMPTY_ICON
private val EMPTY_ICON: ImageVector by lazy {
    androidx.compose.ui.graphics.vector.ImageVector.Builder(
        name = "Empty",
        defaultWidth = 1.dp,
        defaultHeight = 1.dp,
        viewportWidth = 1f,
        viewportHeight = 1f,
    ).build()
}
