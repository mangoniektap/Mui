import * as React from "react"
import { type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"
import { ToggleGroup as ToggleGroupPrimitive } from "radix-ui"

import { toggleVariants } from "@/components/ui/toggle"
import { animateGlassPress, GlassLens } from "@/styles/surfaces/liquid-glass"

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

/** 完整对齐时底层文字和图标继承激活色，局部覆盖时由透镜绘制颜色。 */
const SEG_BASE =
  "relative z-10 inline-flex items-center justify-center rounded-full font-medium whitespace-nowrap outline-none text-muted-foreground hover:text-foreground data-[covered]:text-inherit data-[covered]:hover:text-inherit focus-visible:ring-2 focus-visible:ring-ring/50 disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4"

/** 玻璃滑动的缓动（与开关 thumb 的吸附同一条手感） */
const GLIDE = "translate 280ms cubic-bezier(0.32, 0.72, 0, 1)"

/**
 * 画布外扩边距 = 段高 × 这个比例：单边要同时装下按压外鼓（0.3×短边 ≈ 0.15h）
 * 与惯性挤扁时的纵向膨胀（约 0.17h），给小了那一下会被画布切掉。
 */
/** 画布外扩要同时装下按压外鼓（0.3×短边）与惯性应变放大；只按外鼓给会把玻璃切平 */
const LENS_MARGIN_RATIO = 0.6

/** 只有透镜与段实际对齐时，才给底层的整个段着色。 */
function updateSegmentCoverage(item: HTMLElement, segment: DOMRect, glass?: DOMRect) {
  const aligned = glass !== undefined &&
    Math.abs(segment.left - glass.left) < 1 &&
    Math.abs(segment.top - glass.top) < 1 &&
    Math.abs(segment.width - glass.width) < 1 &&
    Math.abs(segment.height - glass.height) < 1
  item.toggleAttribute("data-covered", aligned)
}

/** 一个段的盒子（相对 Root 的 padding box） */
type SegBox = {
  value: string | null
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
      s.value === b[i].value &&
      s.x === b[i].x &&
      s.y === b[i].y &&
      s.w === b[i].w &&
      s.h === b[i].h &&
      s.on === b[i].on
  )

/**
 * 图标 → 位图。canvas 画不了 React 节点，只能把 lucide 的 svg 序列化后当图片贴。
 * lucide 用 stroke="currentColor"，序列化前先换成真实色，否则在图片里等于黑色。
 * 按元素缓存，颜色变了才重建（选中/未选中两种）。
 */
const iconCache = new WeakMap<
  SVGElement,
  { color: string; img: HTMLImageElement }
>()

function iconImage(svg: SVGElement, color: string): HTMLImageElement | null {
  const hit = iconCache.get(svg)
  if (hit && hit.color === color) {
    return hit.img.complete && hit.img.naturalWidth > 0 ? hit.img : null
  }
  const markup = new XMLSerializer()
    .serializeToString(svg)
    .replaceAll("currentColor", color)
  const img = new Image()
  const url = URL.createObjectURL(new Blob([markup], { type: "image/svg+xml" }))
  const releaseUrl = () => URL.revokeObjectURL(url)
  img.addEventListener("load", releaseUrl, { once: true })
  img.addEventListener("error", releaseUrl, { once: true })
  img.src = url
  iconCache.set(svg, { color, img })
  return null // 第一帧先返回空，等 onload 后由下一帧（live）补上
}

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
  style,
  children,
  ...props
}: React.ComponentProps<typeof ToggleGroupPrimitive.Root> &
  VariantProps<typeof toggleVariants> & {
    spacing?: number
    /** 栏目清单（个数 = 长度）：只为单选组准备，给了就不用再写 ToggleGroupItem */
    items?: ToggleGroupSegmentedItem[]
    /** 玻璃覆盖处的文字和图标颜色，默认使用主题主色。 */
    activeColor?: string
  }) {
  /** 只有 type="single" 换成胶囊底座：底座白色 classical，选中区域是玻璃 */
  const segmented = props.type === "single"
  const rootRef = React.useRef<HTMLDivElement>(null)
  /** 玻璃块（= 段盒子）本身，画布比它四周各外扩 lensMargin */
  const glassRef = React.useRef<HTMLSpanElement>(null)
  /** 外扩量：paintTrack 里要用，但它随段高变，用 ref 传进去 */
  const marginRef = React.useRef(0)
  /** 每个段的盒子（相对 Root 的 padding box）：玻璃按它摆位、也按它吸附 */
  const [segs, setSegs] = React.useState<SegBox[]>([])
  /** 轮廓、颜色和阴影共用同一进度，不随 React 的渲染时机切换。 */
  const pressProgressRef = React.useRef(0)
  const pointerHeld = React.useRef(false)
  const cancelPressAnimation = React.useRef<(() => void) | null>(null)
  React.useEffect(() => () => cancelPressAnimation.current?.(), [])
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
    // 非受控组的选中态读取 Radix 写入的 data-state。
    const read = () => {
      const els = Array.from(
        root.querySelectorAll<HTMLElement>('[data-slot="toggle-group-item"]')
      )
      const next = els.map((el) => ({
        value: el.getAttribute("data-value"),
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
      attributeFilter: ["data-state", "data-value"],
    })
    // 栏宽随文字 / 字号 / 栏数变 → 位置要重量
    const ro = new ResizeObserver(read)
    ro.observe(root)
    return () => {
      mo.disconnect()
      ro.disconnect()
    }
  }, [segmented, count])

  const value = props.type === "single" ? props.value : undefined
  // 受控组直接使用已提交的值，松手时不等 MutationObserver 才离开旧段。
  const active = segs.findIndex((segment) => value === undefined ? segment.on : segment.value === value)
  const trackMin = segs[0]?.x ?? 0
  const trackMax = segs[segs.length - 1]?.x ?? trackMin
  const clampX = (x: number) => Math.min(Math.max(x, trackMin), trackMax)
  const box = segs[pending ?? active]
  const glassLeft = drag ? clampX(drag.baseX + drag.dx) : box?.x ?? 0
  const refreshCoverage = React.useCallback(() => {
    const glass = glassRef.current?.getBoundingClientRect()
    for (const item of rootRef.current?.querySelectorAll<HTMLElement>('[data-slot="toggle-group-item"]') ?? []) {
      updateSegmentCoverage(item, item.getBoundingClientRect(), glass)
    }
  }, [])
  React.useLayoutEffect(refreshCoverage, [refreshCoverage, segmented, box, glassLeft])
  /** 画布外扩量：玻璃中心与 inset 都读它，两处必须同源 */
  const lensMargin = box ? Math.ceil(box.h * LENS_MARGIN_RATIO) : 0
  marginRef.current = lensMargin

  /**
   * 由浏览器提供页面背景，轨道和导航文字独立绘制以保留原来的着色和折射。
   * 玻璃是最顶层，段本身被它盖住，所以内容必须在这里重绘一遍再折射，
   * 否则玻璃滑过时字会整块消失。滑动时底座相对画布在动，所以用 live 每帧重画。
   */
  const paintTrack = React.useCallback(
    (ctx: CanvasRenderingContext2D, w: number, h: number, dpr: number) => {
      const root = rootRef.current
      const glass = glassRef.current
      if (!root || !glass) return
      const cs = getComputedStyle(root)
      const r = root.getBoundingClientRect()
      const g = glass.getBoundingClientRect()
      const m = marginRef.current
      // 画布左上（CSS 坐标）= 玻璃块左上 − 外扩量；以下全部换算成画布坐标
      const dx = r.left - g.left + m
      const dy = r.top - g.top + m

      ctx.save()
      ctx.scale(dpr, dpr)
      ctx.clearRect(0, 0, w / dpr, h / dpr)

      // 底座：实色胶囊 + 一圈投影（白底座压在白页面上，全靠这圈影子给出边界）
      const radius = r.height / 2
      ctx.save()
      ctx.shadowColor = "rgba(0, 0, 0, 0.12)"
      ctx.shadowBlur = 4
      ctx.shadowOffsetY = 1
      ctx.fillStyle = cs.backgroundColor
      ctx.beginPath()
      ctx.roundRect(dx, dy, r.width, r.height, radius)
      ctx.fill()
      ctx.restore()

      // 底座的 1px 描边也要画进纹理：玻璃块压在底座上、斜面会把框线折出来，
      // 不画的话玻璃盖住的那一段边框就断了（canvas 范围外由 DOM 补，颜色同源所以接得上）。
      const bw = root.clientLeft || 1
      ctx.strokeStyle = cs.borderTopColor
      ctx.lineWidth = bw
      ctx.beginPath()
      ctx.roundRect(
        dx + bw / 2,
        dy + bw / 2,
        r.width - bw,
        r.height - bw,
        radius - bw / 2
      )
      ctx.stroke()

      ctx.restore()
    },
    []
  )

  /**
   * 内容层：字 / 图标单独画进 uInk，走 q = vv - n*shift ——
   * 只被凸透镜折射，不参与 backdrop 那层的凹透镜压缩，所以尺寸不变。
   */
  const paintInk = React.useCallback(
    (
      ctx: CanvasRenderingContext2D,
      _w: number,
      _h: number,
      dpr: number
    ) => {
      const root = rootRef.current
      const glass = glassRef.current
      if (!root || !glass) return
      const g = glass.getBoundingClientRect()
      const m = marginRef.current
      ctx.save()
      ctx.scale(dpr, dpr)
      const tint = getComputedStyle(root).color
      for (const el of root.querySelectorAll<HTMLElement>(
        '[data-slot="toggle-group-item"]'
      )) {
        const er = el.getBoundingClientRect()
        updateSegmentCoverage(el, er, g)
        const cx = er.left - g.left + m + er.width / 2
        const cy = er.top - g.top + m + er.height / 2
        const svg = el.querySelector<SVGElement>("svg")
        if (svg) {
          const img = iconImage(svg, tint)
          if (img) {
            const sr = svg.getBoundingClientRect()
            ctx.drawImage(
              img,
              sr.left - g.left + m,
              sr.top - g.top + m,
              sr.width,
              sr.height
            )
          }
        }
        const label = el.lastElementChild as HTMLElement | null
        const text = label?.textContent?.trim() || el.textContent?.trim()
        if (!text) continue
        const lr = label?.getBoundingClientRect()
        const ls = getComputedStyle(label ?? el)
        ctx.font = `${ls.fontStyle} ${ls.fontWeight} ${ls.fontSize} ${ls.fontFamily}`
        ctx.fillStyle = tint
        ctx.textAlign = "center"
        ctx.textBaseline = "middle"
        ctx.fillText(
          text,
          lr && lr.width ? lr.left - g.left + m + lr.width / 2 : cx,
          lr && lr.height ? lr.top - g.top + m + lr.height / 2 : cy
        )
      }
      ctx.restore()
    },
    []
  )

  /** 用共用的按压曲线从当前进度推进，连点时沿当前轮廓继续。 */
  function animatePress(to: 0 | 1) {
    cancelPressAnimation.current?.()
    if (pressProgressRef.current === to) return

    cancelPressAnimation.current = animateGlassPress(pressProgressRef.current, to, (progress) => {
      pressProgressRef.current = progress
      glassRef.current?.style.setProperty("--glass-segment-press", String(progress))
      // 回缩的第一帧也等于 1，只有放大结束才可以切入回缩。
      if (to === 1 && progress === 1 && !pointerHeld.current) animatePress(0)
    })
  }

  function handlePointerDown(e: React.PointerEvent<HTMLDivElement>) {
    if (!segmented || drag || !segs.length || e.button !== 0 || !e.isPrimary) return
    const el =
      e.target instanceof Element
        ? e.target.closest<HTMLButtonElement>('[data-slot="toggle-group-item"]')
        : null
    if (!el || el.disabled) return
    // 捕获在原按钮上，导航栏随页面换位置时也能收到松手事件和正确的 click。
    el.setPointerCapture(e.pointerId)
    swallow.current = false
    pointerHeld.current = true
    animatePress(1)
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
    if (dx === drag.dx && moved === drag.moved) return
    setDrag({ ...drag, dx, moved })
  }

  function handlePointerUp() {
    pointerHeld.current = false
    if (pressProgressRef.current === 1) animatePress(0)
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

  function cancelPointer() {
    if (!pointerHeld.current) return
    pointerHeld.current = false
    swallow.current = false
    animatePress(0)
    setDrag(null)
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
          ...style,
          ...(segmented && {
            "--seg-active-color": activeColor?.trim() || "var(--primary)",
            color: "var(--seg-active-color, var(--primary, var(--foreground)))",
          }),
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
      onPointerCancel={segmented ? cancelPointer : undefined}
      onLostPointerCapture={segmented ? cancelPointer : undefined}
      onClickCapture={segmented ? handleClickCapture : undefined}
    >
      <ToggleGroupContext.Provider value={{ variant, size, spacing, segmented }}>
        {segmented && items
          ? items.map((item) => (
              <ToggleGroupPrimitive.Item
                key={item.value}
                data-slot="toggle-group-item"
                data-value={item.value}
                value={item.value}
                className={cn(SEG_BASE, item.icon ? SEG_STACK : SEG_TEXT)}
              >
                {item.icon}
                <span>{item.label}</span>
              </ToggleGroupPrimitive.Item>
            ))
          : children}
      </ToggleGroupContext.Provider>
      {/* 玻璃是最顶层（z-20）：段在它下面，内容由背纹理重绘后透过玻璃看。
          组件负责按压阶段，引擎按进度计算外鼓，并处理折射和运动惯性。 */}
      {segmented && box && (
        <span
          ref={glassRef}
          onTransitionEnd={refreshCoverage}
          aria-hidden
          className="pointer-events-none absolute top-0 left-0 z-20 [--segment-shadow-offset:1px] dark:[--segment-shadow-offset:0px]"
          style={
            {
              translate: `${glassLeft}px ${box.y}px`,
              width: box.w,
              height: box.h,
              // 只有真拖起来才关过渡：按下那一下要"滑"过去，不能瞬移；
              // 拖动中必须有过渡就得跟手失效，会拖影。宽高不加：栏等宽不需要，
              // 且宽高过渡会每帧触发 ResizeObserver 重建纹理。
              transition: drag?.moved ? "none" : GLIDE,
              // 画布外扩量：GlassLens 的 inset 类读它，与 shapes 的中心同源
              "--lens-margin": `${lensMargin}px`,
              "--glass-gray": "calc(1 - var(--glass-segment-press, 0))",
              "--shadow-glass-sm": "0 var(--segment-shadow-offset) 3px color-mix(in srgb, var(--shadow-glass-ink) calc(100% * var(--glass-segment-press, 0)), transparent)",
            } as React.CSSProperties
          }
        >
          <GlassLens
            live
            motion={3}
            motionReference={rootRef}
            lens={pressProgressRef}
            shapes={[
              {
                cx: lensMargin + box.w / 2,
                cy: lensMargin + box.h / 2,
                hw: box.w / 2,
                hh: box.h / 2,
                radius: box.h / 2,
                swell: 1,
              },
            ]}
            browserBackdrop
            backdrop={paintTrack}
            ink={paintInk}
            fallback={
              <span
                className="absolute rounded-full"
                style={{
                  inset: lensMargin,
                  backgroundColor: "color-mix(in oklab, var(--muted) calc(100% - 35% * var(--glass-segment-press, 0)), transparent)",
                  filter: "drop-shadow(var(--shadow-glass-sm))",
                  scale: "calc(1 + 0.2 * var(--glass-segment-press, 0))",
                }}
              />
            }
            // 单选组单独给 0.10：滑块只轻压背景轨道，不把段里的字明显缩小
            className="absolute inset-[calc(var(--lens-margin)*-1)] [--glass-lens-compress:0.10] [--glass-lens-decay:0.7]"
          />
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
      data-value={props.value}
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
