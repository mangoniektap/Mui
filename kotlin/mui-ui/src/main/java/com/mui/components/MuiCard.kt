package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius
import com.mui.theme.muiElevation

/**
 * 卡片：rounded-xl、纸面 --card、sm 阴影、内部纵向排布（gap-6 py-6）。
 *
 * @param modifier 布局修饰（宽度等由调用方给）
 * @param content 卡片内容（Column 作用域：CardHeader/CardTitle/CardContent/CardFooter）
 */
@Composable
fun MuiCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalMuiPalette.current
    val shape = RoundedCornerShape(MuiRadius.xl)
    Column(
        modifier = modifier
            .background(palette.card, shape)
            .border(1.dp, palette.border, shape)
            .muiElevation("sm", shape)
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        content = content,
    )
}

/**
 * 卡片头区：标题 + 描述 + 右侧动作，水平 24dp 内边距。
 *
 * @param content 头区内容（建议 CardTitle / CardDescription / CardAction 组合）
 */
@Composable
fun MuiCardHeader(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        content()
    }
}

/** 卡片标题：font-semibold leading-none */
@Composable
fun MuiCardTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    MuiText(
        text = text,
        modifier = modifier,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
    )
}

/** 卡片描述：text-sm muted-foreground */
@Composable
fun MuiCardDescription(
    text: String,
    modifier: Modifier = Modifier,
) {
    MuiText(
        text = text,
        modifier = modifier,
        color = LocalMuiPalette.current.mutedForeground,
    )
}

/** 卡片右上动作区：贴头区右侧 */
@Composable
fun MuiCardAction(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        content = content,
    )
}

/** 卡片正文：水平 24dp 内边距 */
@Composable
fun MuiCardContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalMuiContentColor provides LocalMuiPalette.current.cardForeground) {
        Column(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp), content = content)
    }
}

/** 卡片底区：水平 24dp 内边距 */
@Composable
fun MuiCardFooter(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
