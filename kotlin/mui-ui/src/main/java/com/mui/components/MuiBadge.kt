package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.token.MuiPalette
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette

/** 徽标外观档位（badge.tsx 的 variant） */
enum class BadgeVariant { Default, Secondary, Destructive, Outline, Ghost, Link }

/**
 * 徽标：胶囊形、text-xs font-medium、单行、px-2 py-0.5。
 *
 * @param variant 外观档位（默认 Default）
 * @param content 内容（Row 作用域；文字用 MuiText，颜色自动取内容色）
 */
@Composable
fun MuiBadge(
    modifier: Modifier = Modifier,
    variant: BadgeVariant = BadgeVariant.Default,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    val surface = badgeSurface(variant, palette, isDark)
    val shape = RoundedCornerShape(50)

    Row(
        modifier = modifier
            .background(surface.background, shape)
            .then(
                if (variant == BadgeVariant.Outline) {
                    Modifier.border(1.dp, palette.border, shape)
                } else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CompositionLocalProvider(LocalMuiContentColor provides surface.foreground) {
            content()
        }
    }
}

/** 一档徽标的解析外观 */
private data class BadgeSurface(val background: Color, val foreground: Color)

/** variant × 主题 → 颜色（outline / ghost / link 底透明） */
private fun badgeSurface(variant: BadgeVariant, palette: MuiPalette, isDark: Boolean): BadgeSurface =
    when (variant) {
        BadgeVariant.Default -> BadgeSurface(palette.primary, palette.primaryForeground)
        BadgeVariant.Secondary -> BadgeSurface(palette.secondary, palette.secondaryForeground)
        BadgeVariant.Destructive -> BadgeSurface(
            background = if (isDark) palette.destructive.copy(alpha = palette.destructive.alpha * 0.60f) else palette.destructive,
            foreground = Color.White,
        )
        BadgeVariant.Outline -> BadgeSurface(Color.Transparent, palette.foreground)
        BadgeVariant.Ghost -> BadgeSurface(Color.Transparent, palette.foreground)
        BadgeVariant.Link -> BadgeSurface(Color.Transparent, palette.primary)
    }

/** 徽标文字样式：text-xs font-medium */
fun muiBadgeTextStyle(): androidx.compose.ui.text.TextStyle =
    androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp)
