package com.mui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.PopupPositionProvider
import com.mui.icon.MuiIcons
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.theme.muiElevation
import com.mui.theme.rememberMuiInteractionSource
import com.mui.token.MuiRadius
import kotlinx.coroutines.delay

/** 弹出位置（宿主顶/底） */
enum class ToastSide { Top, Bottom }

enum class MuiToastType { Default, Success, Info, Warning, Error, Loading }

/** 进出场时长 */
private const val TOAST_ANIM_MS = 200

/** 单条 toast 的内部条目（id 用于稳定复用与精确移除） */
internal class ToastItem(val id: Long, val message: String, val type: MuiToastType, val durationMillis: Long) {
    var dismissed by mutableStateOf(false)
}

/**
 * Toast 状态：收集待显示条目；show 压入一条，自动或点击关闭。
 */
@Stable
class MuiToastState internal constructor() {
    internal val items = mutableStateListOf<ToastItem>()
    private var nextId = 0L

    /** 普通提示，默认显示 4 秒。 */
    fun show(message: String, durationMillis: Long = 4000) {
        show(message, MuiToastType.Default, durationMillis)
    }

    /** 返回提示 id。Loading 在显式关闭前保持显示。 */
    fun show(message: String, type: MuiToastType, durationMillis: Long = 4000): Long {
        val id = nextId++
        items += ToastItem(id, message, type, durationMillis.coerceAtLeast(0))
        return id
    }

    fun dismiss(id: Long) {
        items.firstOrNull { it.id == id }?.dismissed = true
    }

    internal fun remove(id: Long) {
        items.removeAll { it.id == id }
    }
}

/** 创建 Toast 状态（触发方与宿主共用一个实例） */
@Composable
fun rememberMuiToastState(): MuiToastState = remember { MuiToastState() }

/**
 * Toast 宿主：非聚焦 Popup 挂在顶/底，弹出消息条（bg-popover、border、lg 阴影、rounded-lg、
 * px-4 py-3、14sp，文案 popover-foreground），新条目靠外侧，自动关闭 + 点击关闭，
 * 进出场 = 200ms 纵向滑移 + 淡入淡出。
 * 输入：MuiToastState；输出：不拦截外部点击的浮层。
 * 硬约束：宿主放在根布局附近（Popup 锚点 = 最近布局节点）；无 material3 依赖。
 *
 * @param state toast 状态（rememberMuiToastState）
 * @param side 弹出位置（默认 Bottom）
 * @param modifier 宿主布局修饰（应用于 Popup 内的列表）
 */
@Composable
fun MuiToastHost(
    state: MuiToastState,
    modifier: Modifier = Modifier,
    side: ToastSide = ToastSide.Bottom,
) {
    val palette = LocalMuiPalette.current
    val shape = RoundedCornerShape(MuiRadius.lg)
    val density = LocalDensity.current
    val wideOffset = with(density) { 24.dp.roundToPx() }
    val mobileOffset = with(density) { 16.dp.roundToPx() }
    val mobileWidth = with(density) { 600.dp.roundToPx() }
    val position = remember(side, wideOffset, mobileOffset, mobileWidth) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val inset = if (windowSize.width <= mobileWidth) mobileOffset else wideOffset
                val x = (windowSize.width - popupContentSize.width - inset).coerceAtLeast(0)
                val y = if (side == ToastSide.Top) inset else (windowSize.height - popupContentSize.height - inset).coerceAtLeast(0)
                return IntOffset(x, y)
            }
        }
    }
    if (state.items.isEmpty()) return
    Popup(
        popupPositionProvider = position,
        properties = PopupProperties(focusable = false),
    ) {
        Column(
            modifier = modifier.widthIn(max = 356.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 新条目靠外侧：顶部侧 newest-first，底部侧反转
            val shownItems = state.items.takeLast(3)
            val displayOrder = if (side == ToastSide.Top) state.items.asReversed() else state.items
            for (item in displayOrder) {
                key(item.id) {
                    var visible by remember(item.id) { mutableStateOf(false) }
                    LaunchedEffect(item.id) {
                        visible = true
                        if (item.type != MuiToastType.Loading) {
                            delay(item.durationMillis)
                            state.dismiss(item.id)
                        }
                    }
                    LaunchedEffect(item.dismissed) {
                        if (item.dismissed) {
                            visible = false
                            delay(TOAST_ANIM_MS.toLong())
                            state.remove(item.id)
                        }
                    }
                    AnimatedVisibility(
                        visible = visible && item in shownItems,
                        enter = fadeIn(tween(150)) +
                            slideInVertically(tween(TOAST_ANIM_MS)) { if (side == ToastSide.Top) -it else it },
                        exit = fadeOut(tween(150)) +
                            slideOutVertically(tween(TOAST_ANIM_MS)) { if (side == ToastSide.Top) -it else it },
                    ) {
                        val clickSource = rememberMuiInteractionSource()
                        Row(
                            modifier = Modifier
                                .background(palette.popover, shape)
                                .border(1.dp, palette.border, shape)
                                .muiElevation("lg", shape)
                                .clickable(
                                    interactionSource = clickSource,
                                    indication = null,
                                ) { state.dismiss(item.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CompositionLocalProvider(LocalMuiContentColor provides palette.popoverForeground) {
                                MuiToastIcon(item.type, palette.popoverForeground)
                                MuiText(text = item.message, fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 提示图标沿用库内图标和同一前景色。 */
@Composable
private fun MuiToastIcon(type: MuiToastType, color: Color) {
    when (type) {
        MuiToastType.Default -> Unit
        MuiToastType.Loading -> MuiSpinner()
        MuiToastType.Success, MuiToastType.Info -> {
            val icon = if (type == MuiToastType.Success) MuiIcons.CircleCheck else MuiIcons.Info
            Image(rememberVectorPainter(icon), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(color))
        }
        MuiToastType.Warning, MuiToastType.Error -> Canvas(Modifier.size(16.dp)) {
            val stroke = 1.5.dp.toPx()
            val scale = size.minDimension
            if (type == MuiToastType.Warning) {
                val path = Path().apply {
                    moveTo(scale * 0.5f, scale * 0.1f)
                    lineTo(scale * 0.92f, scale * 0.85f)
                    lineTo(scale * 0.08f, scale * 0.85f)
                    close()
                }
                drawPath(path, color, style = Stroke(stroke))
                drawLine(color, Offset(scale * 0.5f, scale * 0.35f), Offset(scale * 0.5f, scale * 0.58f), stroke)
                drawCircle(color, stroke / 2, Offset(scale * 0.5f, scale * 0.72f))
            } else {
                drawCircle(color, radius = scale * 0.42f, style = Stroke(stroke))
                drawLine(color, Offset(scale * 0.34f, scale * 0.34f), Offset(scale * 0.66f, scale * 0.66f), stroke)
                drawLine(color, Offset(scale * 0.34f, scale * 0.66f), Offset(scale * 0.66f, scale * 0.34f), stroke)
            }
        }
    }
}
