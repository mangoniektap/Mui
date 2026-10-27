package com.mui.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.icon.MuiIcons
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.theme.muiElevation
import com.mui.token.MuiRadius
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// TODO: web 的 Previous/Next 覆盖 px-2.5（按钮默认 px-4）未移植，沿用 MuiButton 默认内边距
// TODO: web 的 sm: 断点文字显隐（小屏隐藏 Previous/Next 文字）未移植

/**
 * 分页。两种形态：
 *
 * **① 转盘模式（默认）**：三参数（currentPage / pageCount / onPageChange）给齐且不传 [content]。
 * 组件自己画页码：**同一时刻最多 [visiblePages] 个**，中间是当前页，左右按距离缩小、变淡；
 * 按住左右拖时**只有页码在动**（外框、两侧按钮都不动），越过 [dragStep] 就换一页并立即回调，
 * 松手按甩动速度补 0~3 页并吸附对齐。
 *
 * **② 自定义内容模式**：传了 [content] 就按调用方排的件渲染（页码钮 / 上页 / 下页 / 省略号），
 * 拖动时整排 1:1 跟手（补足不足一格的零头），松手吸附回位。
 *
 * @param modifier 布局修饰
 * @param currentPage 当前页序号（0 起）
 * @param pageCount 总页数
 * @param onPageChange 换页回调（拖动跨格、甩动、点页码都会走这里）
 * @param dragStep 拖动多远算翻一页（默认 40dp）
 * @param visiblePages 转盘模式同时显示的页码个数（默认 5）
 * @param content 自定义分页内容（null = 走转盘模式）
 */
@Composable
fun MuiPagination(
    modifier: Modifier = Modifier,
    currentPage: Int? = null,
    pageCount: Int? = null,
    onPageChange: ((Int) -> Unit)? = null,
    dragStep: Dp = 40.dp,
    visiblePages: Int = PAGER_VISIBLE,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    val page = currentPage
    val count = pageCount
    val change = onPageChange
    val slot: @Composable RowScope.() -> Unit = content ?: {}

    // ① 转盘模式：页码由组件自己画，只有页码动
    if (content == null && page != null && count != null && change != null && count > 1) {
        PickerPager(
            modifier = modifier,
            page = page,
            count = count,
            change = change,
            dragStep = dragStep,
            visible = visiblePages,
        )
        return
    }

    if (page == null || count == null || change == null || count <= 1) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = slot,
        )
        return
    }

    val stepPx = with(LocalDensity.current) { dragStep.toPx() }
    // 手势期间页码会一直变，而 pointerInput 一因 key 变化重启就会打断拖动，
    // 所以 key 只放页数，当前页与回调走「最新值」读取。
    val latestPage = rememberUpdatedState(page)
    val latestChange = rememberUpdatedState(change)
    val scope = rememberCoroutineScope()
    /** 整排位移（px）：拖动时由手指驱动，松手后由吸附动画拉回 0 */
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(count, stepPx) {
                val tracker = VelocityTracker()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    tracker.resetTracking()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    val base = latestPage.value
                    var dragged = 0f
                    var started = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val ch = event.changes.firstOrNull { it.id == down.id } ?: break
                            tracker.addPosition(ch.uptimeMillis, ch.position)
                            dragged += ch.positionChange().x
                            // 越过 touch slop 才接管：短按/点击要留给页码钮
                            if (!started && abs(dragged) > viewConfiguration.touchSlop) started = true
                            if (started) {
                                // 吞掉指针：否则松手时那一发会同时触发下面页码钮的 click
                                ch.consume()
                                val target = (base + (-(dragged / stepPx).roundToInt()))
                                    .coerceIn(0, count - 1)
                                // 页码每跳一格整排就重排一格，补上不足一格的零头才 1:1 跟手
                                dragOffset = dragged + (target - base) * stepPx
                                if (target != latestPage.value) latestChange.value(target)
                            }
                            if (!ch.pressed) break
                        }
                    } finally {
                        if (started) {
                            // 甩动补页：速度越大翻得越多（横放转盘的手感），上限 3 页
                            val vx = tracker.calculateVelocity().x
                            val fling = (-vx / 900f).roundToInt().coerceIn(-3, 3)
                            val from = latestPage.value
                            val to = (from + fling).coerceIn(0, count - 1)
                            if (to != from) latestChange.value(to)
                            // 吸附回原位：从当前零头缓出到 0（手势作用域里不能直接挂动画）
                            val start = dragOffset
                            scope.launch {
                                animate(start, 0f, animationSpec = tween(SNAP_MS, easing = EaseOutCubic)) { v, _ ->
                                    dragOffset = v
                                }
                            }
                        } else {
                            dragOffset = 0f
                        }
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.offset { IntOffset(dragOffset.roundToInt(), 0) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = slot,
        )
    }
}

