package com.mui.components

import androidx.compose.foundation.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.token.MuiPalette
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.theme.MuiRadius
import com.mui.theme.muiElevation
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource

/** 按钮外观档位（button.tsx 的 variant） */
enum class ButtonVariant { Default, Destructive, Outline, Secondary, Ghost, Link }

/** 按钮尺寸档位（button.tsx 的 size；Icon* 为方形图标钮） */
enum class ButtonSize { Default, Xs, Sm, Lg, Icon, IconXs, IconSm, IconLg }

/**
 * 按钮：rounded-md、text-sm font-medium、无 ripple、聚焦 3dp 环。
 * 外观 = classical 表面配方（primary / outline / secondary / ghost / link）。
 *
 * @param onClick 点击回调
 * @param variant 外观档位（默认 Default）
 * @param size 尺寸档位（默认 Default）
 * @param enabled 可用性（false = 半透明且不可点）
 * @param invalid 校验失败态：聚焦环转 destructive
 * @param interactionSource 交互源（按压/悬停/聚焦状态出口）
 * @param content 按钮内容（Row 作用域；文字用 MuiText，颜色自动取内容色）
 */
@Composable
fun MuiButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Default,
    size: ButtonSize = ButtonSize.Default,
    enabled: Boolean = true,
    invalid: Boolean = false,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
    content: @Composable RowScope.() -> Unit,
) {
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    val surface = buttonSurface(variant, palette, isDark, hovered, pressed)
    val shape = RoundedCornerShape(MuiRadius.md)

    val sizeModifier = when (size) {
        ButtonSize.Default -> Modifier.defaultMinSize(minHeight = 36.dp).padding(horizontal = 16.dp)
        ButtonSize.Xs -> Modifier.defaultMinSize(minHeight = 24.dp).padding(horizontal = 8.dp)
        ButtonSize.Sm -> Modifier.defaultMinSize(minHeight = 32.dp).padding(horizontal = 12.dp)
        ButtonSize.Lg -> Modifier.defaultMinSize(minHeight = 40.dp).padding(horizontal = 24.dp)
        ButtonSize.Icon -> Modifier.size(36.dp)
        ButtonSize.IconXs -> Modifier.size(24.dp)
        ButtonSize.IconSm -> Modifier.size(32.dp)
        ButtonSize.IconLg -> Modifier.size(40.dp)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(surface.background)
            .then(if (surface.showBorder) Modifier.border(1.dp, surface.borderColor, shape) else Modifier)
            .then(if (variant == ButtonVariant.Outline) Modifier.muiElevation("xs", shape) else Modifier)
            .muiFocusRing(interactionSource, shape, invalid)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .then(sizeModifier)
            .then(if (!enabled) Modifier.alpha(0.5f) else Modifier),
    ) {
        CompositionLocalProvider(LocalMuiContentColor provides surface.foreground) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(gapOf(size)),
            ) {
                content()
            }
        }
    }
}

/** 一档按钮的解析外观 */
private data class ButtonSurface(
    val background: Color,
    val foreground: Color,
    val borderColor: Color,
    val showBorder: Boolean,
)

/** classical 配方：variant × 主题 × 悬停/按压 → 颜色 */
private fun buttonSurface(
    variant: ButtonVariant,
    palette: MuiPalette,
    isDark: Boolean,
    hovered: Boolean,
    pressed: Boolean,
): ButtonSurface {
    fun frac(c: Color, f: Float) = c.copy(alpha = c.alpha * f)
    val active = hovered || pressed
    return when (variant) {
        ButtonVariant.Default -> ButtonSurface(
            background = if (active) frac(palette.primary, 0.90f) else palette.primary,
            foreground = palette.primaryForeground,
            borderColor = Color.Transparent,
            showBorder = false,
        )
        ButtonVariant.Destructive -> ButtonSurface(
            background = if (isDark) {
                if (active) frac(frac(palette.destructive, 0.60f), 0.90f) else frac(palette.destructive, 0.60f)
            } else {
                if (active) frac(palette.destructive, 0.90f) else palette.destructive
            },
            foreground = Color.White,
            borderColor = Color.Transparent,
            showBorder = false,
        )
        ButtonVariant.Outline -> ButtonSurface(
            background = when {
                isDark && active -> frac(palette.input, 0.50f)
                isDark -> frac(palette.input, 0.30f)
                active -> palette.accent
                else -> palette.background
            },
            foreground = if (active) palette.accentForeground else palette.foreground,
            borderColor = if (isDark) palette.input else palette.border,
            showBorder = true,
        )
        ButtonVariant.Secondary -> ButtonSurface(
            background = if (active) frac(palette.secondary, 0.80f) else palette.secondary,
            foreground = palette.secondaryForeground,
            borderColor = Color.Transparent,
            showBorder = false,
        )
        ButtonVariant.Ghost -> ButtonSurface(
            background = when {
                active && isDark -> frac(palette.accent, 0.50f)
                active -> palette.accent
                else -> Color.Transparent
            },
            foreground = if (active) palette.accentForeground else palette.foreground,
            borderColor = Color.Transparent,
            showBorder = false,
        )
        ButtonVariant.Link -> ButtonSurface(
            background = Color.Transparent,
            foreground = palette.primary,
            borderColor = Color.Transparent,
            showBorder = false,
        )
    }
}

/** 每档的内容间距（web 的 gap-*） */
private fun gapOf(size: ButtonSize) = when (size) {
    ButtonSize.Xs -> 4.dp
    ButtonSize.Sm -> 6.dp
    else -> 8.dp
}

/** 按钮文字样式：text-sm（xs 档 text-xs）+ font-medium */
fun muiButtonTextStyle(size: ButtonSize): androidx.compose.ui.text.TextStyle =
    androidx.compose.ui.text.TextStyle(
        fontSize = if (size == ButtonSize.Xs) 12.sp else 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = if (size == ButtonSize.Xs) 16.sp else 20.sp,
    )
