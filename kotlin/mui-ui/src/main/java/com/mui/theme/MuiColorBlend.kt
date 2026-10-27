package com.mui.theme

import androidx.compose.ui.graphics.Color

/**
 * 无色端点：「同色相 + 零透明度」。
 *
 * 为什么不能直接用 [Color.Transparent] 当动画的无色端点：
 * [androidx.compose.animation.animateColorAsState] 把颜色拆成 R/G/B/A 四个通道各自插值，
 * 而 `Color.Transparent` 的 RGB 其实是 (0, 0, 0) —— 它是「黑」，不是「无色」。
 * 于是「透明 → accent 淡青(#EBF6F4)」这段过渡里，中间帧的 RGB 被一路拉到接近黑：
 *
 *   白底合成实测：t=0.1 #E8E8E8 · t=0.3 #C8C9C8 · t=0.5 #BABDBC · t=0.7 #C0C5C4
 *   —— 整整一段纯灰（饱和度 0~3%），然后才收束到 #EBF6F4。
 *
 * 肉眼看到的就是「按下去先灰一下，然后才变成正常的淡青色」。
 *
 * 用 [faded] 表示「没有背景」：RGB 全程恒等于它将要变成的那个颜色，动画只走 alpha，
 * 视觉上是干净的淡入（#FFFFFF → #F5FAFA → #EBF6F4）。
 *
 * 约定：凡是要参与颜色动画的「透明」端点，写 `目标色.faded()`，不要写 `Color.Transparent`。
 * 不参与动画的纯装饰性透明（例如不绘制的 borderColor）可以继续用 `Color.Transparent`。
 */
fun Color.faded(): Color = copy(alpha = 0f)
