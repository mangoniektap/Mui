package com.mui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mui.theme.LocalMuiPalette
import kotlinx.coroutines.launch

// TODO: web 的键盘左右键翻页（onKeyDownCapture）与 embla opts/plugins 未移植。
// TODO: 竖向轮播（orientation=vertical）未移植，只做 HorizontalPager。

/**
 * 轮播：HorizontalPager 封装 + 页指示器 + 左右翻页钮。
 * 输入页数 + 指示器/翻页钮槽 + 页内容；输出可翻页的轮播。
 * 硬约束：指示器默认圆点（active = primary）；翻页钮默认两侧圆形 outline 钮（Canvas 箭头）。
 *
 * @param pageCount 页数
 * @param modifier 布局修饰
 * @param pageSpacing 页间距（默认 16dp，web 的 pl-4）
 * @param indicator 页指示器槽（参数 当前页 / 页数；null = 默认圆点）
 * @param navigation 翻页钮槽（参数 onPrev / canPrev / onNext / canNext；null = 默认两侧圆钮）
 * @param content 页内容（参数 页码）
 */
@Composable
fun MuiCarousel(
    pageCount: Int,
    modifier: Modifier = Modifier,
    pageSpacing: Dp = 16.dp,
    indicator: (@Composable (page: Int, pageCount: Int) -> Unit)? = null,
    navigation: (@Composable (onPrev: () -> Unit, canPrev: Boolean, onNext: () -> Unit, canNext: Boolean) -> Unit)? = null,
    content: @Composable (page: Int) -> Unit,
) {
    val palette = LocalMuiPalette.current
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    val canPrev = pagerState.currentPage > 0
    val canNext = pagerState.currentPage < pageCount - 1
    val onPrev: () -> Unit = {
        if (canPrev) scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }
    val onNext: () -> Unit = {
        if (canNext) scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box {
            HorizontalPager(
                state = pagerState,
                pageSpacing = pageSpacing,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                content(page)
            }
            if (navigation != null) {
                navigation(onPrev, canPrev, onNext, canNext)
            } else {
                Box(Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) {
                    MuiCarouselNavButton(onClick = onPrev, enabled = canPrev, directionLeft = true)
                }
                Box(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
                    MuiCarouselNavButton(onClick = onNext, enabled = canNext, directionLeft = false)
                }
            }
        }
        Box(Modifier.align(Alignment.CenterHorizontally)) {
            if (indicator != null) {
                indicator(pagerState.currentPage, pageCount)
            } else {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (page in 0 until pageCount) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (page == pagerState.currentPage) palette.primary else palette.muted,
                                    CircleShape,
                                ),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 默认翻页钮：32dp 圆形 outline 钮，Canvas 箭头。
 *
 * @param onClick 翻页回调
 * @param enabled 是否可翻
 * @param directionLeft true = 左箭头
 */
@Composable
private fun MuiCarouselNavButton(
    onClick: () -> Unit,
    enabled: Boolean,
    directionLeft: Boolean,
) {
    val palette = LocalMuiPalette.current
    val interactionSource = rememberMuiInteractionSource()
    MuiButton(
        onClick = onClick,
        variant = ButtonVariant.Outline,
        size = ButtonSize.Icon,
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        Canvas(modifier = Modifier.size(16.dp)) {
            val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            val midY = size.height / 2f
            val color = palette.foreground
            if (directionLeft) {
                drawLine(color, Offset(size.width - 3.dp.toPx(), midY), Offset(3.dp.toPx(), midY), stroke.width)
                drawLine(color, Offset(3.dp.toPx(), midY), Offset(8.dp.toPx(), midY - 5.dp.toPx()), stroke.width)
                drawLine(color, Offset(3.dp.toPx(), midY), Offset(8.dp.toPx(), midY + 5.dp.toPx()), stroke.width)
            } else {
                drawLine(color, Offset(3.dp.toPx(), midY), Offset(size.width - 3.dp.toPx(), midY), stroke.width)
                drawLine(color, Offset(size.width - 3.dp.toPx(), midY), Offset(size.width - 8.dp.toPx(), midY - 5.dp.toPx()), stroke.width)
                drawLine(color, Offset(size.width - 3.dp.toPx(), midY), Offset(size.width - 8.dp.toPx(), midY + 5.dp.toPx()), stroke.width)
            }
        }
    }
}
