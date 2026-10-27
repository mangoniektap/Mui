package com.mui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
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
            // 浮层：bg-popover + border + md 阴影 + rounded-md，锚在触发器下方
            val popupShape = RoundedCornerShape(MuiRadius.md)
            Popup(
                alignment = Alignment.TopStart,
                offset = androidx.compose.ui.unit.IntOffset(0, triggerHeightPx + 4),
                properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
                onDismissRequest = { setOpen(false) },
            ) {
                // web：浮层宽度 = max(内容宽, 触发器宽, min-w-[8rem])，高度上限 320dp。
                // Compose 没有「宽度 = 最宽子项」，用 IntrinsicSize.Max 顶替（fillMaxWidth 会
                // 把 Column 撑到窗口宽，浮层就变成整屏一条）。
                val minW = with(LocalDensity.current) {
                    maxOf(128.dp, triggerWidthPx.toDp())
                }
                Column(
                    Modifier
                        .width(IntrinsicSize.Max)
                        .widthIn(min = minW)
                        .heightIn(min = 36.dp, max = 320.dp)
                        .background(palette.popover, popupShape)
                        .border(1.dp, palette.border, popupShape)
                        .muiElevation("md", popupShape)
                        .padding(4.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    var lastGroup: String? = null
                    options.forEach { option ->
                        if (option.group != null && option.group != lastGroup) {
                            MuiText(
                                text = option.group,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
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
                                .padding(start = 8.dp, end = 32.dp, top = 6.dp, bottom = 6.dp),
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
