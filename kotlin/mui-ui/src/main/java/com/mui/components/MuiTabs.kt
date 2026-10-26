package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiPalette
import com.mui.theme.MuiRadius
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource

/** 一个标签页：值 + 文字 */
data class TabItem(
    /** 页唯一值（选中时随回调传出） */
    val value: String,
    /** 标签文字 */
    val label: String,
)

/** 标签页列表变体：default = muted 底圆角块；line = 无底 + 下划线 */
enum class TabsVariant { Default, Line }

/** 标签页方向：Horizontal（标签在上）/ Vertical（标签在左） */
enum class TabsOrientation { Horizontal, Vertical }

/**
 * 标签页：标签条 + 内容区；内容由调用方按选中值切换。
 *
 * @param items 标签清单（顺序即左到右）
 * @param value 受控选中值（null = 非受控）
 * @param defaultSelected 初始选中值（非受控用，默认第一项）
 * @param onValueChange 选中值回调
 * @param variant 列表变体（默认 Default）
 * @param orientation 方向（默认 Horizontal）
 * @param disabled 禁用整组
 * @param content 内容区：参数 = 当前选中值
 */
@Composable
fun MuiTabs(
    items: List<TabItem>,
    modifier: Modifier = Modifier,
    value: String? = null,
    defaultSelected: String? = null,
    onValueChange: ((String) -> Unit)? = null,
    variant: TabsVariant = TabsVariant.Default,
    orientation: TabsOrientation = TabsOrientation.Horizontal,
    disabled: Boolean = false,
    content: @Composable (selected: String) -> Unit,
) {
    val palette = LocalMuiPalette.current
    var inner by remember { mutableStateOf(defaultSelected ?: items.firstOrNull()?.value) }
    val selected = value ?: inner

    fun select(next: String) {
        if (value == null) inner = next
        onValueChange?.invoke(next)
    }

    Column(modifier = modifier) {
        val listShape = if (variant == TabsVariant.Line) RoundedCornerShape(0.dp) else RoundedCornerShape(MuiRadius.lg)
        val listModifier = when (orientation) {
            TabsOrientation.Horizontal -> Modifier.fillMaxWidth()
            TabsOrientation.Vertical -> Modifier.width(androidx.compose.ui.unit.Dp.Unspecified)
        }
        Box(
            modifier = listModifier
                .clip(listShape)
                .background(
                    if (variant == TabsVariant.Default) palette.muted else Color.Transparent,
                    listShape,
                )
                .padding(3.dp),
        ) {
            Row(
                modifier = when (orientation) {
                    TabsOrientation.Horizontal -> Modifier.fillMaxWidth()
                    TabsOrientation.Vertical -> Modifier.fillMaxHeight()
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (variant == TabsVariant.Line) Arrangement.spacedBy(4.dp) else Arrangement.Start,
            ) {
                items.forEach { item ->
                    MuiTabTrigger(
                        item = item,
                        selected = item.value == selected,
                        variant = variant,
                        orientation = orientation,
                        enabled = !disabled,
                        onClick = { select(item.value) },
                    )
                }
            }
        }
        Box(Modifier.padding(top = 8.dp)) {
            content(selected)
        }
    }
}

/** 单个标签按钮：rounded-md、text-sm font-medium；选中态按变体着色 */
@Composable
private fun MuiTabTrigger(
    item: TabItem,
    selected: Boolean,
    variant: TabsVariant,
    orientation: TabsOrientation,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    val interaction = rememberMuiInteractionSource()
    val shape = RoundedCornerShape(MuiRadius.md)
    val fg = if (selected) {
        palette.foreground
    } else {
        palette.foreground.copy(alpha = palette.foreground.alpha * 0.6f)
    }
    Column(
        modifier = Modifier
            .alpha(if (!enabled) 0.5f else 1f)
            .clip(shape)
            .background(
                when {
                    variant == TabsVariant.Default && selected && isDark ->
                        palette.input.copy(alpha = palette.input.alpha * 0.30f)
                    variant == TabsVariant.Default && selected -> palette.background
                    else -> Color.Transparent
                },
                shape,
            )
            .muiFocusRing(interaction, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MuiText(text = item.label, fontSize = 14.sp, lineHeight = 20.sp, color = fg)
        if (variant == TabsVariant.Line && selected) {
            // line 变体选中线：横排画在字下、竖排画在右缘
            Box(
                Modifier
                    .padding(top = 2.dp)
                    .then(
                        if (orientation == TabsOrientation.Horizontal) {
                            Modifier.fillMaxWidth().height(2.dp)
                        } else {
                            Modifier.width(2.dp).height(16.dp)
                        }
                    )
                    .background(palette.foreground),
            )
        }
    }
}
