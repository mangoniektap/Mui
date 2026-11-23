package com.mui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.glass.EaseOutQuad
import com.mui.glass.GlassBody
import com.mui.glass.GlassLens
import com.mui.glass.GlassShadowLevel
import com.mui.glass.GlassTokens
import com.mui.glass.LensShape
import com.mui.glass.glassCanvasPx
import com.mui.glass.glassPageColor
import com.mui.theme.LocalMuiPalette
import com.mui.theme.muiElevation
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource
import com.mui.token.MuiRadius
import com.mui.token.MuiTypography
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * 本文件是 web `src/components/ui/toggle-group.tsx` 的逐行移植。
 *
 * 移植原则：**结构、常量、状态机、几何推导全部一一对应**，注释里的 `web:` 就是对面那一行。
 * 不在这里"改良"手感 —— 手感由玻璃引擎与令牌决定，组件只负责把几何和状态喂准。
 * 任何与 web 不同的地方都必须写成 ⚠️ 并说明原因，否则就是移植偏差（历史 bug 全部出在这）。
 */

/** 单选组的一个栏目。web: `ToggleGroupSegmentedItem` */
data class SegmentedItem(
    /** 选中值（提交给 onValueChange 的那个） */
    val value: String,
    /** 栏目文字（同一组内请保持等长：等长才等宽，玻璃只滑位置不变宽） */
    val label: String,
    /** 图标（null = 纯文字段） */
    val icon: ImageVector? = null,
)

/** web: `type="single"` / `type="multiple"` */
enum class ToggleGroupType { Single, Multiple }

/**
 * 分段控件的一档尺寸。
 *
 * ⚠️ 这是 web 没有的档位（web 的分段控件只有一套几何：`h-8 px-4 text-sm`）。
 *    [Sm] 与 web 逐像素对齐，是默认值；[Lg] 只是给触屏加大，几何仍按同一套公式推，
 *    所以玻璃那一层不会错位。别在外面 `scale()` —— 命中区不跟着变。
 */
enum class ToggleGroupSize { Sm, Lg }

// ─────────────────────────── web: 常量区 ───────────────────────────

/** web: `const SEG_TEXT = "h-8 px-4 text-sm"` */
private val SEG_TEXT_H = 32.dp
private val SEG_TEXT_PAD_H = 16.dp
private val SEG_TEXT_SIZE = 14.sp
private val SEG_TEXT_LINE = 20.sp

/** web: `const SEG_STACK = "flex-col gap-1 px-4 py-2 text-[11px] leading-none"` */
private val SEG_STACK_GAP = 4.dp
private val SEG_STACK_PAD_V = 8.dp
private val SEG_STACK_SIZE = 11.sp
private val SEG_ICON = 16.dp
/** `leading-none` ⇒ 文字行盒高 = 字号本身 */
private val SEG_STACK_TEXT_H = 11.dp
/** 图标 + 文字段的自身高度 = py-2 ×2 + 图标 16 + 间距 4 + 文字 11 = 47 */
private val SEG_STACK_H = SEG_STACK_PAD_V * 2 + SEG_ICON + SEG_STACK_GAP + SEG_STACK_TEXT_H

/** web: `TopGroup.Root` 的 `p-0.5` */
private val ROOT_PAD = 3.dp

/** web: `const GLIDE = "translate 280ms cubic-bezier(0.32, 0.72, 0, 1)"` */
private const val GLIDE_MS = 280
private val GLIDE_EASING = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

/** web: `transition-colors duration-[280ms] ease-[cubic-bezier(0.32,0.72,0,1)]` */
private const val SEG_COLOR_MS = 280

/** web: `const LENS_MARGIN_RATIO = 0.6` —— 画布外扩 = 段高 × 这个比例 */
private const val LENS_MARGIN_RATIO = 0.6f

/** web: 玻璃的拖动阈值 `Math.abs(dx) > 3` */
private val DRAG_SLOP = 3.dp

