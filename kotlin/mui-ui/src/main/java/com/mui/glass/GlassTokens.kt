package com.mui.glass

import androidx.compose.ui.graphics.Color

/** 一个主题下的玻璃外观（颜色 + 明暗差异化的边缘强度）。 */
data class GlassPalette(
    /** --glass-body-color 灰本体的颜色 */
    val bodyColor: Color,
    /** --glass-gray-color 平灰档的灰 */
    val grayColor: Color,
    /** --glass-rim-light 顶/底高光颜色 */
    val rimLight: Color,
    /** --glass-rim-dark 两侧暗边颜色 */
    val rimDark: Color,
    /** --glass-rim-top 顶边高光强度 */
    val rimTop: Float,
    /** --glass-rim-bottom 底边高光强度 */
    val rimBottom: Float,
    /** --glass-rim-side 两侧暗边强度 */
    val rimSide: Float,
)

/** 由 tools/gen_kotlin_tokens.py 从 web/src/styles/tokens/glass.css 生成，勿手改。 */
object GlassTokens {
    val Light = GlassPalette(
        bodyColor = Color(0xFF717171),
        grayColor = Color(0xFFC4C4C4),
        rimLight = Color(0xFFFFFFFF),
        rimDark = Color(0xFF555555),
        rimTop = 0.45f,
        rimBottom = 0.22f,
        rimSide = 0.4f,
    )

    val Dark = GlassPalette(
        bodyColor = Color(0xFF4D4D4D),
        grayColor = Color(0xFF333333),
        rimLight = Color(0xFFEBEBEB),
        rimDark = Color(0xFF000000),
        rimTop = 0.6f,
        rimBottom = 0.35f,
        rimSide = 0.4f,
    )

    /** --glass-body-shade 透明档最厚处的浓度 0..1 */
    const val bodyShade = 0.24f
    /** --glass-body-peak 最厚处的纵向位置 0..1（0 = 顶） */
    const val bodyPeak = 0.22f
    /** --glass-body-fade-x 左右衰减量 */
    const val bodyFadeX = 0.35f
    /** --glass-gray-alpha 平灰档不透明度 */
    const val grayAlpha = 0.5f
    /** --glass-rim-width 边缘窗口宽度（dp） */
    const val rimWidthDp = 2f
    /** --glass-lens-band 斜面宽度系数 */
    const val lensBand = 0.45f
    /** --glass-lens-bend 静止态放大率 - 1 */
    const val lensBend = 0.33f
    /** --glass-lens-compress 按压压缩率 */
    const val lensCompress = 0.3f
    /** --glass-lens-chroma 色散总宽度（dp，像素量非倍率） */
    const val lensChromaDp = 3f
    /** --glass-press-swell 按压外鼓系数 */
    const val pressSwell = 0.3f
    /** --glass-press-duration 按压动画时长（毫秒） */
    const val pressDurationMs = 150f
    /** --glass-body-duration 换档动画时长（毫秒） */
    const val bodyDurationMs = 200f
}
