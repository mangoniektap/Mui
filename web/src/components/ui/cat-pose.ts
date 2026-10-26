/**
 * MIBU 像素猫的姿态系统（纯函数：无 React / DOM 依赖）。
 *
 * 输入：动作名与当前姿态量；输出：该动作的目标姿态、逼近一步后的姿态、
 * 各部件振荡相位。姿态量全部是连续数值（角度 / 位移 / 频率），
 * 因此任意两个动作之间只是数值平滑过渡，不存在关键帧切换的跳变。
 *
 * 硬约束：振荡靠相位累加驱动——改频率只改相位推进快慢，相位本身不重置；
 * 位移类使用 SVG 用户坐标（viewBox 的 24 格），缩放猫时幅度等比跟随。
 */

/** 猫的动作：idle 是常态，alert 表示警觉（被召唤 / 被点击）。 */
export type MibuCatAct = "idle" | "wave" | "wag" | "sleep" | "hop" | "alert";

/** 一整套姿态量。位移类使用 SVG 用户坐标（viewBox 的 24 格）。 */
export interface MibuPose {
  /** 整体垂直位移，负值向上 */
  bodyY: number;
  /** 呼吸起伏振幅 */
  breathAmp: number;
  /** 呼吸频率（Hz） */
  breathHz: number;
  /** 头部倾角（度，绕下巴） */
  headTilt: number;
  /** 前爪基准角（度，负值抬起） */
  armAngle: number;
  /** 前爪摆动振幅（度） */
  armAmp: number;
  /** 前爪摆动频率（Hz） */
  armHz: number;
  /** 尾巴基准角（度，负值上翘） */
  tailTilt: number;
  /** 尾巴摆动振幅（度） */
  tailAmp: number;
  /** 尾巴摆动频率（Hz） */
  tailHz: number;
  /** 跳跃振幅（用户坐标） */
  hopAmp: number;
  /** 跳跃频率（Hz，一次完整起落算一次） */
  hopHz: number;
  /** 眼睛开合，1 为全睁 */
  eyeOpen: number;
  /** 眨眼频率（Hz） */
  blinkHz: number;
}

/** 常态姿态：慢摆尾、轻呼吸、偶尔眨眼。所有动作都是它在某些维度上的改法。 */
const MIBU_BASE_POSE: MibuPose = {
  bodyY: 0,
  breathAmp: 0.4,
  breathHz: 0.22,
  headTilt: 0,
  armAngle: 0,
  armAmp: 0,
  armHz: 1.8,
  tailTilt: 0,
  tailAmp: 7,
  tailHz: 0.22,
  hopAmp: 0,
  hopHz: 1.1,
  eyeOpen: 1,
  blinkHz: 0.19,
};

/** 每个动作 = 常态姿态上的一组覆盖值；未列出的量沿用常态。 */
const ACT_POSES: Record<MibuCatAct, Partial<MibuPose>> = {
  idle: {},
  wave: {
    armAngle: -30,
    armAmp: 26,
    headTilt: -3,
    breathAmp: 0.5,
    tailAmp: 9,
    tailHz: 0.5,
  },
  wag: { tailAmp: 17, tailHz: 3.3, headTilt: 2, breathAmp: 0.45 },
  sleep: {
    bodyY: 0.5,
    headTilt: 9,
    eyeOpen: 0.1,
    blinkHz: 0,
    breathAmp: 0.55,
    breathHz: 0.13,
    tailTilt: -2,
    tailAmp: 3,
    tailHz: 0.09,
  },
  hop: { hopAmp: 2.6, headTilt: -2, breathAmp: 0.22, tailAmp: 12, tailHz: 2.2 },
  alert: {
    armAngle: -14,
    headTilt: -2,
    breathAmp: 0.5,
    tailTilt: -28,
    tailAmp: 12,
    tailHz: 2.4,
    eyeOpen: 1.1,
  },
};

/** 取某个动作的目标姿态（完整姿态，未列出的量沿用常态）。 */
export function mibuActPose(act: MibuCatAct): MibuPose {
  return { ...MIBU_BASE_POSE, ...ACT_POSES[act] };
}

/** 去掉一切周期性运动，只留姿态本身。给 prefers-reduced-motion 用。 */
export function mibuStillPose(pose: MibuPose): MibuPose {
  return { ...pose, breathAmp: 0, armAmp: 0, tailAmp: 0, hopAmp: 0, blinkHz: 0 };
}

/** 姿态逼近速率（1/秒）：约 0.3 秒走完九成路程。 */
const POSE_RATE = 7.5

/** 帧率无关的指数逼近：不依赖帧率，也不会越过目标造成回弹。 */
function approach(current: number, target: number, dt: number, rate = POSE_RATE): number {
  return current + (target - current) * (1 - Math.exp(-rate * dt))
}

/** 把整组姿态朝目标推进一步。 */
export function approachPose(current: MibuPose, target: MibuPose, dt: number): MibuPose {
  const next = { ...current }
  for (const key of Object.keys(target) as (keyof MibuPose)[]) {
    next[key] = approach(current[key], target[key], dt)
  }
  return next
}

/** 各部件各自的振荡相位（弧度），只累加、不重置。 */
export interface MibuPhase {
  breath: number;
  arm: number;
  tail: number;
  hop: number;
  blink: number;
}

export const MIBU_BASE_PHASE: MibuPhase = { breath: 0, arm: 0, tail: 0, hop: 0, blink: 0 };

const TAU = Math.PI * 2;

/** 推进相位，并把弧度取模，避免长时间运行后精度漂移。 */
export function advance(value: number, hz: number, dt: number): number {
  return (value + hz * TAU * dt) % TAU;
}

/** 眨眼曲线：一个周期末尾快速闭合一次，返回 0~1 的闭合程度。 */
export function mibuBlink(phase: number): number {
  const t = (phase % TAU) / TAU;
  if (t < 0.94) return 0;
  return Math.sin(((t - 0.94) / 0.06) * Math.PI);
}

/** 由眼睛开合反推「睡意」，用来淡入淡出 Zzz：眼睛闭得越紧，睡得越沉。 */
export function mibuSleepiness(pose: MibuPose): number {
  return Math.max(0, Math.min(1, (1 - pose.eyeOpen) * 1.25 - 0.25));
}
