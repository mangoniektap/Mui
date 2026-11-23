package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.style.TextAlign
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
 * 输入框组合（input-group.tsx 的 InputGroup）：前后置槽（图标/按钮/文本）包住输入区，
 * 共用一层边框/底色/阴影（边框合并为一层，内部输入不再带边框）；聚焦环画在容器上。
 * 输入 = value/onValueChange + 前后置槽；输出 = onValueChange。
 * 硬约束：禁 material3；视觉全走令牌。
 *
 * @param value 受控文本
 * @param onValueChange 文本变化回调
 * @param modifier 布局修饰
 * @param enabled 可用性（false = 半透明不可输入）
 * @param invalid 校验失败态：边框与聚焦环转 destructive
 * @param placeholder 占位文本（null = 无）
 * @param visualTransformation 文本变换（密码等，默认原样）
 * @param textAlign 输入区文字对齐（web 侧 `className="text-center"`；步进器的数字居中用它）
 * @param keyboardOptions 键盘类型/输入法动作（数字键盘等）
 * @param startAddon 前置槽（图标/文本，null = 无；自带左内边距 12dp）
 * @param endAddon 后置槽（图标/按钮，null = 无；自带右内边距 12dp）
 * @param interactionSource 交互源（输入框与容器共用，聚焦环出口）
 */
@Composable
fun MuiInputGroup(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    invalid: Boolean = false,
    placeholder: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textAlign: TextAlign = TextAlign.Start,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    startAddon: (@Composable RowScope.() -> Unit)? = null,
    endAddon: (@Composable RowScope.() -> Unit)? = null,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
) {
    MuiInputGroup(
        value, onValueChange, multiline = false, modifier = modifier, enabled = enabled, invalid = invalid,
        placeholder = placeholder, visualTransformation = visualTransformation, textAlign = textAlign,
        keyboardOptions = keyboardOptions, startAddon = startAddon, endAddon = endAddon,
        interactionSource = interactionSource,
    )
}

/** 单行或多行输入组，附加内容可放在输入区两侧或上下。 */
@Composable
fun MuiInputGroup(
    value: String,
    onValueChange: (String) -> Unit,
    multiline: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    invalid: Boolean = false,
    placeholder: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textAlign: TextAlign = TextAlign.Start,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    startAddon: (@Composable RowScope.() -> Unit)? = null,
    endAddon: (@Composable RowScope.() -> Unit)? = null,
    blockStartAddon: (@Composable RowScope.() -> Unit)? = null,
    blockEndAddon: (@Composable RowScope.() -> Unit)? = null,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
) {
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    val shape = RoundedCornerShape(MuiRadius.md)
    val isInvalid = invalid || LocalMuiFormFieldInvalid.current
    val borderColor = if (isInvalid) palette.destructive else palette.input
    val controlBackground = if (isDark) {
        palette.input.copy(alpha = palette.input.alpha * 0.30f)
    } else {
        Color.Transparent
    }

    val focusRequester = remember { FocusRequester() }
    val addonInteraction = rememberMuiInteractionSource()
    val submit = LocalMuiFormSubmit.current
    val focusField = Modifier.clickable(
        interactionSource = addonInteraction,
        indication = null,
        enabled = enabled,
        onClick = { focusRequester.requestFocus() },
    )
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .muiElevation("xs", shape)
            .muiFocusRing(interactionSource, shape, isInvalid)
            .clip(shape)
            .background(controlBackground, shape)
            .border(1.dp, borderColor, shape),
    ) {
        if (blockStartAddon != null) {
            MuiInputGroupAddon(
                Modifier.fillMaxWidth().then(focusField).padding(start = 12.dp, top = 12.dp, end = 12.dp),
                blockStartAddon,
            )
        }
        Row(
            Modifier.fillMaxWidth().heightIn(
                min = when {
                    multiline -> 64.dp
                    blockStartAddon != null || blockEndAddon != null -> 32.dp
                    else -> 36.dp
                },
            ),
            verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically,
        ) {
            if (startAddon != null) {
                MuiInputGroupAddon(focusField.padding(start = 12.dp, top = 6.dp, bottom = 6.dp), startAddon)
            }
            val fieldPadding = Modifier.padding(
                start = if (startAddon == null) 12.dp else 8.dp,
                end = if (endAddon == null) 12.dp else 8.dp,
                top = if (multiline) 8.dp else 0.dp,
                bottom = if (multiline) 8.dp else 0.dp,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).then(fieldPadding).focusRequester(focusRequester),
                enabled = enabled,
                singleLine = !multiline,
                keyboardOptions = keyboardOptions,
                keyboardActions = if (submit == null || multiline) KeyboardActions.Default
                    else KeyboardActions(onDone = { submit() }),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = palette.foreground,
                    textAlign = textAlign,
                ),
                cursorBrush = SolidColor(palette.primary),
                visualTransformation = visualTransformation,
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
            if (endAddon != null) {
                MuiInputGroupAddon(focusField.padding(end = 12.dp, top = 6.dp, bottom = 6.dp), endAddon)
            }
        }
        if (blockEndAddon != null) {
            MuiInputGroupAddon(
                Modifier.fillMaxWidth().then(focusField).padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                blockEndAddon,
            )
        }
    }
}

@Composable
private fun MuiInputGroupAddon(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    CompositionLocalProvider(LocalMuiContentColor provides LocalMuiPalette.current.mutedForeground) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/**
 * 输入组文本（InputGroupText）：text-sm text-muted-foreground 的槽内文本。
 *
 * @param text 文本内容
 * @param modifier 布局修饰
 */
@Composable
fun MuiInputGroupText(
    text: String,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalMuiContentColor provides LocalMuiPalette.current.mutedForeground) {
        MuiText(text = text, modifier = modifier, fontSize = 14.sp, lineHeight = 20.sp)
    }
}
