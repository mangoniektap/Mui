package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius

// TODO: web 的 tinted 用 oklch(from primary) 动态调色，这里以 mui100/mui800 令牌近似。
// TODO: web 的悬停变色（气泡为可点元素时）与 focus ring 未移植（内容为普通布局）。
// TODO: BubbleReactions 的绝对定位（浮出气泡外）未移植，仅提供浮标外观，位置由调用方摆。

/** 气泡外观档位（bubble.tsx 的 variant） */
enum class BubbleVariant { Default, Secondary, Muted, Tinted, Outline, Ghost, Destructive }

/** 浮标纵向侧位（bubble.tsx 的 reactions side） */
enum class BubbleReactionSide { Top, Bottom }

/** 当前气泡档位（BubbleContent 内部读取用） */
private val LocalMuiBubbleVariant = staticCompositionLocalOf { BubbleVariant.Default }

/** 气泡内容解析外观 */
private data class BubbleContentSurface(val background: Color, val foreground: Color, val showBorder: Boolean)

/**
 * 气泡组：纵向气泡列表段（gap-2）。
 *
 * @param modifier 布局修饰
 * @param content 组内容（建议若干 MuiBubble）
 */
@Composable
fun MuiBubbleGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/**
 * 气泡容器：最多占 80% 宽，End 对齐时靠右（web 的 Bubble）。
 * 输入 variant + align + 内容槽；输出定宽约束的气泡列。
 * 硬约束：宽度上限 80% 用 fillMaxWidth(0.8f)+wrapContentWidth 实现；Ghost 档不限宽。
 *
 * @param modifier 布局修饰
 * @param variant 外观档位（默认 Default）
 * @param align 对齐方向（默认 Start）
 * @param content 气泡内容（建议 MuiBubbleContent）
 */
@Composable
fun MuiBubble(
    modifier: Modifier = Modifier,
    variant: BubbleVariant = BubbleVariant.Default,
    align: MessageAlign = MessageAlign.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val widthLimit = if (variant == BubbleVariant.Ghost) Modifier else Modifier.fillMaxWidth(0.8f)
    Column(
        modifier = modifier
            .then(widthLimit)
            .wrapContentWidth(if (align == MessageAlign.End) Alignment.End else Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

/**
 * 气泡内容体：圆角 xl、px-3 py-2 的上色面（web 的 BubbleContent）。
 * 颜色按外层 MuiBubble 的 variant 解析；Ghost 档去底色/圆角/内边距。
 *
 * @param modifier 布局修饰
 * @param tail 是否画小尾巴（左下角 8dp 三角，颜色随底色）
 * @param content 气泡文本/内容（文字用 MuiText，颜色自动取内容色）
 */
@Composable
fun MuiBubbleContent(
    modifier: Modifier = Modifier,
    tail: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    val variant = LocalMuiBubbleVariant.current
    val surface = bubbleContentSurface(variant, palette, isDark)
    val ghost = variant == BubbleVariant.Ghost
    val shape = if (ghost) RoundedCornerShape(0.dp) else RoundedCornerShape(MuiRadius.xl)

    Box(
        modifier = modifier
            .background(surface.background, shape)
            .then(
                if (surface.showBorder) {
                    Modifier.border(1.dp, palette.border, shape)
                } else Modifier
            )
            .clip(shape)
            .then(
                if (tail && !ghost) {
                    Modifier.drawBehind {
                        val tailSize = 8.dp.toPx()
                        val inset = 12.dp.toPx()
                        val path = Path().apply {
                            moveTo(inset, size.height - 0.5f)
                            lineTo(inset + tailSize, size.height - 0.5f)
                            lineTo(inset, size.height + tailSize)
                            close()
                        }
                        drawPath(path, surface.background)
                    }
                } else Modifier
            )
            .then(if (ghost) Modifier else Modifier.padding(horizontal = 12.dp, vertical = 8.dp)),
    ) {
        CompositionLocalProvider(LocalMuiContentColor provides surface.foreground) {
            content()
        }
    }
}

/** variant × 主题 → 气泡面颜色 */
private fun bubbleContentSurface(
    variant: BubbleVariant,
    palette: com.mui.token.MuiPalette,
    isDark: Boolean,
): BubbleContentSurface {
    fun frac(c: Color, f: Float) = c.copy(alpha = c.alpha * f)
    return when (variant) {
        BubbleVariant.Default -> BubbleContentSurface(palette.primary, palette.primaryForeground, false)
        BubbleVariant.Secondary -> BubbleContentSurface(palette.secondary, palette.secondaryForeground, false)
        BubbleVariant.Muted -> BubbleContentSurface(palette.muted, palette.foreground, false)
        BubbleVariant.Tinted -> BubbleContentSurface(
            background = if (isDark) palette.mui800 else palette.mui100,
            foreground = palette.foreground,
            showBorder = false,
        )
        BubbleVariant.Outline -> BubbleContentSurface(palette.background, palette.foreground, true)
        BubbleVariant.Ghost -> BubbleContentSurface(Color.Transparent, palette.foreground, false)
        BubbleVariant.Destructive -> BubbleContentSurface(
            background = frac(palette.destructive, if (isDark) 0.20f else 0.10f),
            foreground = palette.destructive,
            showBorder = false,
        )
    }
}

/**
 * 气泡浮标：胶囊形小徽条（表情回应等；web 的 BubbleReactions）。
 * 硬约束：位置不自动浮出，调用方用 Box/offset 摆放。
 *
 * @param modifier 布局修饰（含 offset 等）
 * @param side 语义侧位（仅标记，Top/Bottom）
 * @param align 语义对位（仅标记，Start/End）
 * @param content 浮标内容（Row 作用域）
 */
@Composable
fun MuiBubbleReactions(
    modifier: Modifier = Modifier,
    side: BubbleReactionSide = BubbleReactionSide.Bottom,
    align: MessageAlign = MessageAlign.End,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val palette = LocalMuiPalette.current
    Row(
        modifier = modifier
            .background(palette.muted, CircleShape)
            .border(3.dp, palette.card, CircleShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}