/** 一档分段控件的几何（字号 / 行高 / 段高 / 左右内边距），布局与背面重绘共用同一份 */
private class SegMetrics(
    val text: TextUnit,
    val line: TextUnit,
    val height: Dp,
    val padH: Dp,
)

/** web 精确档 */
private val SEG_METRICS_SM = SegMetrics(SEG_TEXT_SIZE, SEG_TEXT_LINE, SEG_TEXT_H, SEG_TEXT_PAD_H)

/** 触屏档（web 没有，等比放大：段高 46、字号 16） */
private val SEG_METRICS_LG = SegMetrics(16.sp, 22.sp, 46.dp, 18.dp)

private fun metricsOf(size: ToggleGroupSize): SegMetrics = when (size) {
    ToggleGroupSize.Sm -> SEG_METRICS_SM
    ToggleGroupSize.Lg -> SEG_METRICS_LG
}

/** web: `type SegBox = { x, y, w, h, on }` */
private data class SegBox(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val on: Boolean,
)

/** 一个段的实测几何（相对根容器 border box）：段盒 / 图标盒 / 文字盒 */
private data class SegGeom(
    val box: Rect = Rect.Zero,
    val icon: Rect? = null,
    val label: Rect? = null,
)

/** web: `type Drag = { x0, baseX, w, dx, moved }` */
private data class DragState(
    val x0: Float,
    val baseX: Float,
    val w: Float,
    val dx: Float,
    val moved: Boolean,
)

