package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalMuiPalette

/**
 * 表单字段的纵向组（field.tsx 的 Field）：标签 + 描述 + 错误 + 控件槽，间距 gap-3 = 12dp。
 * 输入 = 各子部件槽；输出 = 无状态纯布局。硬约束：禁 material3；视觉全走令牌。
 * TODO: web 的 responsive 档（容器查询切换横纵）未移植，只有 vertical / horizontal。
 *
 * @param modifier 布局修饰
 * @param orientation 排布方向（Vertical = 纵列控件占满宽 / Horizontal = 横行居中）
 * @param content 字段内容（建议 MuiFieldLabel / 控件 / MuiFieldDescription / MuiFieldError）
 */
@Composable
fun MuiField(
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Vertical,
    content: @Composable () -> Unit,
) {
    when (orientation) {
        Orientation.Vertical ->
            Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
        Orientation.Horizontal ->
            Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}

/**
 * 字段集（FieldSet）：多个 Field 的外层纵列，间距 gap-6 = 24dp。
 *
 * @param modifier 布局修饰
 * @param content 字段集内容（建议 MuiFieldLegend + 多个 MuiField）
 */
@Composable
fun MuiFieldSet(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp), content = content)
}

/**
 * 字段集标题（FieldLegend）：font-medium，legend 档 text-base(16sp) / label 档 text-sm(14sp)，下距 12dp。
 *
 * @param text 标题文本
 * @param modifier 布局修饰
 * @param variant 档位（Legend = 16sp 大标题 / Label = 14sp 小标题）
 */
@Composable
fun MuiFieldLegend(
    text: String,
    modifier: Modifier = Modifier,
    variant: FieldLegendVariant = FieldLegendVariant.Legend,
) {
    MuiText(
        text = text,
        modifier = modifier.padding(bottom = 12.dp),
        fontSize = if (variant == FieldLegendVariant.Legend) 16.sp else 14.sp,
        lineHeight = if (variant == FieldLegendVariant.Legend) 24.sp else 20.sp,
        fontWeight = FontWeight.Medium,
    )
}

/** 字段集标题档位（field.tsx 的 data-variant） */
enum class FieldLegendVariant { Legend, Label }

/**
 * 字段组（FieldGroup）：多个字段/字段集的外层纵列，间距 gap-7 = 28dp。
 *
 * @param modifier 布局修饰
 * @param content 组内容
 */
@Composable
fun MuiFieldGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(28.dp), content = content)
}

/**
 * 字段内容列（FieldContent）：flex-1 纵列 gap-1.5 = 6dp（横排 Field 里包标签+描述）。
 *
 * @param modifier 布局修饰
 * @param content 内容（建议 MuiLabel + MuiFieldDescription）
 */
@Composable
fun MuiFieldContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
}

/**
 * 字段标题（FieldTitle）：text-sm font-medium，行内可带前置元素，间距 8dp。
 *
 * @param text 标题文本
 * @param modifier 布局修饰
 * @param leading 前置元素槽（图标等，可空）
 */
@Composable
fun MuiFieldTitle(
    text: String,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit = {},
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        leading()
        MuiText(text = text, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * 字段描述（FieldDescription）：text-sm text-muted-foreground。
 *
 * @param text 描述文本
 * @param modifier 布局修饰
 */
@Composable
fun MuiFieldDescription(
    text: String,
    modifier: Modifier = Modifier,
) {
    MuiText(text = text, modifier = modifier, fontSize = 14.sp, lineHeight = 21.sp, color = LocalMuiPalette.current.mutedForeground)
}

/**
 * 字段分隔（FieldSeparator）：一条横穿线 + 可选居中文本（文本带 bg-background 底遮住线）。
 * 输入 = 可选文本；输出 = 无。硬约束：web 的 -my-2 负外边距未移植（调用方自行收间距）。
 *
 * @param modifier 布局修饰
 * @param text 分隔文本（null = 纯线）
 */
@Composable
fun MuiFieldSeparator(
    modifier: Modifier = Modifier,
    text: String? = null,
) {
    val palette = LocalMuiPalette.current
    Box(modifier = modifier.fillMaxWidth().height(20.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.border))
        if (text != null) {
            Box(modifier = Modifier.background(palette.background).padding(horizontal = 8.dp)) {
                MuiText(text = text, fontSize = 14.sp, lineHeight = 20.sp, color = palette.mutedForeground)
            }
        }
    }
}

/**
 * 字段错误（FieldError）：text-sm text-destructive；多条错误渲染为纵向列表。
 * 输入 = 单条 message 或 errors 列表；输出 = 无（为空时不占位）。
 *
 * @param message 单条错误文本（null = 走 errors）
 * @param modifier 布局修饰
 * @param errors 多条错误列表（非空且 message 为空时逐条渲染）
 */
@Composable
fun MuiFieldError(
    message: String? = null,
    modifier: Modifier = Modifier,
    errors: List<String> = emptyList(),
) {
    val body = message ?: when (errors.size) {
        0 -> return
        1 -> errors[0]
        else -> null
    }
    if (body != null) {
        MuiText(
            text = body,
            modifier = modifier,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = LocalMuiPalette.current.destructive,
        )
        return
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (error in errors) {
            MuiText(text = error, fontSize = 14.sp, lineHeight = 20.sp, color = LocalMuiPalette.current.destructive)
        }
    }
}
