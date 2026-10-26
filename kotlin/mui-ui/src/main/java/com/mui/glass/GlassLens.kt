package com.mui.glass

import android.graphics.RuntimeShader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Size
import com.mui.token.MuiElevation
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

/** easeOutQuad：起步快、越走越慢（= animateGlassPress 的曲线） */
val EaseOutQuad: Easing = Easing { t -> 1f - (1f - t) * (1f - t) }

/** 玻璃影子的档位查表结果：颜色 / 偏移 / 模糊（dp） */
data class GlassShadowParams(val color: androidx.compose.ui.graphics.Color, val dx: Float, val dy: Float, val blur: Float)

/**
 * 运动惯性状态：画布位置 → 速度 → 沿运动方向的加速度 → 阻尼弹簧。
 * 应变由加速度驱动（匀速不形变），轴只取横/纵（带 25% 滞回），方向在速度归零后锁存。
 * position 取 root 坐标（web 版用文档坐标，页面滚动会被计入，拇指拖动场景两者一致）。
 */
class GlassInertiaState {
    /** 最近一次量到的节点位置（root 坐标） */
    var position: Offset = Offset.Zero

    private var _strain by mutableFloatStateOf(0f)
    private var _axis by mutableIntStateOf(0)

    /** 沿应变轴的应变 ε（正 = 拉长 / 负 = 挤扁） */
    var strain: Float
        get() = _strain
        private set(value) {
            _strain = value
        }

    /** 应变轴：0 = 横 / 1 = 纵 */
    var axis: Int
        get() = _axis
        private set(value) {
            _axis = value
        }

    private var ready = false
    private var lastX = 0f
    private var lastY = 0f
    private var lastT = 0f
    private var vx = 0f
    private var vy = 0f
    private var pvx = 0f
    private var pvy = 0f
    private var dir = 1
    private var acc = 0f
    private var vel = 0f

    private var _strain by mutableFloatStateOf(0f)
    private var _axis by mutableIntStateOf(0)

    /** 每帧推进（nowNanos = 帧时钟），只量 position 的变化 */
    fun step(nowNanos: Long, motion: Int) {
        val px = position.x
        val py = position.y
        val now = nowNanos / 1_000_000f
        val dt = if (ready) min((now - lastT) / 1000f, 0.02f) else 0f
        if (ready && dt > 0f) {
            // 速度低通（时间常数 45ms）
            val kv = 1f - exp(-dt / 0.045f)
            vx += ((px - lastX) / dt - vx) * kv
            vy += ((py - lastY) / dt - vy) * kv
            val sp = hypot(vx, vy)
            val ax = abs(vx)
            val ay = abs(vy)
            // 轴带 25% 滞回；速度低于 40px/s 锁存轴与方向（急停要靠锁存的方向认出挤压侧）
            if (sp > 40f) {
                if (axis == 0 && ay > ax * 1.25f) axis = 1
                else if (axis == 1 && ax > ay * 1.25f) axis = 0
                val along = if (axis == 0) vx else vy
                if (abs(along) > 40f) dir = if (along > 0f) 1 else -1
            }
            // 沿运动方向的加速度（再低通 25ms）
            val aRaw = (if (axis == 0) vx - pvx else vy - pvy) / dt * dir
            acc += (aRaw - acc) * (1f - exp(-dt / 0.025f))
            pvx = vx
            pvy = vy
            // 目标应变 ∝ 加速度：3500px/s² 吃满一档，每档 3.5%
            val target = (acc / 3500f).coerceIn(-1f, 1f) * 0.035f * motion
            // 刚度 700、阻尼比 ≈ 0.5
            vel += (-700f * (strain - target) - 27f * vel) * dt
            strain = (strain + vel * dt).coerceIn(-0.35f, 0.45f)
        }
        lastX = px
        lastY = py
        lastT = now
        ready = true
    }

    /** 档位关闭：全部清零，下一轮重新起算 */
    fun reset() {
        ready = false
        pvx = 0f
        pvy = 0f
        acc = 0f
        vel = 0f
        strain = 0f
        axis = 0
    }
}

