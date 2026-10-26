import { useEffect, useRef, type RefObject } from "react";

import {
  advance,
  approachPose,
  mibuActPose,
  mibuBlink,
  mibuSleepiness,
  mibuStillPose,
  MIBU_BASE_PHASE,
  type MibuCatAct,
  type MibuPhase,
  type MibuPose,
} from "@/components/ui/cat-pose";

/** 猫身上可以被驱动的部件。 */
interface MibuCatParts {
  body: RefObject<SVGGElement | null>;
  head: RefObject<SVGGElement | null>;
  arm: RefObject<SVGGElement | null>;
  tail: RefObject<SVGGElement | null>;
  eye: RefObject<SVGGElement | null>;
  zzz: RefObject<SVGGElement | null>;
}

/** Zzz 气泡飘完一轮的时间（秒）。 */
const ZZZ_PERIOD = 2.2;

/** 单帧最多承认的时间，避免切回标签页时姿态一步到位、相位跳一大截。 */
const MAX_STEP = 0.05;

let reducedMotion: boolean | null = null;
function prefersReducedMotion(): boolean {
  if (reducedMotion === null) {
    reducedMotion =
      typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  }
  return reducedMotion;
}

function rotate(node: SVGGElement | null, deg: number) {
  if (node) node.style.transform = `rotate(${deg.toFixed(2)}deg)`;
}

/**
 * 把动作翻译成猫的姿态：一个常驻的渲染循环，每帧先让当前姿态朝目标姿态逼近一步，
 * 再按相位算出各部件角度写进 SVG。动作切换、打断、提前结束都只是目标值换了，
 * 当前姿态永远从原处平滑挪过去，所以不存在突变。
 *
 * 输入：act 动作名 + parts 六个部件的 ref；输出：无（逐帧写 SVG style.transform）。
 * 硬约束：部件绕哪儿转由 CSS 管（transform-box / origin），角度一律由本 hook 写；
 * 不加 will-change，让浏览器每帧重绘矢量，像素边缘才不会被纹理化糊掉。
 */
export function useMibuCatPose(act: MibuCatAct, parts: MibuCatParts) {
  const target = useRef<MibuPose>(mibuActPose(act));
  const pose = useRef<MibuPose>(mibuActPose(act));
  const phase = useRef<MibuPhase>({ ...MIBU_BASE_PHASE });
  const zzzCycle = useRef(0);

  // 目标姿态跟着动作走；渲染循环下一帧开始朝它挪。
  useEffect(() => {
    const next = mibuActPose(act);
    target.current = prefersReducedMotion() ? mibuStillPose(next) : next;
  }, [act]);

  useEffect(() => {
    let raf = 0;
    let last = performance.now();

    const tick = (now: number) => {
      raf = requestAnimationFrame(tick);
      const dt = Math.min((now - last) / 1000, MAX_STEP);
      last = now;

      const next = approachPose(pose.current, target.current, dt);
      pose.current = next;

      // 频率只影响相位推进的快慢，相位本身连续，所以提速降速都不跳。
      const ph = phase.current;
      ph.breath = advance(ph.breath, next.breathHz, dt);
      ph.arm = advance(ph.arm, next.armHz, dt);
      ph.tail = advance(ph.tail, next.tailHz, dt);
      ph.hop = advance(ph.hop, next.hopHz, dt);
      ph.blink = advance(ph.blink, next.blinkHz, dt);
      zzzCycle.current = (zzzCycle.current + dt / ZZZ_PERIOD) % 1;

      const breathe = Math.sin(ph.breath) * next.breathAmp;
      const hop = Math.abs(Math.sin(ph.hop)) * next.hopAmp;
      const eye = next.eyeOpen * (1 - 0.88 * mibuBlink(ph.blink));

      if (parts.body.current) {
        parts.body.current.style.transform = `translateY(${(next.bodyY + breathe - hop).toFixed(3)}px)`;
      }
      rotate(parts.head.current, next.headTilt);
      rotate(parts.arm.current, next.armAngle + next.armAmp * Math.sin(ph.arm));
      rotate(parts.tail.current, next.tailTilt + next.tailAmp * Math.sin(ph.tail));

      if (parts.eye.current) {
        parts.eye.current.style.transform = `scaleY(${eye.toFixed(3)})`;
      }

      const sleep = mibuSleepiness(next);
      const node = parts.zzz.current;
      if (node) {
        // 睡意把 Zzz 整体淡入淡出，周期负责让它一轮一轮往上飘。
        const fade = zzzCycle.current < 0.25 ? zzzCycle.current / 0.25 : 1 - (zzzCycle.current - 0.25) / 0.75;
        node.style.opacity = (sleep * Math.max(0, fade)).toFixed(3);
        node.style.transform = `translate(${(zzzCycle.current * 1.8).toFixed(2)}px, ${(-zzzCycle.current * 3.2).toFixed(2)}px)`;
      }
    };

    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
    // parts 里全是稳定的 ref 对象，循环只建一次。
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
}
