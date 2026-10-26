package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource

// TODO: web 的 [a]:hover:bg-accent/50 只对链接生效；这里所有可点条目统一 hover。
// TODO: web 的 group-has（有描述时媒体上移对齐）未移植。
// TODO: web 的 Description 链接样式未移植。

/** 条目外观档位（item.tsx 的 variant） */
enum class ItemVariant { Default, Outline, Muted }

/** 条目尺寸档位（item.tsx 的 size） */
enum class ItemSize { Default, Sm }

/** 条目媒体档位（item.tsx 的 media variant） */
enum class ItemMediaVariant { Default, Icon, Image }

/**
 * 条目：媒体 + 内容 + 动作的横向三段布局（rounded-md、内边距 16dp）。
 * 输入 variant/size + 可点回调 + 内容槽；输出整行条目。
 * 硬约束：条目面 bg 透明 / outline 描边 / muted 半底；hover bg-accent/50。
 *
 * @param modifier 布局修饰
 * @param variant 外观档位（默认 Default）
 * @param size 尺寸档位（默认 Default）
 * @param onClick 点击回调（null = 不可点、无 hover 高亮）
 * @param interactionSource 交互源
 * @param content 条目内容（Row 作用域：MuiItemMedia / MuiItemContent / MuiItemActions）
 */
@Composable
fun MuiItem(
    modifier: Modifier = Modifier,
    variant: ItemVariant = ItemVariant.Default,
    size: ItemSize = ItemSize.Default,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
    content: @Composable RowScope.() -> Unit,
) {
    val palette = LocalMuiPalette.current
    val hovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(MuiRadius.md)

    fun frac(c: Color, f: Float) = c.copy(alpha = c.alpha * f)
    val baseBackground = when (variant) {
        ItemVariant.Default -> Color.Transparent
        ItemVariant.Outline -> Color.Transparent
        ItemVariant.Muted -> frac(palette.muted, 0.50f)
    }
    val background = if (hovered && onClick != null) frac(palette.accent, 0.50f) else baseBackground

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background, shape)
            .then(
                if (variant == ItemVariant.Outline) {
                    Modifier.border(1.dp, palette.border, shape)
                } else Modifier
            )
            .muiFocusRing(interactionSource, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else Modifier
            )
            .then(
                if (size == ItemSize.Default) {
                    Modifier.padding(16.dp)
                } else {
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (size == ItemSize.Default) 16.dp else 10.dp),
        content = content,
    )
}

/**
 * 条目媒体：图标/图片方位（web 的 ItemMedia）。
 *
 * @param modifier 布局修饰
 * @param variant 媒体档位（Icon = 32dp 方框 / Image = 40dp 裁剪方框 / Default = 透明）
 * @param content 媒体内容（Canvas 图标或图片组件）
 */
@Composable
fun MuiItemMedia(
    modifier: Modifier = Modifier,
    variant: ItemMediaVariant = ItemMediaVariant.Default,
    content: @Composable () -> Unit,
) {
    val palette = LocalMuiPalette.current
    when (variant) {
        ItemMediaVariant.Default -> Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
        ItemMediaVariant.Icon -> Box(
            modifier = modifier
                .size(32.dp)
                .background(palette.muted, RoundedCornerShape(6.dp))
                .border(1.dp, palette.border, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
        ItemMediaVariant.Image -> Box(
            modifier = modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/**
 * 条目内容列：标题 + 描述的纵向主体（占满剩余宽）。
 *
 * @param modifier 布局修饰
 * @param content 内容（建议 MuiItemTitle / MuiItemDescription）
 */
@Composable
fun MuiItemContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

/**
 * 条目标题：text-sm font-medium 行。
 *
 * @param modifier 布局修饰
 * @param text 标题文本
 */
@Composable
fun MuiItemTitle(
    modifier: Modifier = Modifier,
    text: String,
) {
    MuiText(
        text = text,
        modifier = modifier,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    )
}

/**
 * 条目描述：text-sm muted-foreground，最多两行。
 *
 * @param modifier 布局修饰
 * @param text 描述文本
 */
@Composable
fun MuiItemDescription(
    modifier: Modifier = Modifier,
    text: String,
) {
    MuiText(
        text = text,
        modifier = modifier,
        color = LocalMuiPalette.current.mutedForeground,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        maxLines = 2,
    )
}

/**
 * 条目动作区：贴条目右侧的按钮排。
 *
 * @param modifier 布局修饰
 * @param content 动作内容（Row 作用域：若干 MuiButton）
 */
@Composable
fun MuiItemActions(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/**
 * 条目头：占满一整行的顶栏（标题/动作两端对齐）。
 *
 * @param modifier 布局修饰
 * @param content 头部内容
 */
@Composable
fun MuiItemHeader(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        content = content,
    )
}

/**
 * 条目尾：占满一整行的底栏（两端对齐）。
 *
 * @param modifier 布局修饰
 * @param content 尾部内容
 */
@Composable
fun MuiItemFooter(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        content = content,
    )
}

/**
 * 条目组：纵向条目列表容器。
 *
 * @param modifier 布局修饰
 * @param content 组内容（若干 MuiItem / MuiItemSeparator）
 */
@Composable
fun MuiItemGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), content = content)
}

/**
 * 条目分隔线：横向 1dp --border 线。
 *
 * @param modifier 布局修饰
 */
@Composable
fun MuiItemSeparator(
    modifier: Modifier = Modifier,
) {
    MuiSeparator(modifier = modifier, orientation = Orientation.Horizontal)
}