/**
 * 液态玻璃画布（引擎唯一出口，对应 web 版 <GlassLens>）。
 *
 * 节点内容 = 调用方画进离屏位图的背面（页面 / 轨道 / 图像），经 AGSL shader
 * 逐像素折射 / 色散 / 本体 / 边缘后输出；轮廓外只画影子。主题 / 参数变化自动重绘。
 *
 * @param shapes 最多 [GLASS_SHAPE_MAX] 块，按顺序叠（后面的盖前面），坐标为画布左上角 dp
 * @param backdrop 画背面内容；在 DrawScope 上执行，dp 已按本机密度换算
 * @param live 每帧重绘（球在动 / 背面跟着变时用）
 * @param fillOutside true = 背面铺满画布（画布本身是背景）；false = 轮廓外只画影子
 * @param motion 运动惯性档位 0..5：0 关闭；只有真的会动的玻璃才给
 * @param body 本体档位；三档间两条通道独立插值（200ms），灰→透明路径不过白
 * @param pressed 按住态：引擎自己跑按压动画，进度同时驱动中心凹透镜与 swell
 * @param lens 手动指定按压进度 0..1，覆盖 pressed 的动画（多块玻璃各自按压时用）
 * @param lensBand 覆盖 --glass-lens-band（小件玻璃给更窄的斜面，如开关球 0.3）
 * @param shadow 影子档位（引擎层绘制，与 web 的 drop-shadow 同语义）
 */
