package com.mui.components

import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.mui.icon.MuiIcons
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius
import com.mui.theme.muiElevation
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource

/** 一个选项：值 / 文字 / 分组标题（变化时插入一行组标签）/ 禁用 */
data class SelectOption(
    /** 选中时随回调传出的值 */
    val value: String,
    /** 显示文字 */
    val label: String,
    /** 分组标题（null = 不分组；相邻同名组只显示一次标题行） */
    val group: String? = null,
    /** 禁用：不可选、半透明 */
    val disabled: Boolean = false,
)

/**
 * 下拉选择：classical 触发表面（h-9/h-8 描边）+ 浮层列表（bg-popover + md 阴影），
 * 选中项右侧画 ✓；键盘导航未移植。
 *
 * @param options 选项清单
 * @param value 受控选中值（null = 非受控）
 * @param defaultValue 初始选中值（非受控用，默认第一项）
 * @param onValueChange 选中回调
 * @param placeholder 未选中时的占位文字（null = 显示第一项）
 * @param triggerSize 触发器档位：default = h-9 / sm = h-8
 * @param expanded 展开状态受控（null = 内部自持）
 * @param onExpandedChange 展开/收起回调
 * @param invalid 校验失败态：触发器转 destructive
 * @param disabled 禁用：半透明且不可展开
 * @param interactionSource 触发器交互源
 */
