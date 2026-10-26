import { useCallback, useEffect, useRef, useState } from "react";
import { createPortal, flushSync } from "react-dom";

import { classicalFocusRing } from "@/styles/surfaces/classical";
import { cn } from "@/lib/utils";

/** 点击后光斑从灯心扩散铺满视口的时长 */
const GROW_MS = 520;
/** 铺满后淡出到目标内容的时长 */
const FADE_MS = 180;
const EASE = "cubic-bezier(0.22, 1, 0.36, 1)";

/** 三档直径（px）：灯是圆点，size 只改直径 */
const SIZES = { sm: 10, md: 12, lg: 16 } as const;

/** 扩散转场的原点：灯心坐标与该灯自身颜色 */
export interface TrafficLightOrigin {
  /** 灯心在视口中的横坐标 */
  x: number;
  /** 灯心在视口中的纵坐标 */
  y: number;
  /** 该灯自身的颜色：光斑铺的就是它 */
  color: string;
}

/** 一盏灯：值 / 名称 / 自身颜色 */
export interface TrafficLight {
  /** 唯一值：点亮时随 onValueChange 传出 */
  value: string;
  /** 无障碍名称 */
  label: string;
  /** 灯自身颜色（红绿灯用固定三色，不读主题） */
  color: string;
}

export interface TrafficLightsProps extends Omit<React.ComponentProps<"div">, "children"> {
  /** 灯列表，顺序即左到右 */
  items: TrafficLight[];
  /** 受控：当前点亮的灯 */
  value?: string;
  /** 非受控：默认点亮的灯 */
  defaultValue?: string;
  /** 切换回调，第二参给出灯心坐标与颜色；在光斑铺满视口时触发 */
  onValueChange?: (value: string, origin: TrafficLightOrigin) => void;
  /** 灯直径档位 */
  size?: keyof typeof SIZES;
  disabled?: boolean;
  className?: string;
}

/**
 * 红绿灯：一排圆点开关，点击的灯先以自身颜色从灯心扩散铺满视口，
 * 再淡出到目标内容——所以切换内容是「被颜色盖过去」而不是硬跳。
 *
 * 输入：items（灯列表）/ value（受控）/ onValueChange；输出：无（纯交互组件）。
 * 硬约束：光斑用 portal 挂到 body，不被任何 overflow 容器裁剪；
 * 铺满的瞬间才同步切内容（flushSync），避免淡出时闪出旧内容。
 */
export function TrafficLights({
  items,
  value,
  defaultValue,
  onValueChange,
  size = "md",
  disabled,
  className,
  ...props
}: TrafficLightsProps) {
  const [inner, setInner] = useState(defaultValue ?? items[0]?.value);
  const controlled = value !== undefined;
  const active = controlled ? value : inner;

  const orbRef = useRef<HTMLDivElement>(null);
  const animationRef = useRef<Animation | null>(null);
  const generationRef = useRef(0);

  const resetOrb = useCallback(() => {
    const node = orbRef.current;
    if (!node) return;
    node.style.visibility = "hidden";
    node.style.clipPath = "";
    node.style.opacity = "";
    node.style.background = "";
  }, []);

  const stopAnimation = useCallback(() => {
    animationRef.current?.cancel();
    animationRef.current = null;
  }, []);

  // 卸载时收尾：取消在途动画、收起光斑，避免节点没了回调还在跑。
  useEffect(
    () => () => {
      stopAnimation();
      resetOrb();
    },
    [stopAnimation, resetOrb]
  );

  function handleClick(item: TrafficLight, event: React.MouseEvent<HTMLButtonElement>) {
    if (disabled || item.value === active) return;

    const rect = event.currentTarget.getBoundingClientRect();
    const origin: TrafficLightOrigin = {
      x: rect.left + rect.width / 2,
      y: rect.top + rect.height / 2,
      color: item.color,
    };

    const commit = () => {
      if (!controlled) setInner(item.value);
      onValueChange?.(item.value, origin);
    };

    const node = orbRef.current;
    // 减弱动态或不支持 portal 时：不跑光斑，直接切换。
    if (
      !node ||
      (typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches)
    ) {
      commit();
      return;
    }

    stopAnimation();
    const generation = ++generationRef.current;

    const radius =
      Math.hypot(
        Math.max(origin.x, window.innerWidth - origin.x),
        Math.max(origin.y, window.innerHeight - origin.y)
      ) + 32;
    const from = `circle(0px at ${origin.x}px ${origin.y}px)`;
    const to = `circle(${radius}px at ${origin.x}px ${origin.y}px)`;

    node.style.visibility = "visible";
    node.style.background = origin.color;
    node.style.clipPath = from;
    node.style.opacity = "1";

    const grow = node.animate(
      [
        { clipPath: from, opacity: 1 },
        { clipPath: to, opacity: 1 },
      ],
      { duration: GROW_MS, easing: EASE, fill: "forwards" }
    );
    animationRef.current = grow;

    grow.onfinish = () => {
      if (generation !== generationRef.current) return;

      grow.cancel();
      animationRef.current = null;
      node.style.clipPath = to;

      // 先同步把底层内容切到目标，再淡出光斑，避免闪出错误内容。
      flushSync(commit);

      const fade = node.animate([{ opacity: 1 }, { opacity: 0 }], {
        duration: FADE_MS,
        easing: "ease-out",
        fill: "forwards",
      });
      animationRef.current = fade;

      fade.onfinish = () => {
        if (generation !== generationRef.current) return;
        animationRef.current = null;
        resetOrb();
      };
    };
  }

  const diameter = SIZES[size];

  return (
    <>
      <div
        role="group"
        className={cn("flex w-fit items-center gap-2", className)}
        {...props}
      >
        {items.map((item) => (
          <button
            key={item.value}
            type="button"
            disabled={disabled}
            aria-label={item.label}
            aria-pressed={item.value === active}
            onClick={(event) => handleClick(item, event)}
            style={{ width: diameter, height: diameter, backgroundColor: item.color }}
            className={cn(
              "cursor-pointer rounded-full shadow-xs outline-none disabled:pointer-events-none disabled:opacity-50",
              classicalFocusRing
            )}
          />
        ))}
      </div>

      {createPortal(
        <div
          ref={orbRef}
          aria-hidden="true"
          className="traffic-lights__orb pointer-events-none invisible fixed inset-0 z-[9999] [will-change:clip-path,opacity]"
        />,
        document.body
      )}
    </>
  );
}
