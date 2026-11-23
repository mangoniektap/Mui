/** @file Compose 液态玻璃的动画、几何与离屏渲染。 */
package com.mui.glass

import android.graphics.RenderEffect
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mui.token.MuiElevation
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt

/** easeOutQuad：起步快、越走越慢（= animateGlassPress 的曲线） */
val EaseOutQuad: Easing = Easing { t -> 1f - (1f - t) * (1f - t) }

/** 玻璃影子的档位查表结果：颜色 / 偏移 / 模糊。 */
data class GlassShadowParams(val color: Color, val dx: Float, val dy: Float, val blur: Float)

/**
 * 运动惯性状态：两个时间常数不同的跟随点之差（带通位移）。
 *
 * 不做数值微分 —— 原实现用 a = (v - pv)/dt 驱动弹簧，人手无法匀速，
 * 位置量化与手速微抖会被 1/dt² 放大，弹簧被持续激励 ⇒ 抖动。
 * 这里 dt 只出现在指数平滑的指数里：fast 紧跟（20ms）、slow 慢跟（55ms）。
 * lag = fast - slow 是**带符号的速度量**，其符号只代表运动方向、不是相位，
 * 所以 target 取 |lag|（运动强度）：左右 / 上下拖动的形变完全一致。
 * target 已两级平滑，strain 只做一阶跟随：不再串欠阻尼弹簧（过冲会把 1+ε 顶出
 * 画布的固定 margin 导致轮廓被切平，还会拖出 300ms+ 的慢动作回落）。
 */
class GlassInertiaState {
    /** 最近一次量到的节点位置（root 坐标） */
    var position: Offset = Offset.Zero

    private var _strain by mutableFloatStateOf(0f)
    private var _axis by mutableIntStateOf(0)

    /** 沿应变轴的应变 ε ≥ 0（运动强度，与运动方向无关） */
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
    private var fastX = 0f
    private var fastY = 0f
    private var slowX = 0f
    private var slowY = 0f
    private var lastFrameNanos = 0L

