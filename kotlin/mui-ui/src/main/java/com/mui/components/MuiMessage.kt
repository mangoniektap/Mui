package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette

// TODO: web 的 group-has 前缀（footer 出现时头像上移、ghost 消息去内边距）未移植。
// TODO: web 的 flex-row-reverse 用显式 avatar/content 槽位顺序替代。

/** 消息对齐方向（message.tsx 的 align；Start = 左侧 / End = 右侧） */
enum class MessageAlign { Start, End }

/** 当前消息对齐方向（Header/Footer 内部读取用） */
val LocalMuiMessageAlign = staticCompositionLocalOf { MessageAlign.Start }

/**
 * 消息组：纵向消息列表段（gap-2）。
 *
 * @param modifier 布局修饰
 * @param content 组内容（建议若干 MuiMessage）
 */
@Composable
fun MuiMessageGroup(
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
 * 消息行：头像 + 内容列，End 时头像在右、内容右对齐（聊天气泡布局骨架）。
 * 输入 align + 头像槽 + 内容槽；输出整行消息。
 * 硬约束：头像贴行底（self-end）；行内间距 8dp。
 *
 * @param modifier 布局修饰
 * @param align 对齐方向（默认 Start）
 * @param avatar 头像槽（建议 MuiMessageAvatar；null = 无头像）
 * @param content 消息内容列（MessageHeader / MuiBubble / MessageFooter 等）
 */
@Composable
fun MuiMessage(
    modifier: Modifier = Modifier,
    align: MessageAlign = MessageAlign.Start,
    avatar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalMuiMessageAlign provides align) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (align == MessageAlign.Start && avatar != null) {
                Box(Modifier.align(Alignment.Bottom)) { avatar() }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = if (align == MessageAlign.End) Alignment.End else Alignment.Start,
                content = content,
            )
            if (align == MessageAlign.End && avatar != null) {
                Box(Modifier.align(Alignment.Bottom)) { avatar() }
            }
        }
    }
}

/**
 * 消息头像：圆形 bg-muted 底座（web 的 MessageAvatar）。
 *
 * @param modifier 布局修饰（尺寸由调用方给，默认最小 32dp 见方）
 * @param content 头像内容（文字/Canvas 图形）
 */
@Composable
fun MuiMessageAvatar(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
            .background(LocalMuiPalette.current.muted, CircleShape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * 消息头：小字说明行（px-3 text-xs font-medium muted-foreground）。
 *
 * @param modifier 布局修饰
 * @param text 文本（null = 用 content 槽）
 * @param content 自定义内容槽
 */
@Composable
fun MuiMessageHeader(
    modifier: Modifier = Modifier,
    text: String? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val palette = LocalMuiPalette.current
    CompositionLocalProvider(LocalMuiContentColor provides palette.mutedForeground) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (text != null) MuiText(text = text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            content?.invoke()
        }
    }
}

/**
 * 消息尾：小字时间戳/状态行（End 对齐时整行靠右）。
 *
 * @param modifier 布局修饰
 * @param text 文本（null = 用 content 槽）
 * @param content 自定义内容槽
 */
@Composable
fun MuiMessageFooter(
    modifier: Modifier = Modifier,
    text: String? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val palette = LocalMuiPalette.current
    val align = LocalMuiMessageAlign.current
    CompositionLocalProvider(LocalMuiContentColor provides palette.mutedForeground) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (align == MessageAlign.End) Arrangement.End else Arrangement.Start,
        ) {
            if (text != null) MuiText(text = text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            content?.invoke()
        }
    }
}
