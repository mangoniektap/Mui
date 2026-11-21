import * as React from "react";

import { cn } from "@/lib/utils";

export type BarSurfaceProps = Omit<React.ComponentProps<"div">, "ref"> & {
  /** true = 底栏（顶边通透），false = 顶栏（底边通透） */
  bottom?: boolean;
};

/** 通透度按 e 为底的幂函数衰减：t=0 → 100%，t=1 → 0% */
const DECAY = 3;
const MIN_CLARITY = 0;
const STOPS = 24;
const BLUR = "blur(var(--bar-frost-blur, 12px))";

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
  className,
  style,
  children,
  ...props
}: BarSurfaceProps) {
  const mask = React.useMemo(() => frostMask(bottom), [bottom]);

  // 顶栏横向占满；底栏只占右侧内容区（左边让开 w-60 侧栏，小屏侧栏隐藏则占满）。
  // 内联 position:fixed 压住调用点的 sticky，保证条带贴在视口边上。
  return (
    <div
      data-slot="bar-surface"
      className={cn(
        "fixed z-30 h-14",
        bottom ? "right-0 bottom-0 left-0 md:left-60" : "inset-x-0 top-0",
        className
      )}
      style={{ position: "fixed", ...style }}
      {...props}
    >
      <div
        aria-hidden
        data-slot="bar-frost"
        className="pointer-events-none absolute inset-0 -z-10"
        style={{
          backdropFilter: BLUR,
          WebkitBackdropFilter: BLUR,
          maskImage: mask,
          WebkitMaskImage: mask,
        }}
      />
      {children}
    </div>
  );
}
