package com.mui.token

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/** 一层投影：偏移 / 模糊 / 颜色（spread 已按 shadow.css 的设计折进 blur）。 */
data class MuiShadowSpec(val dx: Dp, val dy: Dp, val blur: Dp, val color: Color)

/** 玻璃引擎的影子档（drop-shadow 语义：无 spread）。 */
data class GlassShadowSpec(val dx: Dp, val dy: Dp, val blur: Dp, val color: Color)

/** 由 tools/gen_kotlin_tokens.py 从 web/src/styles/tokens/shadow.css 生成，勿手改。 */
object MuiElevation {
    // 亮色：黑晕向下偏移 = 投影
    /** --elevation-2xs */
    val light2xs: List<MuiShadowSpec> = listOf(MuiShadowSpec(dx = 0.dp, dy = 1.dp, blur = 0.dp, color = Color(0x0D000000)))

    /** --elevation-xs */
    val lightXs: List<MuiShadowSpec> = listOf(MuiShadowSpec(dx = 0.dp, dy = 1.dp, blur = 2.dp, color = Color(0x0D000000)))

    /** --elevation-sm */
    val lightSm: List<MuiShadowSpec> = listOf(MuiShadowSpec(dx = 0.dp, dy = 1.dp, blur = 3.dp, color = Color(0x1A000000)), MuiShadowSpec(dx = 0.dp, dy = 1.dp, blur = 2.dp, color = Color(0x1A000000)))

    /** --elevation-md */
    val lightMd: List<MuiShadowSpec> = listOf(MuiShadowSpec(dx = 0.dp, dy = 4.dp, blur = 6.dp, color = Color(0x1A000000)), MuiShadowSpec(dx = 0.dp, dy = 2.dp, blur = 4.dp, color = Color(0x1A000000)))

    /** --elevation-lg */
    val lightLg: List<MuiShadowSpec> = listOf(MuiShadowSpec(dx = 0.dp, dy = 10.dp, blur = 15.dp, color = Color(0x1A000000)), MuiShadowSpec(dx = 0.dp, dy = 4.dp, blur = 6.dp, color = Color(0x1A000000)))

    /** --elevation-xl */
    val lightXl: List<MuiShadowSpec> = listOf(MuiShadowSpec(dx = 0.dp, dy = 20.dp, blur = 25.dp, color = Color(0x1A000000)), MuiShadowSpec(dx = 0.dp, dy = 8.dp, blur = 10.dp, color = Color(0x1A000000)))

    /** --elevation-2xl */
    val light2xl: List<MuiShadowSpec> = listOf(MuiShadowSpec(dx = 0.dp, dy = 25.dp, blur = 50.dp, color = Color(0x40000000)))

    // 暗色：零偏移白晕 = 边缘亮
    /** --elevation-2xs */
    val dark2xs: List<MuiShadowSpec> = listOf(MuiShadowSpec(dx = 0.dp, dy = 0.dp, blur = 1.dp, color = Color(0x12FFFFFF)))

    /** --elevation-xs */
    val darkXs: List<MuiShadowSpec> = listOf(MuiShadowSpec(dx = 0.dp, dy = 0.dp, blur = 2.dp, color = Color(0x17FFFFFF)))

    /** --elevation-sm */
    val darkSm: List<MuiShadowSpec> = listOf(MuiShadowSpec(dx = 0.dp, dy = 0.dp, blur = 3.dp, color = Color(0x1CFFFFFF)))

    /** --elevation-md */
    val darkMd: List<MuiShadowSpec> = listOf(MuiShadowSpec(dx = 0.dp, dy = 0.dp, blur = 5.dp, color = Color(0x21FFFFFF)))

    /** --elevation-lg */
    val darkLg: List<MuiShadowSpec> = listOf(MuiShadowSpec(dx = 0.dp, dy = 0.dp, blur = 9.dp, color = Color(0x26FFFFFF)))

    /** --elevation-xl */
    val darkXl: List<MuiShadowSpec> = listOf(MuiShadowSpec(dx = 0.dp, dy = 0.dp, blur = 15.dp, color = Color(0x2BFFFFFF)))

    /** --elevation-2xl */
    val dark2xl: List<MuiShadowSpec> = listOf(MuiShadowSpec(dx = 0.dp, dy = 0.dp, blur = 25.dp, color = Color(0x33FFFFFF)))

    /** 玻璃影子档位：与 shadow.css 的 --shadow-glass-* 一一对应（亮色）。 */
    val glassShadowLight: Map<String, GlassShadowSpec> = mapOf(
        "xs" to GlassShadowSpec(dx = 0.dp, dy = 1.dp, blur = 2.dp, color = Color(0x0D000000)), "sm" to GlassShadowSpec(dx = 0.dp, dy = 1.dp, blur = 3.dp, color = Color(0x1A000000)), "md" to GlassShadowSpec(dx = 0.dp, dy = 4.dp, blur = 4.dp, color = Color(0x1A000000)), "lg" to GlassShadowSpec(dx = 0.dp, dy = 10.dp, blur = 9.dp, color = Color(0x1A000000)), "xl" to GlassShadowSpec(dx = 0.dp, dy = 20.dp, blur = 15.dp, color = Color(0x1A000000))
    )

    /** 玻璃影子档位（暗色：白晕零偏移）。 */
    val glassShadowDark: Map<String, GlassShadowSpec> = mapOf(
        "xs" to GlassShadowSpec(dx = 0.dp, dy = 0.dp, blur = 2.dp, color = Color(0x17FFFFFF)), "sm" to GlassShadowSpec(dx = 0.dp, dy = 0.dp, blur = 3.dp, color = Color(0x1FFFFFFF)), "md" to GlassShadowSpec(dx = 0.dp, dy = 0.dp, blur = 5.dp, color = Color(0x1FFFFFFF)), "lg" to GlassShadowSpec(dx = 0.dp, dy = 0.dp, blur = 9.dp, color = Color(0x1FFFFFFF)), "xl" to GlassShadowSpec(dx = 0.dp, dy = 0.dp, blur = 15.dp, color = Color(0x1FFFFFFF))
    )

    /** --shadow-glass-ink */
    val glassInkLight: Color = Color(0x1A000000)
    val glassInkDark: Color = Color(0x1FFFFFFF)
}
