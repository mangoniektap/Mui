package com.mui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource

/**
 * N 格验证码输入（input-otp.tsx）：隐藏的 BasicTextField 承接真实键盘输入，
 * 前方摆 N 个 36dp 格子显示字符与光标槽；聚焦格 = border-ring + 3dp 环；点任意格聚焦输入。
 * 输入 = value/onValueChange（整串字符，最长 slotCount）；输出 = onValueChange。
 * 硬约束：禁 material3；视觉全走令牌。
 *
 * @param value 受控整串字符（超出 slotCount 的部分被截断）
 * @param onValueChange 整串变化回调
 * @param modifier 布局修饰
 * @param slotCount 格子数（默认 6）
 * @param enabled 可用性（false = 半透明不可输入）
 * @param invalid 校验失败态：边框与聚焦环转 destructive
 * @param interactionSource 交互源（隐藏输入框的聚焦状态出口）
 */
@Composable
fun MuiInputOtp(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    slotCount: Int = 6,
    enabled: Boolean = true,
    invalid: Boolean = false,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
) {
    MuiInputOtp(value, onValueChange, listOf(slotCount), modifier, enabled, invalid, interactionSource)
}

/** 分组验证码，组内格子相接，组间默认显示分隔符。 */
@Composable
fun MuiInputOtp(
    value: String,
    onValueChange: (String) -> Unit,
    groups: List<Int>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    invalid: Boolean = false,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    separator: @Composable () -> Unit = { MuiText("−", fontSize = 14.sp) },
) {
    val groupSizes = groups.filter { it > 0 }
    val slotCount = groupSizes.sum()
    if (slotCount == 0) return
    val invalidField = invalid || LocalMuiFormFieldInvalid.current
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    val focused by interactionSource.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }

    // 光标闪烁：web animate-caret-blink duration-1000
    val caretTransition = rememberInfiniteTransition(label = "otp-caret")
    val caretAlpha by caretTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "otp-caret-alpha",
    )

    // 活动格 = 光标所在位（聚焦时才有）
    val activeIndex = if (focused) value.length.coerceAtMost(slotCount - 1) else -1

    Row(
        modifier = modifier.alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        var start = 0
        groupSizes.forEachIndexed { group, count ->
            val first = start
            start += count
            if (group > 0) separator()
            Row(horizontalArrangement = Arrangement.spacedBy((-1).dp)) {
                repeat(count) { slot ->
                    val index = first + slot
                    val cellShape = RoundedCornerShape(
                        topStart = if (slot == 0) MuiRadius.md else 0.dp,
                        bottomStart = if (slot == 0) MuiRadius.md else 0.dp,
                        topEnd = if (slot == count - 1) MuiRadius.md else 0.dp,
                        bottomEnd = if (slot == count - 1) MuiRadius.md else 0.dp,
                    )
                    val isActive = index == activeIndex
                    val char = if (index < value.length) value[index].toString() else null
                    val borderColor = when {
                        invalidField -> palette.destructive
                        isActive -> palette.ring
                        else -> palette.input
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .zIndex(if (isActive) 1f else 0f)
                            .muiFocusRing(focused = isActive, shape = cellShape, invalid = invalidField)
                            .clip(cellShape)
                            .background(
                                if (isDark) palette.input.copy(alpha = palette.input.alpha * 0.30f) else Color.Transparent,
                                cellShape,
                            )
                            .border(1.dp, borderColor, cellShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = enabled,
                                onClick = { focusRequester.requestFocus() },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (char != null) {
                            MuiText(text = char, fontSize = 14.sp, lineHeight = 20.sp)
                        } else if (isActive) {
                            // 假光标：1×16dp 竖条，bg-foreground，闪烁
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(16.dp)
                                    .alpha(caretAlpha)
                                    .background(palette.foreground),
                            )
                        }
                    }
                }
            }
        }
    }

    // 隐藏的真实输入框：1dp 透明块，承接键盘输入与聚焦
    BasicTextField(
        value = value.take(slotCount),
        onValueChange = { next -> onValueChange(next.take(slotCount)) },
        modifier = Modifier
            .alpha(0f)
            .size(1.dp)
            .focusRequester(focusRequester),
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(fontSize = 1.sp, color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = keyboardOptions,
        interactionSource = interactionSource,
    )
}