@Composable
fun GlassLens(
    shapes: List<LensShape>,
    backdrop: DrawScope.() -> Unit,
    modifier: Modifier = Modifier,
    live: Boolean = false,
    fillOutside: Boolean = false,
    motion: Int = 0,
    body: GlassBody = GlassBody.TRANSPARENT,
    pressed: Boolean = false,
    lens: Float? = null,
    lensBand: Float? = null,
    shadow: GlassShadowLevel = GlassShadowLevel.SM,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val glassPalette = com.mui.theme.LocalGlassPalette.current
    val isDark = com.mui.theme.LocalIsDark.current

    var nodeSize by remember { mutableStateOf(IntSize.Zero) }
    val shader = remember { GlassShaderSource.compile() }
    val bitmapRef = remember { mutableStateOf<ImageBitmap?>(null) }
    val drawScopeHelper = remember { CanvasDrawScope() }
    val inertia = remember { GlassInertiaState() }
    val frameTick = remember { mutableIntStateOf(0) }

    // 按压进度：easeOutQuad，时长 = --glass-press-duration
    val press = remember { Animatable(0f) }
    LaunchedEffect(pressed) {
        val target = if (pressed) 1f else 0f
        if (press.value != target) {
            press.animateTo(target, tween(GlassTokens.pressDurationMs.roundToInt(), easing = EaseOutQuad))
        }
    }
    // 本体两条独立通道：灰→透明只走 gray 那条 1→0，路径上不经过纯白
    val jade by animateFloatAsState(
        targetValue = if (body == GlassBody.JADE) 1f else 0f,
        animationSpec = tween(GlassTokens.bodyDurationMs.roundToInt(), easing = EaseOut),
    )
    val gray by animateFloatAsState(
        targetValue = if (body == GlassBody.GRAY) 1f else 0f,
        animationSpec = tween(GlassTokens.bodyDurationMs.roundToInt(), easing = EaseOut),
    )

    // 常驻帧循环：live 逐帧重绘；motion>0 时顺带推进惯性
    val running = live || motion > 0
    LaunchedEffect(running, motion) {
        if (!running) {
            inertia.reset()
            return@LaunchedEffect
        }
        while (true) {
            val nanos = withFrameNanos { it }
            if (motion > 0) inertia.step(nanos, motion)
            if (live) frameTick.value++
        }
    }

    // 影子档位（亮/暗两套）
    val shadowSpec = remember(shadow, isDark) {
        val table = if (isDark) MuiElevation.glassShadowDark else MuiElevation.glassShadowLight
        if (shadow == GlassShadowLevel.NONE) null
        else table[shadow.key]?.let { GlassShadowParams(it.color, it.dx, it.dy, it.blur) }
    }

    Box(
        modifier
            .onSizeChanged { nodeSize = it }
            .onGloballyPositioned { inertia.position = it.positionInRoot() }
            .drawBehind {
                drawTick.value // 订阅 live 帧循环
                val w = size.width
                val h = size.height
                if (w <= 0 || h <= 0) return@drawBehind
                val bmp = bitmapRef.value
                    ?.takeIf { it.width == w && it.height == h }
                    ?: ImageBitmap(w, h).also { bitmapRef.value = it }
                drawScopeHelper.draw(density, layoutDirection, Canvas(bmp), Size(w.toFloat(), h.toFloat())) {
                    backdrop()
                }
                drawImage(bmp)
            }
            .graphicsLayer {
                drawTick.value // live 时逐帧重建 effect，保证 uniform 生效
                val w = nodeSize.width
                val h = nodeSize.height
                if (w <= 0 || h <= 0) return@graphicsLayer
                val dpr = density.density
                val pressSwell = GlassTokens.pressSwell * press.value
                val cen = FloatArray(GLASS_SHAPE_MAX * 2)
                val hal = FloatArray(GLASS_SHAPE_MAX * 2)
                val rad = FloatArray(GLASS_SHAPE_MAX)
                val bul = FloatArray(GLASS_SHAPE_MAX)
                for (i in 0 until GLASS_SHAPE_MAX) {
                    val s = shapes.getOrNull(i) ?: continue
                    cen[i * 2] = s.cx * dpr
                    cen[i * 2 + 1] = s.cy * dpr
                    hal[i * 2] = s.hw * dpr
                    hal[i * 2 + 1] = s.hh * dpr
                    rad[i] = s.radius * dpr
                    bul[i] = (s.bulge + s.swell * min(s.hw, s.hh) * 2f * pressSwell) * dpr
                }
                shader.setFloatUniform("uRes", floatArrayOf(w.toFloat(), h.toFloat()))
                shader.setFloatUniform("uCenter", cen)
                shader.setFloatUniform("uHalf", hal)
                shader.setFloatUniform("uRadius", rad)
                shader.setFloatUniform("uBulge", bul)
                shader.setFloatUniform("uBendBand", floatArrayOf(lensBand ?: GlassTokens.lensBand))
                shader.setFloatUniform("uBend", floatArrayOf(GlassTokens.lensBend))
                shader.setFloatUniform("uCompress", floatArrayOf(GlassTokens.lensCompress))
                shader.setFloatUniform("uLens", floatArrayOf((lens ?: press.value).coerceIn(0f, 1f)))
                shader.setFloatUniform("uChroma", floatArrayOf(GlassTokens.lensChromaDp * dpr))
                shader.setFloatUniform("uBody", floatArrayOf(jade))
                shader.setFloatUniform("uGray", floatArrayOf(gray))
                shader.setFloatUniform(
                    "uGrayColor",
                    floatArrayOf(glassPalette.grayColor.red, glassPalette.grayColor.green, glassPalette.grayColor.blue),
                )
                shader.setFloatUniform("uAlpha", floatArrayOf(1f - (1f - GlassTokens.grayAlpha) * gray))
                shader.setFloatUniform(
                    "uBodyColor",
                    floatArrayOf(glassPalette.bodyColor.red, glassPalette.bodyColor.green, glassPalette.bodyColor.blue),
                )
                shader.setFloatUniform("uBodyShade", floatArrayOf(GlassTokens.bodyShade))
                shader.setFloatUniform("uBodyPeak", floatArrayOf(GlassTokens.bodyPeak))
                shader.setFloatUniform("uBodyFadeX", floatArrayOf(GlassTokens.bodyFadeX))
                shader.setFloatUniform(
                    "uRimLight",
                    floatArrayOf(glassPalette.rimLight.red, glassPalette.rimLight.green, glassPalette.rimLight.blue),
                )
                shader.setFloatUniform(
                    "uRimDark",
                    floatArrayOf(glassPalette.rimDark.red, glassPalette.rimDark.green, glassPalette.rimDark.blue),
                )
                shader.setFloatUniform("uRimTop", floatArrayOf(glassPalette.rimTop))
                shader.setFloatUniform("uRimBottom", floatArrayOf(glassPalette.rimBottom))
                shader.setFloatUniform("uRimSide", floatArrayOf(glassPalette.rimSide))
                shader.setFloatUniform("uRimWidth", floatArrayOf(GlassTokens.rimWidthDp * dpr))
                shader.setFloatUniform(
                    "uMotionAxis",
                    if (inertia.axis == 0) floatArrayOf(1f, 0f) else floatArrayOf(0f, 1f),
                )
                shader.setFloatUniform("uMotionAmp", floatArrayOf(if (motion > 0) inertia.strain else 0f))
                shader.setFloatUniform("uDpr", floatArrayOf(dpr))
                shader.setFloatUniform("uFill", floatArrayOf(if (fillOutside) 1f else 0f))
                if (shadowSpec != null) {
                    shader.setColorUniform("uShadowColor", shadowSpec.color.toArgb())
                    shader.setFloatUniform("uShadowOffset", floatArrayOf(shadowSpec.dx * dpr, shadowSpec.dy * dpr))
                    shader.setFloatUniform("uShadowBlur", floatArrayOf(shadowSpec.blur * dpr))
                } else {
                    shader.setFloatUniform("uShadowColor", floatArrayOf(0f, 0f, 0f, 0f))
                    shader.setFloatUniform("uShadowOffset", floatArrayOf(0f, 0f))
                    shader.setFloatUniform("uShadowBlur", floatArrayOf(1f))
                }
                renderEffect = android.graphics.RenderEffect
                    .createRuntimeShaderEffect(shader, "uTex")
                    .asComposeRenderEffect()
            }
    )
}

/** 把「页面」铺满画布：透镜背面最底下的一层（调用方再往上画结构） */
fun DrawScope.paintPageBackdrop(pageColor: androidx.compose.ui.graphics.Color) {
    drawRect(pageColor)
}
