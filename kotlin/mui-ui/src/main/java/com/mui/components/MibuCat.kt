package com.mui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mui.theme.LocalMuiPalette
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource
import kotlin.math.floor
import kotlin.math.sin
import kotlinx.coroutines.delay

/** 像素猫三专用色：取值与 MREAD 原版亮色逐字节一致，不区分亮暗主题（styles/mibu-cat.css） */
val MibuFur = Color(0xFF17A89A)
val MibuLight = Color(0xFF47D1C1)
val MibuInk = Color(0xFF083B38)

/** 待机时的随机互动：duration 只决定这个动作待多久 */
private val IDLE_ACTS = listOf(
    MibuCatAct.Wave to 1800L,
    MibuCatAct.Wag to 2000L,
    MibuCatAct.Sleep to 3200L,
    MibuCatAct.Hop to 1000L,
)

/** Zzz 气泡飘完一轮的时间（秒） */
private const val ZZZ_PERIOD = 2.2

/** 单帧最多承认的时间（秒），避免切回应用时相位跳一大截 */
private const val MAX_STEP = 0.05f

/** 系统动画时长缩放（0 = 等效 prefers-reduced-motion） */
private fun animatorScaleZero(context: android.content.Context): Boolean {
    val s = android.provider.Settings.Global.getFloat(
        context.contentResolver,
        android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    return s == 0f
}

/**
 * 像素猫标记：整格矩形拼出猫身，姿态由帧循环逐帧写入（CatPose 纯函数驱动）。
 * 三种颜色只读 MibuFur / MibuLight / MibuInk，与 MREAD 原版一致、不区分主题。
 *
 * @param act 当前动作；姿态朝目标连续逼近，换动作无跳变
 * @param modifier 画布尺寸（正方形，24 格铺满）
 */
@Composable
fun MibuCatMark(act: MibuCatAct, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val reduced = remember { animatorScaleZero(context) }
    var target by remember { mutableStateOf(if (reduced) mibuStillPose(mibuActPose(act)) else mibuActPose(act)) }
    var pose by remember { mutableStateOf(mibuActPose(act)) }
    var phase by remember { mutableStateOf(MibuPhase()) }
    var zzzCycle by remember { mutableStateOf(0.0) }

    LaunchedEffect(act, reduced) {
        val next = mibuActPose(act)
        target = if (reduced) mibuStillPose(next) else next
    }
    LaunchedEffect(reduced) {
        if (reduced) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1e9).toFloat().coerceAtMost(MAX_STEP)
            last = now
            pose = approachPose(pose, target, dt)
            phase = MibuPhase(
                breath = advance(phase.breath, pose.breathHz, dt),
                arm = advance(phase.arm, pose.armHz, dt),
                tail = advance(phase.tail, pose.tailHz, dt),
                hop = advance(phase.hop, pose.hopHz, dt),
                blink = advance(phase.blink, pose.blinkHz, dt),
            )
            zzzCycle = (zzzCycle + dt / ZZZ_PERIOD) % 1.0
        }
    }

    Canvas(modifier = modifier) {
        drawCat(pose, phase, zzzCycle)
    }
}