    /** 每帧推进（nowNanos = 帧时钟），只量 position 的变化 */
    fun step(nowNanos: Long, motion: Int) {
        val px = position.x
        val py = position.y
        if (!ready) {
            fastX = px
            slowX = px
            fastY = py
            slowY = py
            lastFrameNanos = nowNanos
            ready = true
            strain = 0f
            axis = 0
            return
        }
        val dt = ((nowNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        val kf = 1f - exp(-dt / 0.02f)
        val ks = 1f - exp(-dt / 0.055f)
        fastX += (px - fastX) * kf
        fastY += (py - fastY) * kf
        slowX += (px - slowX) * ks
        slowY += (py - slowY) * ks
        val lx = fastX - slowX
        val ly = fastY - slowY
        // 轴只取横 / 纵，用平滑后的 lag 判，带 25% 滞回
        val ax = abs(lx)
        val ay = abs(ly)
        if (axis == 0 && ay > ax * 1.25f) axis = 1
        else if (axis == 1 && ax > ay * 1.25f) axis = 0
        // 匀速时 lag ≈ v·(τs−τf)，8px 吃满一档；τs−τf 由 60ms 缩到 35ms，
        // 除数同比缩小才保持上一轮定好的灵敏度。
        // 取 |lag|：符号只是运动方向，不是「起步 / 急停」相位；当相位用会让
        // 向左走 ε<0、shader 把放大轴翻到垂直轴 ⇒ 玻璃反而变粗。
        val along = if (axis == 0) lx else ly
        val target = (abs(along) / 8f).coerceIn(0f, 1f) * 0.05f * motion
        // target 已两级平滑，只做一阶跟随：二阶弹簧会过冲并拖出慢动作。
        // 20/55/28ms 三条时间常数让起步跟手、回落干脆，不再粘稠
        strain += (target - strain) * (1f - exp(-dt / 0.028f))
        lastFrameNanos = nowNanos
    }

    /** 档位关闭：全部清零，下一轮重新起算 */
    fun reset() {
        ready = false
        strain = 0f
        axis = 0
    }
}

/**
 * 液态玻璃画布（引擎唯一出口，对应 web 版 <GlassLens>）。
 *
 * 节点内容 = 调用方画进离屏图层的背面（页面 / 轨道 / 图像），经 AGSL shader
 * 逐像素折射 / 色散 / 本体 / 边缘后输出；轮廓外只画影子。主题 / 参数变化自动重绘。
 *
 * @param shapes 最多 [GLASS_SHAPE_MAX] 块，按顺序叠（后面的盖前面），坐标为画布左上角 dp
 * @param live 每帧重绘（球在动 / 背面跟着变时用）
 * @param fillOutside true = 背面铺满画布（画布本身是背景）；false = 轮廓外只画影子
 * @param motion 运动惯性档位 0..5：0 关闭；只有真的会动的玻璃才给
 * @param body 本体档位；三档间两条通道独立插值（200ms），灰→透明路径不过白
 * @param pressed 按住态：引擎自己跑按压动画，进度只驱动 swell 与换档，不参与光学
 * @param lens 手动指定按压进度 0..1，覆盖 pressed 的动画（多块玻璃各自按压时用）；只驱动外鼓
 * @param lensBand 覆盖 --glass-lens-band（小件玻璃给更窄的斜面，如开关球 0.3）
 * @param lensCompress 覆盖 --glass-lens-compress（凹透镜压缩率，常驻；小件玻璃给小值如 0.2）
 * @param lensDecay 覆盖 --glass-lens-decay（斜面归零位置：0.5 = 顶底归零，越大越早归零）
 * @param shadow 影子档位（引擎层绘制，与 web 的 drop-shadow 同语义）
 * @param backdrop 画背面内容；在 DrawScope 上执行，dp 已按本机密度换算。
 *   放最后是为了能写成尾随 lambda —— 调用点全是 `GlassLens(...) { paintPageBackdrop(...) }` 这个形状。
 */
@Composable
fun GlassLens(
    shapes: List<LensShape>,
    modifier: Modifier = Modifier,
    live: Boolean = false,
    fillOutside: Boolean = false,
    motion: Int = 0,
    body: GlassBody = GlassBody.TRANSPARENT,
    pressed: Boolean = false,
    lens: Float? = null,
    lensBand: Float? = null,
    lensCompress: Float? = null,
    lensDecay: Float? = null,
    shadow: GlassShadowLevel = GlassShadowLevel.SM,
    /** 可选内容层（文字 / 图标，透明底），只折射不缩小。 */
    ink: (DrawScope.() -> Unit)? = null,
    backdrop: DrawScope.() -> Unit,
) = GlassLens(
    pageColor = Color.Unspecified,
    shapes = shapes,
    modifier = modifier,
    live = live,
    fillOutside = fillOutside,
    motion = motion,
    body = body,
    pressed = pressed,
    lens = lens,
    lensBand = lensBand,
    lensCompress = lensCompress,
    lensDecay = lensDecay,
    shadow = shadow,
    ink = ink,
    backdrop = backdrop,
)

/** 控件专用入口。页面图层先录入背面，再叠上随控件移动的轨道。 */
@Composable
internal fun GlassLens(
    pageColor: Color,
    shapes: List<LensShape>,
    modifier: Modifier = Modifier,
    live: Boolean = false,
    fillOutside: Boolean = false,
    motion: Int = 0,
    body: GlassBody = GlassBody.TRANSPARENT,
    pressed: Boolean = false,
    lens: Float? = null,
    lensBand: Float? = null,
    lensCompress: Float? = null,
    lensDecay: Float? = null,
    shadow: GlassShadowLevel = GlassShadowLevel.SM,
    ink: (DrawScope.() -> Unit)? = null,
    backdrop: DrawScope.() -> Unit,
) {
    val glassPalette = com.mui.theme.LocalGlassPalette.current
    val isDark = com.mui.theme.LocalIsDark.current
    val densityScale = LocalDensity.current.density
    val source = LocalGlassBackdrop.current.takeIf { pageColor != Color.Unspecified }
    var lensOrigin by remember(source) { mutableStateOf<Offset?>(null) }
    val shader = remember { GlassShaderSource.compile() }
    val input = rememberGraphicsLayer()
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

    LaunchedEffect(live, motion) {
        if (motion <= 0) inertia.reset()
        while (live || motion > 0) {
            val nanos = withFrameNanos { it }
            if (motion > 0) inertia.step(nanos, motion)
            if (live) frameTick.value++
        }
    }

    val shadowSpec = (if (isDark) MuiElevation.glassShadowDark else MuiElevation.glassShadowLight)[shadow.key]

    Box(
        modifier
            .onGloballyPositioned {
                val position = it.localToRoot(Offset.Zero)
                inertia.position = position / densityScale
                if (source != null) lensOrigin = position
            }
            .drawWithCache {
                val width = size.width.roundToInt()
                val height = size.height.roundToInt()
                if (width == 0 || height == 0) return@drawWithCache onDrawBehind {}

                val dpr = density
                val pressSwell = GlassTokens.pressSwell * (lens ?: press.value)
                val centers = FloatArray(GLASS_SHAPE_MAX * 2) { -1e4f }
                val halves = FloatArray(GLASS_SHAPE_MAX * 2)
                val radii = FloatArray(GLASS_SHAPE_MAX)
                val bulges = FloatArray(GLASS_SHAPE_MAX)
                shapes.take(GLASS_SHAPE_MAX).forEachIndexed { index, shape ->
                    centers[index * 2] = shape.cx * dpr
                    centers[index * 2 + 1] = shape.cy * dpr
                    halves[index * 2] = shape.hw * dpr
                    halves[index * 2 + 1] = shape.hh * dpr
                    radii[index] = shape.radius * dpr
                    bulges[index] = (shape.bulge + shape.swell * min(shape.hw, shape.hh) * 2f * pressSwell) * dpr
                }

                shader.setFloatUniform("uRes", width.toFloat(), height.toFloat())
                shader.setFloatUniform("uCenter", centers)
                shader.setFloatUniform("uHalf", halves)
                shader.setFloatUniform("uRadius", radii)
                shader.setFloatUniform("uBulge", bulges)
                shader.setFloatUniform("uBendBand", lensBand ?: GlassTokens.lensBand)
                shader.setFloatUniform("uBend", GlassTokens.lensBend)
                shader.setFloatUniform("uCompress", lensCompress ?: GlassTokens.lensCompress)
                shader.setFloatUniform("uChroma", GlassTokens.lensChromaDp * dpr)
                shader.setFloatUniform("uSpan", GlassTokens.lensSpan)
                shader.setFloatUniform("uDecay", lensDecay ?: GlassTokens.lensDecay)
                shader.setFloatUniform("uBody", jade)
                shader.setFloatUniform("uGray", gray)
                shader.setColorUniform("uGrayColor", glassPalette.grayColor.toArgb())
                shader.setFloatUniform("uAlpha", 1f - (1f - GlassTokens.grayAlpha) * gray)
                shader.setColorUniform("uBodyColor", glassPalette.bodyColor.toArgb())
                shader.setFloatUniform("uBodyShade", GlassTokens.bodyShade)
                shader.setFloatUniform("uBodyPeak", GlassTokens.bodyPeak)
                shader.setFloatUniform("uBodyFadeX", GlassTokens.bodyFadeX)
                shader.setColorUniform("uRimLight", glassPalette.rimLight.toArgb())
                shader.setColorUniform("uRimDark", glassPalette.rimDark.toArgb())
                shader.setFloatUniform("uRimTop", glassPalette.rimTop)
                shader.setFloatUniform("uRimBottom", glassPalette.rimBottom)
                shader.setFloatUniform("uRimSide", glassPalette.rimSide)
                shader.setFloatUniform("uRimWidth", GlassTokens.rimWidthDp)
                val horizontal = inertia.axis == 0
                shader.setFloatUniform("uMotionAxis", if (horizontal) 1f else 0f, if (horizontal) 0f else 1f)
                shader.setFloatUniform("uMotionAmp", if (motion > 0) inertia.strain else 0f)
                shader.setFloatUniform("uDpr", dpr)
                shader.setFloatUniform("uFill", if (fillOutside) 1f else 0f)
                shader.setFloatUniform("uHasInk", if (ink == null) 0f else 1f)
                shader.setColorUniform("uShadowColor", shadowSpec?.color?.toArgb() ?: android.graphics.Color.TRANSPARENT)
                shader.setFloatUniform("uShadowOffset", shadowSpec?.dx?.toPx() ?: 0f, shadowSpec?.dy?.toPx() ?: 0f)
                shader.setFloatUniform("uShadowBlur", shadowSpec?.blur?.toPx() ?: 0f)
                input.renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "uTex").asComposeRenderEffect()

                onDrawBehind {
                    if (live) frameTick.intValue
                    val viewport = size
                    // 两份输入在同一张 GPU 图层里连续录制。每块都裁到逻辑画布范围，
                    // shader 采样时再分别夹取，防止折射越界串到另一块。
                    input.record(this, layoutDirection, IntSize(width, if (ink == null) height else height * 2)) {
                        clipRect(right = viewport.width, bottom = viewport.height) {
                            drawContext.size = viewport
                            if (pageColor != Color.Unspecified) drawRect(pageColor)
                            val pageOrigin = source?.origin
                            val glassOrigin = lensOrigin
                            if (source != null && pageOrigin != null && glassOrigin != null) {
                                val offset = pageOrigin - glassOrigin
                                translate(offset.x, offset.y) { drawLayer(source.layer) }
                            }
                            backdrop()
                        }
                        if (ink != null) {
                            translate(top = viewport.height) {
                                clipRect(right = viewport.width, bottom = viewport.height) {
                                    drawContext.size = viewport
                                    ink()
                                }
                            }
                        }
                    }
                    clipRect { drawLayer(input) }
                }
            },
    )
}

/** 把「页面」铺满画布：透镜背面最底下的一层（调用方再往上画结构） */
fun DrawScope.paintPageBackdrop(pageColor: androidx.compose.ui.graphics.Color) {
    drawRect(pageColor)
}

/**
 * 玻璃画布占位：内容按 (width × height) **固定测量**（不受父约束夹），
 * 再显式摆在 (offsetX, offsetY) —— 等价 web 的 `position:absolute; inset:-Npx`。
 *
 * 父容器的约束与对齐都不参与几何（直接父容器必须是 TopStart 对齐）；
 * [GlassLens] 的 uRes 与 [LensShape] 全部以这块画布为坐标系。
 * 外扩时 offsetX/offsetY 为负：画布比玻璃大一圈，折射与外鼓才有地方画。
 *
 * 本节点对父容器**报告 0×0**：web 里这层是 `position:absolute`，不参与父级排布。
 * 早先返回 (width, height) 会让父容器把画布算进自己的尺寸 —— 分段控件底座因此比
 * 里面那排段高出「上下各一圈 margin」，整块内容被顶到上沿（实测底座 65.8dp vs 段 40dp）。
 *
 * @param width 画布宽（= 玻璃外形 + 四周 margin）
 * @param height 画布高
 * @param offsetX 画布左缘相对父容器左缘的偏移
 * @param offsetY 画布上缘相对父容器上缘的偏移
 */
fun Modifier.glassCanvas(
    width: Dp,
    height: Dp,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
): Modifier = layout { measurable, _ ->
    val w = width.roundToPx()
    val h = height.roundToPx()
    val p = measurable.measure(Constraints.fixed(w, h))
    // 画布自己仍是 w×h（GlassLens 的 graphicsLayer 要按它开），只是不向父级报尺寸
    layout(0, 0) { p.place(offsetX.roundToPx(), offsetY.roundToPx()) }
}

/**
 * `glassCanvas` 的整像素版：尺寸与偏移都由调用点算好（px）。
 *
 * 存在的理由：玻璃的球心 / 背面结构都靠「真实边界 − 画布原点」反推，调用点必须和摆放层
 * **用同一套取整**。dp 版内部是 `roundToPx()`，而它对负数不是对称的 ——
 * `round(13.5) = 14` 但 `round(-13.5) = -13`。记账时用正的外扩量、摆放时传负的 dp，
 * 两者就差 1px：开关的球整体偏下 1px（实测球心 993 vs 轨道中心 992），看起来"没垂直居中"。
 *
 * @param widthPx 画布宽（px）
 * @param heightPx 画布高（px）
 * @param offsetXPx 画布左缘相对父容器左缘的偏移（px，可为负）
 * @param offsetYPx 画布上缘相对父容器上缘的偏移（px，可为负）
 */
fun Modifier.glassCanvasPx(
    widthPx: Int,
    heightPx: Int,
    offsetXPx: Int,
    offsetYPx: Int,
): Modifier = layout { measurable, _ ->
    val p = measurable.measure(Constraints.fixed(widthPx, heightPx))
    layout(0, 0) { p.place(offsetXPx, offsetYPx) }
}
