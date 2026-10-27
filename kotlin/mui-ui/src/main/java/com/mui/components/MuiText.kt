package com.mui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.mui.token.MuiTypography
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette

/** 容器继承给 MuiText 的默认文字样式；显式参数优先。 */
internal val LocalMuiTextStyle = staticCompositionLocalOf { TextStyle.Default }

/**
 * Mui 文本基元：全库文本统一入口（BasicText 封装）。
 *
 * @param text 文本内容
 * @param color 文字颜色（Unspecified = 继承容器内容色，再退到 foreground）
 * @param fontSize 字号（Unspecified = TextStyle 决定）
 * @param fontWeight 字重（null = 常规）
 * @param fontFamily 字体族（默认 sans）
 * @param lineHeight 行高
 * @param letterSpacing 字距（Unspecified = 随字号）
 * @param maxLines 最大行数（默认不限）
 * @param overflow 溢出处理（默认裁剪）
 * @param textAlign 对齐方式（null = 起点对齐）
 * @param textDecoration 划线样式（null = 无）
 */
@Composable
fun MuiText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = MuiTypography.sans,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
    textDecoration: TextDecoration? = null,
) {
    val contentColor = LocalMuiContentColor.current
    val effectiveColor = when {
        color != Color.Unspecified -> color
        contentColor != Color.Unspecified -> contentColor
        else -> LocalMuiPalette.current.foreground
    }
    BasicText(
        text = text,
        modifier = modifier,
        style = LocalMuiTextStyle.current.merge(
            TextStyle(
                    color = effectiveColor,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    fontFamily = fontFamily,
                    lineHeight = lineHeight,
                    letterSpacing = letterSpacing,
                    textAlign = textAlign ?: TextAlign.Unspecified,
                    textDecoration = textDecoration,
                ),
            ),
        maxLines = maxLines,
        overflow = overflow,
    )
}
