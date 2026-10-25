import * as React from "react"
import { cn } from "@/lib/utils"
import { Switch as SwitchPrimitive } from "radix-ui"
import { classicalFocusRing } from "@/styles/surfaces/classical"
import { GlassLens, paintPageBackdrop } from "@/styles/surfaces/liquid-glass"

/**
 * 开关：点击切换；按住可沿轨道左右拖动，松手按落点是否过半吸附。
 *
 * 玻璃球走液态玻璃表面（surfaces/liquid-glass）：折射 / 色散都在 shader 里，
 * 按压动画（外鼓 + 中心凹透镜）也由引擎的 pressed 跑，本组件只交一个布尔；
 * 本体档位（transparent / jade / gray）由 props 给，按住时切成透明档。
 * 背面由本组件解析式绘制（页面底色 + 轨道胶囊），所以球里能看到轨道边缘被折射。
 * CSS 只管球的位移与形状。
 * 受控与非受控均可，内部持一份状态——拖动提交绕不开 Radix 补发的 click。
 */

/** 拖动判定阈值：位移超过它才算拖动，否则当点击（toggle） */
const DRAG_THRESHOLD = 3

/** 透镜画布比球外扩的边距：容纳按压外鼓，否则鼓出部分被画布裁掉 */
const LENS_MARGIN = 6

/** thumb 与轨道四周各留的缝：露出一道淡边，四边等粗 */
const TRACK_INSET = 1.5

/** 三档尺寸：轨道 w×h；thumb 高 = 轨道高 − 2×缝，四边等粗 */
const SIZES = {
  sm: { trackW: 24, trackH: 14, thumbW: 16, thumbH: 11 },
  md: { trackW: 32, trackH: 16, thumbW: 20, thumbH: 13 },
  lg: { trackW: 40, trackH: 20, thumbW: 24, thumbH: 17 },
} as const


