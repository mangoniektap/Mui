package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius
import com.mui.theme.muiElevation
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource

/**
 * 多行文本域：classical 表面同 Input、min-h-16（64dp）起、px-3 py-2。
 *
 * @param value 受控文本
 * @param onValueChange 文本变化回调
 * @param enabled 可用性（false = 半透明不可输入）
 * @param invalid 校验失败态：边框与聚焦环转 destructive
 * @param placeholder 占位文本（null = 无）
 * @param minHeight 最小高度（默认 64dp = min-h-16）
 * @param interactionSource 交互源
 */
@Composable
fun MuiTextarea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    invalid: Boolean = false,
    placeholder: String? = null,
    minHeight: androidx.compose.ui.unit.Dp = 64.dp,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
) {
    val palette = LocalMuiPalette.current
    val shape = RoundedCornerShape(MuiRadius.md)
    val borderColor = if (invalid) palette.destructive else palette.input

    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .background(Color.Transparent, shape)
            .border(1.dp, borderColor, shape)
            .muiElevation("xs", shape)
            .muiFocusRing(interactionSource, shape, invalid)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            textStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = palette.foreground),
            cursorBrush = SolidColor(palette.primary),
            interactionSource = interactionSource,
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty() && placeholder != null) {
                        MuiText(text = placeholder, color = palette.mutedForeground)
                    }
                    inner()
                }
            },
        )
    }
}
