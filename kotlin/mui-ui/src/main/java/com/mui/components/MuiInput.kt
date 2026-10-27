package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius
import com.mui.theme.muiElevation
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource

/**
 * 输入框：classical 表面（边框 --input / 底透明 / 暗色 bg-input/30 / xs 阴影）、
 * h-9 rounded-md px-3、聚焦 3dp 环、placeholder = muted-foreground。
 *
 * @param value 受控文本
 * @param onValueChange 文本变化回调
 * @param enabled 可用性（false = 半透明不可输入）
 * @param invalid 校验失败态：边框与聚焦环转 destructive
 * @param placeholder 占位文本（null = 无）
 * @param visualTransformation 文本变换（密码等，默认原样）
 * @param singleLine 是否单行（默认 true）
 * @param keyboardOptions 键盘类型/输入法动作（数字键盘等；web 侧对应 inputMode / type）
 * @param interactionSource 交互源
 * @param decorationBoxSuffix 输入区右侧附加内容槽位（图标等，紧跟文本）
 */
@Composable
fun MuiInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    invalid: Boolean = false,
    placeholder: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
    content: @Composable () -> Unit = {},
) {
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    val shape = RoundedCornerShape(MuiRadius.md)
    val borderColor = if (invalid) palette.destructive else palette.input
    val controlBackground = if (isDark) {
        palette.input.copy(alpha = palette.input.alpha * 0.30f)
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }

    Box(
        modifier = modifier
            .height(36.dp)
            .background(controlBackground, shape)
            .border(1.dp, borderColor, shape)
            .muiElevation("xs", shape)
            .muiFocusRing(interactionSource, shape, invalid)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            textStyle = TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = palette.foreground,
            ),
            cursorBrush = SolidColor(palette.primary),
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            decorationBox = { inner ->
                CompositionLocalProvider(LocalMuiContentColor provides palette.mutedForeground) {
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty() && placeholder != null) {
                                MuiText(text = placeholder, color = palette.mutedForeground)
                            }
                            inner()
                        }
                        if (value.isNotEmpty()) content()
                    }
                }
            },
        )
    }
}
