package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius
import com.mui.theme.muiElevation

// TODO: web 的条目位置在首帧按 Single 渲染、组合完成后纠正；动态增删条目需整组重组才更新位置。
// TODO: web 的 focus-visible z-10 抬升与 select-trigger 圆角特例未移植。

/** 组内条目位置：单件 / 首 / 中 / 尾（决定圆角与分隔线） */
private enum class ButtonGroupPosition { Single, First, Middle, Last }

/** 按钮组方向（组合内部读取用） */
internal val LocalMuiButtonGroupOrientation = staticCompositionLocalOf { Orientation.Horizontal }

/**
 * 按钮组作用域：条目按组合顺序自动分配首/中/尾位置。
 * 输入条目槽；输出带裁剪圆角与 1dp 分隔线的条目容器。
 */
class MuiButtonGroupScope internal constructor() {
    private var itemCount by mutableIntStateOf(0)
    private var composedIndex = 0

    internal fun resetIndex() {
        composedIndex = 0
    }

    internal fun commitCount() {
        itemCount = composedIndex
    }

    /**
     * 组内一个条目：自动按位置裁剪圆角并画分隔线。
     * @param modifier 布局修饰
     * @param content 条目内容（MuiButton / MuiButtonGroupText 等）
     */
    @Composable
    fun MuiButtonGroupItem(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        val index = composedIndex++
        val palette = LocalMuiPalette.current
        val orientation = LocalMuiButtonGroupOrientation.current
        val position = when {
            itemCount <= 1 -> ButtonGroupPosition.Single
            index == 0 -> ButtonGroupPosition.First
            index == itemCount - 1 -> ButtonGroupPosition.Last
            else -> ButtonGroupPosition.Middle
        }
        Box(
            modifier = modifier
                .then(
                    when (orientation) {
                        Orientation.Horizontal -> Modifier.fillMaxHeight()
                        Orientation.Vertical -> Modifier.fillMaxWidth()
                    }
                )
                .clip(groupItemShape(position, orientation))
                .then(
                    if (index > 0) {
                        Modifier.drawBehind {
                            when (orientation) {
                                Orientation.Horizontal -> drawRect(
                                    palette.border,
                                    topLeft = Offset(0f, 0f),
                                    size = Size(1.dp.toPx(), size.height),
                                )
                                Orientation.Vertical -> drawRect(
                                    palette.border,
                                    topLeft = Offset(0f, 0f),
                                    size = Size(size.width, 1.dp.toPx()),
                                )
                            }
                        }
                    } else Modifier
                ),
        ) {
            content()
        }
    }
}

/** 按位置/方向解析条目裁剪形状（首件外圆角、中件直角） */
private fun groupItemShape(position: ButtonGroupPosition, orientation: Orientation): Shape {
    val r = MuiRadius.md
    return when {
        position == ButtonGroupPosition.Single ->
            RoundedCornerShape(r)
        orientation == Orientation.Horizontal && position == ButtonGroupPosition.First ->
            RoundedCornerShape(topStart = r, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = r)
        orientation == Orientation.Horizontal && position == ButtonGroupPosition.Last ->
            RoundedCornerShape(topStart = 0.dp, topEnd = r, bottomEnd = r, bottomStart = 0.dp)
        orientation == Orientation.Vertical && position == ButtonGroupPosition.First ->
            RoundedCornerShape(topStart = r, topEnd = r, bottomEnd = 0.dp, bottomStart = 0.dp)
        orientation == Orientation.Vertical && position == ButtonGroupPosition.Last ->
            RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = r, bottomStart = r)
        else ->
            RoundedCornerShape(0.dp)
    }
}

/**
 * 按钮组：横向/纵向排布一组按钮件，条目拉伸等高/等宽，件间 1dp 分隔线。
 * 输入 orientation + 一组 MuiButtonGroupItem 槽；输出拼接后的组。
 * 硬约束：子件自身圆角被条目裁剪覆盖（对应 web 的 rounded-l-none / border-l-0）。
 *
 * @param modifier 布局修饰
 * @param orientation 排布方向（Horizontal = 横向 / Vertical = 纵向）
 * @param content 组内容（MuiButtonGroupScope 作用域：写若干 MuiButtonGroupItem）
 */
@Composable
fun MuiButtonGroup(
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
    content: @Composable MuiButtonGroupScope.() -> Unit,
) {
    val scope = remember { MuiButtonGroupScope() }
    CompositionLocalProvider(LocalMuiButtonGroupOrientation provides orientation) {
        when (orientation) {
            Orientation.Horizontal -> Row(modifier.height(IntrinsicSize.Min)) { groupBody(scope, content) }
            Orientation.Vertical -> Column(modifier.width(IntrinsicSize.Min)) { groupBody(scope, content) }
        }
    }
}

/** 组内条目计数：组合前清零，组合完写入（供条目解析位置） */
@Composable
private fun groupBody(
    scope: MuiButtonGroupScope,
    content: @Composable MuiButtonGroupScope.() -> Unit,
) {
    scope.resetIndex()
    scope.content()
    SideEffect { scope.commitCount() }
}

/**
 * 组内文本件：bg-muted + border + xs 阴影的标签段（web 的 ButtonGroupText）。
 *
 * @param modifier 布局修饰
 * @param content 内容（Row 作用域；文字用 MuiText）
 */
@Composable
fun MuiButtonGroupText(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val palette = LocalMuiPalette.current
    val shape = RoundedCornerShape(MuiRadius.md)
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .background(palette.muted, shape)
            .border(1.dp, palette.border, shape)
            .muiElevation("xs", shape)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        CompositionLocalProvider(LocalMuiContentColor provides palette.foreground) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}

/**
 * 组内分隔件：1dp 线，颜色 --input（web 的 ButtonGroupSeparator）。
 *
 * @param modifier 布局修饰
 * @param orientation 线方向（默认 Vertical = 竖线，配合横向组）
 */
@Composable
fun MuiButtonGroupSeparator(
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Vertical,
) {
    val color = LocalMuiPalette.current.input
    val m = modifier.background(color)
    when (orientation) {
        Orientation.Vertical -> m.width(1.dp).fillMaxHeight()
        Orientation.Horizontal -> m.height(1.dp).fillMaxWidth()
    }
}
