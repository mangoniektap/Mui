package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiPalette
import com.mui.theme.muiElevation
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource

/**
 * 单选组容器：行/列排布（web 的 grid gap-3 = 12dp）。
 * 输入 = 内容槽（每个条目用 MuiRadioGroupItem，配 MuiLabelRow 加标签）；
 * 输出 = 各条目自身的 onSelect 回调。硬约束：禁 material3；视觉全走令牌。
 * TODO: web 的键盘方向键在组内循环移动焦点未移植。
 *
 * @param modifier 布局修饰
 * @param orientation 排布方向（Vertical = 纵列 / Horizontal = 横行）
 * @param content 条目内容（每个条目一个 MuiRadioGroupItem）
 */
@Composable
fun MuiRadioGroup(
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Vertical,
    content: @Composable () -> Unit,
) {
    when (orientation) {
        Orientation.Vertical ->
            Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
        Orientation.Horizontal ->
            Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}

/**
 * 单选圆点：16dp 圆圈；选中 = 内实心 8dp 圆点 + border-primary；未选 = border-input 描边；
 * xs 阴影、聚焦 3dp 环、disabled 半透明。输入 = selected / onSelect；输出 = onSelect。
 *
 * @param selected 是否选中（选中画内圆点）
 * @param onSelect 点击回调（组内互斥由调用方维护）
 * @param modifier 布局修饰
 * @param disabled 禁用：半透明且不响应
 * @param invalid 校验失败态：边框与聚焦环转 destructive
 * @param interactionSource 交互源（聚焦环出口）
 */
@Composable
fun MuiRadioGroupItem(
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
    invalid: Boolean = false,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
) {
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    val shape = RoundedCornerShape(50)
    val dotColor = palette.primary
    val borderColor = when {
        invalid -> palette.destructive
        selected -> palette.primary
        else -> palette.input
    }
    val circleBackground = if (isDark) {
        palette.input.copy(alpha = palette.input.alpha * 0.30f)
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }

    Box(
        modifier = modifier
            .size(16.dp)
            .alpha(if (disabled) 0.5f else 1f)
            .clip(shape)
            .background(circleBackground, shape)
            .border(1.dp, borderColor, shape)
            .muiElevation("2xs", shape)
            .muiFocusRing(interactionSource, shape, invalid)
            .focusable(enabled = !disabled, interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !disabled,
                onClick = onSelect,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(dotColor, RoundedCornerShape(50)),
            )
        }
    }
}

/**
 * 单选条目 + 标签的一行组合（MuiLabelRow 风格：垂直居中、间距 8dp）。
 * 输入 = 选中态 / 回调 / 标签文本；输出 = onSelect。
 *
 * @param selected 是否选中
 * @param onSelect 点击回调（整行可点）
 * @param label 标签文本
 * @param modifier 布局修饰
 * @param disabled 禁用：半透明且不响应
 * @param interactionSource 交互源（圆点聚焦环出口）
 */
@Composable
fun MuiRadioLabelRow(
    selected: Boolean,
    onSelect: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
) {
    Row(
        modifier = modifier
            .alpha(if (disabled) 0.5f else 1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !disabled,
                onClick = onSelect,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MuiRadioGroupItem(
            selected = selected,
            onSelect = onSelect,
            disabled = disabled,
            interactionSource = interactionSource,
        )
        MuiLabel(text = label)
    }
}
