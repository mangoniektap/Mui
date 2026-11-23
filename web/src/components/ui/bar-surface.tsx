import * as React from "react";

import { cn } from "@/lib/utils";

export type BarSurfaceProps = Omit<React.ComponentProps<"div">, "ref"> & {
  /** true = 底栏（顶边通透），false = 顶栏（底边通透） */
  bottom?: boolean;
  /** 由最近的定位容器限定尺寸，适合内容列中的栏。 */
  contained?: boolean;
};

/** 通透度按 e 为底的幂函数衰减：t=0 → 100%，t=1 → 0% */
const DECAY = 3;
const MIN_CLARITY = 0;
const STOPS = 24;
const BLUR_RADIUS = "var(--bar-frost-blur, 12px)";
const BLUR = `blur(${BLUR_RADIUS})`;
const FROST_TINT = "var(--bar-frost-tint, color-mix(in oklab, var(--muted) 80%, transparent))";
const SAMPLE_MARGIN = `calc(3 * ${BLUR_RADIUS})`;
const SAMPLE_INSET = `calc(-1 * ${SAMPLE_MARGIN})`;

/**
 * 磨砂层的不透明度 = 1 − 通透度，用 mask 的 alpha 承载这条曲线。
 * p(t) = 100 × (e^(−k·t) − e^(−k)) / (1 − e^(−k))，归一化保证两端精确 100 / 0。
 * to top 的 0% 在底边（顶栏通透侧），to bottom 的 0% 在顶边（底栏通透侧）。
 */
function frostMask(bottom: boolean) {
  const tail = Math.exp(-DECAY);
  const denom = 1 - tail;
  const parts: string[] = [];
  for (let i = 0; i <= STOPS; i++) {
    const t = i / STOPS;
    const clarity =
      MIN_CLARITY + ((1 - MIN_CLARITY) * (Math.exp(-DECAY * t) - tail)) / denom;
    parts.push(`rgba(0,0,0,${(1 - clarity).toFixed(4)}) ${(t * 100).toFixed(2)}%`);
  }
  return `linear-gradient(to ${bottom ? "bottom" : "top"}, ${parts.join(", ")})`;
}

export function BarSurface({
  bottom = false,
  contained = false,
  className,
  style,
  children,
  ...props
}: BarSurfaceProps) {
  // 背景滤镜以元素边框为采样边界。只在通透侧外扩三个模糊半径，
  // 让栏外的内容也能平滑影响栏内亮度，蒙版仍按原始栏高渐变。
  const mask = React.useMemo(
    () => `${frostMask(bottom)} ${bottom ? "bottom" : "top"} / 100% calc(100% - ${SAMPLE_MARGIN}) no-repeat`,
    [bottom]
  );

  // 视口模式保留展示页的固定边栏间距，容器模式沿用调用方的定位边界。
  return (
    <div
      data-slot="bar-surface"
      className={cn(
        contained
          ? "absolute inset-0 -z-10"
          : cn("fixed z-30 h-14", bottom ? "right-0 bottom-0 left-0 md:left-60" : "inset-x-0 top-0"),
        className
      )}
      style={{ position: contained ? "absolute" : "fixed", ...style }}
      {...props}
    >
      <div
        aria-hidden
        data-slot="bar-frost"
        className="pointer-events-none absolute inset-x-0 -z-10"
        style={{
          top: bottom ? SAMPLE_INSET : 0,
          bottom: bottom ? 0 : SAMPLE_INSET,
          backgroundColor: FROST_TINT,
          backdropFilter: BLUR,
          WebkitBackdropFilter: BLUR,
          mask,
          WebkitMask: mask,
        }}
      />
      {children}
    </div>
  );
}