function Switch({
  className,
  size = "md",
  checked: checkedProp,
  defaultChecked,
  onCheckedChange,
  disabled,
  ...props
}: React.ComponentProps<typeof SwitchPrimitive.Root> & {
  size?: keyof typeof SIZES
}) {
  const { trackW, trackH, thumbW, thumbH } = SIZES[size]
  const [inner, setInner] = React.useState(defaultChecked ?? false)
  const controlled = checkedProp !== undefined
  const checked = controlled ? checkedProp : inner

  const rootRef = React.useRef<HTMLButtonElement>(null)
  const thumbRef = React.useRef<HTMLSpanElement>(null)

  /** 按住态：按压动画（外鼓 + 中心凹透镜 + 本体换档）全交给 GlassLens 的 pressed */
  const [pressed, setPressed] = React.useState(false)

  const [drag, setDrag] = React.useState<{ x0: number; moved: boolean } | null>(
    null
  )
  const [dx, setDx] = React.useState(0)
  /** 拖动提交后吞掉 Radix 紧接着补发的那次 click，否则一帧内改两次 */
  const swallowClick = React.useRef(false)

  /**
   * 球从一端走到另一端的位移 = 轨道内容宽 − 球宽 − 两端各留的缝。
   * 实测而不是写死 calc(100%-2px)——那个写法只在某个尺寸组合下凑巧对，
   * 球一改尺寸球就跑出轨道外。
   */
  const [travel, setTravel] = React.useState(0)
  React.useLayoutEffect(() => {
    const root = rootRef.current
    const thumb = thumbRef.current
    if (!root || !thumb) return
    const read = () =>
      setTravel(
        Math.max(0, root.clientWidth - thumb.offsetWidth - TRACK_INSET * 2)
      )
    read()
    const ro = new ResizeObserver(read)
    ro.observe(root)
    return () => ro.disconnect()
  }, [size])

  function commit(next: boolean) {
    if (!controlled) setInner(next)
    onCheckedChange?.(next)
  }

  function handlePointerDown(e: React.PointerEvent<HTMLButtonElement>) {
    if (disabled) return
    setPressed(true)
    swallowClick.current = false
    e.currentTarget.setPointerCapture(e.pointerId)
    setDrag({ x0: e.clientX, moved: false })
    setDx(0)
  }

  function handlePointerMove(e: React.PointerEvent<HTMLButtonElement>) {
    if (!drag) return
    const d = e.clientX - drag.x0
    if (!drag.moved && Math.abs(d) > DRAG_THRESHOLD)
      setDrag({ ...drag, moved: true })
    setDx(d)
  }

  function handlePointerUp() {
    setPressed(false)
    if (!drag) return
    if (drag.moved) {
      swallowClick.current = true
      commit((checked ? travel : 0) + dx > travel / 2)
    }
    setDrag(null)
    setDx(0)
  }

  /** 球的水平位置：四周各留 TRACK_INSET（露出一道淡边），拖动期间跟手并夹在轨道内。
   *  transition:none 只在真正拖起来后才挂——静止按压只是球不动，别提前把过渡关掉。 */
  const thumbX =
    TRACK_INSET +
    (drag
      ? Math.min(travel, Math.max(0, (checked ? travel : 0) + dx))
      : checked
        ? travel
        : 0)
  const thumbStyle: React.CSSProperties & { "--lens-margin": string } = {
    // 尺寸写在 style 上：SIZES 是唯一来源，CSS 类里不再留一份
    width: thumbW,
    height: thumbH,
    translate: `${thumbX}px`,
    transition: drag?.moved ? "none" : undefined,
    // 画布外扩量：GlassLens 的 inset 类读它，与 shapes 的 cx/cy 同源
    "--lens-margin": `${LENS_MARGIN}px`,
  }

  /** 透镜背面：页面底色 + 轨道胶囊（每帧重画，跟着球走；坐标按 dpr 换设备像素） */
  const paintBackdrop = React.useCallback(
    (ctx: CanvasRenderingContext2D, w: number, h: number, dpr: number) => {
      const thumb = thumbRef.current
      const root = rootRef.current
      if (!thumb || !root) return
      const t = thumb.getBoundingClientRect()
      const r = root.getBoundingClientRect()
      // 纹理的原点 = 画布左上角（画布比球外扩 LENS_MARGIN），不是球的左上角。
      // 拿球当原点，整块背面会偏左上 LENS_MARGIN 像素，球里折出来的轨道边缘就跟球外错位。
      const box = thumb.firstElementChild?.getBoundingClientRect() ?? t
      // 页面 = 轨道身后那一层。传 root 自己会把轨道色当成页面色铺满画布（球四周出现方块）。
      paintPageBackdrop(ctx, w, h, root.parentElement)
      ctx.fillStyle = getComputedStyle(root).backgroundColor
      ctx.beginPath()
      ctx.roundRect(
        (r.left - box.left) * dpr,
        (r.top - box.top) * dpr,
        r.width * dpr,
        r.height * dpr,
        (r.height / 2) * dpr
      )
      ctx.fill()
    },
    []
  )

  return (
    <SwitchPrimitive.Root
      ref={rootRef}
      data-slot="switch"
      disabled={disabled}
      checked={checked}
      onCheckedChange={(next) => {
        if (swallowClick.current) {
          swallowClick.current = false
          return
        }
        commit(next)
      }}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onPointerLeave={() => setPressed(false)}
      onPointerCancel={() => {
        setPressed(false)
        setDrag(null)
        setDx(0)
      }}
      style={{ width: trackW, height: trackH }}
      className={cn(
        "peer inline-flex shrink-0 items-center rounded-full shadow-xs transition-colors outline-none select-none touch-none disabled:cursor-not-allowed disabled:opacity-50 data-[state=checked]:bg-primary data-[state=unchecked]:bg-input dark:data-[state=unchecked]:bg-input/80",
        classicalFocusRing,
        className
      )}
      {...props}
    >
      <SwitchPrimitive.Thumb
        ref={thumbRef}
        data-slot="switch-thumb"
        style={thumbStyle}
        // 球是小件：斜面比例要比卡牌窄，否则 bevel 占到半径一半，边缘糊成一圈
        className="[--glass-lens-band:0.3] pointer-events-none relative block rounded-full transition-[translate] duration-[var(--glass-press-duration)] ease-out"
      >
        <GlassLens
          live
          motion={5} // 水滴惯性：加速度驱动，起步甩长、急停挤扁后回弹
          body={pressed ? "transparent" : "jade"}
          pressed={pressed}
          shapes={[
            {
              cx: LENS_MARGIN + thumbW / 2,
              cy: LENS_MARGIN + thumbH / 2,
              hw: thumbW / 2,
              hh: thumbH / 2,
              radius: thumbH / 2, // 胶囊圆角 = 半高
              swell: 1, // 按住外鼓，量由引擎按 --glass-press-swell × 短边算
            },
          ]}
          backdrop={paintBackdrop}
          fallback={
            <span className="absolute inset-0 rounded-full bg-foreground/6" />
          }
          className="absolute inset-[calc(var(--lens-margin)*-1)] [transition-property:--glass-jade,--glass-gray] duration-[var(--glass-body-duration)] ease-out"
        />
      </SwitchPrimitive.Thumb>
    </SwitchPrimitive.Root>
  )
}

export { Switch }
