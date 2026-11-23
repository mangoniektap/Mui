/** @file Compose 模态面板的遮罩与边缘描边。 */
package com.mui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** 对齐 Web 的单层遮罩。使用 Compose 自绘遮罩时将窗口自带 dim 设为 0。 */
@Composable
internal fun MuiDialogDim(amount: Float) {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect { window?.setDimAmount(amount) }
}

/** 渲染并响应面板之外的区域，遮罩不参与面板尺寸计算。 */
@Composable
internal fun BoxScope.MuiModalScrim(open: Boolean, onDismiss: (() -> Unit)?) {
    val dismiss by rememberUpdatedState(onDismiss)
    val opacity = remember { Animatable(0f) }
    LaunchedEffect(open) { opacity.animateTo(if (open) 0.5f else 0f, tween(200)) }
    Box(
        Modifier.matchParentSize()
            .drawBehind { drawRect(Color.Black.copy(alpha = opacity.value)) }
            .pointerInput(open) { if (open) detectTapGestures { dismiss?.invoke() } },
    )
}

/** 居中对话框在 200ms 淡入缩放结束前保持窗口存活。 */
@Composable
internal fun MuiCenteredDialog(
    open: Boolean,
    onDismiss: () -> Unit,
    dismissOnBackdrop: Boolean = true,
    content: @Composable () -> Unit,
) {
    val transition = remember { MutableTransitionState(false) }
    transition.targetState = open
    if (!open && !transition.currentState && transition.isIdle) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        MuiDialogDim(0f)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MuiModalScrim(open, onDismiss.takeIf { dismissOnBackdrop })
            AnimatedVisibility(
                visibleState = transition,
                enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.95f),
                exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.95f),
            ) {
                Box(Modifier.excludeModalScrimTouches()) { content() }
            }
        }
    }
}

/** 接收面板空白处的命中，使它不会落到背后的遮罩，同时保留子项的输入处理。 */
internal fun Modifier.excludeModalScrimTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope { while (true) awaitPointerEvent() }
}

/** 浮层从指定屏幕边缘展开，在面向页面的一侧画 1dp 描边。 */
internal fun Modifier.muiPanelEdge(side: MuiPopupSide, color: Color): Modifier = drawBehind {
    val width = 1.dp.toPx()
    val half = width / 2
    val (start, end) = when (side) {
        MuiPopupSide.Top -> Offset(0f, size.height - half) to Offset(size.width, size.height - half)
        MuiPopupSide.Right -> Offset(half, 0f) to Offset(half, size.height)
        MuiPopupSide.Bottom -> Offset(0f, half) to Offset(size.width, half)
        MuiPopupSide.Left -> Offset(size.width - half, 0f) to Offset(size.width - half, size.height)
    }
    drawLine(color, start, end, width)
}
