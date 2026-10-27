package com.mui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.theme.muiElevation
import kotlinx.coroutines.delay

/** 滑出方向（sheet.tsx 的 side） */
enum class SheetSide { Top, Right, Bottom, Left }

/** 进出场时长（web 开 500ms / 关 300ms，此处统一 300ms） */
private const val SHEET_ANIM_MS = 300

/** web 遮罩 bg-black/50（web 原文即字面黑 50%，非令牌色） */
private val sheetScrim = Color(0x80000000)

/**
 * 侧滑面板：模态 Dialog + 黑 50% 遮罩 + 四向滑入面板（bg-background、无边框、lg 阴影、纵向排布 gap-4），
 * 左右向 = 高度撑满、宽 3/4 上限 384dp；上下向 = 宽度撑满、高度随内容。
 * 输入：受控/非受控 open、方向、内容槽；输出：滑出浮层。
 * 硬约束：面板无边框（classical 规则注明 sheet 无 border）；进出场 = 300ms 平移 + 遮罩淡入淡出。
 *
 * TODO: web 的开启 500ms / 关闭 300ms 时长差异未移植（统一 300ms）。
 *
 * @param open 受控开关（null = 非受控，走 defaultOpen）
 * @param defaultOpen 初始开关状态（非受控时用）
 * @param onOpenChange 开关变化回调（遮罩点击/返回键/关闭钮都触发 false）
 * @param side 滑出方向（默认 Right）
 * @param showCloseButton 是否显示右上角关闭钮（默认 true）
 * @param modifier 布局修饰（应用于面板）
 * @param content 面板内容（Column 作用域）
 */
@Composable
fun MuiSheet(
    open: Boolean? = null,
    defaultOpen: Boolean = false,
    onOpenChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    side: SheetSide = SheetSide.Right,
    showCloseButton: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var inner by remember { mutableStateOf(defaultOpen) }
    val isOpen = open ?: inner
    fun setOpen(value: Boolean) {
        if (open == null) inner = value
        onOpenChange(value)
    }

    // 面板窗口在关闭动画播完前保持组合（否则出场动画被对话框销毁打断）
    var windowShown by remember { mutableStateOf(false) }
    LaunchedEffect(isOpen) { if (isOpen) windowShown = true }
    if (!windowShown) return

    val palette = LocalMuiPalette.current
    val spec = tween<IntOffset>(SHEET_ANIM_MS, easing = FastOutSlowInEasing)
    val enter: EnterTransition
    val exit: ExitTransition
    when (side) {
        SheetSide.Right -> {
            enter = slideInHorizontally(spec) { it }
            exit = slideOutHorizontally(spec) { it }
        }
        SheetSide.Left -> {
            enter = slideInHorizontally(spec) { -it }
            exit = slideOutHorizontally(spec) { -it }
        }
        SheetSide.Top -> {
            enter = slideInVertically(spec) { -it }
            exit = slideOutVertically(spec) { -it }
        }
        SheetSide.Bottom -> {
            enter = slideInVertically(spec) { it }
            exit = slideOutVertically(spec) { it }
        }
    }
    val alignment = when (side) {
        SheetSide.Right -> Alignment.CenterEnd
        SheetSide.Left -> Alignment.CenterStart
        SheetSide.Top -> Alignment.TopCenter
        SheetSide.Bottom -> Alignment.BottomCenter
    }
    val panelSizing = when (side) {
        SheetSide.Right, SheetSide.Left -> Modifier.fillMaxHeight().fillMaxWidth(0.75f).widthIn(max = 384.dp)
        SheetSide.Top, SheetSide.Bottom -> Modifier.fillMaxWidth()
    }

    Dialog(
        onDismissRequest = { setOpen(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(sheetScrim)) {
            AnimatedVisibility(
                visible = isOpen,
                enter = enter,
                exit = exit,
                modifier = Modifier.align(alignment).then(panelSizing),
            ) {
                Box(
                    modifier = modifier
                        .background(palette.background)
                        .muiElevation("lg", RectangleShape),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (side == SheetSide.Right || side == SheetSide.Left) Modifier.fillMaxHeight() else Modifier),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        content = content,
                    )
                    if (showCloseButton) {
                        SheetCloseX(
                            onClick = { setOpen(false) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-16).dp, y = 16.dp),
                        )
                    }
                }
            }
        }
        LaunchedEffect(isOpen) {
            if (!isOpen) {
                delay(SHEET_ANIM_MS.toLong())
                windowShown = false
            }
        }
    }
}

/** 面板头区：p-4 纵向排布（gap-1.5） */
@Composable
fun MuiSheetHeader(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** 面板标题：font-semibold foreground */
@Composable
fun MuiSheetTitle(text: String, modifier: Modifier = Modifier) {
    MuiText(text = text, modifier = modifier, fontWeight = FontWeight.SemiBold)
}

/** 面板描述：text-sm muted-foreground */
@Composable
fun MuiSheetDescription(text: String, modifier: Modifier = Modifier) {
    val palette = LocalMuiPalette.current
    CompositionLocalProvider(LocalMuiContentColor provides palette.mutedForeground) {
        MuiText(text = text, modifier = modifier, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

/** 面板底区：p-4 纵向排布（gap-2），推送到底可用 Modifier.weight(1f) 传入 */
@Composable
fun ColumnScope.MuiSheetFooter(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** 右上角关闭钮：16dp X 图形，70% 不透明度 */
@Composable
private fun SheetCloseX(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalMuiPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(16.dp)
            .alpha(0.7f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 1.5.dp.toPx()
            drawLine(palette.foreground, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth = stroke)
            drawLine(palette.foreground, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = stroke)
        }
    }
}
