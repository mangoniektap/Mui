import * as React from "react"
import { type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"
import { ToggleGroup as ToggleGroupPrimitive } from "radix-ui"

import { toggleVariants } from "@/components/ui/toggle"
import { BackdropGlass } from "@/styles/surfaces/backdrop-glass"

/**
 * 单选组的一个栏目。
 * label 必给；给了 icon 就变成「图标在上、文字在下」的竖排形态。
 * 同一组里各栏 label 请保持**等长**（等长才等宽，玻璃滑块滑动时宽度不变、只滑位置）。
 */
export type ToggleGroupSegmentedItem = {
  value: string
  label: string
  icon?: React.ReactNode
}

/** 段：纯文字（高 32，底座高 = 它 + 2×Root 的 p-0.5） */
const SEG_TEXT = "h-8 px-4 text-sm"
/** 段：图标 + 文字（图标 16 + 间距 4 + 文字 14 + 上下 padding 16 = 50） */
const SEG_STACK = "flex-col gap-1 px-4 py-2 text-[11px] leading-none"

/** 段在玻璃对齐时使用激活色。折射由玻璃直接读取下方的渲染内容。 */
const SEG_BASE =
  "relative z-10 inline-flex items-center justify-center rounded-full font-medium whitespace-nowrap outline-none transition-colors duration-[280ms] ease-[cubic-bezier(0.32,0.72,0,1)] text-muted-foreground hover:text-foreground data-[covered=true]:text-[color:var(--seg-active-color)] focus-visible:ring-2 focus-visible:ring-ring/50 disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4"

/** 玻璃滑动的缓动（与开关 thumb 的吸附同一条手感） */
const GLIDE = "translate 280ms cubic-bezier(0.32, 0.72, 0, 1)"

/** 一个段的盒子（相对 Root 的 padding box） */
type SegBox = {
  x: number
  y: number
  w: number
  h: number
  on: boolean
}

const sameBoxes = (a: SegBox[], b: SegBox[]) =>
  a.length === b.length &&
  a.every(
    (s, i) =>
      s.x === b[i].x &&
      s.y === b[i].y &&
      s.w === b[i].w &&
      s.h === b[i].h &&
      s.on === b[i].on
  )

const ToggleGroupContext = React.createContext<
  VariantProps<typeof toggleVariants> & {
    spacing?: number
    /** 单选组走胶囊底座 + 玻璃滑块的分段控件形态；多选组仍是原来那排按钮 */
    segmented?: boolean
  }
>({
  size: "default",
  variant: "default",
  spacing: 0,
  segmented: false,
})

function ToggleGroup({
  className,
  variant,
  size,
  spacing = 0,
  items,
  activeColor,
  children,
  ...props
}: React.ComponentProps<typeof ToggleGroupPrimitive.Root> &
  VariantProps<typeof toggleVariants> & {
    spacing?: number
    /** 栏目清单（个数 = 长度）：只为单选组准备，给了就不用再写 ToggleGroupItem */
    items?: ToggleGroupSegmentedItem[]
    /** 玻璃与段对齐时该段的文字和图标颜色。 */
    activeColor?: string
  }) {
  /** 只有 type="single" 换成胶囊底座：底座白色 classical，选中区域是玻璃 */
  const segmented = props.type === "single"
  const rootRef = React.useRef<HTMLDivElement>(null)
  /** 每个段的盒子（相对 Root 的 padding box）：玻璃按它摆位、也按它吸附 */
  const [segs, setSegs] = React.useState<SegBox[]>([])
  /** 按住时玻璃扩大并启用折射。 */
  const [pressed, setPressed] = React.useState(false)
  /** 拖动中：起点、起始段左边缘与宽、累计位移、是否真的拖起来了 */
  const [drag, setDrag] = React.useState<{
    x0: number
    baseX: number
    w: number
    dx: number
    moved: boolean
  } | null>(null)
  /** 松手后吸附到的段号：压在 data-state 更新之前，避免玻璃先弹回旧段 */
  const [pending, setPending] = React.useState<number | null>(null)
  /** 拖动结束浏览器会补发一次 click，先拦下来（提交那一发自己放行） */
  const swallow = React.useRef(false)
  const count = items ? items.length : React.Children.count(children)

  React.useLayoutEffect(() => {
    if (!segmented) {
      setSegs([])
      return
    }
    const root = rootRef.current
    if (!root) return
    // 段的位置全量量一遍：玻璃要滑到哪一段、松手吸附到哪一段，都从这份盒子算。
    // 选中态不接管 value / onValueChange，只读 Radix 自己写的 data-state。
    const read = () => {
      const els = Array.from(
        root.querySelectorAll<HTMLElement>('[data-slot="toggle-group-item"]')
      )
      const next = els.map((el) => ({
        x: el.offsetLeft,
        y: el.offsetTop,
        w: el.offsetWidth,
        h: el.offsetHeight,
        on: el.getAttribute("data-state") === "on",
      }))
      setSegs((prev) => (sameBoxes(prev, next) ? prev : next))
      // 松手提交已生效（落点那段变 on）→ 收掉 pending，之后位置全交给 data-state
      setPending((p) => (p !== null && next[p]?.on ? null : p))
    }
    read()
    const mo = new MutationObserver(read)
    mo.observe(root, {
      attributes: true,
      subtree: true,
      attributeFilter: ["data-state"],
    })
    // 栏宽随文字 / 字号 / 栏数变 → 位置要重量
    const ro = new ResizeObserver(read)
    ro.observe(root)
    return () => {
      mo.disconnect()
      ro.disconnect()
    }
  }, [segmented, count])

  const active = segs.findIndex((s) => s.on)
  const trackMin = segs[0]?.x ?? 0
  const trackMax = segs[segs.length - 1]?.x ?? trackMin
  const clampX = (x: number) => Math.min(Math.max(x, trackMin), trackMax)
  const box = segs[pending ?? active]
  const glassLeft = drag ? clampX(drag.baseX + drag.dx) : box?.x ?? 0
  // 拖动时 Radix 的选中值还未提交，颜色按玻璃与段的位置判断。
  const coveredIndex = segs.findIndex(
    (s) => Math.abs(s.x - glassLeft) < 1 && Math.abs(s.w - (box?.w ?? 0)) < 1
  )

  function handlePointerDown(e: React.PointerEvent<HTMLDivElement>) {
    if (!segmented || drag || !segs.length) return
    const el =
      e.target instanceof Element
        ? e.target.closest<HTMLElement>('[data-slot="toggle-group-item"]')
        : null
    if (!el) return
    setPressed(true)
    // 按下即锚定：按到的那一段不是当前选中段就立刻切过去，玻璃当场滑到指针下，不等松手。
    // 程序化 click 要临时放行（否则被下面的 capture 逻辑当成拖动补发吞掉），
    // 松手后浏览器派发的那次真 click 再拦。
    if (el.getAttribute("data-state") !== "on") {
      swallow.current = false
      el.click()
      swallow.current = true
    }
    // 拖动基准就用"按下的那一段"，与刚提交的选中段同源，拖起来不会从旧位置跳一下
    setDrag({
      x0: e.clientX,
      baseX: el.offsetLeft,
      w: el.offsetWidth,
      dx: 0,
      moved: false,
    })
  }

  function handlePointerMove(e: React.PointerEvent<HTMLDivElement>) {
    if (!drag) return
    const dx = e.clientX - drag.x0
    const moved = drag.moved || Math.abs(dx) > 3
    // 指针捕获必须等到真拖起来才做：pointerdown 就捕获会把 pointerup 抢到 Root，
    // 浏览器随之把 click 派发给 Root 而不是那一段 <button>，Radix 收不到点击
    // ——表现就是"玻璃只外鼓、不换位"。
    if (moved && !drag.moved) e.currentTarget.setPointerCapture(e.pointerId)
    if (dx === drag.dx && moved === drag.moved) return
    setDrag({ ...drag, dx, moved })
  }

  function handlePointerUp() {
    setPressed(false)
    if (!drag) return
    const d = drag
    setDrag(null)
    // 没真的拖起来：当普通点击，Radix 自己切
    if (!d.moved) return
    swallow.current = true
    // 玻璃中心落在哪一段就吸到哪一段
    const center = clampX(d.baseX + d.dx) + d.w / 2
    let to = 0
    let best = Infinity
    segs.forEach((s, i) => {
      const dist = Math.abs(s.x + s.w / 2 - center)
      if (dist < best) {
        best = dist
        to = i
      }
    })
    if (segs[to]?.on) return
    const el = rootRef.current?.querySelectorAll<HTMLElement>(
      '[data-slot="toggle-group-item"]'
    )[to]
    if (!el) return
    setPending(to)
    swallow.current = false // 这一发是提交，放行
    el.click()
    swallow.current = true // 后续浏览器补发的 click 继续拦
  }

  function handleClickCapture(e: React.MouseEvent<HTMLDivElement>) {
    // 拖动结束后浏览器补发的那一次 click：拦掉，不参与选中
    if (swallow.current) {
      swallow.current = false
      e.stopPropagation()
      e.preventDefault()
      return
    }
    // 单选组不允许"取消选中"：点已经选中的那一栏什么都不做。
    // 否则 Radix 会把值清成空串，玻璃失去锚点直接消失。
    const el =
      e.target instanceof Element
        ? e.target.closest('[data-slot="toggle-group-item"]')
        : null
    if (el?.getAttribute("data-state") === "on") {
      e.stopPropagation()
      e.preventDefault()
    }
  }

  return (
    <ToggleGroupPrimitive.Root
      ref={rootRef}
      data-slot="toggle-group"
      data-variant={variant}
      data-size={size}
      data-spacing={spacing}
      style={
        {
          "--gap": spacing,
          // 玻璃与段对齐时让文字和图标使用同一个激活色。
          "--seg-active-color": activeColor ?? "var(--foreground)",
        } as React.CSSProperties
      }
      className={cn(
        segmented
          ? "relative inline-flex w-fit touch-none items-center rounded-full border border-input bg-card p-0.5 shadow-sm select-none"
          : "group/toggle-group flex w-fit items-center gap-[--spacing(var(--gap))] rounded-md data-[spacing=default]:data-[variant=outline]:shadow-xs",
        className
      )}
      {...props}
      onPointerDown={segmented ? handlePointerDown : undefined}
      onPointerMove={segmented ? handlePointerMove : undefined}
      onPointerUp={segmented ? handlePointerUp : undefined}
      onPointerCancel={
        segmented
          ? () => {
              setPressed(false)
              setDrag(null)
            }
          : undefined
      }
      onClickCapture={segmented ? handleClickCapture : undefined}
    >
      <ToggleGroupContext.Provider value={{ variant, size, spacing, segmented }}>
        {segmented && items
          ? items.map((item, i) => (
              <ToggleGroupPrimitive.Item
                key={item.value}
                data-slot="toggle-group-item"
                data-covered={coveredIndex === i ? "true" : undefined}
                value={item.value}
                className={cn(SEG_BASE, item.icon ? SEG_STACK : SEG_TEXT)}
              >
                {item.icon}
                <span>{item.label}</span>
              </ToggleGroupPrimitive.Item>
            ))
          : segmented
            ? React.Children.map(children, (child, i) =>
                React.isValidElement<{ "data-covered"?: string }>(child)
                  ? React.cloneElement(child, {
                      "data-covered": coveredIndex === i ? "true" : undefined,
                    })
                  : child
              )
            : children}
      </ToggleGroupContext.Provider>
      {segmented && box && box.w > 0 && box.h > 0 && (
        <span
          aria-hidden="true"
          className="pointer-events-none absolute top-0 left-0 z-20 motion-reduce:transition-none"
          style={{
            translate: `${glassLeft}px ${box.y}px`,
            width: box.w,
            height: box.h,
            transition: drag?.moved ? "none" : GLIDE,
          }}
        >
          <BackdropGlass width={box.w} height={box.h} pressed={pressed} />
        </span>
      )}
    </ToggleGroupPrimitive.Root>
  )
}

function ToggleGroupItem({
  className,
  children,
  variant,
  size,
  ...props
}: React.ComponentProps<typeof ToggleGroupPrimitive.Item> &
  VariantProps<typeof toggleVariants>) {
  const context = React.useContext(ToggleGroupContext)

  return (
    <ToggleGroupPrimitive.Item
      data-slot="toggle-group-item"
      data-variant={context.variant || variant}
      data-size={context.size || size}
      data-spacing={context.spacing}
      className={cn(
        context.segmented
          ? cn(SEG_BASE, SEG_TEXT)
          : cn(
              toggleVariants({
                variant: context.variant || variant,
                size: context.size || size,
              }),
              "w-auto min-w-0 shrink-0 px-3 focus:z-10 focus-visible:z-10",
              "data-[spacing=0]:rounded-none data-[spacing=0]:shadow-none data-[spacing=0]:first:rounded-l-md data-[spacing=0]:last:rounded-r-md data-[spacing=0]:data-[variant=outline]:border-l-0 data-[spacing=0]:data-[variant=outline]:first:border-l"
            ),
        className
      )}
      {...props}
    >
      {children}
    </ToggleGroupPrimitive.Item>
  )
}

export { ToggleGroup, ToggleGroupItem }
