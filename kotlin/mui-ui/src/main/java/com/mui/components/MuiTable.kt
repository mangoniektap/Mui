package com.mui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.theme.rememberMuiInteractionSource

// TODO: web 的 TableFooter（bg-muted/50 顶线）未移植。
// TODO: web 的行扩展（aria-expanded）与 data-selected 由调用方以 selected 参数近似。
// TODO: 横向滚动容器（overflow-x-auto）由调用方包 MuiScrollArea 实现。

/**
 * 表格：语义表格容器（caption-bottom、text-sm）。
 * 输入表格件槽；输出整表列布局。
 * 硬约束：宽度占满；行列全部用 Row/Column 组合（无原生表格语义）。
 *
 * @param modifier 布局修饰
 * @param content 表内容（MuiTableHeader / MuiTableBody / MuiTableCaption）
 */
@Composable
fun MuiTable(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalMuiContentColor provides LocalMuiPalette.current.foreground) {
        Column(modifier = modifier.fillMaxWidth(), content = content)
    }
}

/**
 * 表头容器：内部行的底部分隔线（[&_tr]:border-b）。
 *
 * @param modifier 布局修饰
 * @param content 表头行（若干 MuiTableRow）
 */
@Composable
fun MuiTableHeader(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), content = content)
}

/**
 * 表体容器：行分隔由行自绘（最后一行传 showBorder = false）。
 *
 * @param modifier 布局修饰
 * @param content 表体行（若干 MuiTableRow）
 */
@Composable
fun MuiTableBody(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), content = content)
}

/**
 * 表行：横向单元格排，hover bg-muted/50，底部分隔 1dp。
 * 输入 selected/onClick + 单元格槽；输出整行。
 * 硬约束：底边线画在行外沿（border-b）；选中态 bg-muted 覆盖 hover。
 *
 * @param modifier 布局修饰
 * @param selected 选中态（bg-muted 常亮）
 * @param showBorder 是否画底部分隔线（表体最后一行传 false）
 * @param onClick 点击回调（null = 不可点、无 hover 高亮）
 * @param interactionSource 交互源
 * @param content 行内容（Row 作用域：MuiTableHead / MuiTableCell）
 */
@Composable
fun MuiTableRow(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    showBorder: Boolean = true,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
    content: @Composable RowScope.() -> Unit,
) {
    val palette = LocalMuiPalette.current
    val hovered by interactionSource.collectIsHoveredAsState()

    fun frac(c: Color, f: Float) = c.copy(alpha = c.alpha * f)
    val background = when {
        selected -> palette.muted
        hovered && onClick != null -> frac(palette.muted, 0.50f)
        else -> Color.Transparent
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .then(
                if (showBorder) {
                    Modifier.drawBehind {
                        drawLine(
                            color = palette.border,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                } else Modifier
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * 表头单元格：h-10 px-2 左对齐 font-medium（放 MuiTableRow 内以使用 weight）。
 *
 * @param modifier 布局修饰（列宽用 Modifier.weight）
 * @param text 表头文本（null = 用 content 槽）
 * @param content 自定义表头内容
 */
@Composable
fun RowScope.MuiTableHead(
    modifier: Modifier = Modifier,
    text: String? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val palette = LocalMuiPalette.current
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (text != null) {
            MuiText(text = text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = palette.foreground)
        }
        content?.invoke()
    }
}

/**
 * 表体单元格：p-2 左对齐垂直居中（放 MuiTableRow 内以使用 weight）。
 *
 * @param modifier 布局修饰（列宽用 Modifier.weight）
 * @param text 单元格文本（null = 用 content 槽）
 * @param content 自定义单元格内容
 */
@Composable
fun RowScope.MuiTableCell(
    modifier: Modifier = Modifier,
    text: String? = null,
    content: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (text != null) MuiText(text = text, fontSize = 14.sp)
        content?.invoke()
    }
}

/**
 * 表格题注：mt-4 text-sm muted-foreground。
 *
 * @param modifier 布局修饰
 * @param text 题注文本
 */
@Composable
fun MuiTableCaption(
    modifier: Modifier = Modifier,
    text: String,
) {
    MuiText(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        color = LocalMuiPalette.current.mutedForeground,
        fontSize = 14.sp,
    )
}
