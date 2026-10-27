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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.glass.GlassBody
import com.mui.glass.GlassLens
import com.mui.glass.glassCanvas
import com.mui.glass.GlassShadowLevel
import com.mui.glass.LensShape
import com.mui.glass.paintPageBackdrop
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiPageColor
import com.mui.theme.LocalMuiPalette
import com.mui.theme.muiElevation
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource
import com.mui.token.MuiRadius
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

/** 段：纯文字（web `h-8 px-4 text-sm`） */
private val SEG_TEXT_H = 32.dp
/** 段：图标 + 文字（web `py-2 + gap-1 + size-4 + text-[11px] leading-none` = 8+16+4+11+8） */
private val SEG_STACK_PAD_V = 8.dp
private val SEG_STACK_GAP = 4.dp
private val SEG_ICON = 16.dp
private val SEG_PAD_H = 16.dp
private val SEG_STACK_H = SEG_STACK_PAD_V * 2 + SEG_ICON + SEG_STACK_GAP + 11.dp

/** 一个段的实测几何（相对根容器 border box）：段盒 / 图标盒 / 文字盒 */
private data class SegGeom(
    val box: Rect = Rect.Zero,
    val icon: Rect? = null,
    val label: Rect? = null,
)

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

    /** 每个段相对 Root 的实测几何（段盒 / 图标盒 / 文字盒，px） */
    var geodes by remember { mutableStateOf<List<SegGeom>>(emptyList()) }
    /** 段盒子视图：玻璃摆位、命中判定、吸附全按它算 */
    val segs: List<Rect> = geodes.map { it.box }

    fun updateGeom(i: Int, transform: (SegGeom) -> SegGeom) {
        geodes = geodes.toMutableList().also { list ->
            while (list.size <= i) list.add(SegGeom())
            list[i] = transform(list[i])
        }
    }

    fun commit(next: String) {
        if (value == null) inner = next
        onValueChange?.invoke(next)
    }

    // 段盒子位置：逐段 onGloballyPositioned 量（相对 root）
    val rootOrigin = remember { mutableStateOf(Offset.Zero) }
    val rootSize = remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    // 内容原点（根容器的 padding）：玻璃画布的 offset 是相对内容原点给的
    val contentPadPx = 2.dp.value * density.density

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

    val baseShape = RoundedCornerShape(50)
    val resolvedPage = if (pageColor != Color.Unspecified) pageColor else palette.background
    val tint = activeColor ?: palette.foreground

    Box(
        modifier = modifier
            .onGloballyPositioned {
                rootOrigin.value = it.boundsInRoot().topLeft
                rootSize.value = it.size
            }
            .alpha(if (disabled) 0.5f else 1f)
            // 不裁切：玻璃画布要外扩到根容器外（web 无 overflow:hidden），裁了会把
            // 外扩区与按住的影子切掉。底座本身用带 shape 的 background/border 画圆角。
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
        contentAlignment = Alignment.TopStart,
    ) {
        // 段行
        Row(verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { i, item ->
                val on = item.value == current
                val segInteraction = rememberMuiInteractionSource()
                // 段内三块几何各自实测（相对根容器）：玻璃背纹理按它们重绘图标与文字，
                // 不再用「中心 ± 10dp」这类推算 —— 那套偏移一旦字号或行高变了就错位。
                fun rel(coords: androidx.compose.ui.layout.LayoutCoordinates): Rect =
                    coords.boundsInRoot().translate(-rootOrigin.value)

                Box(
                    Modifier
                        .onGloballyPositioned { coords -> updateGeom(i) { it.copy(box = rel(coords)) } }
                        .clip(CircleShape)
                        .height(if (item.icon != null) SEG_STACK_H else SEG_TEXT_H)
                        .clickable(
                            interactionSource = segInteraction,
                            indication = null,
                            enabled = !disabled,
                        ) {
                            // 单选组不允许取消选中：点已选中的那栏什么都不做
                            if (!on) commitAt(i)
                        }
                        .muiFocusRing(segInteraction, CircleShape)
                        .padding(horizontal = SEG_PAD_H),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item.icon != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(SEG_STACK_GAP),
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(item.icon),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(SEG_ICON)
                                    .onGloballyPositioned { coords -> updateGeom(i) { it.copy(icon = rel(coords)) } },
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
                                modifier = Modifier
                                    .onGloballyPositioned { coords -> updateGeom(i) { it.copy(label = rel(coords)) } },
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
                            modifier = Modifier
                                .onGloballyPositioned { coords -> updateGeom(i) { it.copy(label = rel(coords)) } },
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
            // 画布原点（相对根容器，浮点）：球心与背面全部按「取整后的原点」反推，
            // dp→px 的余数补回浮点 uniform —— 否则玻璃内容与真实底座/文字差出亚像素。
            val d = density.density
            val glassLeftPx = glassLeft - marginPx
            val glassTopPx = activeBox.top - marginPx
            val originX = glassLeftPx.roundToInt()
            val originY = glassTopPx.roundToInt()
            // canvas 直接挂在 GlassLens 上（不再套一层 Box）：
            // glassCanvas 对外报 0×0，套一层会让里层 matchParentSize 拿到 0×0 的父尺寸，
            // GlassLens 的图形层随之变 0，着色器的 uRes 全错（实测玻璃糊成一团大圆）。
            GlassLens(
                modifier = Modifier
                    .offset {
                        // offset 相对内容原点（padding 2dp），所以减回去
                        IntOffset(
                            (originX - contentPadPx).roundToInt(),
                            (originY - contentPadPx).roundToInt(),
                        )
                    }
                    .glassCanvas(
                        with(density) { segW.toDp() } + marginDp * 2,
                        with(density) { segHpx.toDp() } + marginDp * 2,
                    ),
                live = true,
                motion = 3,
                body = if (pressed) GlassBody.TRANSPARENT else GlassBody.GRAY,
                pressed = pressed,
                lensCompress = 0f,
                shadow = if (pressed) GlassShadowLevel.SM else GlassShadowLevel.NONE,
                shapes = listOf(
                    LensShape(
                        cx = (glassLeft + segW / 2f - originX) / d,
                        cy = (activeBox.top + segHpx / 2f - originY) / d,
                        hw = segW / density.density / 2f,
                        hh = segHpx / density.density / 2f,
                        radius = segHpx / density.density / 2f,
                        swell = 1f,
                    )
                ),
            ) {
                    // 背面 = 玻璃压着的那层：页面 + 底座（带投影与描边）+ 每段的字与图标。
                    // 一律按「画布原点 = 根容器里的 (originX, originY)」反推位置 ——
                    // 拖动时底座/文字相对画布在滑动（web 同款，见 toggle-group.tsx 的 dx/dy）。
                    paintPageBackdrop(resolvedPage)
                    val rootW = rootSize.value.width.toFloat()
                    val rootH = rootSize.value.height.toFloat()
                    val baseL = -originX.toFloat()
                    val baseT = -originY.toFloat()
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
                            baseL,
                            baseT,
                            baseL + rootW,
                            baseT + rootH,
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
                            baseL + 0.5f * density.density,
                            baseT + 0.5f * density.density,
                            baseL + rootW - 0.5f * density.density,
                            baseT + rootH - 0.5f * density.density,
                            rootH / 2f - 0.5f * density.density,
                            rootH / 2f - 0.5f * density.density,
                            fw,
                        )
                        fw.style = android.graphics.Paint.Style.FILL
                    }
                    // 每段的字与图标重绘（玻璃盖住的区域能透出折射后的内容）。
                    // 位置全部取实测盒（web 侧同样读 getBoundingClientRect）；
                    // 拿不到实测值就跳过 —— 宁可这一帧不重绘，也不要用推算值错位。
                    geodes.forEachIndexed { i, g ->
                        val item = items.getOrNull(i) ?: return@forEachIndexed
                        g.icon?.let { iconBox ->
                            val painter = painters.getOrNull(i) ?: return@let
                            drawIntoCanvas { canvas ->
                                canvas.save()
                                canvas.translate(iconBox.left - originX, iconBox.top - originY)
                                with(painter) {
                                    draw(
                                        Size(iconBox.width, iconBox.height),
                                        colorFilter = ColorFilter.tint(tint),
                                    )
                                }
                                canvas.restore()
                            }
                        }
                        g.label?.let { labelBox ->
                            val style = TextStyle(
                                fontSize = if (item.icon != null) 11.sp else 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = tint,
                            )
                            val lr = textMeasurer.measure(item.label, style)
                            drawText(
                                lr,
                                topLeft = Offset(
                                    labelBox.center.x - originX - lr.size.width / 2f,
                                    labelBox.center.y - originY - lr.size.height / 2f,
                                ),
                            )
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
