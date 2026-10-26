package com.mui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// TODO: web 的滚到底浮动按钮（MessageScrollerButton）、滚动淡出遮罩、scrollbar 样式未移植。
// TODO: web 的 scrollAnchor / content-visibility 占位优化未移植。

/**
 * 消息滚动容器：LazyColumn 列表，数据变化自动滚到底（reverse 时滚到顶）。
 * 输入 itemCount + 列表项槽；输出自动跟随的滚动列表。
 * 硬约束：条目间距 32dp（web 的 gap-8）。
 *
 * @param itemCount 条目数（变化时触发自动滚动）
 * @param modifier 布局修饰
 * @param reverseLayout 是否反向排布（新消息在顶部时置 true）
 * @param state 列表状态（调用方可自持以监听滚动）
 * @param contentPadding 内容内边距
 * @param content 列表项定义（LazyListScope：items / item）
 */
@Composable
fun MuiMessageScroller(
    itemCount: Int,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyListScope.() -> Unit,
) {
    LaunchedEffect(itemCount, reverseLayout) {
        if (itemCount > 0) {
            val target = if (reverseLayout) 0 else itemCount - 1
            state.animateScrollToItem(target)
        }
    }
    LazyColumn(
        state = state,
        modifier = modifier,
        reverseLayout = reverseLayout,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(32.dp),
        content = content,
    )
}
