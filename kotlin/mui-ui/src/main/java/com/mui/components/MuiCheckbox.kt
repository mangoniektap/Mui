package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius
import com.mui.theme.muiElevation
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource

/**
 * 复选框：16dp 圆角 sm 方框、border-input、xs 阴影；选中 = bg-primary + ✓、
 * indeterminate = 中横线；聚焦 3dp 环；disabled 半透明。
 * 输入 = checked / defaultChecked / onCheckedChange（受控非受控与 web 对齐）；输出 = 状态变化回调。
 * 硬约束：禁 material3；视觉全走令牌。
 * TODO: web 的圆角是 rounded-[4px]（任意值），本实现按任务规范走 MuiRadius.sm。
 *
 * @param checked 受控状态（null = 非受控，走 defaultChecked）
 * @param onCheckedChange 状态变化回调（null = 只读展示）
 * @param modifier 布局修饰
 * @param defaultChecked 初始状态（非受控时用）
 * @param indeterminate 半选态：中横线（优先于 checked 的 ✓）
 * @param disabled 禁用：半透明且不响应
 * @param invalid 校验失败态：边框与聚焦环转 destructive
 * @param interactionSource 交互源（聚焦环出口）
 */
@Composable
fun MuiCheckbox(
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    defaultChecked: Boolean = false,
    indeterminate: Boolean = false,
    disabled: Boolean = false,
    invalid: Boolean = false,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
) {
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    var inner by remember { mutableStateOf(defaultChecked) }
    val isChecked = checked ?: inner
    val shape = RoundedCornerShape(MuiRadius.sm)

    val background = when {
        isChecked || indeterminate -> palette.primary
        isDark -> palette.input.copy(alpha = palette.input.alpha * 0.30f)
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val borderColor = when {
        invalid -> palette.destructive
        isChecked || indeterminate -> palette.primary
        else -> palette.input
    }
    val markColor = palette.primaryForeground

    Box(
        modifier = modifier
            .size(16.dp)
            .alpha(if (disabled) 0.5f else 1f)
            .clip(shape)
            .background(background, shape)
            .border(1.dp, borderColor, shape)
            .muiElevation("2xs", shape)
            .muiFocusRing(interactionSource, shape, invalid)
            .focusable(enabled = !disabled && onCheckedChange != null, interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !disabled && onCheckedChange != null,
                onClick = {
                    val next = !isChecked
                    if (checked == null) inner = next
                    onCheckedChange?.invoke(next)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
            when {
                indeterminate -> drawIndeterminateMark(markColor)
                isChecked -> drawCheckMark(markColor)
            }
        }
    }
}

/** ✓：两段折线（左中 → 下谷 → 右上），圆头 1.5dp 描边 */
private fun DrawScope.drawCheckMark(color: androidx.compose.ui.graphics.Color) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(w * 0.14f, h * 0.52f)
        lineTo(w * 0.42f, h * 0.80f)
        lineTo(w * 0.88f, h * 0.22f)
    }
    drawPath(path, color, style = stroke)
}

/** 中横线：半选态的横杠（占框高一半） */
private fun DrawScope.drawIndeterminateMark(color: androidx.compose.ui.graphics.Color) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(
        color = color,
        start = Offset(w * 0.15f, h / 2f),
        end = Offset(w * 0.85f, h / 2f),
        strokeWidth = stroke.width,
        cap = StrokeCap.Round,
    )
}
