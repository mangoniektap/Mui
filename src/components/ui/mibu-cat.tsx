import { useEffect, useRef, useState } from "react";

import { useMibuCatPose } from "@/hooks/use-mibu-cat-pose";
import { cn } from "@/lib/utils";
import type { MibuCatAct } from "./cat-pose";

/** 待机时的随机互动：duration 只决定这个动作待多久，不参与任何过渡计算。 */
const IDLE_ACTS: { act: MibuCatAct; duration: number }[] = [
  { act: "wave", duration: 1800 },
  { act: "wag", duration: 2000 },
  { act: "sleep", duration: 3200 },
  { act: "hop", duration: 1000 },
];

/**
 * 像素猫的三种颜色，取值与 MREAD 原版亮色一模一样（MREAD 的
 * --brand-strong / --brand / --brand-foreground），定义在 styles/mibu-cat.css：
 * 猫不区分亮暗主题，两种主题共用这一组色。
 */
const FUR = "var(--mibu-fur)";
const LIGHT = "var(--mibu-light)";
const INK = "var(--mibu-ink)";

interface MibuCatMarkProps {
  /** 当前动作；姿态由 useMibuCatPose 逐帧写入，换动作只是目标值连续变过去 */
  act?: MibuCatAct;
  className?: string;
}

/**
 * 像素猫本体：全部用整格矩形拼出来，配合 crispEdges 保持像素边缘。
 * 部件分组即动作层级——身体整体起伏，头、前爪、尾巴、眼睛各成一组，
 * 由 useMibuCatPose 每帧写入姿态。
 *
 * 输入：act 动作名；输出：SVG（无回调）。
 * 硬约束：各部件绕哪儿转由 Tailwind 任意属性类管（transform-box / origin），
 * 角度一律由 hook 写 style.transform，两者不冲突；三种颜色只读
 * --mibu-fur / --mibu-light / --mibu-ink，与 MREAD 原版一致。
 */
export function MibuCatMark({ act = "idle", className }: MibuCatMarkProps) {
  const body = useRef<SVGGElement>(null);
  const head = useRef<SVGGElement>(null);
  const arm = useRef<SVGGElement>(null);
  const tail = useRef<SVGGElement>(null);
  const eye = useRef<SVGGElement>(null);
  const zzz = useRef<SVGGElement>(null);

  useMibuCatPose(act, { body, head, arm, tail, eye, zzz });

  return (
    <svg
      viewBox="0 0 24 24"
      aria-hidden="true"
      className={cn("mibu-cat-mark block size-full overflow-visible [shape-rendering:crispEdges]", className)}
    >
      <g ref={body} className="mibu-cat__body">
        <g
          ref={tail}
          className="mibu-cat__tail [transform-box:fill-box] [transform-origin:0%_100%]"
        >
          <rect x="18" y="17" width="2" height="5" fill={FUR} />
          <rect x="20" y="14" width="2" height="4" fill={FUR} />
          <rect x="22" y="12" width="2" height="2" fill={FUR} />
        </g>

        <rect x="7" y="13" width="10" height="9" fill={FUR} />
        <rect x="13" y="17" width="3" height="5" fill={LIGHT} />

        {/* 胡须长在脸上，所以跟着头一起歪。 */}
        <g
          ref={head}
          className="mibu-cat__head [transform-box:fill-box] [transform-origin:50%_100%]"
        >
          <rect x="1" y="9" width="3" height="1" fill={INK} />
          <rect x="1" y="11" width="3" height="1" fill={INK} />
          <rect x="20" y="9" width="3" height="1" fill={INK} />
          <rect x="20" y="11" width="3" height="1" fill={INK} />

          <rect x="6" y="1" width="2" height="1" fill={FUR} />
          <rect x="5" y="2" width="4" height="2" fill={FUR} />
          <rect x="6" y="3" width="2" height="1" fill={LIGHT} />
          <rect x="16" y="1" width="2" height="1" fill={FUR} />
          <rect x="15" y="2" width="4" height="2" fill={FUR} />
          <rect x="16" y="3" width="2" height="1" fill={LIGHT} />
          <rect x="5" y="4" width="14" height="10" fill={FUR} />

          <g
            ref={eye}
            className="mibu-cat__eye [transform-box:fill-box] [transform-origin:50%_50%]"
          >
            <rect x="8" y="6" width="2" height="3" fill={INK} />
            <rect x="14" y="6" width="2" height="3" fill={INK} />
          </g>

          <rect x="9" y="10" width="6" height="3" fill={LIGHT} />
          <rect x="11" y="10" width="2" height="1" fill={INK} />
        </g>

        <g
          ref={arm}
          className="mibu-cat__arm [transform-box:fill-box] [transform-origin:50%_0%]"
        >
          <rect x="8" y="17" width="3" height="5" fill={LIGHT} />
        </g>
      </g>

      {/* Zzz 常驻在树上，可见度交给姿态：睡着时淡入飘走，醒了自然收掉。 */}
      <g ref={zzz} className="mibu-cat__zzz" fill={INK} opacity={0}>
        <rect x="20" y="4" width="2" height="2" />
        <rect x="22" y="1" width="1" height="1" />
      </g>
    </svg>
  );
}

export interface MibuCatProps extends Omit<React.ComponentProps<"button">, "children"> {
  /** 当前动作（受控）；不传则内部从 defaultAct 起步，待机随机播小动作 */
  act?: MibuCatAct;
  /** 非受控时的初始动作 */
  defaultAct?: MibuCatAct;
  /** 待机是否随机做小动作（招手 / 摇尾 / 打盹 / 蹦跶） */
  idleActs?: boolean;
}

/**
 * 像素猫按钮：默认待机并随机做小动作，可受控指定动作。
 *
 * 输入：act / defaultAct / idleActs / onClick；输出：无（纯交互装饰）。
 * 硬约束：颜色只读 --mibu-fur / --mibu-light / --mibu-ink（取值同 MREAD 原版）；
 * 姿态由 useMibuCatPose 逐帧写 SVG，组件本身不持有任何动画关键帧。
 */
export function MibuCat({
  act,
  defaultAct = "idle",
  idleActs = true,
  className,
  ...props
}: MibuCatProps) {
  const controlled = act !== undefined;
  const [inner, setInner] = useState<MibuCatAct>(defaultAct);
  const current = controlled ? act : inner;

  // 受控或关掉随机动作时，内部状态回到常态，避免下次非受控时从半个动作起步。
  useEffect(() => {
    if (controlled || !idleActs) {
      setInner("idle");
      return;
    }
    let timer = 0;
    const schedule = () => {
      timer = window.setTimeout(() => {
        const next = IDLE_ACTS[Math.floor(Math.random() * IDLE_ACTS.length)];
        setInner(next.act);
        timer = window.setTimeout(() => {
          setInner("idle");
          schedule();
        }, next.duration);
      }, 3200 + Math.random() * 4200);
    };
    schedule();
    return () => window.clearTimeout(timer);
  }, [controlled, idleActs]);

  return (
    <button
      type="button"
      aria-label="像素猫"
      className={cn(
        "relative size-12 shrink-0 cursor-pointer rounded-xl outline-none transition duration-150 hover:-translate-y-px hover:bg-accent/50 focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:pointer-events-none disabled:opacity-50",
        className
      )}
      {...props}
    >
      <MibuCatMark act={current} className="size-full p-0.5" />
    </button>
  );
}
