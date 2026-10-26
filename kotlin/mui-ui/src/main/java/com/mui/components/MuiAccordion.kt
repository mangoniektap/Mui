package com.mui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource

// TODO: web 的手风琴互斥展开（type="single"/"multiple" 语义）由调用方自管 open 状态实现

/**
 * 手风琴：可展开收起的列表项容器（边框分隔 + chevron 旋转 + 高度动画 + 聚焦环）。
 * 输入 = 标题 + 展开态 + 内容槽；输出 = 展开/收起回调。
 * 硬约束：无 ripple；高度动画 200ms；视觉值全走令牌。
 *
 * @param modifier 布局修饰
 * @param content 手风琴内容（Column 作用域：排布 MuiAccordionItem）
 */
@Composable
fun MuiAccordion(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier, content = content)
}

/**
 * 手风琴项：底部 1dp 分隔线（末项可关），触发器行 py-4 text-sm font-medium，
 * chevron-down 16dp muted-foreground 展开 rotate 180°，内容高度动画展开收起。
 *
 * @param title 触发器标题
 * @param open 是否展开
 * @param onOpenChange 展开/收起回调
 * @param modifier 布局修饰
 * @param showBottomBorder 底部分隔线（最后一项传 false 对应 web 的 last:border-b-0）
 * @param content 展开后的内容（收起时高度动画到 0 并裁剪）
 */
@Composable
fun MuiAccordionItem(
    title: String,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showBottomBorder: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalMuiPalette.current
    val interactionSource = rememberMuiInteractionSource()
    val hovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(MuiRadius.md)

    val chevronRotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
    )

    var contentHeight by remember { mutableIntStateOf(0) }
    val animatedHeight by animateIntAsState(
        targetValue = if (open) contentHeight else 0,
        animationSpec = tween(durationMillis = 200),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                if (showBottomBorder) {
                    drawLine(
                        color = palette.border,
                        start = Offset(0f, size.height - 0.5f),
                        end = Offset(size.width, size.height - 0.5f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = shape)
                .muiFocusRing(interactionSource, shape)
                .focusable(interactionSource = interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) { onOpenChange(!open) }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
        ) {
            MuiText(
                text = title,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = if (hovered) TextDecoration.Underline else null,
            )
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .rotate(chevronRotation),
            ) {
                MenuChevronIcon(direction = MenuIconDirection.Down, color = palette.mutedForeground)
            }
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
                modifier = Modifier
                    .onSizeChanged { contentHeight = it.height }
                    .padding(bottom = 16.dp),
            ) {
                Column(content = content)
            }
        }
    }
}
