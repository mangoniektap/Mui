package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius
import com.mui.theme.rememberMuiInteractionSource

// TODO: web 的 AttachmentTrigger（整条覆盖的透明按钮层）未移植，用 onClick 参数替代。
// TODO: web 的按子件有无切换内边距（has-data-slot 条件 padding）简化为统一内边距。
// TODO: web 的 uploading/processing shimmer 动画未移植。

/** 附件状态（attachment.tsx 的 state） */
enum class AttachmentState { Idle, Uploading, Processing, Error, Done }

/** 附件尺寸档位（attachment.tsx 的 size） */
enum class AttachmentSize { Default, Sm, Xs }

/** 附件媒体档位（attachment.tsx 的 media variant） */
enum class AttachmentMediaVariant { Icon, Image }

/** 附件内部上下文（子件读取用） */
private val LocalMuiAttachmentSize = staticCompositionLocalOf { AttachmentSize.Default }
private val LocalMuiAttachmentState = staticCompositionLocalOf { AttachmentState.Done }
private val LocalMuiAttachmentOrientation = staticCompositionLocalOf { Orientation.Horizontal }

/**
 * 附件条：文件图标 + 名字 + 大小 + 移除钮的卡片面小块（rounded-xl bg-card border）。
 * 输入 state/size/orientation + 可点回调 + 内容槽；输出附件条。
 * 硬约束：Error 态描边 destructive/30；Idle 态虚线描边；hover（可点时）bg-muted/50。
 *
 * @param modifier 布局修饰
 * @param state 附件状态（默认 Done）
 * @param size 尺寸档位（默认 Default）
 * @param orientation 排布方向（Horizontal = 横条 / Vertical = 竖卡）
 * @param onClick 点击回调（null = 不可点、无 hover 高亮）
 * @param interactionSource 交互源
 * @param content 附件内容（Row/Column 作用域：MuiAttachmentMedia 等）
 */
@Composable
fun MuiAttachment(
    modifier: Modifier = Modifier,
    state: AttachmentState = AttachmentState.Done,
    size: AttachmentSize = AttachmentSize.Default,
    orientation: Orientation = Orientation.Horizontal,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
    content: @Composable () -> Unit,
) {
    val palette = LocalMuiPalette.current
    val hovered by interactionSource.collectIsHoveredAsState()
    val radius = if (size == AttachmentSize.Xs) MuiRadius.lg else MuiRadius.xl
    val shape = RoundedCornerShape(radius)

    fun frac(c: Color, f: Float) = c.copy(alpha = c.alpha * f)
    val borderColor = if (state == AttachmentState.Error) frac(palette.destructive, 0.30f) else palette.border
    val background = when {
        hovered && onClick != null -> frac(palette.muted, 0.50f)
        else -> palette.card
    }

    val borderModifier = if (state == AttachmentState.Idle) {
        Modifier.drawBehind {
            drawRoundRect(
                color = borderColor,
                cornerRadius = CornerRadius(radius.value * density, radius.value * density),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                ),
            )
        }
    } else {
        Modifier.border(1.dp, borderColor, shape)
    }

    val layoutModifier = when (orientation) {
        // 横条
        Orientation.Horizontal -> Modifier
            .widthIn(min = 160.dp)
            .background(background, shape)
            .clip(shape)
        // 竖卡
        Orientation.Vertical -> Modifier
            .width(96.dp)
            .background(background, shape)
            .clip(shape)
    }

    CompositionLocalProvider(
        LocalMuiAttachmentSize provides size,
        LocalMuiAttachmentState provides state,
        LocalMuiAttachmentOrientation provides orientation,
        LocalMuiContentColor provides palette.cardForeground,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = modifier
                .then(layoutModifier)
                .then(borderModifier)
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
                    when (size) {
                        AttachmentSize.Default -> Modifier.padding(8.dp)
                        AttachmentSize.Sm -> Modifier.padding(6.dp)
                        AttachmentSize.Xs -> Modifier.padding(4.dp)
                    }
                ),
            contentAlignment = if (orientation == Orientation.Vertical) Alignment.TopCenter else Alignment.CenterStart,
        ) {
            if (orientation == Orientation.Vertical) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(gapOfAttachment(size)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { content() }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(gapOfAttachment(size)),
                ) { content() }
            }
        }
    }
}

