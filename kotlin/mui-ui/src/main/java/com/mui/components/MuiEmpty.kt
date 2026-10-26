package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius

// TODO: web 的 border-dashed（无边宽，实际不可见）未画；链接下划线样式未移植。

/** 空状态媒体档位（empty.tsx 的 media variant） */
enum class EmptyMediaVariant { Default, Icon }

/**
 * 空状态：图标圈 + 标题 + 描述 + 动作的纵向居中布局。
 * 输入各槽位；输出居中的空态面板（圆角 lg、内边距 24dp、间距 24dp）。
 * 硬约束：全部纵向居中、文字居中；无可见边框（对应 web 的 dashed 无边宽）。
 *
 * @param modifier 布局修饰
 * @param content 空态内容（建议 EmptyMedia / EmptyHeader / EmptyContent）
 */
@Composable
fun MuiEmpty(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent, RoundedCornerShape(MuiRadius.lg))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        content = content,
    )
}

/**
 * 空状态头区：标题 + 描述的居中列。
 *
 * @param modifier 布局修饰
 * @param content 头区内容（建议 MuiEmptyTitle / MuiEmptyDescription）
 */
@Composable
fun MuiEmptyHeader(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/**
 * 空状态媒体：图标圈（Icon 档 = 40dp rounded-lg bg-muted）。
 *
 * @param modifier 布局修饰
 * @param variant 媒体档位（默认 Default = 透明 / Icon = 图标圈）
 * @param content 媒体内容（Canvas 图标或图片）
 */
@Composable
fun MuiEmptyMedia(
    modifier: Modifier = Modifier,
    variant: EmptyMediaVariant = EmptyMediaVariant.Default,
    content: @Composable () -> Unit,
) {
    val palette = LocalMuiPalette.current
    when (variant) {
        EmptyMediaVariant.Default -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            content()
        }
        EmptyMediaVariant.Icon -> Box(
            modifier = modifier
                .size(40.dp)
                .background(palette.muted, RoundedCornerShape(MuiRadius.lg)),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

/**
 * 空状态标题：text-lg font-medium。
 *
 * @param modifier 布局修饰
 * @param text 标题文本
 */
@Composable
fun MuiEmptyTitle(
    modifier: Modifier = Modifier,
    text: String,
) {
    MuiText(
        text = text,
        modifier = modifier,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
    )
}

/**
 * 空状态描述：text-sm muted-foreground，居中。
 *
 * @param modifier 布局修饰
 * @param text 描述文本
 */
@Composable
fun MuiEmptyDescription(
    modifier: Modifier = Modifier,
    text: String,
) {
    MuiText(
        text = text,
        modifier = modifier,
        color = LocalMuiPalette.current.mutedForeground,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        textAlign = TextAlign.Center,
    )
}

/**
 * 空状态动作区：标题下的动作列（按钮等）。
 *
 * @param modifier 布局修饰
 * @param content 动作内容（Column 作用域）
 */
@Composable
fun MuiEmptyContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalMuiContentColor provides LocalMuiPalette.current.foreground) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}
