package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiPalette
import com.mui.theme.MuiRadius
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

    Box(modifier = modifier) {
        // 触发器
        Box(
            Modifier
                .alpha(if (disabled) 0.5f else 1f)
                .height(h)
                .onSizeChanged { triggerHeightPx = it.height }
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
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val label = current?.label ?: placeholder
                MuiText(
                    text = label ?: "—",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = if (current == null && placeholder != null) palette.mutedForeground else palette.foreground,
                )
                // 下箭头（size-4 opacity-50）
                androidx.compose.foundation.Canvas(Modifier.size(16.dp)) {
                    val w = size.width
                    val hpx = size.height
                    val stroke = 1.5.dp.toPx()
                    drawLine(
                        palette.mutedForeground.copy(alpha = 0.5f),
                        Offset(stroke, hpx * 0.35f),
                        Offset(w / 2f, hpx * 0.65f),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    drawLine(
                        palette.mutedForeground.copy(alpha = 0.5f),
                        Offset(w / 2f, hpx * 0.65f),
                        Offset(w - stroke, hpx * 0.35f),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }
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
                Column(
                    Modifier
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
                        Row(
                            Modifier
                                .alpha(if (option.disabled) 0.5f else 1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(MuiRadius.sm))
                                .background(
                                    if (option.value == selectedValue) palette.accent else androidx.compose.ui.graphics.Color.Transparent,
                                    RoundedCornerShape(MuiRadius.sm),
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = !option.disabled,
                                ) { pick(option) }
                                .padding(start = 8.dp, end = 32.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            MuiText(text = option.label, fontSize = 14.sp, lineHeight = 20.sp)
                            if (option.value == selectedValue) {
                                // 选中 ✓（画在右侧 pr-8 的位置）
                                androidx.compose.foundation.Canvas(Modifier.size(16.dp)) {
                                    val w = size.width
                                    val stroke = 2.dp.toPx()
                                    drawLine(
                                        palette.accentForeground,
                                        Offset(w * 0.15f, w * 0.55f),
                                        Offset(w * 0.4f, w * 0.8f),
                                        strokeWidth = stroke,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    )
                                    drawLine(
                                        palette.accentForeground,
                                        Offset(w * 0.4f, w * 0.8f),
                                        Offset(w * 0.85f, w * 0.25f),
                                        strokeWidth = stroke,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    )
                                }
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
