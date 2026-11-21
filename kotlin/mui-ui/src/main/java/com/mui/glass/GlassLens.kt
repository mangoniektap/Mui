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
import androidx.compose.ui.graphics.asAndroidBitmap
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
import kotlin.math.min
import kotlin.math.roundToInt

/** easeOutQuad：起步快、越走越慢（= animateGlassPress 的曲线） */
val EaseOutQuad: Easing = Easing { t -> 1f - (1f - t) * (1f - t) }

/** 玻璃影子的档位查表结果：颜色 / 偏移 / 模糊（dp） */
data class GlassShadowParams(val color: androidx.compose.ui.graphics.Color, val dx: Float, val dy: Float, val blur: Float)

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
    private var lastT = 0f

    /** 每帧推进（nowNanos = 帧时钟），只量 position 的变化 */
    fun step(nowNanos: Long, motion: Int) {
        val px = position.x
        val py = position.y
        val now = nowNanos / 1_000_000f
        if (!ready) {
            fastX = px
            slowX = px
            fastY = py
            slowY = py
            lastT = now
            ready = true
            strain = 0f
            axis = 0
            return
        }
        val dt = ((now - lastT) / 1000f).coerceIn(0.001f, 0.05f)
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
        lastT = now
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
 * 节点内容 = 调用方画进离屏位图的背面（页面 / 轨道 / 图像），经 AGSL shader
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
    /** 可选内容层（文字 / 图标，透明底）：单独注入 uInk，只折射不缩小 */
    ink: (DrawScope.() -> Unit)? = null,
    backdrop: DrawScope.() -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val glassPalette = com.mui.theme.LocalGlassPalette.current
    val isDark = com.mui.theme.LocalIsDark.current

    val shader = remember { GlassShaderSource.compile() }
    /* ⚠️ renderEffect 必须每帧新建：Compose 的 graphicsLayer 靠「参数变了」才会
       重新配置并 invalidate 图层。复用同一个 RenderEffect 对象时参数恒等，图层停在第一帧
       （那时 uRes 还是 1×1、SDF 全在画布外），整块玻璃直接消失。
       屏闪的根因不是它，是 ink 位图与 backdrop 分处两个阶段更新 —— 那个在下面单独修。 */
    val bitmapRef = remember { mutableStateOf<ImageBitmap?>(null) }
    val inkBitmapRef = remember { mutableStateOf<ImageBitmap?>(null) }
    val inkFallback = remember { ImageBitmap(1, 1) }
    /** 已绑到 uInk 的位图（普通引用，不参与 snapshot 失效） */
    val boundInkRef = remember { arrayOfNulls<ImageBitmap?>(1) }
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
                val pressSwell = GlassTokens.pressSwell * (lens ?: press.value)
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
                shader.setFloatUniform("uChroma", floatArrayOf(GlassTokens.lensChromaDp * dpr))
                shader.setFloatUniform("uSpan", floatArrayOf(GlassTokens.lensSpan))
                shader.setFloatUniform("uDecay", floatArrayOf(lensDecay ?: GlassTokens.lensDecay))
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
                /* 内容层（第二张位图 uInk）只在这里**绑定**：
                   位图与 BitmapShader 只在尺寸变化时新建，内容由 drawBehind 与
                   backdrop 同一次 draw 重画（见下）。两层各自负责压缩 / 不压缩的语义不变。
                   早先两者分处两个阶段且每帧重建 GPU 资源，采样会错开一帧。 */
                if (ink != null && w > 0f && h > 0f) {
                    val iw = w.toInt().coerceAtLeast(1)
                    val ih = h.toInt().coerceAtLeast(1)
                    val ibmp = inkBitmapRef.value
                        ?.takeIf { it.width == iw && it.height == ih }
                        ?: ImageBitmap(iw, ih).also { inkBitmapRef.value = it }
                    if (boundInkRef[0] !== ibmp) {
                        shader.setInputShader(
                            "uInk",
                            android.graphics.BitmapShader(
                                ibmp.asAndroidBitmap(),
                                android.graphics.Shader.TileMode.CLAMP,
                                android.graphics.Shader.TileMode.CLAMP,
                            ),
                        )
                        boundInkRef[0] = ibmp
                    }
                    shader.setFloatUniform("uHasInk", floatArrayOf(1f))
                } else {
                    if (boundInkRef[0] !== inkFallback) {
                        shader.setInputShader(
                            "uInk",
                            android.graphics.BitmapShader(
                                inkFallback.asAndroidBitmap(),
                                android.graphics.Shader.TileMode.CLAMP,
                                android.graphics.Shader.TileMode.CLAMP,
                            ),
                        )
                        boundInkRef[0] = inkFallback
                    }
                    shader.setFloatUniform("uHasInk", floatArrayOf(0f))
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
                // ink 与 backdrop 必须在同一次 draw 里更新：uTex 采的是本节点的绘制结果，
                // uInk 采的是这张 CPU 位图，两者错开一个阶段就会逐帧相差一帧。
                if (ink != null) {
                    inkBitmapRef.value?.let { ibmp ->
                        ibmp.asAndroidBitmap().eraseColor(android.graphics.Color.TRANSPARENT)
                        drawScopeHelper.draw(
                            density,
                            layoutDirection,
                            Canvas(ibmp),
                            Size(ibmp.width.toFloat(), ibmp.height.toFloat()),
                        ) {
                            ink()
                        }
                    }
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