/** 一帧的姿态绘制：部件分组 = 动作层级（身体 → 头/眼/爪/尾巴/zzz） */
private fun DrawScope.drawCat(pose: MibuPose, phase: MibuPhase, zzzCycle: Double) {
    val cell = size.minDimension / 24f
    fun u(x: Float) = x * cell
    fun rect(x: Float, y: Float, w: Float, h: Float, color: Color) {
        drawRect(color, topLeft = Offset(u(x), u(y)), size = Size(u(w), u(h)))
    }

    val breathe = sin(phase.breath).toFloat() * pose.breathAmp
    val hop = sin(phase.hop).toFloat().let { kotlin.math.abs(it) } * pose.hopAmp
    val eye = pose.eyeOpen * (1f - 0.88f * mibuBlink(phase.blink))
    val bodyY = pose.bodyY + breathe - hop

    // Zzz：睡意淡入淡出，周期往上飘
    val sleep = mibuSleepiness(pose)
    val fade = if (zzzCycle < 0.25) (zzzCycle / 0.25).toFloat() else (1 - (zzzCycle - 0.25) / 0.75).toFloat()
    if (sleep > 0f && fade > 0f) {
        val zx = (zzzCycle * 1.8).toFloat()
        val zy = (-zzzCycle * 3.2).toFloat()
        translate(left = u(zx), top = u(zy)) {
            drawRect(
                MibuInk.copy(alpha = MibuInk.alpha * sleep * fade),
                topLeft = Offset(u(20f), u(4f)),
                size = Size(u(2f), u(2f)),
            )
            drawRect(
                MibuInk.copy(alpha = MibuInk.alpha * sleep * fade),
                topLeft = Offset(u(22f), u(1f)),
                size = Size(u(1f), u(1f)),
            )
        }
    }

    // 身体组：整体垂直起伏
    translate(left = 0f, top = u(bodyY)) {
        // 尾巴：绕自身盒左下角（18,19）摆动
        rotate(
            degrees = pose.tailTilt + pose.tailAmp * sin(phase.tail).toFloat(),
            pivot = Offset(u(18f), u(19f)),
        ) {
            rect(18f, 17f, 2f, 5f, MibuFur)
            rect(20f, 14f, 2f, 4f, MibuFur)
            rect(22f, 12f, 2f, 2f, MibuFur)
        }
        rect(7f, 13f, 10f, 9f, MibuFur)
        rect(13f, 17f, 3f, 5f, MibuLight)

        // 头组：胡须长在脸上跟着头一起歪；绕下巴（12,14）转
        rotate(
            degrees = pose.headTilt,
            pivot = Offset(u(12f), u(14f)),
        ) {
            rect(1f, 9f, 3f, 1f, MibuInk)
            rect(1f, 11f, 3f, 1f, MibuInk)
            rect(20f, 9f, 3f, 1f, MibuInk)
            rect(20f, 11f, 3f, 1f, MibuInk)

            rect(6f, 1f, 2f, 1f, MibuFur)
            rect(5f, 2f, 4f, 2f, MibuFur)
            rect(6f, 3f, 2f, 1f, MibuLight)
            rect(16f, 1f, 2f, 1f, MibuFur)
            rect(15f, 2f, 4f, 2f, MibuFur)
            rect(16f, 3f, 2f, 1f, MibuLight)
            rect(5f, 4f, 14f, 10f, MibuFur)

            // 眼组：绕眼睛盒中心（12,7.5）开合
            scale(scaleX = 1f, scaleY = eye.coerceAtLeast(0f), pivot = Offset(u(12f), u(7.5f))) {
                rect(8f, 6f, 2f, 3f, MibuInk)
                rect(14f, 6f, 2f, 3f, MibuInk)
            }

            rect(9f, 10f, 6f, 3f, MibuLight)
            rect(11f, 10f, 2f, 1f, MibuInk)
        }

        // 前爪：绕爪顶（9.5,17）摆动
        rotate(
            degrees = pose.armAngle + pose.armAmp * sin(phase.arm).toFloat(),
            pivot = Offset(u(9.5f), u(17f)),
        ) {
            rect(8f, 17f, 3f, 5f, MibuLight)
        }
    }
}

/**
 * 像素猫按钮：默认待机并随机做小动作，可受控指定动作。
 *
 * @param act 受控动作（null = 非受控，内部按 idleActs 随机播小动作）
 * @param defaultAct 非受控时的初始动作
 * @param idleActs 待机是否随机做小动作（招手 / 摇尾 / 打盹 / 蹦跶）
 * @param onClick 点击回调（无默认行为）
 * @param disabled 禁用：半透明且不可点
 * @param interactionSource 交互源（悬停/按压/聚焦环出口）
 */
@Composable
fun MibuCat(
    modifier: Modifier = Modifier,
    act: MibuCatAct? = null,
    defaultAct: MibuCatAct = MibuCatAct.Idle,
    idleActs: Boolean = true,
    onClick: (() -> Unit)? = null,
    disabled: Boolean = false,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
) {
    val palette = LocalMuiPalette.current
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    var inner by remember { mutableStateOf(defaultAct) }
    val current = act ?: inner

    LaunchedEffect(act != null, idleActs) {
        if (act != null || !idleActs) {
            inner = MibuCatAct.Idle
            return@LaunchedEffect
        }
        while (true) {
            delay((3200 + floor(Math.random() * 4200)).toLong())
            val nextAct = IDLE_ACTS.floorAt(Math.random() * IDLE_ACTS.size)
            inner = nextAct.first
            delay(nextAct.second)
            inner = MibuCatAct.Idle
        }
    }

    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .size(48.dp)
            .alpha(if (disabled) 0.5f else 1f)
            .clip(shape)
            .background(
                if (hovered || pressed) {
                    palette.accent.copy(alpha = palette.accent.alpha * 0.5f)
                } else Color.Transparent,
                shape,
            )
            .muiFocusRing(interactionSource, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !disabled,
                onClick = { onClick?.invoke() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        MibuCatMark(
            act = current,
            modifier = Modifier
                .size(44.dp)
                .alpha(1f),
        )
    }
}

/** 随机取待机小动作 */
private fun <T> List<T>.floorAt(index: Double): T =
    get(kotlin.math.floor(index).toInt().coerceIn(0, lastIndex))
