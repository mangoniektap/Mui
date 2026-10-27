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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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
 * @param live 每帧重绘（球在动 / 背面跟着变时用）
 * @param fillOutside true = 背面铺满画布（画布本身是背景）；false = 轮廓外只画影子
 * @param motion 运动惯性档位 0..5：0 关闭；只有真的会动的玻璃才给
 * @param body 本体档位；三档间两条通道独立插值（200ms），灰→透明路径不过白
 * @param pressed 按住态：引擎自己跑按压动画，进度同时驱动中心凹透镜与 swell
 * @param lens 手动指定按压进度 0..1，覆盖 pressed 的动画（多块玻璃各自按压时用）
 * @param lensBand 覆盖 --glass-lens-band（小件玻璃给更窄的斜面，如开关球 0.3）
 * @param lensCompress 覆盖 --glass-lens-compress（按压压缩率，小件玻璃给小值如 0.2）
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
    shadow: GlassShadowLevel = GlassShadowLevel.SM,
    backdrop: DrawScope.() -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val glassPalette = com.mui.theme.LocalGlassPalette.current
    val isDark = com.mui.theme.LocalIsDark.current

    val shader = remember { GlassShaderSource.compile() }
    val bitmapRef = remember { mutableStateOf<ImageBitmap?>(null) }
    val drawScopeHelper = remember { CanvasDrawScope() }
    val inertia = remember { GlassInertiaState() }
    val frameTick = remember { mutableIntStateOf(0) }

    /* 画布尺寸在**组合期**拿出来（onSizeChanged 写 state → 触发重组），
       再当成普通值捕获进 graphicsLayer 的块里。
       不能在块内读 `size`：图层参数只在节点第一次落位时算一遍，那一刻尺寸还是 0，
       而"从 0 变成真值"不会让块重跑 —— effect 会永远带着 uRes = 1×1。 */
    var nodeSize by remember { mutableStateOf(IntSize.Zero) }
    val canvasW = nodeSize.width.toFloat()
    val canvasH = nodeSize.height.toFloat()

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
        else table[shadow.key]?.let { spec ->
            with(density) {
                GlassShadowParams(spec.color, spec.dx.toPx(), spec.dy.toPx(), spec.blur.toPx())
            }
        }
    }

    Box(
        modifier
            .onSizeChanged { nodeSize = it }
            .onGloballyPositioned { inertia.position = it.positionInRoot() }
            /* ① 折射层。**必须排在 drawBehind 之前**：这一层就是 web 那面 canvas，
               它的内容 = 下面画出来的背面，着色器折的就是它。
               顺序反了（drawBehind 在前）背面会落在图层外面，uTex 是空的 ——
               折空场等于不折，光学特性会整块消失。
               `size` 从图层作用域读（绘制期），不在组合期量。 */
            .graphicsLayer {
                frameTick.value // live 时逐帧重建 effect，保证 uniform 生效
                val dpr = density.density
                /* 尺寸来自组合期捕获的值（见上方 canvasW/canvasH）。
                   这里**不早退**：块可能带着 0 尺寸跑一次，早退会把 renderEffect 留成 null，
                   而下一帧不会因为"尺寸变了"自动重来。 */
                val w = canvasW.coerceAtLeast(1f)
                val h = canvasH.coerceAtLeast(1f)
                val pressSwell = GlassTokens.pressSwell * press.value
                val cen = FloatArray(GLASS_SHAPE_MAX * 2)
                val hal = FloatArray(GLASS_SHAPE_MAX * 2)
                val rad = FloatArray(GLASS_SHAPE_MAX)
                val bul = FloatArray(GLASS_SHAPE_MAX)
                for (i in 0 until GLASS_SHAPE_MAX) {
                    val s = shapes.getOrNull(i)
                    // 没用到的槽位必须停到画布外（web 的 EMPTY 是 cx/cy = -1e4）。
                    // 留全 0 的话 SDF 在画布原点退化成一个半径 0 的点，那一点 cov = 1，
                    // 于是一颗灰点正好落在玻璃画布的左上角。
                    if (s == null) {
                        cen[i * 2] = -1e4f
                        cen[i * 2 + 1] = -1e4f
                        continue
                    }
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
                shader.setFloatUniform("uCompress", floatArrayOf(lensCompress ?: GlassTokens.lensCompress))
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
                // rimWidth 传 dp：着色器里自己乘 uDpr（web 也是 px 原值 × dpr 一次）。
                // 这里再乘一遍 dpr 会让边缘窗口宽度翻倍（2dp → 9px 而非 4.5px），
                // 白心被啃掉一圈，开关的球看起来偏小、边缘发脏。
                shader.setFloatUniform("uRimWidth", floatArrayOf(GlassTokens.rimWidthDp))
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
                    // uShadowColor 是 layout(color)，只能用 color API 设；透明档传 TRANSPARENT
                    shader.setColorUniform("uShadowColor", android.graphics.Color.TRANSPARENT)
                    shader.setFloatUniform("uShadowOffset", floatArrayOf(0f, 0f))
                    shader.setFloatUniform("uShadowBlur", floatArrayOf(1f))
                }
                renderEffect = android.graphics.RenderEffect
                    .createRuntimeShaderEffect(shader, "uTex")
                    .asComposeRenderEffect()
            }
            /* ② 背面。画进上面那个图层里 —— 它是 uTex 的唯一来源，
               上屏时会被着色器的输出整个换掉，所以这里画的不是"玻璃"，是"玻璃背后的画面"。 */
            .drawBehind {
                frameTick.value // live：背面跟着球走 / 状态变
                /* 尺寸还没经组合期回流之前先不画：那一刻着色器的 uRes 还是 1×1，
                   画了也会被折成一片糊。等下一帧尺寸到位再画。 */
                if (canvasW <= 0f || canvasH <= 0f) return@drawBehind
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) return@drawBehind
                val wi = w.toInt()
                val hi = h.toInt()
                val bmp = bitmapRef.value
                    ?.takeIf { it.width == wi && it.height == hi }
                    ?: ImageBitmap(wi, hi).also { bitmapRef.value = it }
                drawScopeHelper.draw(density, layoutDirection, Canvas(bmp), Size(w, h)) {
                    backdrop()
                }
                drawImage(bmp)
            }
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
