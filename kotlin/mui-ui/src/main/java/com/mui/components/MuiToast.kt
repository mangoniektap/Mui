package com.mui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.theme.muiElevation
import com.mui.theme.rememberMuiInteractionSource
import com.mui.token.MuiRadius
import kotlinx.coroutines.delay

/** 弹出位置（宿主顶/底） */
enum class ToastSide { Top, Bottom }

/** 进出场时长 */
private const val TOAST_ANIM_MS = 200

/** 单条 toast 的内部条目（id 用于稳定复用与精确移除） */
internal class ToastItem(val id: Long, val message: String, val durationMillis: Long)

/**
 * Toast 状态：收集待显示条目；show 压入一条，自动或点击关闭。
 */
@Stable
class MuiToastState internal constructor() {
    internal val items = mutableStateListOf<ToastItem>()
    private var nextId = 0L

    /**
     * 弹出一条 toast
     * @param message 文本
     * @param durationMillis 自动关闭时长（默认 3000ms）
     */
    fun show(message: String, durationMillis: Long = 3000) {
        items += ToastItem(id = nextId++, message = message, durationMillis = durationMillis)
    }

    internal fun dismiss(id: Long) {
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
 * TODO: web 的类型图标槽（success/info/warning/error/loading）与 next-themes 主题联动未移植。
 *
 * @param state toast 状态（rememberMuiToastState）
 * @param side 弹出位置（默认 Top）
 * @param modifier 宿主布局修饰（应用于 Popup 内的列表）
 */
@Composable
fun MuiToastHost(
    state: MuiToastState,
    modifier: Modifier = Modifier,
    side: ToastSide = ToastSide.Top,
) {
    val palette = LocalMuiPalette.current
    val shape = RoundedCornerShape(MuiRadius.lg)
    val density = LocalDensity.current
    val edgeOffset = with(density) { 16.dp.roundToPx() }
    Popup(
        alignment = if (side == ToastSide.Top) Alignment.TopCenter else Alignment.BottomCenter,
        offset = IntOffset(0, if (side == ToastSide.Top) edgeOffset else -edgeOffset),
        properties = PopupProperties(focusable = false),
    ) {
        Column(
            modifier = modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 新条目靠外侧：顶部侧 newest-first，底部侧反转
            val displayOrder = if (side == ToastSide.Top) state.items.asReversed() else state.items
            for (item in displayOrder) {
                key(item.id) {
                    var visible by remember(item.id) { mutableStateOf(false) }
                    LaunchedEffect(item.id) {
                        visible = true
                        delay(item.durationMillis)
                        visible = false
                        delay(TOAST_ANIM_MS.toLong())
                        state.dismiss(item.id)
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(150)) +
                            slideInVertically(tween(TOAST_ANIM_MS)) { if (side == ToastSide.Top) -it else it },
                        exit = fadeOut(tween(150)) +
                            slideOutVertically(tween(TOAST_ANIM_MS)) { if (side == ToastSide.Top) -it else it },
                    ) {
                        val clickSource = rememberMuiInteractionSource()
                        Box(
                            modifier = Modifier
                                .background(palette.popover, shape)
                                .border(1.dp, palette.border, shape)
                                .muiElevation("lg", shape)
                                .clickable(
                                    interactionSource = clickSource,
                                    indication = null,
                                ) { state.dismiss(item.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            CompositionLocalProvider(LocalMuiContentColor provides palette.popoverForeground) {
                                MuiText(text = item.message, fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
