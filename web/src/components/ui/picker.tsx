"use client"

import * as React from "react"
import { X } from "lucide-react"

import { cn } from "@/lib/utils"
import { classicalFocusRing } from "@/styles/surfaces/classical"
import { GlassLens, paintPageBackdrop } from "@/styles/surfaces/liquid-glass"

/**
 * 转盘选择器：调用方只给**数值范围 + 单位**，组件自己铺出整条刻度。
 *
 * 交互：鼠标滚轮 + 按住上下拖；松手（或滚轮停 140ms）吸附到最近一档。
 * 中间那条是 gray 档玻璃，只当"选中框"，pointer-events-none，指针直接穿透。
 * 项沿圆周排布（sin 定位置、cos 定大小与淡出、rotateX 做倾斜），像真的滚轮。
 */

/** 一档的高度；容器高 = ITEM_H × VISIBLE */
const ITEM_H = 36
/** 可见档数：中间那档 + 上下各 2 档，边缘靠 mask 淡出 */
const VISIBLE = 5
/**
 * 转盘曲率半径（px）：值越小越"弯"，给大了就退回直上直下。
 * 要跟着 ITEM_H / VISIBLE 一起调。
 */
const RADIUS = 64
/** 轮盘的固定宽度：触发器和轮盘都按它来，宽度是确定的就不用再量 */
const WHEEL_W = 160
/** 中心档的顶部位置（相对容器）：由上面两个常量推出，别在渲染里重算 */
const CENTER = (VISIBLE * ITEM_H - ITEM_H) / 2
/** 吸附动画时长（缓出三次，比按压那条更长：滚动要"落定"的感觉） */
const SNAP_MS = 260

const clamp = (v: number, lo: number, hi: number) => Math.min(Math.max(v, lo), hi)

/** 数字缓动：只用在这里（滚动吸附）；按压那条曲线在引擎里，别混用 */
function animateTo(
  from: number,
  to: number,
  dur: number,
  onUpdate: (v: number) => void
) {
  const t0 = performance.now()
  let raf = 0
  const step = (t: number) => {
    const p = Math.min(1, (t - t0) / dur)
    const e = 1 - Math.pow(1 - p, 3)
    onUpdate(from + (to - from) * e)
    if (p < 1) raf = requestAnimationFrame(step)
  }
  raf = requestAnimationFrame(step)
  return () => cancelAnimationFrame(raf)
}

