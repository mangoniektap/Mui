import * as React from "react"
import { cn } from "@/lib/utils"
import { Slider as SliderPrimitive } from "radix-ui"
import { GlassLens, paintPageBackdrop } from "@/styles/surfaces/liquid-glass"

/** 拇指件尺寸（px）：胶囊，高沿用原来那个圆的直径（轨道 8px 上本来就够），只把宽度加上去 */
const THUMB_W = 24
const THUMB_H = 16

/** 透镜画布比拇指件外扩的边距：容纳按压外鼓，否则鼓出部分被画布裁掉 */
const LENS_MARGIN = 6

/**
 * 玻璃拇指件：胶囊（宽 > 高、圆角 = 半高），不是正圆。折射 / 色散都在引擎里，
 * 本体 bool 与按压动画由 pressed 驱动。
 * 背面由本组件解析式绘制（页面底色 + 轨道胶囊 + 已填充段），胶囊里能看到轨道边缘被折射。
 * Radix 的 Thumb 本身是 position:absolute，画布外扩就相对它算。
 */
function GlassThumb() {
  const ref = React.useRef<HTMLSpanElement>(null)
  const [pressed, setPressed] = React.useState(false)

  /** 透镜背面：页面底色 + 轨道胶囊 + 已填充段 */
  const paintBackdrop = React.useCallback(
    (ctx: CanvasRenderingContext2D, w: number, h: number, dpr: number) => {
      const thumb = ref.current
      const root = thumb?.closest('[data-slot="slider"]') as HTMLElement | null
      if (!thumb || !root) return
      // 纹理原点 = 画布左上角（画布比球外扩 LENS_MARGIN），不是球的左上角
      const box =
        thumb.firstElementChild?.getBoundingClientRect() ??
        thumb.getBoundingClientRect()
      // 页面 = 轨道身后那一层。传 root 自己会把轨道色当成页面色铺满画布
      paintPageBackdrop(ctx, w, h, root.parentElement)
      const track = root.querySelector<HTMLElement>('[data-slot="slider-track"]')
      if (!track) return
      const t = track.getBoundingClientRect()
      const rail = () => {
        ctx.beginPath()
        ctx.roundRect(
          (t.left - box.left) * dpr,
          (t.top - box.top) * dpr,
          t.width * dpr,
          t.height * dpr,
          (Math.min(t.width, t.height) / 2) * dpr
        )
      }
      ctx.fillStyle = getComputedStyle(track).backgroundColor
      rail()
      ctx.fill()

      const range = track.querySelector<HTMLElement>('[data-slot="slider-range"]')
      const r = range?.getBoundingClientRect()
      if (!range || !r || r.width === 0 || r.height === 0) return
      // 已填充段：端头画成弧（圆角 = 半高）——玻璃里看到的是一条弧边，不是直切。
      // 左端那半个圆角与轨道自己的圆头重合，再靠 clip 收一次，不会露出方角。
      const x = (r.left - box.left) * dpr
      const y = (r.top - box.top) * dpr
      const rw = r.width * dpr
      const rh = r.height * dpr
      ctx.save()
      rail()
      ctx.clip()
      ctx.fillStyle = getComputedStyle(range).backgroundColor
      ctx.beginPath()
      ctx.roundRect(x, y, rw, rh, Math.min(rw, rh) / 2)
      ctx.fill()
      ctx.restore()
    },
    []
  )

  const thumbStyle: React.CSSProperties & { "--lens-margin": string } = {
    width: THUMB_W,
    height: THUMB_H,
    // 画布外扩量：GlassLens 的 inset 类读它，与 shapes 的 cx/cy 同源
    "--lens-margin": `${LENS_MARGIN}px`,
  }

  return (
    <SliderPrimitive.Thumb
      ref={ref}
      data-slot="slider-thumb"
      onPointerDown={() => setPressed(true)}
      onPointerUp={() => setPressed(false)}
      onPointerLeave={() => setPressed(false)}
      onPointerCancel={() => setPressed(false)}
      // 小件：斜面比例与按压压缩都比卡牌窄；不挂 hover 环——点完指针停在球上会亮一圈
      className="[--glass-lens-band:0.3] [--glass-lens-compress:0.20] relative block shrink-0 rounded-full ring-ring/50 transition-[color,box-shadow] focus-visible:ring-4 focus-visible:outline-hidden disabled:pointer-events-none disabled:opacity-50"
      style={thumbStyle}
    >
      <GlassLens
        live
        motion={5} // 水滴惯性：加速度驱动，起步甩长、急停挤扁后回弹
        body={pressed ? "transparent" : "jade"}
        pressed={pressed}
        shapes={[
          {
            cx: LENS_MARGIN + THUMB_W / 2,
            cy: LENS_MARGIN + THUMB_H / 2,
            hw: THUMB_W / 2,
            hh: THUMB_H / 2,
            radius: THUMB_H / 2, // 胶囊圆角 = 半高
            swell: 1, // 按住外鼓，量由引擎按 --glass-press-swell × 短边算
          },
        ]}
        backdrop={paintBackdrop}
        fallback={
          <span className="absolute inset-0 rounded-full bg-foreground/6" />
        }
        className="absolute inset-[calc(var(--lens-margin)*-1)] [transition-property:--glass-jade,--glass-gray] duration-[var(--glass-body-duration)] ease-out"
      />
    </SliderPrimitive.Thumb>
  )
}

function Slider({
  className,
  defaultValue,
  value,
  min = 0,
  max = 100,
  ...props
}: React.ComponentProps<typeof SliderPrimitive.Root>) {
  const _values = React.useMemo(
    () =>
      Array.isArray(value)
        ? value
        : Array.isArray(defaultValue)
          ? defaultValue
          : [min, max],
    [value, defaultValue, min, max]
  )

  return (
    <SliderPrimitive.Root
      data-slot="slider"
      defaultValue={defaultValue}
      value={value}
      min={min}
      max={max}
      className={cn(
        "relative flex w-full touch-none items-center select-none data-[disabled]:opacity-50 data-[orientation=vertical]:h-full data-[orientation=vertical]:min-h-44 data-[orientation=vertical]:w-auto data-[orientation=vertical]:flex-col",
        className
      )}
      {...props}
    >
      <SliderPrimitive.Track
        data-slot="slider-track"
        className={cn(
          "relative grow overflow-hidden rounded-full bg-muted data-[orientation=horizontal]:h-2 data-[orientation=horizontal]:w-full data-[orientation=vertical]:h-full data-[orientation=vertical]:w-1.5"
        )}
      >
        <SliderPrimitive.Range
          data-slot="slider-range"
          className={cn(
            "absolute bg-primary data-[orientation=horizontal]:h-full data-[orientation=vertical]:w-full"
          )}
        />
      </SliderPrimitive.Track>
      {Array.from({ length: _values.length }, (_, index) => (
        <GlassThumb key={index} />
      ))}
    </SliderPrimitive.Root>
  )
}

export { Slider }
