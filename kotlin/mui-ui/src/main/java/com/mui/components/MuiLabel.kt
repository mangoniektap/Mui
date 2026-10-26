package com.mui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 标签：text-sm font-medium leading-none，水平排布可带前置图标。
 *
 * @param text 标签文本
 */
@Composable
fun MuiLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    MuiText(
        text = text,
        modifier = modifier,
        fontSize = 14.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
    )
}

/** 标签 + 控件的一行组合：图标/文本与控件水平居中，间距 8dp（gap-2） */
@Composable
fun MuiLabelRow(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}