function Picker({
  min,
  max,
  step = 1,
  unit,
  value,
  defaultValue,
  onValueChange,
  format,
  disabled,
  className,
}: {
  /** 范围：闭区间，从 min 起每 step 一档 */
  min: number
  max: number
  /** 步长，默认 1；小数步长（0.1）也支持 */
  step?: number
  /** 单位后缀（"kg"），固定在数字右侧、不参与滚动 */
  unit?: string
  value?: number
  defaultValue?: number
  onValueChange?: (value: number) => void
  /** 数字怎么显示，默认 String(v)；要补零或加千分位时给这个 */
  format?: (value: number) => string
  disabled?: boolean
  className?: string
}) {
  const values = React.useMemo(() => {
    const n = Math.floor((max - min) / step + 1e-9) + 1
    return Array.from({ length: Math.max(n, 0) }, (_, i) =>
      Number((min + i * step).toFixed(6))
    )
  }, [min, max, step])

  const [inner, setInner] = React.useState(defaultValue ?? values[0] ?? min)
  const current = value ?? inner
  const index = Math.max(
    0,
    values.findIndex((v) => Math.abs(v - current) < step / 2)
  )

  const [open, setOpen] = React.useState(false)
  /** 滚动偏移（px）：0 = 第一档正好在中心 */
  const [scroll, setScroll] = React.useState(index * ITEM_H)
  const scrollRef = React.useRef(scroll)
  scrollRef.current = scroll

  const rootRef = React.useRef<HTMLDivElement>(null)
  /** 轮盘容器：滚轮的原生监听挂它上面，玻璃条的背面也取它那层的背景 */
  const wheelRef = React.useRef<HTMLDivElement>(null)
  const cancelRef = React.useRef<(() => void) | null>(null)
  const dragRef = React.useRef<{ y0: number; s0: number } | null>(null)
  const wheelTimer = React.useRef<number | null>(null)

  const maxScroll = Math.max(0, (values.length - 1) * ITEM_H)
  const label = (v: number) => (format ? format(v) : String(v))

  // 展开时把轮盘定位到当前档
  React.useLayoutEffect(() => {
    if (open) setScroll(clamp(index * ITEM_H, 0, maxScroll))
  }, [open, index, maxScroll])

  // 点外面收起
  React.useEffect(() => {
    if (!open) return
    const onDown = (e: PointerEvent) => {
      if (!rootRef.current?.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener("pointerdown", onDown)
    return () => document.removeEventListener("pointerdown", onDown)
  }, [open])

  React.useEffect(() => () => cancelRef.current?.(), [])

  // 滚轮必须用非 passive 的原生监听：React 的 onWheel 是 passive 的，preventDefault
  // 不生效，转盘一滚整页会跟着滚。
  React.useEffect(() => {
    const el = wheelRef.current
    if (!open || !el) return
    const onWheel = (e: WheelEvent) => {
      e.preventDefault()
      cancelRef.current?.()
      setScroll((s) => clamp(s + e.deltaY, -ITEM_H, maxScroll + ITEM_H))
      if (wheelTimer.current) window.clearTimeout(wheelTimer.current)
      wheelTimer.current = window.setTimeout(snap, 140)
    }
    el.addEventListener("wheel", onWheel, { passive: false })
    return () => el.removeEventListener("wheel", onWheel)
  }, [open, maxScroll])

  /** 停到最近一档并提交 */
  function snap() {
    const i = clamp(Math.round(scrollRef.current / ITEM_H), 0, values.length - 1)
    cancelRef.current?.()
    cancelRef.current = animateTo(
      scrollRef.current,
      i * ITEM_H,
      SNAP_MS,
      setScroll
    )
    const next = values[i]
    if (next === undefined || next === current) return
    if (value === undefined) setInner(next)
    onValueChange?.(next)
  }

  function handlePointerDown(e: React.PointerEvent<HTMLDivElement>) {
    cancelRef.current?.()
    e.currentTarget.setPointerCapture(e.pointerId)
    dragRef.current = { y0: e.clientY, s0: scrollRef.current }
  }

  function handlePointerMove(e: React.PointerEvent<HTMLDivElement>) {
    const d = dragRef.current
    if (!d) return
    setScroll(clamp(d.s0 - (e.clientY - d.y0), -ITEM_H, maxScroll + ITEM_H))
  }

  function handlePointerUp() {
    if (!dragRef.current) return
    dragRef.current = null
    // 拖过头的那点不用单独处理：snap 按档位夹取，目标点一定在合法区间内
    snap()
  }

  return (
    <div
      ref={rootRef}
      // w-fit + self-start：外层常被放在 flex-col 里，默认 stretch 会把 inline-block
      // 拉满整行，轮盘就跟着变成一大条——这里必须按内容收窄
      className={cn("relative inline-block w-fit self-start", className)}
    >
      <button
        type="button"
        disabled={disabled}
        onClick={() => setOpen((o) => !o)}
        className={cn(
          "inline-flex h-9 items-center gap-1 rounded-md border border-input bg-transparent px-3 text-sm shadow-xs outline-none dark:bg-input/30",
          classicalFocusRing,
          "disabled:pointer-events-none disabled:opacity-50",
          // 展开时它被轮盘盖住（轮盘中心就是它的中心），所以让它隐形但保留占位
          open && "invisible"
        )}
      >
        <span className="whitespace-nowrap tabular-nums">
          {values.length ? label(current) : "—"}
          {unit ? <span className="ml-1 text-muted-foreground">{unit}</span> : null}
        </span>
      </button>

      {open && (
        <div
          ref={wheelRef}
          // 中心对准原方框：当前值留在原来那个位置，上下各自长出其它档。
          // 用 translate 属性（不是 transform）居中，这样容器的 animate-in 缩放不会把它顶掉。
          className="absolute top-1/2 left-1/2 z-50 w-40 -translate-x-1/2 -translate-y-1/2 animate-in overflow-hidden rounded-2xl border bg-popover shadow-lg fade-in-0 zoom-in-95 select-none duration-150"
          style={{ height: VISIBLE * ITEM_H }}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={handlePointerUp}
        >
          {/* perspective 必须挂在这一层：它只作用于**直接子元素**的 transform，
              放到外面那层的话，项的 rotateX 就完全没有透视，倾斜会变成纯压扁 */}
          <div
            className="relative h-full w-full [perspective:600px]"
            style={{
              maskImage:
                "linear-gradient(to bottom, transparent, #000 20%, #000 80%, transparent)",
            }}
          >
            {/* 选中框：gray 档玻璃，只做背景高亮，不接收指针 */}
            <span
              aria-hidden
              className="pointer-events-none absolute inset-x-0 z-0"
              style={{ top: CENTER, height: ITEM_H }}
            >
              <GlassLens
                body="gray"
                shadow="none"
                shapes={[
                  {
                    cx: WHEEL_W / 2,
                    cy: ITEM_H / 2,
                    hw: WHEEL_W / 2,
                    hh: ITEM_H / 2,
                    radius: ITEM_H / 2,
                  },
                ]}
                // 背面取转盘自己那一层（bg-popover），不是再上一层的页面色，
                // 否则玻璃条会比它所在的浮层更暗、像一块补丁
                backdrop={(ctx, w, h) =>
                  paintPageBackdrop(ctx, w, h, wheelRef.current)
                }
                className="absolute inset-0"
              />
            </span>

            {/* 数值槽 + 固定的单位：只有数字在滚，单位钉在右侧不动；
                槽宽按数字本身（等宽数字两位 ≈ 1.3em），kg 才紧贴、不会悬空 */}
            <div className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center gap-1">
              <div className="relative h-full w-[1.3em]">
                {values.map((v, i) => {
                  // 沿圆周排布：θ = 该档离中心的弧长 / 半径。
                  // sin 给纵向位置（边缘被压缩、不再等距），cos 给大小与透明度，
                  // rotateX 让它向屏幕里倒 —— 三条合起来才是"转盘"。
                  const theta = (i * ITEM_H - scroll) / RADIUS
                  const c = Math.cos(theta)
                  if (c <= 0.02) return null // 转到背面就不画了
                  const y = Math.sin(theta) * RADIUS
                  const dist = Math.abs(theta)
                  const scale = 0.7 + 0.3 * c
                  const opacity = Math.min(1, Math.pow(c, 1.4) * 1.5)
                  return (
                    <div
                      key={v}
                      className={cn(
                        "absolute inset-x-0 flex items-center justify-center text-sm tabular-nums",
                        dist < 0.2
                          ? "font-semibold text-foreground"
                          : "font-normal text-muted-foreground"
                      )}
                      style={{
                        height: ITEM_H,
                        transform: `translateY(${CENTER + y}px) rotateX(${-theta}rad) scale(${scale})`,
                        opacity,
                      }}
                    >
                      {label(v)}
                    </div>
                  )
                })}
              </div>
              {unit ? (
                <span className="text-sm text-muted-foreground">{unit}</span>
              ) : null}
            </div>
          </div>
          {/* 关闭按钮放在 mask 之外（否则会被上下那条渐变一起淡掉） */}
          <button
            type="button"
            aria-label="关闭"
            // 必须拦住 pointerdown：容器的 pointer capture 会把 pointerup 抢走，
            // 浏览器随之把 click 派发给容器而不是这颗按钮，× 就点不动了
            onPointerDown={(e) => e.stopPropagation()}
            onClick={() => setOpen(false)}
            className="absolute top-2 right-2 z-20 grid size-6 place-items-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          >
            <X className="size-3.5" />
          </button>
        </div>
      )}
    </div>
  )
}

export { Picker }
