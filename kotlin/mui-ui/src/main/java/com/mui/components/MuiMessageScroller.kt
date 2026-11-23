package com.mui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import kotlinx.coroutines.launch

/**
 * 消息滚动容器。用户向上翻阅时保持其位置，回到底部后继续跟随新消息。
 * 输入 itemCount + 列表项槽；输出自动跟随的滚动列表。
 * 硬约束：条目间距默认 32dp（web 的 gap-8）；密排列表（日志）由调用点传更小的值。
 *
 * @param itemCount 条目数（变化时触发自动滚动）
 * @param modifier 布局修饰
 * @param reverseLayout 是否反向排布（新消息在顶部时置 true）
 * @param itemSpacing 条目间距（web 的 gap-8 = 32px；日志这类密排传 0~2dp）
 * @param state 列表状态（调用方可自持以监听滚动）
 * @param contentPadding 内容内边距
 * @param content 列表项定义（LazyListScope：items / item）
 */
@Composable
fun MuiMessageScroller(
    itemCount: Int,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    itemSpacing: Dp = 32.dp,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyListScope.() -> Unit,
) {
    MuiMessageScroller(
        itemCount, modifier, reverseLayout, itemSpacing, state, contentPadding,
        autoScroll = true, content = content,
    )
}

/** 支持自动跟随和返回末尾按钮。anchorIndex 可用于跳到指定消息。 */
@Composable
fun MuiMessageScroller(
    itemCount: Int,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    itemSpacing: Dp = 32.dp,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    autoScroll: Boolean,
    showScrollButton: Boolean = true,
    anchorIndex: Int? = null,
    content: LazyListScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val atEnd = if (reverseLayout) !state.canScrollBackward else !state.canScrollForward
    val latestIndex = if (reverseLayout) 0 else (itemCount - 1).coerceAtLeast(0)
    var initialized by remember(state, reverseLayout) { mutableStateOf(false) }
    var following by remember(state, reverseLayout) { mutableStateOf(true) }
    var lastAnchor by remember(state, reverseLayout) { mutableStateOf<Int?>(null) }
    var programmatic by remember(state) { mutableStateOf(false) }

    LaunchedEffect(state, reverseLayout) {
        snapshotFlow {
            val endReached = if (reverseLayout) !state.canScrollBackward else !state.canScrollForward
            endReached to state.isScrollInProgress
        }.collect { (endReached, scrolling) ->
            if (!programmatic) {
                if (endReached) following = true
                else if (scrolling) following = false
            }
        }
    }
    LaunchedEffect(itemCount, state, reverseLayout, autoScroll, anchorIndex) {
        if (itemCount <= 0) {
            initialized = false
            lastAnchor = null
            return@LaunchedEffect
        }
        val anchor = anchorIndex?.takeIf { it in 0 until itemCount }
        val target = anchor?.takeIf { it != lastAnchor }
            ?: latestIndex.takeIf { !initialized || autoScroll && following }
        lastAnchor = anchor
        if (target != null) {
            programmatic = true
            try {
                state.scrollToItem(target)
                following = target == latestIndex
                initialized = true
            } finally {
                programmatic = false
            }
        }
    }
    Box(modifier) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            reverseLayout = reverseLayout,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            content = content,
        )
        AnimatedVisibility(
            visible = showScrollButton && initialized && !atEnd,
            modifier = Modifier.align(if (reverseLayout) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(vertical = 16.dp),
            enter = fadeIn() + scaleIn(initialScale = 0.95f),
            exit = fadeOut() + scaleOut(targetScale = 0.95f),
        ) {
            MuiButton(
                onClick = {
                    scope.launch {
                        programmatic = true
                        try {
                            state.animateScrollToItem(latestIndex)
                            following = true
                        } finally {
                            programmatic = false
                        }
                    }
                },
                modifier = Modifier.semantics { contentDescription = "滚动到最新消息" },
                variant = ButtonVariant.Secondary,
                size = ButtonSize.IconSm,
            ) {
                val color = LocalMuiContentColor.current.takeOrDefault(LocalMuiPalette.current.foreground)
                Canvas(Modifier.size(16.dp)) {
                    val arrowStart = if (reverseLayout) size.height * 0.8f else size.height * 0.2f
                    val arrowEnd = size.height - arrowStart
                    val cross = size.height / 2
                    val stroke = 1.5.dp.toPx()
                    drawLine(color, Offset(center.x, arrowStart), Offset(center.x, arrowEnd), stroke, StrokeCap.Round)
                    drawLine(color, Offset(size.width * 0.25f, cross), Offset(center.x, arrowEnd), stroke, StrokeCap.Round)
                    drawLine(color, Offset(size.width * 0.75f, cross), Offset(center.x, arrowEnd), stroke, StrokeCap.Round)
                }
            }
        }
    }
}