@Composable
fun MuiSelect(
    options: List<SelectOption>,
    modifier: Modifier = Modifier,
    value: String? = null,
    defaultValue: String? = null,
    onValueChange: ((String) -> Unit)? = null,
    placeholder: String? = null,
    triggerSize: SelectTriggerSize = SelectTriggerSize.Default,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    invalid: Boolean = false,
    disabled: Boolean = false,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
) {
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    var innerValue by remember { mutableStateOf(defaultValue) }
    var innerOpen by remember { mutableStateOf(false) }

    val selectedValue = value ?: innerValue
    val isOpen = expanded ?: innerOpen
    val shape = RoundedCornerShape(MuiRadius.md)

    fun setOpen(next: Boolean) {
        if (expanded == null) innerOpen = next
        onExpandedChange?.invoke(next)
    }

    fun pick(option: SelectOption) {
        if (option.disabled) return
        if (value == null) innerValue = option.value
        onValueChange?.invoke(option.value)
        setOpen(false)
    }

    val current = options.firstOrNull { it.value == selectedValue }
    val h = if (triggerSize == SelectTriggerSize.Sm) 32.dp else 36.dp
    val triggerShape = shape
    val borderColor = if (invalid) palette.destructive else palette.input
    var triggerHeightPx by remember { mutableStateOf(0) }
    var triggerWidthPx by remember { mutableStateOf(0) }
    /** 触发器在窗口里的矩形（px）：item-aligned 定位与上下反转都按它算 */
    var triggerWinRect by remember { mutableStateOf(IntRect.Zero) }

    Box(modifier = modifier) {
        // 触发器
        Box(
            Modifier
                .alpha(if (disabled) 0.5f else 1f)
                .height(h)
                .onSizeChanged {
                    triggerHeightPx = it.height
                    triggerWidthPx = it.width
                }
                .onGloballyPositioned { triggerWinRect = it.boundsInWindow().toLegacyIntRect() }
                .clip(triggerShape)
                .background(
                    if (isDark) {
                        palette.input.copy(alpha = palette.input.alpha * 0.30f)
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },
                    triggerShape,
                )
                .border(1.dp, borderColor, triggerShape)
                .muiElevation("xs", triggerShape)
                .muiFocusRing(interactionSource, triggerShape, invalid)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = !disabled,
                ) { setOpen(!isOpen) }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            // web：`flex w-fit items-center justify-between gap-2` —— 触发器宽度跟着内容走。
            // 这里**不能**给 fillMaxWidth：Row 一旦撑满，外层的 max 约束（调用点的 widthIn）
            // 就成了触发器宽度，下拉框会横向占满整行。
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val label = current?.label ?: placeholder
                MuiText(
                    text = label ?: "—",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (current == null && placeholder != null) palette.mutedForeground else palette.foreground,
                )
                // 下箭头：lucide ChevronDown（size-4 opacity-50），与 web 同一张图
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(MuiIcons.ChevronDown),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                        palette.mutedForeground.copy(alpha = 0.5f),
                    ),
                )
            }
        }

        if (isOpen) {
            // 浮层：bg-popover + border + md 阴影 + rounded-md。
            // 定位 = web 的 Radix `position="item-aligned" align="center"`：把「已选项中心」
            // 对齐到触发器中心，剩余项排在它上下；下方放不下就翻到上方；两边都不够则贴边 + 截断滚动。
            val popupShape = RoundedCornerShape(MuiRadius.md)
            val density = LocalDensity.current
            val view = LocalView.current
            val winH = view.rootView.height

            // 等高行（与 web select-item 的 py-1.5 + text-sm/lineHeight-20 等价）：
            // 固定行高后，选中项在浮层内的中心偏移可精确累加，item-aligned 才算得准。
            val itemHDp = ITEM_H_DP
            val groupHDp = GROUP_H_DP
            val padHDp = 4 // Column 的 padding(4.dp)：行偏移要加上外层留白

            // 选中项中心相对浮层内容顶的偏移（px）——按行高累计，分组标题行也算一行。
            data class RowKind(val group: Boolean, val option: SelectOption?)
            val rows = ArrayList<RowKind>()
            var lastGroup: String? = null
            options.forEach { o ->
                if (o.group != null && o.group != lastGroup) { rows.add(RowKind(true, null)); lastGroup = o.group }
                else if (o.group == null) { lastGroup = null }
                rows.add(RowKind(false, o))
            }
            val selectedIndex = rows.indexOfFirst { it.option?.value == selectedValue }
            val selectedCenterPx = with(density) {
                var acc = (padHDp.dp.toPx())
                for (i in 0 until selectedIndex.coerceAtLeast(0)) {
                    acc += (if (rows[i].group) groupHDp else itemHDp).dp.toPx()
                }
                if (selectedIndex >= 0) acc + itemHDp.dp.toPx() / 2f else acc
            }

            // 可用高度：下方 / 上方各自能容纳多少（扣 gap），取大者作为浮层 max；都不够则取较小者贴边。
            val gapPx = with(density) { 4.dp.toPx() }
            val belowAvail = (winH.toFloat() - triggerWinRect.bottom - gapPx).coerceAtLeast(0f)
            val aboveAvail = (triggerWinRect.top.toFloat() - gapPx).coerceAtLeast(0f)
            val maxHpx = maxOf(belowAvail, aboveAvail).coerceAtMost(with(density) { 320.dp.toPx() })

            val scrollState = rememberScrollState()
            // 滚动只负责「内容超过视口时把选中项带进视口中央」。
            // 内容不超视口时不动（滚动=0），对齐完全交给 provider 的 idealTop——
            // 这样选中项物理位置 = popupTop + S == triggerCenter，与 Radix item-aligned 一致。
            LaunchedEffect(isOpen, selectedValue, maxHpx) {
                if (!isOpen) return@LaunchedEffect
                // 视口/最大滚动量在首帧才就绪，等一帧再算。
                withFrameNanos {}
                val maxV = scrollState.maxValue
                if (maxV <= 0) return@LaunchedEffect
                val target = (selectedCenterPx - maxHpx / 2f)
                    .coerceIn(0f, maxV.toFloat())
                    .roundToInt()
                scrollState.animateScrollTo(target)
            }

            Popup(
                popupPositionProvider = SelectItemAlignedPosition(
                    density = density,
                    triggerRect = triggerWinRect,
                    selectedCenterPx = selectedCenterPx,
                    maxHpx = maxHpx,
                    gapPx = gapPx,
                    winH = winH,
                ),
                onDismissRequest = { setOpen(false) },
                properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
            ) {
                val minW = with(density) { maxOf(128.dp, triggerWidthPx.toDp()) }
                Column(
                    Modifier
                        .width(IntrinsicSize.Max)
                        .widthIn(min = minW)
                        .heightIn(
                            min = 36.dp,
                            max = with(density) { maxHpx.toDp() },
                        )
                        .background(palette.popover, popupShape)
                        .border(1.dp, palette.border, popupShape)
                        .muiElevation("md", popupShape)
                        .padding(padHDp.dp)
                        .verticalScroll(scrollState),
                ) {
                    var lastGroup: String? = null
                    options.forEach { option ->
                        if (option.group != null && option.group != lastGroup) {
                            MuiText(
                                text = option.group,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(groupHDp.dp)
                                    .padding(horizontal = 8.dp),
                                fontSize = 12.sp,
                                color = palette.mutedForeground,
                            )
                            lastGroup = option.group
                        } else if (option.group == null) {
                            lastGroup = null
                        }
                        val itemInteraction = remember { MutableInteractionSource() }
                        val pressed by itemInteraction.collectIsPressedAsState()
                        Row(
                            Modifier
                                .alpha(if (option.disabled) 0.5f else 1f)
                                .fillMaxWidth()
                                .height(itemHDp.dp)
                                .clip(RoundedCornerShape(MuiRadius.sm))
                                // web 只有 focus/hover 才铺 accent 底，选中态只靠右侧 ✓ 表达
                                .background(
                                    if (pressed) palette.accent else androidx.compose.ui.graphics.Color.Transparent,
                                    RoundedCornerShape(MuiRadius.sm),
                                )
                                .clickable(
                                    interactionSource = itemInteraction,
                                    indication = null,
                                    enabled = !option.disabled,
                                ) { pick(option) }
                                .padding(start = 8.dp, end = 32.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            // web 的 ✓ 是 absolute right-2：贴着条目右缘，与文字长短无关
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            MuiText(text = option.label, fontSize = 14.sp, lineHeight = 20.sp)
                            if (option.value == selectedValue) {
                                // 选中 ✓：lucide Check（size-4），右缘 pr-8 的位置由 padding 给出
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(MuiIcons.Check),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(palette.foreground),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 触发器档位：default = h-9 / sm = h-8 */
enum class SelectTriggerSize { Default, Sm }

/** 选项行高（dp）：= web select-item 的 py-1.5(6*2) + text-sm/lineHeight-20。固定行高才能
 *  精确累加「选中项中心」偏移，复现 Radix `position="item-aligned"` 的对齐基准。 */
private const val ITEM_H_DP = 32
/** 分组标题行高（dp）：= web select-label 的 py-1.5 + text-xs。 */
private const val GROUP_H_DP = 28

/**
 * item-aligned 定位：把浮层内「已选项中心」对齐到触发器中心；下方放不下翻上方；两侧都不够贴边。
 * 等价于 web Radix Select `position="item-aligned" align="center"`（仅向上/向下反转 + 截断滚动，
 * 不引入 KT 侧原本就没有的 slide 动画）。
 *
 * @param selectedCenterPx 选中项中心相对浮层内容顶的偏移（已含外层 padding）。
 * @param maxHpx 浮层允许的最大高度（取自窗口上下可用空间，已扣 gap，上限 320dp）。
 */
private class SelectItemAlignedPosition(
    private val density: androidx.compose.ui.unit.Density,
    private val triggerRect: IntRect,
    private val selectedCenterPx: Float,
    private val maxHpx: Float,
    private val gapPx: Float,
    private val winH: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            triggerRect.left
        } else {
            triggerRect.right - popupContentSize.width
        }
        val triggerCenterY = triggerRect.top + triggerRect.height / 2
        // 优先理想位：选中项中心 = 触发器中心。
        val idealTop = (triggerCenterY - selectedCenterPx).roundToInt()
        // 上下可用空间（已扣 gap）。
        val belowAvail = (winH - triggerRect.bottom - gapPx).coerceAtLeast(0f)
        val aboveAvail = (triggerRect.top - gapPx).coerceAtLeast(0f)
        // 浮层实际高度受内容与上限约束。
        val popupH = popupContentSize.height.toFloat().coerceAtMost(maxHpx)
        // 下方能否完整放下（选中项中心仍要对齐触发器中心 → 浮层顶 = idealTop）。
        val fitsBelow = idealTop >= 0 && idealTop.toFloat() + popupH <= winH.toFloat()
        val y = when {
            fitsBelow -> idealTop
            belowAvail >= aboveAvail -> {
                // 下方更宽：贴触发器下沿 + gap 展开向下。
                (triggerRect.bottom + gapPx).roundToInt()
            }
            else -> {
                // 上方更宽：贴触发器上沿 - gap 展开向上。
                (triggerRect.top - gapPx - popupH).roundToInt().coerceAtLeast(0)
            }
        }
        return IntOffset(x, y.coerceIn(0, (winH.toFloat() - popupH).roundToInt().coerceAtLeast(0)))
    }
}

/** boundsInWindow 返回新版 Rect，转成本文件用的 legacy IntRect（避免再引一个 Rect 类型）。 */
private fun androidx.compose.ui.geometry.Rect.toLegacyIntRect(): IntRect =
    IntRect(
        left = left.roundToInt(),
        top = top.roundToInt(),
        right = right.roundToInt(),
        bottom = bottom.roundToInt(),
    )
