package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius
import com.mui.theme.rememberMuiInteractionSource

// TODO: web 的键盘上下选择与回车执行（cmdk data-selected）未移植，这里仅悬停高亮
// TODO: web 的 CommandDialog（弹窗壳）未移植，只移植命令面板本体

/** 命令面板条目：文字 + 可选快捷键 + 点击回调 */
data class MuiCommandItemSpec(
    val label: String,
    val shortcut: String? = null,
    val onClick: () -> Unit = {},
)

/** 命令面板分组：组标题 + 条目列表 */
data class MuiCommandGroupSpec(
    val label: String,
    val items: List<MuiCommandItemSpec>,
)

/**
 * 命令面板：bg-popover rounded-md（无 border/shadow，照 web command 本体配方）+
 * 顶部搜索行（h-9 border-b、放大镜 16dp 50% 透明）+ LazyColumn 过滤列表（max 300dp）+
 * 分组标题（text-xs font-medium muted-foreground）+ 空态 + 底部提示行。
 * 输入 = 分组数据；输出 = 条目点击与查询变化。
 * 硬约束：过滤为 label 忽略大小写包含；无 ripple。
 *
 * @param groups 分组数据（渲染前按查询过滤）
 * @param modifier 布局修饰
 * @param placeholder 搜索框占位文本
 * @param query 受控查询词（null = 非受控，内部自持）
 * @param onQueryChange 查询词变化回调（受控时必传）
 * @param emptyText 无结果提示（居中 text-sm py-6）
 * @param footer 底部提示行内容（Row 作用域；上边框分隔）
 */
@Composable
fun MuiCommand(
    groups: List<MuiCommandGroupSpec>,
    modifier: Modifier = Modifier,
    placeholder: String = "Type a command or search...",
    query: String? = null,
    onQueryChange: ((String) -> Unit)? = null,
    emptyText: String = "No results found.",
    footer: @Composable RowScope.() -> Unit = {},
) {
    val palette = LocalMuiPalette.current
    var innerQuery by remember { mutableStateOf("") }
    val search = query ?: innerQuery

    fun setSearch(next: String) {
        if (query == null) innerQuery = next
        onQueryChange?.invoke(next)
    }

    val filtered = groups.mapNotNull { group ->
        val hit = group.items.filter { it.label.contains(search, ignoreCase = true) }
        if (hit.isEmpty()) null else group to hit
    }

    val shape = RoundedCornerShape(MuiRadius.md)
    Column(
        modifier = modifier
            .background(palette.popover, shape)
            .clip(shape),
    ) {
        CompositionLocalProvider(LocalMuiContentColor provides palette.popoverForeground) {
            // 搜索行：h-9 border-b px-3 gap-2，图标 16dp opacity-50
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CommandSearchIcon(color = palette.mutedForeground.copy(alpha = palette.mutedForeground.alpha * 0.50f))
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = search,
                        onValueChange = ::setSearch,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = palette.foreground),
                        cursorBrush = SolidColor(palette.primary),
                    )
                    if (search.isEmpty()) {
                        MuiText(text = placeholder, color = palette.mutedForeground, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
            MuiSeparator(modifier = Modifier.fillMaxWidth())
            // 结果列表：max-h-[300px]
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                if (filtered.isEmpty()) {
                    item(key = "empty") {
                        MuiText(
                            text = emptyText,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                filtered.forEach { (group, items) ->
                    item(key = "group-${group.label}") {
                        MuiText(
                            text = group.label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            color = palette.mutedForeground,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    items(items, key = { "${group.label}-${it.label}" }) { item ->
                        MuiCommandItemRow(item = item)
                    }
                }
            }
            // 底部提示行：border-t
            MuiSeparator(modifier = Modifier.fillMaxWidth())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = footer,
            )
        }
    }
}

/**
 * 单条命令行：px-2 py-1.5 rounded-sm gap-2 text-sm；悬停 bg-accent；快捷键右对齐。
 *
 * @param item 条目数据
 * @param modifier 布局修饰
 * @param enabled 可用性（false = 半透明不可点）
 * @param interactionSource 交互源
 */
@Composable
fun MuiCommandItemRow(
    item: MuiCommandItemSpec,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
) {
    val palette = LocalMuiPalette.current
    val hovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(MuiRadius.sm)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clip(shape)
            .background(if (hovered) palette.accent else Color.Transparent, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = item.onClick,
            )
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MuiText(text = item.label, fontSize = 14.sp, lineHeight = 20.sp)
        if (item.shortcut != null) {
            Spacer(modifier = Modifier.weight(1f))
            MuiText(
                text = item.shortcut,
                color = palette.mutedForeground,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.1.em,
            )
        }
    }
}

/** 放大镜图标：圆环 + 手柄（16dp，两笔线） */
@Composable
private fun CommandSearchIcon(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        drawCircle(
            color = color,
            radius = w * 0.28f,
            center = Offset(w * 0.42f, h * 0.42f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
        )
        drawLine(
            color = color,
            start = Offset(w * 0.64f, h * 0.64f),
            end = Offset(w * 0.85f, h * 0.85f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
