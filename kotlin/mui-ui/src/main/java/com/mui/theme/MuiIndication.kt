package com.mui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode

/**
 * Mui 的「无指示器」：什么都不画。
 *
 * ## 为什么库要主动提供它
 *
 * Compose Foundation 把 [androidx.compose.foundation.LocalIndication] 的默认值设成内部的
 * `DefaultDebugIndication`，它在按下时画的是：
 *
 * ```kotlin
 * override fun ContentDrawScope.draw() {
 *     drawContent()
 *     if (isPressed) drawRect(Color.Black.copy(alpha = 0.3f), size = size)
 *     else if (isHovered || isFocused) drawRect(Color.Black.copy(alpha = 0.1f), size = size)
 * }
 * ```
 *
 * 一块**铺满 bounds 的矩形** 30% 黑 —— 它不认识 shape、不裁圆角，只认矩形边界。于是任何
 * 没显式写 `indication = null` 的 `clickable` 按下去都会：
 *
 * 1. 底色被压暗一档（淡青 accent `#EBF6F4` 压 30% 黑 ≈ `#A4ACAB`，看起来就是「灰」）；
 * 2. 圆角之外露出一圈直角灰边（因为它只画矩形）。
 *
 * mui-ui 内部的 38 处 clickable 都显式写了 `indication = null`，所以组件本身不受影响；
 * 但**接入方的 app 代码**（卡片、整行、自写芯片…）只要用默认重载 `clickable(onClick = …)`
 * 就会踩到这块灰罩 —— 这正是「按钮按下去先变灰」最常见的真实来源。
 *
 * [MuiTheme] 在主题层统一 provides 本对象，接入方无需在自己的 Theme 里重复处理；
 * 若某处确实需要按压反馈，可在局部 `CompositionLocalProvider(LocalIndication provides …)` 覆盖回来。
 *
 * ## 实现说明
 *
 * 用 [IndicationNodeFactory] 而不是旧的 `rememberUpdatedInstance` / `IndicationInstance`
 * —— 后者在新版 foundation 里已是 DEPRECATION_ERROR 级别，无法实现。
 * `create` 必须每次返回**新**节点：`Modifier.Node` 自带 owner，不能跨布局节点复用实例。
 */
object MuiNoIndication : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode = EmptyIndicationNode()

    override fun hashCode(): Int = -1

    override fun equals(other: Any?): Boolean = other === this
}

/** 空节点：不测量、不绘制，只占住 indication 的位置。 */
private class EmptyIndicationNode : Modifier.Node()