/** 尺寸 → 条目间距（web 的 gap） */
private fun gapOfAttachment(size: AttachmentSize) = when (size) {
    AttachmentSize.Default -> 8.dp
    AttachmentSize.Sm -> 10.dp
    AttachmentSize.Xs -> 6.dp
}

/**
 * 附件媒体：方形图标/缩略图方位（error 态转 destructive 底）。
 *
 * @param modifier 布局修饰
 * @param variant 媒体档位（仅标记：Image 由调用方放图片内容）
 * @param content 媒体内容（Canvas 文件图标或图片）
 */
@Composable
fun MuiAttachmentMedia(
    modifier: Modifier = Modifier,
    variant: AttachmentMediaVariant = AttachmentMediaVariant.Icon,
    content: @Composable () -> Unit,
) {
    val palette = LocalMuiPalette.current
    val size = LocalMuiAttachmentSize.current
    val state = LocalMuiAttachmentState.current
    val orientation = LocalMuiAttachmentOrientation.current
    val isError = state == AttachmentState.Error

    fun frac(c: Color, f: Float) = c.copy(alpha = c.alpha * f)
    val edge = when (size) {
        AttachmentSize.Default -> 40.dp
        AttachmentSize.Sm -> 32.dp
        AttachmentSize.Xs -> 28.dp
    }
    val shape = RoundedCornerShape(if (size == AttachmentSize.Xs) MuiRadius.md else MuiRadius.lg)
    val mediaModifier = if (orientation == Orientation.Vertical) {
        Modifier.fillMaxWidth().aspectRatio(1f)
    } else {
        Modifier.size(edge)
    }
    CompositionLocalProvider(LocalMuiContentColor provides if (isError) palette.destructive else palette.foreground) {
        Box(
            modifier = modifier
                .then(mediaModifier)
                .background(
                    if (isError) frac(palette.destructive, 0.10f) else palette.muted,
                    shape,
                )
                .clip(shape),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/**
 * 附件内容列：标题 + 描述（占满剩余宽）。
 *
 * @param modifier 布局修饰
 * @param content 内容（建议 MuiAttachmentTitle / MuiAttachmentDescription）
 */
@Composable
fun MuiAttachmentContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content,
    )
}

/**
 * 附件标题：单行截断、font-medium（sm/xs 档 12sp，其余 14sp）。
 *
 * @param modifier 布局修饰
 * @param text 文件名
 */
@Composable
fun MuiAttachmentTitle(
    modifier: Modifier = Modifier,
    text: String,
) {
    val size = LocalMuiAttachmentSize.current
    MuiText(
        text = text,
        modifier = modifier.fillMaxWidth(),
        fontSize = if (size == AttachmentSize.Default) 14.sp else 12.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * 附件描述：12sp muted-foreground 单行截断（error 态转 destructive/80）。
 *
 * @param modifier 布局修饰
 * @param text 文件大小等描述
 */
@Composable
fun MuiAttachmentDescription(
    modifier: Modifier = Modifier,
    text: String,
) {
    val palette = LocalMuiPalette.current
    val state = LocalMuiAttachmentState.current
    fun frac(c: Color, f: Float) = c.copy(alpha = c.alpha * f)
    MuiText(
        text = text,
        modifier = modifier.fillMaxWidth(),
        color = if (state == AttachmentState.Error) frac(palette.destructive, 0.80f) else palette.mutedForeground,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * 附件动作区：右上按钮排（移除钮等）。
 *
 * @param modifier 布局修饰
 * @param content 动作内容（Row 作用域：若干 MuiAttachmentAction）
 */
@Composable
fun MuiAttachmentActions(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

/**
 * 附件动作钮：ghost 图标小钮（web 的 AttachmentAction = Button ghost icon-xs）。
 *
 * @param onClick 点击回调
 * @param modifier 布局修饰
 * @param content 钮内容（Canvas 图标，颜色取内容色）
 */
@Composable
fun MuiAttachmentAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    MuiButton(
        onClick = onClick,
        modifier = modifier,
        variant = ButtonVariant.Ghost,
        size = ButtonSize.IconXs,
        content = content,
    )
}

/**
 * 附件组：横向滚动条带（gap-3 py-1，snap 省略）。
 *
 * @param modifier 布局修饰
 * @param content 组内容（若干 MuiAttachment）
 */
@Composable
fun MuiAttachmentGroup(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}
