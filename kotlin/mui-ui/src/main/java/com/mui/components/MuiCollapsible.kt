package com.mui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged

/**
 * 折叠容器：单块内容的展开 / 收起（触发器槽 + 高度动画内容区）。
 * 输入 = 触发器槽 + 内容槽 + 展开态；输出 = 展开/收起回调。
 * 硬约束：无 ripple；高度动画 200ms 收起时裁剪。
 *
 * @param open 是否展开
 * @param onOpenChange 展开/收起回调（触发器点击时回调 !open）
 * @param trigger 触发器槽（任意内容，点击切换展开态）
 * @param modifier 布局修饰
 * @param content 折叠内容（展开高度动画；收起高度到 0 并裁剪）
 */
@Composable
fun MuiCollapsible(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    trigger: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    var contentHeight by remember { mutableIntStateOf(0) }
    val animatedHeight by animateIntAsState(
        targetValue = if (open) contentHeight else 0,
        animationSpec = tween(durationMillis = 200),
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) { onOpenChange(!open) },
        ) {
            trigger()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(constraints.maxWidth, animatedHeight.coerceAtMost(placeable.height)) {
                        placeable.placeRelative(0, 0)
                    }
                },
        ) {
            Box(
                modifier = Modifier.onSizeChanged { contentHeight = it.height },
            ) {
                content()
            }
        }
    }
}
