package com.mui.components

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** 猫的动作：Idle 是常态，Alert 表示警觉（被召唤 / 被点击） */
enum class MibuCatAct { Idle, Wave, Wag, Sleep, Hop, Alert }

/** 一整套姿态量。位移类使用 SVG 用户坐标（viewBox 的 24 格） */
data class MibuPose(
    /** 整体垂直位移，负值向上 */
    val bodyY: Float = 0f,
    /** 呼吸起伏振幅 */
    val breathAmp: Float = 0.4f,
    /** 呼吸频率（Hz） */
    val breathHz: Float = 0.22f,
    /** 头部倾角（度，绕下巴） */
    val headTilt: Float = 0f,
    /** 前爪基准角（度，负值抬起） */
    val armAngle: Float = 0f,
    /** 前爪摆动振幅（度） */
    val armAmp: Float = 0f,
    /** 前爪摆动频率（Hz） */
    val armHz: Float = 1.8f,
    /** 尾巴基准角（度，负值上翘） */
    val tailTilt: Float = 0f,
    /** 尾巴摆动振幅（度） */
    val tailAmp: Float = 7f,
    /** 尾巴摆动频率（Hz） */
    val tailHz: Float = 0.22f,
    /** 跳跃振幅（用户坐标） */
    val hopAmp: Float = 0f,
    /** 跳跃频率（Hz，一次完整起落算一次） */
    val hopHz: Float = 1.1f,
    /** 眼睛开合，1 为全睁 */
    val eyeOpen: Float = 1f,
    /** 眨眼频率（Hz） */
    val blinkHz: Float = 0.19f,
)

/** 各部件各自的振荡相位（弧度），只累加、不重置 */
data class MibuPhase(
    val breath: Double = 0.0,
    val arm: Double = 0.0,
    val tail: Double = 0.0,
    val hop: Double = 0.0,
    val blink: Double = 0.0,
)

/** 姿态逼近速率（1/秒）：约 0.3 秒走完九成路程 */
private const val POSE_RATE = 7.5f

private val TAU = PI * 2

/** 每个动作 = 常态姿态上的一组覆盖值；未列出的量沿用常态 */
private val ACT_POSES: Map<MibuCatAct, MibuPose> = mapOf(
    MibuCatAct.Idle to MibuPose(),
    MibuCatAct.Wave to MibuPose(
        armAngle = -30f, armAmp = 26f, headTilt = -3f, breathAmp = 0.5f, tailAmp = 9f, tailHz = 0.5f,
    ),
    MibuCatAct.Wag to MibuPose(tailAmp = 17f, tailHz = 3.3f, headTilt = 2f, breathAmp = 0.45f),
    MibuCatAct.Sleep to MibuPose(
        bodyY = 0.5f, headTilt = 9f, eyeOpen = 0.1f, blinkHz = 0f,
        breathAmp = 0.55f, breathHz = 0.13f, tailTilt = -2f, tailAmp = 3f, tailHz = 0.09f,
    ),
    MibuCatAct.Hop to MibuPose(hopAmp = 2.6f, headTilt = -2f, breathAmp = 0.22f, tailAmp = 12f, tailHz = 2.2f),
    MibuCatAct.Alert to MibuPose(
        armAngle = -14f, headTilt = -2f, breathAmp = 0.5f,
        tailTilt = -28f, tailAmp = 12f, tailHz = 2.4f, eyeOpen = 1.1f,
    ),
)

/** 取某个动作的目标姿态（完整姿态） */
fun mibuActPose(act: MibuCatAct): MibuPose = ACT_POSES[act] ?: MibuPose()

/** 去掉一切周期性运动，只留姿态本身（系统关动画时用） */
fun mibuStillPose(pose: MibuPose): MibuPose =
    pose.copy(breathAmp = 0f, armAmp = 0f, tailAmp = 0f, hopAmp = 0f, blinkHz = 0f)

/** 帧率无关的指数逼近：不越目标、不回弹 */
private fun approach(current: Float, target: Float, dt: Float, rate: Float = POSE_RATE): Float {
    val k: Float = 1f - exp((-rate * dt).toDouble()).toFloat()
    return current + (target - current) * k
}

/** 把整组姿态朝目标推进一步 */
fun approachPose(current: MibuPose, target: MibuPose, dt: Float): MibuPose = MibuPose(
    bodyY = approach(current.bodyY, target.bodyY, dt),
    breathAmp = approach(current.breathAmp, target.breathAmp, dt),
    breathHz = approach(current.breathHz, target.breathHz, dt),
    headTilt = approach(current.headTilt, target.headTilt, dt),
    armAngle = approach(current.armAngle, target.armAngle, dt),
    armAmp = approach(current.armAmp, target.armAmp, dt),
    armHz = approach(current.armHz, target.armHz, dt),
    tailTilt = approach(current.tailTilt, target.tailTilt, dt),
    tailAmp = approach(current.tailAmp, target.tailAmp, dt),
    tailHz = approach(current.tailHz, target.tailHz, dt),
    hopAmp = approach(current.hopAmp, target.hopAmp, dt),
    hopHz = approach(current.hopHz, target.hopHz, dt),
    eyeOpen = approach(current.eyeOpen, target.eyeOpen, dt),
    blinkHz = approach(current.blinkHz, target.blinkHz, dt),
)

/** 推进相位（弧度取模，避免长时间运行后精度漂移） */
fun advance(value: Double, hz: Float, dt: Float): Double =
    (value + hz * TAU * dt) % TAU

/** 眨眼曲线：一个周期末尾快速闭合一次，返回 0..1 的闭合程度 */
fun mibuBlink(phase: Double): Float {
    val t = ((phase % TAU) / TAU)
    if (t < 0.94) return 0f
    return sin(((t - 0.94) / 0.06) * PI).toFloat()
}

/** 由眼睛开合反推「睡意」：眼睛闭得越紧，睡得越沉 */
fun mibuSleepiness(pose: MibuPose): Float =
    max(0f, min(1f, (1f - pose.eyeOpen) * 1.25f - 0.25f))
