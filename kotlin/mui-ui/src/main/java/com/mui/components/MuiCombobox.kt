package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius
import com.mui.theme.muiElevation
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource
import kotlin.math.roundToInt

/**
 * 可输入 + 下拉过滤列表（combobox.tsx 简化版）：输入框用 MuiInput 表面（h-9 rounded-md border-input），
 * 聚焦/输入时在输入框下方弹出 Popup：bg-popover border(foreground/10) shadow-md rounded-md，
 * 条目 = rounded-sm py-1.5 px-2 text-sm，高亮条目 bg-accent，右侧画 ✓；空结果给提示行。
 * 输入 = options / selected / onSelect；输出 = onSelect。
 * 硬约束：禁 material3；视觉全走令牌；数据 List<String> 起步。
 * TODO: web 的多选 chips（ComboboxChips/Chip/ChipsInput）未移植。
 * TODO: web 的 anchor（chips 触发器）未移植，弹层宽度固定跟随输入框。
 * TODO: web 的 ComboboxGroup/Label/Separator/Empty 独立子部件未移植，合并进主组件（空态文本内联）。
 * TODO: web 的弹层进出场动画（fade/zoom/slide）未移植。
 *
 * @param options 候选列表（ List<String> 起步）
 * @param selected 受控选中项（null = 非受控，内部记录）
 * @param onSelect 选中回调（点条目时触发并收起弹层）
 * @param modifier 布局修饰
 * @param placeholder 占位文本（null = 无）
 * @param enabled 可用性（false = 半透明不可输入）
 * @param emptyText 无结果提示文本（默认「无结果」）
 * @param interactionSource 交互源（输入框聚焦环出口）
 */
@Composable
fun MuiCombobox(
    options: List<String>,
    selected: String? = null,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    emptyText: String = "无结果",
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
) {
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    val density = LocalDensity.current
    val focused by interactionSource.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    // 锚点（输入框）宽度：弹层与其等宽（web 的 w-(--anchor-width)）
    var anchorWidthPx by remember { mutableStateOf(0) }

    var query by remember { mutableStateOf(selected ?: "") }
    var innerSelected by remember { mutableStateOf<String?>(selected) }
    val effectiveSelected = selected ?: innerSelected
    var expanded by remember { mutableStateOf(false) }

    // 聚焦 = 展开；失焦 = 收起（Popup 不抢焦点，点条目时焦点仍在输入框）
    LaunchedEffect(focused) {
        if (focused) expanded = true else expanded = false
    }

    val filtered = remember(query, options) {
        if (query.isEmpty()) options
        else options.filter { it.contains(query, ignoreCase = true) }
    }

    Box(modifier = modifier.onSizeChanged { anchorWidthPx = it.width }) {
        // 输入区：MuiInput 表面（透明底 + border-input + xs 阴影 + 聚焦环）
        val inputShape = RoundedCornerShape(MuiRadius.md)
        BasicTextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp)
                .clip(inputShape)
                .background(
                    if (isDark) {
                        palette.input.copy(alpha = palette.input.alpha * 0.30f)
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },
                    inputShape,
                )
                .border(1.dp, palette.input, inputShape)
                .muiElevation("2xs", inputShape)
                .muiFocusRing(interactionSource, inputShape)
                .focusRequester(focusRequester)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = { focusRequester.requestFocus() },
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            enabled = enabled,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = palette.foreground),
            cursorBrush = SolidColor(palette.primary),
            interactionSource = interactionSource,
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty() && placeholder != null) {
                        MuiText(text = placeholder, color = palette.mutedForeground)
                    }
                    inner()
                }
            },
        )

        if (expanded && enabled) {
            Popup(
                popupPositionProvider = remember(density) { ComboboxPopupPosition(density) },
                onDismissRequest = { expanded = false },
            ) {
                val popupShape = RoundedCornerShape(MuiRadius.md)
                Column(
                    modifier = Modifier
                        .width(with(density) { anchorWidthPx.toDp() })
                        .clip(popupShape)
                        .background(palette.popover, popupShape)
                        .border(1.dp, palette.foreground.copy(alpha = palette.foreground.alpha * 0.10f), popupShape)
                        .muiElevation("md", popupShape)
                        .padding(4.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 376.dp) // web max-h-96 = 384dp，减去 p-1
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (filtered.isEmpty()) {
                            MuiText(
                                text = emptyText,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = palette.mutedForeground,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        } else {
                            for (option in filtered) {
                                MuiComboboxItem(
                                    option = option,
                                    highlighted = option == effectiveSelected,
                                    onClick = {
                                        query = option
                                        innerSelected = option
                                        expanded = false
                                        onSelect(option)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 下拉条目（ComboboxItem）：rounded-sm py-1.5 px-2 pr-8 text-sm；高亮 = bg-accent text-accent-foreground，
 * 右上角固定位置画 ✓（ItemIndicator）。
 *
 * @param option 条目文本
 * @param highlighted 是否高亮（当前选中项）
 * @param onClick 点击回调
 */
@Composable
private fun MuiComboboxItem(
    option: String,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalMuiPalette.current
    val shape = RoundedCornerShape(MuiRadius.sm)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (highlighted) palette.accent else androidx.compose.ui.graphics.Color.Transparent, shape)
            .clickable(
                interactionSource = rememberMuiInteractionSource(),
                indication = null,
                onClick = onClick,
            )
            .padding(start = 8.dp, end = 32.dp, top = 6.dp, bottom = 6.dp),
    ) {
        MuiText(
            text = option,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = if (highlighted) palette.accentForeground else palette.foreground,
        )
        if (highlighted) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
                    drawComboboxCheck(palette.primary)
                }
            }
        }
    }
}

/** ✓ 指示器：两段折线（checkbox 的同款配方），primary 色 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawComboboxCheck(color: androidx.compose.ui.graphics.Color) {
    val w = size.width
    val h = size.height
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(w * 0.14f, h * 0.52f)
        lineTo(w * 0.42f, h * 0.80f)
        lineTo(w * 0.88f, h * 0.22f)
    }
    drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
}

/** 弹层定位：贴在锚点（输入框）正下方左对齐（web sideOffset 6）；放不下时翻到上方 */
private class ComboboxPopupPosition(private val density: androidx.compose.ui.density.Density) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: androidx.compose.ui.unit.IntRect,
        windowSize: androidx.compose.ui.unit.IntSize,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        popupContentSize: androidx.compose.ui.unit.IntSize,
    ): IntOffset {
        val gap = with(density) { 6.dp.toPx().roundToInt() }
        val x = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr) {
            anchorBounds.left
        } else {
            anchorBounds.right - popupContentSize.width
        }
        val below = anchorBounds.bottom + gap
        val fitsBelow = below + popupContentSize.height <= windowSize.height
        val y = if (fitsBelow) below else (anchorBounds.top - gap - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}
