package com.mui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.mui.glass.GlassTokens
import com.mui.token.MuiColors
import com.mui.token.MuiPalette

/** 当前主题的调色板（亮 = colors.css :root，暗 = .dark） */
val LocalMuiPalette = staticCompositionLocalOf<MuiPalette> { MuiColors.Light }

/** 当前主题的玻璃外观（glass.css 的主题差异化部分） */
val LocalGlassPalette = staticCompositionLocalOf { GlassTokens.Light }

/** 当前是否暗色主题（暗色专属规则的判据，如 input/80、destructive 环 40%） */
val LocalIsDark = staticCompositionLocalOf { false }

/** 当前内容前景色：容器类组件（按钮/徽标/警告条…）就地覆盖，MuiText 默认读它 */
val LocalMuiContentColor = staticCompositionLocalOf<Color> { Color.Unspecified }

/** 玻璃背面里「页面」的颜色：玻璃组件透过球折射它；容器可覆盖（默认退到 background） */
val LocalMuiPageColor = staticCompositionLocalOf<Color> { Color.Unspecified }

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
        // 玻璃的「背面」默认就是页面底色。少了这一行，玻璃的 backdrop 画的是
        // Color.Unspecified（= 什么都不画），整块玻璃在视觉上等于不存在。
        LocalMuiPageColor provides palette.background,
        content = content,
    )
}
