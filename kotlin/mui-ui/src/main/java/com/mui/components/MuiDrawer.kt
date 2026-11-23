package com.mui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.Orientation as DragOrientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius
import kotlinx.coroutines.delay

/** 滑出方向（drawer.tsx / vaul 的 direction） */
enum class DrawerSide { Top, Right, Bottom, Left }

private const val DRAWER_ANIM_MS = 500
private val DrawerEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

/**
 * 抽屉：模态 Dialog + 黑 50% 遮罩 + 四向滑入面板（bg-background；vaul 语义，圆角只落在开合对侧），
 * 底部向 = 顶部圆角 lg + 顶描边 + 顶部把手（100×8dp bg-muted）；顶部向 = 底部圆角/描边；
 * 左右向 = 宽 3/4 上限 384dp + 侧描边；上下向高度上限 80%。
 * 输入：受控/非受控 open、方向、内容槽；输出：滑出浮层。
 * 面板支持沿离开方向拖拽关闭。
 *
 * @param open 受控开关（null = 非受控，走 defaultOpen）
 * @param defaultOpen 初始开关状态（非受控时用）
 * @param onOpenChange 开关变化回调（遮罩点击/返回键都触发 false）
 * @param side 滑出方向（默认 Bottom，与 vaul 一致）
 * @param modifier 布局修饰（应用于面板）
 * @param content 面板内容（Column 作用域；底部向时首行自动带把手）
 */
@Composable
fun MuiDrawer(
    open: Boolean? = null,
    defaultOpen: Boolean = false,
    onOpenChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    side: DrawerSide = DrawerSide.Bottom,
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
    val spec = tween<IntOffset>(DRAWER_ANIM_MS, easing = DrawerEasing)
    val vertical = side == DrawerSide.Top || side == DrawerSide.Bottom
    val direction = if (side == DrawerSide.Top || side == DrawerSide.Left) -1 else 1
    val velocityThreshold = with(LocalDensity.current) { 400.dp.toPx() }
    var dragDistance by remember(side) { mutableFloatStateOf(0f) }
    var panelSize by remember(side) { mutableIntStateOf(0) }
    val dragState = rememberDraggableState { delta ->
        dragDistance = (dragDistance + direction * delta).coerceIn(0f, panelSize.toFloat())
    }
    val enter: EnterTransition
    val exit: ExitTransition
    when (side) {
        DrawerSide.Right -> {
            enter = slideInHorizontally(spec) { it }
            exit = slideOutHorizontally(spec) { it }
        }
        DrawerSide.Left -> {
            enter = slideInHorizontally(spec) { -it }
            exit = slideOutHorizontally(spec) { -it }
        }
        DrawerSide.Top -> {
            enter = slideInVertically(spec) { -it }
            exit = slideOutVertically(spec) { -it }
        }
        DrawerSide.Bottom -> {
            enter = slideInVertically(spec) { it }
            exit = slideOutVertically(spec) { it }
        }
    }
    val alignment = when (side) {
        DrawerSide.Right -> AbsoluteAlignment.CenterRight
        DrawerSide.Left -> AbsoluteAlignment.CenterLeft
        DrawerSide.Top -> Alignment.TopCenter
        DrawerSide.Bottom -> Alignment.BottomCenter
    }
    val shape = when (side) {
        DrawerSide.Top -> RoundedCornerShape(bottomStart = MuiRadius.lg, bottomEnd = MuiRadius.lg)
        DrawerSide.Bottom -> RoundedCornerShape(topStart = MuiRadius.lg, topEnd = MuiRadius.lg)
        DrawerSide.Right -> RoundedCornerShape(0.dp)
        DrawerSide.Left -> RoundedCornerShape(0.dp)
    }

    Dialog(
        onDismissRequest = { setOpen(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        MuiDialogDim(0f)
        BoxWithConstraints(Modifier.fillMaxSize()) {
            MuiModalScrim(isOpen) { setOpen(false) }
            // web 的 max-h-[80vh]：上下向面板高度上限 = 容器高的 80%
            val maxPanelHeight = maxHeight * 0.8f
            val panelSizing = when (side) {
                DrawerSide.Right, DrawerSide.Left ->
                    Modifier.fillMaxHeight().width(minOf(maxWidth * 0.75f, 384.dp))
                DrawerSide.Top, DrawerSide.Bottom ->
                    Modifier.fillMaxWidth().heightIn(max = maxPanelHeight)
            }
            AnimatedVisibility(
                visible = isOpen,
                enter = enter,
                exit = exit,
                modifier = Modifier.align(alignment).then(panelSizing),
            ) {
                Box(
                    modifier = modifier
                        .onSizeChanged { panelSize = if (vertical) it.height else it.width }
                        .graphicsLayer {
                            if (vertical) translationY = direction * dragDistance
                            else translationX = direction * dragDistance
                        }
                        .draggable(
                            state = dragState,
                            orientation = if (vertical) DragOrientation.Vertical else DragOrientation.Horizontal,
                            enabled = isOpen,
                            onDragStopped = { velocity ->
                                val outward = velocity * direction
                                if (dragDistance > 0f && (dragDistance >= panelSize * 0.25f || outward > velocityThreshold)) {
                                    setOpen(false)
                                }
                                animate(dragDistance, 0f, animationSpec = tween(DRAWER_ANIM_MS, easing = DrawerEasing)) { distance, _ ->
                                    dragDistance = distance
                                }
                            },
                        )
                        .excludeModalScrimTouches()
                        .clip(shape)
                        .background(palette.background, shape)
                        .muiPanelEdge(MuiPopupSide.valueOf(side.name), palette.border),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (side == DrawerSide.Right || side == DrawerSide.Left) Modifier.fillMaxHeight() else Modifier),
                    ) {
                        if (side == DrawerSide.Bottom) {
                            // 底部向把手：mt-4 h-2 w-[100px] rounded-full bg-muted
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 16.dp)
                                        .size(width = 100.dp, height = 8.dp)
                                        .background(palette.muted, CircleShape),
                                )
                            }
                        }
                        content()
                    }

                }
            }
        }
        LaunchedEffect(isOpen) {
            if (!isOpen) {
                delay(DRAWER_ANIM_MS.toLong())
                windowShown = false
            }
        }
    }
}

/** 面板头区：p-4 纵向排布（gap-1.5） */
@Composable
fun MuiDrawerHeader(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** 面板标题：font-semibold foreground */
@Composable
fun MuiDrawerTitle(text: String, modifier: Modifier = Modifier) {
    MuiText(text = text, modifier = modifier, fontWeight = FontWeight.SemiBold)
}

/** 面板描述：text-sm muted-foreground */
@Composable
fun MuiDrawerDescription(text: String, modifier: Modifier = Modifier) {
    val palette = LocalMuiPalette.current
    CompositionLocalProvider(LocalMuiContentColor provides palette.mutedForeground) {
        MuiText(text = text, modifier = modifier, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

/** 面板底区：p-4 纵向排布（gap-2），推送到底可用 Modifier.weight(1f) 传入 */
@Composable
fun ColumnScope.MuiDrawerFooter(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}
