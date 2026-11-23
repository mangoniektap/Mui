package com.mui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.mui.icon.MuiIcons
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiPalette
import com.mui.theme.muiElevation
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource
import com.mui.token.MuiRadius

/**
 * 可输入 + 下拉过滤列表（combobox.tsx 简化版）：输入框用 MuiInput 表面（h-9 rounded-md border-input），
 * 聚焦/输入时在输入框下方弹出 Popup：bg-popover border(foreground/10) shadow-md rounded-md，
 * 条目 = rounded-sm py-1.5 px-2 text-sm，高亮条目 bg-accent，右侧画 ✓；空结果给提示行。
 * 输入 = options / selected / onSelect；输出 = onSelect。
 * 硬约束：禁 material3；视觉全走令牌；数据 List<String> 起步。
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
    var innerSelected by remember { mutableStateOf<String?>(null) }
    val selection = selected ?: innerSelected
    MuiComboboxInput(
        options = options,
        selected = setOfNotNull(selection),
        onToggle = {
            if (selected == null) innerSelected = it
            onSelect(it)
        },
        modifier = modifier,
        placeholder = placeholder,
        enabled = enabled,
        emptyText = emptyText,
        interactionSource = interactionSource,
        multiple = false,
    )
}

/** 多选组合框，选中项显示为可移除的 chips，输入框继续过滤候选项。 */
@Composable
fun MuiCombobox(
    options: List<String>,
    selected: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    emptyText: String = "无结果",
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
) {
    MuiComboboxInput(
        options = options,
        selected = selected,
        onToggle = { option -> onSelectionChange(if (option in selected) selected - option else selected + option) },
        modifier = modifier,
        placeholder = placeholder,
        enabled = enabled,
        emptyText = emptyText,
        interactionSource = interactionSource,
        multiple = true,
    )
}

@Composable
private fun MuiComboboxInput(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier,
    placeholder: String?,
    enabled: Boolean,
    emptyText: String,
    interactionSource: MutableInteractionSource,
    multiple: Boolean,
) {
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    val density = LocalDensity.current
    val focused by interactionSource.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    val invalid = LocalMuiFormFieldInvalid.current
    val shape = RoundedCornerShape(MuiRadius.md)
    var anchorWidth by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var highlighted by remember(query) { mutableIntStateOf(-1) }

    fun close() {
        expanded = false
        editing = false
        query = ""
        highlighted = -1
    }

    LaunchedEffect(focused, enabled) {
        if (focused && enabled) expanded = true else close()
    }
    val filtered = remember(query, options) {
        options.filter { it.contains(query, ignoreCase = true) }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(highlighted) {
        if (expanded && highlighted in filtered.indices &&
            listState.layoutInfo.visibleItemsInfo.none { it.index == highlighted }
        ) listState.scrollToItem(highlighted)
    }

    fun choose(option: String) {
        onToggle(option)
        query = ""
        editing = false
        if (!multiple) close()
    }

    Box(modifier.onSizeChanged { anchorWidth = it.width }) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp)
                .alpha(if (enabled) 1f else 0.5f)
                .muiElevation("xs", shape)
                .muiFocusRing(interactionSource, shape, invalid)
                .background(if (isDark) palette.input.copy(alpha = palette.input.alpha * 0.3f) else Color.Transparent, shape)
                .border(1.dp, if (invalid) palette.destructive else palette.input, shape)
                .padding(start = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowRow(
                Modifier.weight(1f).padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (multiple) {
                    selected.forEach { option ->
                        Row(
                            Modifier.background(palette.muted, RoundedCornerShape(MuiRadius.sm)).padding(start = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MuiText(option, fontSize = 12.sp)
                            MuiButton(
                                onClick = { onToggle(option) },
                                modifier = Modifier.semantics { contentDescription = "移除 " + option },
                                variant = ButtonVariant.Ghost,
                                size = ButtonSize.IconXs,
                                enabled = enabled,
                            ) {
                                Image(rememberVectorPainter(MuiIcons.X), null, Modifier.size(12.dp),
                                    colorFilter = ColorFilter.tint(palette.mutedForeground))
                            }
                        }
                    }
                }
                val text = if (editing || multiple) query else selected.firstOrNull().orEmpty()
                BasicTextField(
                    value = text,
                    onValueChange = {
                        query = it
                        editing = true
                        expanded = true
                    },
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp).weight(1f)
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.DirectionDown -> {
                                    expanded = true
                                    highlighted = (highlighted + 1).coerceAtMost(filtered.lastIndex)
                                }
                                Key.DirectionUp -> {
                                    expanded = true
                                    highlighted = if (highlighted < 0) filtered.lastIndex else (highlighted - 1).coerceAtLeast(0)
                                }
                                Key.Enter, Key.NumPadEnter -> {
                                    val item = filtered.getOrNull(highlighted) ?: return@onPreviewKeyEvent false
                                    if (!expanded) return@onPreviewKeyEvent false
                                    choose(item)
                                }
                                Key.Escape -> {
                                    if (!expanded) return@onPreviewKeyEvent false
                                    close()
                                }
                                Key.Backspace -> {
                                    if (!multiple || query.isNotEmpty()) return@onPreviewKeyEvent false
                                    val last = selected.lastOrNull() ?: return@onPreviewKeyEvent false
                                    onToggle(last)
                                }
                                else -> return@onPreviewKeyEvent false
                            }
                            true
                        },
                    enabled = enabled,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = palette.foreground),
                    cursorBrush = SolidColor(palette.primary),
                    interactionSource = interactionSource,
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty() && placeholder != null && selected.isEmpty()) {
                                MuiText(placeholder, color = palette.mutedForeground)
                            }
                            inner()
                        }
                    },
                )
            }
            MuiButton(
                onClick = {
                    if (expanded) close() else {
                        focusRequester.requestFocus()
                        expanded = true
                    }
                },
                variant = ButtonVariant.Ghost,
                size = ButtonSize.IconXs,
                enabled = enabled,
            ) {
                Image(rememberVectorPainter(MuiIcons.ChevronDown), "展开选项", Modifier.size(16.dp),
                    colorFilter = ColorFilter.tint(palette.mutedForeground))
            }
        }
        if (expanded && enabled) {
            Popup(
                popupPositionProvider = rememberMuiPopupPosition(MuiPopupSide.Bottom, 6.dp, alignStart = true),
                onDismissRequest = { close() },
            ) {
                val popupShape = RoundedCornerShape(MuiRadius.md)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.width(with(density) { anchorWidth.toDp() }).heightIn(max = 384.dp)
                        .muiElevation("md", popupShape)
                        .clip(popupShape)
                        .background(palette.popover, popupShape)
                        .border(1.dp, palette.foreground.copy(alpha = palette.foreground.alpha * 0.10f), popupShape)
                        .padding(4.dp),
                ) {
                    if (filtered.isEmpty()) {
                        item {
                            MuiText(emptyText, Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                fontSize = 14.sp, color = palette.mutedForeground,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    } else {
                        items(filtered.size) { index ->
                            val option = filtered[index]
                            MuiComboboxItem(option, index == highlighted, option in selected) { choose(option) }
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
 * @param highlighted 是否由键盘导航高亮。
 * @param selected 是否已选中，控制勾选标记。
 * @param onClick 点击回调
 */
@Composable
private fun MuiComboboxItem(
    option: String,
    highlighted: Boolean,
    selected: Boolean,
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
        if (selected) {
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