/**
 * 开关组。
 *
 * - [ToggleGroupType.Single] → web 的 `type="single"`：白胶囊底座 + 玻璃滑块分段控件
 * - [ToggleGroupType.Multiple] → web 的 `type="multiple"`：一排 toggle 按钮
 *
 * @param items 栏目清单
 * @param value 单选受控值（Multiple 时忽略）
 * @param defaultSingle 单选初始值（非受控）
 * @param onValueChange 单选值回调（按下即锚定 + 松手吸附提交）
 * @param values 多选受控值集合
 * @param defaultValues 多选初始集合
 * @param onValuesChange 多选值回调
 * @param variant 按钮组变体（default / outline，仅 Multiple 生效）
 * @param spacing 按钮间距（0 = 连成一体的按钮组外观）
 * @param activeColor 被玻璃盖住的文字 / 图标染成的颜色。
 *   **null = 主题色 `primary`**（组件自己兜底，引用处不用再传；web 是在示例里传 `var(--primary)`，
 *   这里把它下沉成组件默认值）。传了就按传的覆盖。
 * @param size 高度档位（Sm = web 精确档）
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
    size: ToggleGroupSize = ToggleGroupSize.Sm,
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
            size = size,
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

// ─────────────────────────── 单选组（web 的分段控件） ───────────────────────────

@Composable
private fun SegmentedToggleGroup(
    items: List<SegmentedItem>,
    modifier: Modifier,
    value: String?,
    defaultSingle: String?,
    onValueChange: ((String) -> Unit)?,
    activeColor: Color?,
    size: ToggleGroupSize,
    disabled: Boolean,
) {
    val palette = LocalMuiPalette.current
    val pageColor = glassPageColor()
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val metrics = metricsOf(size)
    /** web: `--seg-active-color` —— 组件默认取主题色 primary；调用方传了 activeColor 才覆盖 */
    val segActiveColor = activeColor ?: palette.primary

    // ── 受控 / 非受控（web 交给 Radix）
    var inner by remember { mutableStateOf(defaultSingle ?: items.firstOrNull()?.value) }
    val current = value ?: inner

    fun commit(next: String) {
        if (value == null) inner = next
        onValueChange?.invoke(next)
    }

    // ── web: useState 区 ────────────────────────────────────────────
    /** web: `const [segs, setSegs] = useState<SegBox[]>([])` —— 每个段的盒子 */
    var geom by remember { mutableStateOf<List<SegGeom>>(emptyList()) }
    /** web: `const [drag, setDrag] = useState<Drag|null>(null)` */
    var drag by remember { mutableStateOf<DragState?>(null) }
    /** web: `const [pending, setPending] = useState<number|null>(null)` —— 松手吸附到哪一段 */
    var pending by remember { mutableStateOf<Int?>(null) }
    var rootOrigin by remember { mutableStateOf<Offset?>(null) }
    val origin = rootOrigin
    var rootSize by remember { mutableStateOf(IntSize.Zero) }

    /**
     * `on` 从哪来：web 读 Radix 写的 `data-state`；这里读我们自己的选中值。
     * 两者都是「逻辑选中」，只用来决定玻璃该停在哪一段，**不用来染色**（见 coveredIndex）。
     */
    val active = items.indexOfFirst { it.value == current }
    // 各节点先记录同一坐标系的实测位置，等到绘制时才换算到控件原点。
    val segs = if (origin == null) emptyList() else geom.take(items.size).mapIndexed { i, segment ->
        SegBox(segment.box.left - origin.x, segment.box.top - origin.y, segment.box.width, segment.box.height, i == active)
    }

    /** web: `const count = items ? items.length : ...` —— 条数变了要重量 */
    val count = items.size

    // ── web: 量几何（read + MutationObserver + ResizeObserver）──────────
    // web 用 offsetLeft/offsetTop/offsetWidth/offsetHeight（相对 Root 的 padding box）。
    // 这里用 boundsInRoot() − 根容器原点，得到的是**根容器 border box 坐标系**；
    // 玻璃摆放时再减去 padding 换到 content 原点（见下面的 offset），二者同源。
    fun put(i: Int, transform: (SegGeom) -> SegGeom) {
        val next = geom.toMutableList()
        while (next.size <= i) next.add(SegGeom())
        next[i] = transform(next[i])
        // ⚠️ 不做"没变就跳过"的手写短路：SegGeom 是 data class，List 相等性与元素逐个相等，
        // 写回相等值时 Compose 自己不会通知 —— 短路在这里是多余的，而且会固化脏几何。
        geom = next
    }

    fun bounds(coords: androidx.compose.ui.layout.LayoutCoordinates) =
        Rect(coords.localToRoot(Offset.Zero), Size(coords.size.width.toFloat(), coords.size.height.toFloat()))

    // web: `setPending((p) => (p !== null && next[p]?.on ? null : p))`
    // —— 松手提交生效（落点那段变 on）后收掉 pending，之后位置全交给选中值。
    SideEffect {
        val p = pending
        if (p != null && p == active) pending = null
    }

    // ── web: 派生量（每次 render 重算）──────────────────────────────
    val trackMin = segs.firstOrNull()?.x ?: 0f
    val trackMax = segs.lastOrNull()?.x ?: trackMin
    fun clampX(x: Float) = x.coerceIn(trackMin, trackMax)

    /** web: `const box = segs[pending ?? active]` */
    val box = segs.getOrNull(pending ?: active)

    /** 玻璃的渲染位置：拖动中跟手，其余时刻读滑行动画的实时值（≈ CSS transition 的计算值） */
    var glassX by remember { mutableFloatStateOf(0f) }
    var placed by remember { mutableStateOf(false) }
    val dragLeft = drag?.takeIf { it.moved }?.let { clampX(it.baseX + it.dx) }
    val glassLeft = dragLeft ?: if (placed) glassX else box?.x ?: 0f

    // 依据当前帧的玻璃边界着色。滑到一半时，由透镜里的 ink 负责绘制被覆盖的部分。
    val tolerance = density.density
    val coveredIndex = if (box == null) -1 else segs.indexOfFirst {
        abs(it.x - glassLeft) < tolerance && abs(it.y - box.y) < tolerance &&
            abs(it.w - box.w) < tolerance && abs(it.h - box.h) < tolerance
    }

    val pressProgress = remember { Animatable(0f) }
    var pressSequence by remember { mutableIntStateOf(0) }
    var pressed by remember { mutableStateOf(false) }
    LaunchedEffect(pressSequence) {
        if (pressSequence == 0) return@LaunchedEffect
        val animation = tween<Float>(GlassTokens.pressDurationMs.roundToInt(), easing = EaseOutQuad)
        pressProgress.animateTo(1f, animation)
        // 快速点击也完整播放一次外鼓，按住不放时停在峰值。
        snapshotFlow { drag == null }.first { it }
        pressed = false
        pressProgress.animateTo(0f, animation)
    }

    /** web: `const lensMargin = box ? Math.ceil(box.h * LENS_MARGIN_RATIO) : 0` */
    val lensMarginPx = box?.let { ceil(it.h * LENS_MARGIN_RATIO).toInt() } ?: 0

    // ── web: 玻璃的滑行（CSS transition 的等价物）────────────────────
    // web 只对 translate 加 280ms 过渡；换段（含按下即锚定）、松手吸附都是同一条。
    // 首帧必须 snap：元素第一次出现就在终点，不会从 0 滑过来。
    LaunchedEffect(box?.x, drag?.moved) {
        if (drag?.moved == true) return@LaunchedEffect
        val target = box?.x ?: return@LaunchedEffect
        if (!placed) {
            placed = true
            glassX = target
            return@LaunchedEffect
        }
        val from = glassX
        if (abs(target - from) < 0.5f) {
            glassX = target
            return@LaunchedEffect
        }
        val t0 = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val p = ((now - t0) / 1_000_000f / GLIDE_MS).coerceIn(0f, 1f)
            glassX = from + (target - from) * GLIDE_EASING.transform(p)
            if (p >= 1f) break
        }
        glassX = target
    }

    /** web: `const swallow = React.useRef(false)` —— 手势处理过的这一发 click 要拦下来，别再提交一次 */
    var swallowClick by remember { mutableStateOf(false) }

    // ── web: 手势（handlePointerDown / Move / Up）───────────────────
    // 手势协程长命：块里捕获的是启动那次组合的快照，所以值都走 rememberUpdatedState。
    val latestItems by rememberUpdatedState(items)
    val latestSegs by rememberUpdatedState(segs)
    val latestCurrent by rememberUpdatedState(current)
    val latestCommit by rememberUpdatedState<(String) -> Unit> { v -> commit(v) }
    val baseShape = RoundedCornerShape(50)

    Box(
        modifier = modifier
            .onGloballyPositioned {
                rootOrigin = it.localToRoot(Offset.Zero)
                rootSize = it.size
            }
            .alpha(if (disabled) 0.5f else 1f)
            // web: `relative inline-flex w-fit touch-none items-center rounded-full
            //       border border-input bg-card p-0.5 shadow-sm select-none`
            // 不裁切：玻璃画布要外扩到根容器之外（web 无 overflow:hidden）。
            .muiElevation("sm", baseShape)
            .drawBehind {
                drawSegmentedTrack(Offset.Zero, this.size, palette.card, palette.input)
            }
            .pointerInput(items.size, count, disabled, geom.size, size) {
                if (disabled || geom.isEmpty()) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val liveSegs = latestSegs
                    /** web 的 clampX：夹在首段 / 末段的左边缘之间 */
                    fun clampLive(x: Float): Float {
                        val a = liveSegs.firstOrNull()?.x ?: 0f
                        val b = liveSegs.lastOrNull()?.x ?: a
                        return x.coerceIn(a, b)
                    }
                    val px = down.position.x
                    val hit = liveSegs.indexOfFirst { px >= it.x && px <= it.x + it.w }
                    if (hit < 0) return@awaitEachGesture
                    val seg = liveSegs[hit]
                    // web: 按下即锚定 —— 按到的那段不是当前选中段就立刻切过去，
                    //      玻璃当场滑到指针下，不等松手。
                    swallowClick = true
                    if (latestItems.getOrNull(hit)?.value != latestCurrent) {
                        latestItems.getOrNull(hit)?.let { latestCommit(it.value) }
                    }
                    // web: 拖动基准就用"按下的那一段"，与刚提交的选中段同源。
                    var d = DragState(x0 = down.position.x, baseX = seg.x, w = seg.w, dx = 0f, moved = false)
                    drag = d
                    pressed = true
                    pressSequence++
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val dx = change.position.x - down.position.x
                            val moved = d.moved || abs(dx) > DRAG_SLOP.toPx()
                            if (moved) {
                                // web: 指针捕获只在真拖起来之后才做（否则 click 派发不到段上）
                                change.consume()
                            }
                            if (d.dx != dx || d.moved != moved) {
                                d = d.copy(dx = dx, moved = moved)
                                drag = d
                            }
                            if (!change.pressed) break
                        }
                    } finally {
                        // web: handlePointerUp —— 没真拖起来就当普通点击（选中值已经切过了）；
                        //      真拖起来了就按"玻璃中心落在哪一段"吸附提交。
                        drag = null
                        if (d.moved) {
                            val endX = clampLive(d.baseX + d.dx)
                            // 先把渲染位置落到松手点，动画再从这儿起步（= CSS 从当前值过渡）
                            glassX = endX
                            val center = endX + d.w / 2f
                            var to = 0
                            var best = Float.MAX_VALUE
                            liveSegs.forEachIndexed { i, s ->
                                val dist = abs(s.x + s.w / 2f - center)
                                if (dist < best) {
                                    best = dist
                                    to = i
                                }
                            }
                            if (liveSegs.getOrNull(to)?.on != true) {
                                // web: setPending(to) 后再 click —— pending 压在 data-state 更新之前，
                                //      避免玻璃先弹回旧段
                                pending = to
                                latestItems.getOrNull(to)?.let { latestCommit(it.value) }
                            }
                        }
                    }
                }
            }
            .padding(ROOT_PAD),
        contentAlignment = Alignment.TopStart,
    ) {
        // ── 段行：web 的 items.map ────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { i, item ->
                val selected = item.value == current
                /** web: `data-[covered=true]:text-[color:var(--seg-active-color)]` */
                val on = i == coveredIndex
                val segColor: Color by animateColorAsState(
                    targetValue = if (on) segActiveColor else palette.mutedForeground,
                    animationSpec = tween<Color>(SEG_COLOR_MS, easing = GLIDE_EASING),
                )
                val segInteraction = rememberMuiInteractionSource()
                // 文字样式只写一处：真实布局与玻璃背面重绘（ink）共用它，
                // 否则重绘的字和底下的字宽度不同，叠起来就是"毛边 / 半截变色"。
                val labelStyle = LocalMuiTextStyle.current.merge(
                    TextStyle(
                        color = segColor,
                        fontSize = if (item.icon != null) SEG_STACK_SIZE else metrics.text,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MuiTypography.sans,
                        lineHeight = if (item.icon != null) SEG_STACK_SIZE else metrics.line,
                    )
                )
                Box(
                    Modifier
                        .onGloballyPositioned { c -> put(i) { g -> g.copy(box = bounds(c)) } }
                        .muiFocusRing(segInteraction, CircleShape)
                        .clip(CircleShape)
                        // web: SEG_TEXT / SEG_STACK 决定段盒尺寸（图标段自身内容高可能超过档位高）
                        .height(if (item.icon != null) maxOf(SEG_STACK_H, metrics.height) else metrics.height)
                        .clickable(
                            interactionSource = segInteraction,
                            indication = null,
                            enabled = !disabled,
                        ) {
                            // web: handleClickCapture —— 手势已经提交过的那一发 click 直接吞掉
                            //      （否则一次按下会提交两次）；单选组也不允许取消选中。
                            if (swallowClick) {
                                swallowClick = false
                            } else if (!selected) {
                                commit(item.value)
                            }
                        }
                        .padding(horizontal = metrics.padH),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item.icon != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(SEG_STACK_GAP),
                        ) {
                            androidx.compose.foundation.Image(
                                painter = rememberVectorPainter(item.icon),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(SEG_ICON)
                                    .onGloballyPositioned { c -> put(i) { it.copy(icon = bounds(c)) } },
                                colorFilter = ColorFilter.tint(segColor),
                            )
                            SegLabel(
                                text = item.label,
                                style = labelStyle,
                                modifier = Modifier.onGloballyPositioned { c -> put(i) { it.copy(label = bounds(c)) } },
                            )
                        }
                    } else {
                        SegLabel(
                            text = item.label,
                            style = labelStyle,
                            modifier = Modifier.onGloballyPositioned { c -> put(i) { it.copy(label = bounds(c)) } },
                        )
                    }
                }
            }
        }

        // ── 玻璃滑块（web: z-20 的 span + GlassLens）─────────────────
        val placedBox = box
        if (origin != null && placedBox != null && placedBox.w > 0f && placedBox.h > 0f) {
            val d = density.density
            val wPx = placedBox.w
            val hPx = placedBox.h
            val marginPx = lensMarginPx
            // 画布原点（相对根容器 border box）：玻璃盒左上 − 外扩量，取整到像素。
            // 球心 / 背面全部按"浮点值 − 这个整数原点"反推，取整丢掉的小数因此不会累积。
            val originX = (glassLeft - marginPx).roundToInt()
            val originY = (placedBox.y - marginPx).roundToInt()
            // 用实测画布原点减去实际内边距，得到相对内容区的偏移。
            val padPx = with(density) { ROOT_PAD.roundToPx() }
            val painters: List<VectorPainter?> = items.map { item ->
                key(item.value) { item.icon?.let { rememberVectorPainter(it) } }
            }
            // 内容层（ink）用与真实排版同一个样式，只换颜色（web: 一律用激活色重绘）
            val inkStyles = items.map { item ->
                LocalMuiTextStyle.current.merge(
                    TextStyle(
                        color = segActiveColor,
                        fontSize = if (item.icon != null) SEG_STACK_SIZE else metrics.text,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MuiTypography.sans,
                        lineHeight = if (item.icon != null) SEG_STACK_SIZE else metrics.line,
                    )
                )
            }

            /** web: `shapes={[{ cx: m + box.w/2, cy: m + box.h/2, hw, hh, radius: box.h/2, swell: 1 }]}` */
            val shapes = listOf(
                LensShape(
                    cx = (glassLeft - originX + wPx / 2f) / d,
                    cy = (placedBox.y - originY + hPx / 2f) / d,
                    hw = wPx / d / 2f,
                    hh = hPx / d / 2f,
                    radius = hPx / d / 2f,
                    swell = 1f,
                )
            )

            GlassLens(
                pageColor = pageColor,
                modifier = Modifier
                    .offset { IntOffset(originX - padPx, originY - padPx) }
                    .glassCanvasPx(
                        widthPx = (wPx + marginPx * 2).roundToInt(),
                        heightPx = (hPx + marginPx * 2).roundToInt(),
                        offsetXPx = 0,
                        offsetYPx = 0,
                    ),
                live = true,
                motion = 3,
                // web: `body={pressed ? "transparent" : "gray"}` `shadow={pressed ? "sm" : "none"}`
                body = if (pressed) GlassBody.TRANSPARENT else GlassBody.GRAY,
                lens = pressProgress.value,
                // web: `[--glass-lens-compress:0.10] [--glass-lens-decay:0.7]`
                lensCompress = 0.10f,
                lensDecay = 0.7f,
                shadow = if (pressed) GlassShadowLevel.SM else GlassShadowLevel.NONE,
                shapes = shapes,
                backdrop = {
                    drawSegmentedTrack(
                        Offset(-originX.toFloat(), -originY.toFloat()),
                        Size(rootSize.width.toFloat(), rootSize.height.toFloat()),
                        palette.card,
                        palette.input,
                    )
                },
                // ── web: paintInk（内容层：字 / 图标，一律用激活色重绘）────
                ink = {
                    geom.forEachIndexed { i, g ->
                        val item = items.getOrNull(i) ?: return@forEachIndexed
                        g.icon?.let { iconBox ->
                            val painter = painters.getOrNull(i) ?: return@let
                            drawIntoCanvas { canvas ->
                                canvas.save()
                                canvas.translate(iconBox.left - origin.x - originX, iconBox.top - origin.y - originY)
                                with(painter) {
                                    draw(
                                        Size(iconBox.width, iconBox.height),
                                        colorFilter = ColorFilter.tint(segActiveColor),
                                    )
                                }
                                canvas.restore()
                            }
                        }
                        g.label?.let { labelBox ->
                            val lr = textMeasurer.measure(item.label, inkStyles[i])
                            drawText(
                                lr,
                                topLeft = Offset(
                                    labelBox.center.x - origin.x - originX - lr.size.width / 2f,
                                    labelBox.center.y - origin.y - originY - lr.size.height / 2f,
                                ),
                            )
                        }
                    }
                },
            )
        }
    }
}