/** 转盘模式：一格宽（dp）与整体高（dp） */
private const val PAGER_SLOT_W = 44f
private const val PAGER_H = 36f

/** 转盘模式同时显示的页码个数 */
private const val PAGER_VISIBLE = 5

/** 吸附动画时长（ms）：两端同一条手感 */
private const val SNAP_MS = 190

/**
 * 横放的转盘式页码：中间是当前页，左右各 (visible-1)/2 个；离中心越远越小、越淡，出框即不画。
 * 拖动只改「连续页位」pos（浮点），页码文字跟着滚；松手吸附到最近整数页（含甩动补页）。
 * 外框尺寸固定，所以拖动时**除页码外没有东西在动**。
 *
 * @param modifier 布局修饰
 * @param page 当前页（0 起）
 * @param count 总页数
 * @param change 换页回调
 * @param dragStep 拖动多远算翻一页
 * @param visible 同时显示的页码个数
 */
@Composable
private fun PickerPager(
    modifier: Modifier,
    page: Int,
    count: Int,
    change: (Int) -> Unit,
    dragStep: Dp = 40.dp,
    visible: Int = PAGER_VISIBLE,
) {
    val palette = LocalMuiPalette.current
    val density = LocalDensity.current
    val stepPx = with(density) { dragStep.toPx() }
    val slotPx = with(density) { PAGER_SLOT_W.dp.toPx() }
    val half = (visible - 1) / 2
    val scope = rememberCoroutineScope()

    // 连续页位（浮点）：拖动时跟手，松手动画到整数页
    var pos by remember { mutableFloatStateOf(page.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    val latestChange = rememberUpdatedState(change)

    // 外部换页（上一页/下一页、别的入口）时对齐过去；拖动中不打断
    LaunchedEffect(page) {
        if (!dragging && abs(pos - page) > 0.01f) {
            animate(pos, page.toFloat(), animationSpec = tween(SNAP_MS, easing = EaseOutCubic)) { v, _ ->
                pos = v
            }
        }
    }

    // 外层：上一页 / 转盘 / 下一页 —— 两侧按钮固定不动，只有中间的页码滚
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MuiPaginationPrevious(
            onClick = { change((page - 1).coerceAtLeast(0)) },
            enabled = page > 0,
        )
        Box(
        modifier = Modifier
            .width((PAGER_SLOT_W * visible).dp)
            .height(PAGER_H.dp)
            .pointerInput(count, stepPx) {
                val tracker = VelocityTracker()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    tracker.resetTracking()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    val startPos = pos
                    var dx = 0f
                    var started = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val ch = event.changes.firstOrNull { it.id == down.id } ?: break
                            tracker.addPosition(ch.uptimeMillis, ch.position)
                            dx += ch.positionChange().x
                            if (!started && abs(dx) > viewConfiguration.touchSlop) {
                                started = true
                                dragging = true
                            }
                            if (started) {
                                ch.consume()
                                // 左拖 = 下一页：页位随位移往右走
                                pos = (startPos - dx / stepPx).coerceIn(0f, (count - 1).toFloat())
                                val nearest = pos.roundToInt()
                                if (nearest != page) latestChange.value(nearest)
                            }
                            if (!ch.pressed) break
                        }
                    } finally {
                        dragging = false
                        if (started) {
                            // 甩动补页：速度越大翻得越多，上限 3 页
                            val vx = tracker.calculateVelocity().x
                            val fling = (-vx / 900f).roundToInt().coerceIn(-3, 3)
                            val target = (pos.roundToInt() + fling).coerceIn(0, count - 1)
                            if (target != page) latestChange.value(target)
                            // 手势作用域里不能直接挂动画，丢回组合作用域
                            val from = pos
                            scope.launch {
                                animate(from, target.toFloat(), animationSpec = tween(SNAP_MS, easing = EaseOutCubic)) { v, _ ->
                                    pos = v
                                }
                            }
                        } else {
                            // 没拖动：当点击，交给下面的页码各自处理（这里什么都不做）
                            pos = page.toFloat()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val center = pos.roundToInt()
        // 选中页的方框：白底 + 1dp 描边 + xs 阴影（= web outline 变体的 border bg-background shadow-xs），
        // 按 (center − pos) 连续跟随当前页
        val frameShape = RoundedCornerShape(MuiRadius.md)
        Box(
            modifier = Modifier
                .offset { IntOffset(((center - pos) * slotPx).roundToInt(), 0) }
                .size(PAGER_SLOT_W.dp, PAGER_H.dp)
                .muiElevation("xs", frameShape)
                .background(palette.card, frameShape)
                .border(1.dp, palette.border, frameShape),
        )
        // 画窗口内的每一页：|p - pos| 决定横向位置、缩放与透明度
        for (p in (center - half - 1)..(center + half + 1)) {
            if (p < 0 || p >= count) continue
            val d = p - pos
            val t = (abs(d) / (half + 0.5f)).coerceIn(0f, 1f)
            if (t >= 1f) continue
            val scale = 1f - 0.42f * t * t
            val active = p == center
            val itemInteraction = remember { MutableInteractionSource() }
            val hovered by itemInteraction.collectIsHoveredAsState()
            val itemPressed by itemInteraction.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .offset { IntOffset((d * slotPx).roundToInt(), 0) }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - 0.6f * t
                    }
                    .size(PAGER_SLOT_W.dp, PAGER_H.dp)
                    .background(
                        if (hovered || itemPressed) palette.accent else Color.Transparent,
                        RoundedCornerShape(MuiRadius.md),
                    )
                    .clip(RoundedCornerShape(MuiRadius.md))
                    .clickable(
                        interactionSource = itemInteraction,
                        indication = null,
                    ) {
                        if (p != page) change(p)
                        scope.launch {
                            animate(pos, p.toFloat(), animationSpec = tween(SNAP_MS, easing = EaseOutCubic)) { v, _ ->
                                pos = v
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                MuiText(
                    text = (p + 1).toString(),
                    fontSize = 15.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) palette.foreground else palette.mutedForeground,
                )
            }
        }
    }
        MuiPaginationNext(
            onClick = { change((page + 1).coerceAtMost(count - 1)) },
            enabled = page < count - 1,
        )
    }
}

/**
 * 页码钮：当前页 outline、其它 ghost，36dp 方钮（web size=icon）。
 *
 * @param page 页码文字（如 "1"）
 * @param onClick 点击回调
 * @param modifier 布局修饰
 * @param isActive 是否当前页
 * @param enabled 可用性（与 web 的 disabled 对齐）
 */
@Composable
fun MuiPaginationLink(
    page: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    enabled: Boolean = true,
) {
    MuiButton(
        onClick = onClick,
        modifier = modifier,
        variant = if (isActive) ButtonVariant.Outline else ButtonVariant.Ghost,
        size = ButtonSize.Icon,
        enabled = enabled,
    ) {
        MuiText(text = page)
    }
}

/**
 * 上一页钮：ghost/outline + 左 chevron + "Previous" 文字。
 *
 * @param onClick 点击回调
 * @param modifier 布局修饰
 * @param isActive 是否当前页
 * @param enabled 可用性（首页时上一页置灰）
 */
@Composable
fun MuiPaginationPrevious(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    enabled: Boolean = true,
) {
    MuiButton(
        onClick = onClick,
        modifier = modifier,
        variant = if (isActive) ButtonVariant.Outline else ButtonVariant.Ghost,
        size = ButtonSize.Default,
        enabled = enabled,
    ) {
        MuiPaginationIcon(MuiIcons.ChevronLeft)
        MuiText(text = "Previous")
    }
}

/**
 * 下一页钮：ghost/outline + "Next" 文字 + 右 chevron。
 *
 * @param onClick 点击回调
 * @param modifier 布局修饰
 * @param isActive 是否当前页
 * @param enabled 可用性（末页时下一页置灰）
 */
@Composable
fun MuiPaginationNext(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    enabled: Boolean = true,
) {
    MuiButton(
        onClick = onClick,
        modifier = modifier,
        variant = if (isActive) ButtonVariant.Outline else ButtonVariant.Ghost,
        size = ButtonSize.Default,
        enabled = enabled,
    ) {
        MuiText(text = "Next")
        MuiPaginationIcon(MuiIcons.ChevronRight)
    }
}

/**
 * 省略号：36dp 方块居中 more-horizontal 三点图标（不可点）。
 *
 * @param modifier 布局修饰
 */
@Composable
fun MuiPaginationEllipsis(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(36.dp),
        contentAlignment = Alignment.Center,
    ) {
        MuiPaginationIcon(MuiIcons.Ellipsis)
    }
}

/**
 * 分页里的小图标（16dp）：颜色取按钮内容色，兜底 foreground。
 * 图形与 web 同一套（lucide 的 chevron-left / chevron-right / more-horizontal），
 * 不再用 Canvas 手画路径 —— 那套是图标集之前的遗留。
 *
 * @param icon 图标（来自 [MuiIcons]）
 */
@Composable
private fun MuiPaginationIcon(icon: ImageVector) {
    val contentColor = LocalMuiContentColor.current
    val fallback = LocalMuiPalette.current.foreground
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(icon),
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
            if (contentColor == androidx.compose.ui.graphics.Color.Unspecified) fallback else contentColor,
        ),
    )
}
