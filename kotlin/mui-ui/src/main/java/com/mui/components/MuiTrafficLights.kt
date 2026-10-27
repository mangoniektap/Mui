package com.mui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mui.theme.LocalMuiPalette
import com.mui.theme.muiElevation
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource
import kotlin.math.hypot
import kotlin.math.max
import kotlinx.coroutines.launch

/** 一盏灯：值 / 名称 / 自身颜色（红绿灯用固定三色，不读主题） */
data class TrafficLightItem(
    /** 唯一值：点亮时随回调传出 */
    val value: String,
    /** 无障碍名称 */
    val label: String,
    /** 灯自身颜色：光斑铺的就是它 */
    val color: Color,
)

/** 扩散转场的原点：灯心窗口坐标与该灯自身颜色 */
data class TrafficLightOrigin(
    /** 灯心在窗口中的横坐标 */
    val x: Float,
    /** 灯心在窗口中的纵坐标 */
    val y: Float,
    /** 该灯自身的颜色：光斑铺满视口用的颜色 */
    val color: Color,
)

/** 三档直径：灯是圆点，size 只改直径 */
enum class TrafficLightSize(val diameter: Dp) {
    Sm(10.dp),
    Md(12.dp),
    Lg(16.dp),
}

/** 点击后光斑从灯心扩散铺满视口的时长 */
private const val ORB_GROW_MS = 520

/** 铺满后淡出到目标内容的时长 */
private const val ORB_FADE_MS = 180

/** 扩散缓动：cubic-bezier(0.22, 1, 0.36, 1) */
private val ORB_EASING = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/** 在途的光斑：原点 + 扩散进度 */
private class OrbState(val origin: TrafficLightOrigin) {
    /** 0..1：0 = 灯心一个点，1 = 铺满视口 */
    val progress = Animatable(0f)
    /** 淡出进度 1..0（铺满后） */
    val fade = Animatable(1f)
}

/**
 * 红绿灯：一排圆点开关，点击的灯先以自身颜色从灯心扩散铺满视口，
 * 再淡出到目标内容——切换内容是「被颜色盖过去」而不是硬跳。
 * 光斑挂全屏 Popup（不被容器裁剪）；铺满的瞬间才提交切换，避免淡出时闪出旧内容。
 *
 * @param items 灯列表，顺序即左到右
 * @param value 受控：当前点亮的灯
 * @param defaultValue 非受控：默认点亮的灯
 * @param onValueChange 切换回调；第二参给出灯心窗口坐标与颜色（在光斑铺满视口时触发）
 * @param size 灯直径档位（默认 Md）
 * @param disabled 禁用：半透明且不可点
 * @param interactionSource 交互源（聚焦环出口）
 */
@Composable
fun MuiTrafficLights(
    items: List<TrafficLightItem>,
    modifier: Modifier = Modifier,
    value: String? = null,
    defaultValue: String? = null,
    onValueChange: ((String, TrafficLightOrigin) -> Unit)? = null,
    size: TrafficLightSize = TrafficLightSize.Md,
    disabled: Boolean = false,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
) {
    val palette = LocalMuiPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inner by remember { mutableStateOf(defaultValue ?: items.firstOrNull()?.value) }
    val active = value ?: inner

    /** 每盏灯的灯心（窗口坐标），点击时取用 */
    val centers = remember { mutableMapOf<String, Offset>() }
    var orb by remember { mutableStateOf<OrbState?>(null) }

    fun reducedMotion(): Boolean = android.provider.Settings.Global.getFloat(
        context.contentResolver,
        android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f

    fun handleClick(item: TrafficLightItem) {
        if (disabled || item.value == active) return
        val center = centers[item.value] ?: Offset.Zero
        val origin = TrafficLightOrigin(center.x, center.y, item.color)
        val commit = {
            if (value == null) inner = item.value
            onValueChange?.invoke(item.value, origin)
        }
        // 减弱动态：不跑光斑，直接切换
        if (reducedMotion()) {
            commit()
            return
        }
        scope.launch {
            val state = OrbState(origin)
            orb = state
            state.progress.animateTo(1f, tween(ORB_GROW_MS, easing = ORB_EASING))
            // 先同步把底层内容切到目标，再淡出光斑
            commit()
            state.fade.animateTo(0f, tween(ORB_FADE_MS, easing = EaseOut))
            if (orb === state) orb = null
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            val shape = CircleShape
            Box(
                Modifier
                    .alpha(if (disabled) 0.5f else 1f)
                    .size(size.diameter)
                    .clip(shape)
                    .background(item.color, shape)
                    .muiElevation("xs", shape)
                    .muiFocusRing(interactionSource, shape)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInWindow()
                        centers[item.value] = Offset(
                            pos.x + coords.size.width / 2f,
                            pos.y + coords.size.height / 2f,
                        )
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = !disabled,
                    ) { handleClick(item) },
            )
        }
    }

    // 光斑：全屏 Popup 承载，从灯心扩散铺满视口再淡出
    val currentOrb = orb
    if (currentOrb != null) {
        Popup(
            alignment = Alignment.TopStart,
            properties = PopupProperties(focusable = false),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            ) {
                val state = currentOrb
                Canvas(Modifier.fillMaxSize()) {
                    val progress = state.progress.value
                    if (progress <= 0f) return@Canvas
                    val radius = hypot(
                        max(state.origin.x, this.size.width - state.origin.x),
                        max(state.origin.y, this.size.height - state.origin.y),
                    ) + 32f
                    drawCircle(
                        color = state.origin.color.copy(alpha = state.origin.color.alpha * state.fade.value),
                        radius = radius * progress,
                        center = Offset(state.origin.x, state.origin.y),
                    )
                }
            }
        }
    }
}
