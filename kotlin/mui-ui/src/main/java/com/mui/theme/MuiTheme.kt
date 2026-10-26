package com.mui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.mui.glass.GlassTokens
import com.mui.token.MuiColors
import com.mui.token.MuiPalette

/** 当前主题的调色板（亮 = colors.css :root，暗 = .dark） */
val LocalMuiPalette = staticCompositionLocalOf<MuiPalette> { MuiColors.Light }

/** 当前主题的玻璃外观（glass.css 的主题差异化部分） */
val LocalGlassPalette = staticCompositionLocalOf { GlassTokens.Light }

/** 当前是否暗色主题（暗色专属规则的判据，如 input/80、destructive 环 40%） */
val LocalIsDark = staticCompositionLocalOf { false }

/**
 * Mui 主题：只提供令牌，不包 MaterialTheme——组件全部读 [LocalMuiPalette]。
 *
 * @param darkTheme 是否暗色（默认跟随系统，对应 web 的 html.dark）
 */
@Composable
fun MuiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) MuiColors.Dark else MuiColors.Light
    val glass = if (darkTheme) GlassTokens.Dark else GlassTokens.Light
    CompositionLocalProvider(
        LocalMuiPalette provides palette,
        LocalGlassPalette provides glass,
        LocalIsDark provides darkTheme,
        content = content,
    )
}