/**
 * 绘制分段底座和描边。正常绘制与玻璃采样共用边界、颜色和抗锯齿。
 */
private fun DrawScope.drawSegmentedTrack(origin: Offset, bounds: Size, fill: Color, border: Color) {
    val stroke = 1.dp.toPx()
    val radius = minOf(bounds.width, bounds.height) / 2f
    if (radius <= stroke / 2f) return
    drawRoundRect(fill, origin, bounds, CornerRadius(radius))
    drawRoundRect(
        color = border,
        topLeft = origin + Offset(stroke / 2f, stroke / 2f),
        size = Size(bounds.width - stroke, bounds.height - stroke),
        cornerRadius = CornerRadius(radius - stroke / 2f),
        style = Stroke(stroke),
    )
}

/** 段文字：与 ink 层共用同一个 TextStyle（连 LocalMuiTextStyle 继承都一致） */
@Composable
private fun SegLabel(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Clip,
    )
}

// ─────────────────────────── 多选组（web 的按钮组） ───────────────────────────

/**
 * web: `toggleVariants`
 * ```
 * inline-flex items-center justify-center gap-2 rounded-md text-sm font-medium
 * hover:bg-muted hover:text-muted-foreground
 * data-[state=on]:bg-accent data-[state=on]:text-accent-foreground
 * variant default: bg-transparent
 * variant outline: border border-input bg-transparent shadow-xs hover:bg-accent hover:text-accent-foreground
 * size default: h-9 min-w-9 px-2 / sm: h-8 min-w-8 px-1.5 / lg: h-10 min-w-10 px-2.5
 * ```
 */
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
    val current = if (onValuesChange == null) inner else values

    fun toggle(v: String) {
        val next = if (v in current) current - v else current + v
        if (onValuesChange == null) inner = next else onValuesChange(next)
    }

    val connected = spacing == 0
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { i, item ->
            val on = item.value in current
            val first = i == 0
            val last = i == items.lastIndex
            // web: spacing=0 → rounded-none + first rounded-l-md + last rounded-r-md
            val shape = when {
                !connected -> RoundedCornerShape(MuiRadius.md)
                first && last -> RoundedCornerShape(MuiRadius.md)
                first -> RoundedCornerShape(topStart = MuiRadius.md, bottomStart = MuiRadius.md)
                last -> RoundedCornerShape(topEnd = MuiRadius.md, bottomEnd = MuiRadius.md)
                else -> RoundedCornerShape(0.dp)
            }
            val interaction = rememberMuiInteractionSource()
            val bg = if (on) palette.accent else Color.Transparent
            val fg = if (on) palette.accentForeground else palette.foreground
            Box(
                Modifier
                    .alpha(if (disabled) 0.5f else 1f)
                    .clip(shape)
                    .background(bg, shape)
                    .then(
                        if (variant == ToggleVariant.Outline) {
                            Modifier.border(1.dp, palette.input, shape)
                        } else Modifier
                    )
                    .muiFocusRing(interaction, shape)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = !disabled,
                    ) { toggle(item.value) }
                    .padding(horizontal = 8.dp)
                    .height(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item.icon?.let {
                        androidx.compose.foundation.Image(
                            painter = rememberVectorPainter(it),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            colorFilter = ColorFilter.tint(fg),
                        )
                    }
                    MuiText(
                        text = item.label,
                        color = fg,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
            if (!connected && spacing > 0) Box(Modifier.size(spacing.dp))
        }
    }
}
