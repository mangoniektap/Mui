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
 * 前方摆 N 个 32dp 圆角 sm 格子显示字符与光标槽；聚焦格 = border-ring + 3dp 环；点任意格聚焦输入。
 * 输入 = value/onValueChange（整串字符，最长 slotCount）；输出 = onValueChange。
 * 硬约束：禁 material3；视觉全走令牌。
 * TODO: web 的 InputOTPGroup / InputOTPSeparator（分段加分隔线）未移植，格间统一 gap-2。
 * TODO: web 的光标闪烁走 CSS animate-caret-blink（阶梯 steps），此处用 Reverse 往返近似。
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
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    val focused by interactionSource.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    val cellShape = RoundedCornerShape(MuiRadius.sm)

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
        repeat(slotCount) { index ->
            val isActive = index == activeIndex
            val char = if (index < value.length) value[index].toString() else null
            val borderColor = when {
                isActive && invalid -> palette.destructive
                isActive -> palette.ring
                else -> palette.input
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(cellShape)
                    .background(
                        if (isDark) palette.input.copy(alpha = palette.input.alpha * 0.30f) else Color.Transparent,
                        cellShape,
                    )
                    .border(1.dp, borderColor, cellShape)
                    .then(
                        if (isActive) {
                            Modifier.muiFocusRing(focused = true, shape = cellShape, invalid = invalid)
                        } else Modifier
                    )
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
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        interactionSource = interactionSource,
    )
}
