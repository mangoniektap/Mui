import * as React from "react"
import { cn } from "@/lib/utils"
import {
  ChevronLeftIcon,
  ChevronRightIcon,
  MoreHorizontalIcon,
} from "lucide-react"

import { buttonVariants, type Button } from "@/components/ui/button"

/** 拖动时的整排位移（px）与「松手吸附中」标志：Pagination 写、PaginationContent 读 */
const PaginationDragContext = React.createContext<{ offset: number; snap: boolean }>({
  offset: 0,
  snap: false,
})

/** 拖动超过这个距离就不当成点击（浏览器 slop 量级） */
const DRAG_CLICK_SLOP = 4

/** 转盘模式：一格宽 / 整体高（px） */
const PAGER_SLOT = 44
const PAGER_H = 36

/** 转盘模式同时显示的页码个数 */
const PAGER_VISIBLE = 5

/** 吸附动画时长（ms）：与 kotlin 侧同一个数 */
const SNAP_MS = 190

type PaginationProps = {
  /** 当前页序号（0 起）；与 pageCount / onPageChange 同时给才启用拖动换页 */
  currentPage?: number
  /** 总页数（≤1 时没有可拖动换的理由，不启用） */
  pageCount?: number
  /** 跨过一格时回调目标页序号（0 起） */
  onPageChange?: (page: number) => void
  /** 拖动多远算翻一页（px，默认 40） */
  dragStep?: number
  /** 转盘模式同时显示的页码个数（默认 5） */
  visiblePages?: number
} & React.ComponentProps<"nav">

function Pagination({
  className,
  currentPage,
  pageCount,
  onPageChange,
  dragStep = 40,
  visiblePages = PAGER_VISIBLE,
  children,
  ...props
}: PaginationProps) {
  /** 没给子节点 = 转盘模式：页码由组件自己画，拖动时只有页码动 */
  const picker = React.Children.count(children) === 0
  const [offset, setOffset] = React.useState(0)
  const [snap, setSnap] = React.useState(false)
  const drag = React.useRef({
    active: false,
    captured: false,
    started: false,
    base: 0,
    dragged: 0,
    startX: 0,
    lastX: 0,
    lastT: 0,
  })
  const suppressClick = React.useRef(false)

  const handlers: React.HTMLAttributes<HTMLElement> = {}
  const page = currentPage
  const count = pageCount
  const change = onPageChange

  if (picker && page != null && count != null && change != null && count > 1) {
    return (
      <PickerPager
        page={page}
        count={count}
        change={change}
        dragStep={dragStep}
        visible={visiblePages}
        className={className}
        {...props}
      />
    )
  }

  if (page != null && count != null && change != null && count > 1) {
    handlers.onPointerDown = (event) => {
      drag.current = {
        active: true,
        captured: false,
        started: false,
        base: page,
        dragged: 0,
        startX: event.clientX,
        lastX: event.clientX,
        lastT: performance.now(),
      }
      suppressClick.current = false
      setSnap(false)
    }
    handlers.onPointerMove = (event) => {
      const state = drag.current
      if (!state.active) return
      state.dragged = event.clientX - state.startX
      // 拖动过阈值才接管指针，点击路径保持原样（否则 setPointerCapture 会打断 click）
      if (!state.captured && Math.abs(state.dragged) > DRAG_CLICK_SLOP) {
        state.captured = true
        state.started = true
        event.currentTarget.setPointerCapture(event.pointerId)
      }
      if (!state.started) return
      // 页码每跳一格整排就重排一格，补上不足一格的零头才 1:1 跟手
      const wanted = -Math.round(state.dragged / dragStep)
      const target = Math.min(Math.max(state.base + wanted, 0), count - 1)
      setOffset(state.dragged + (target - state.base) * dragStep)
      if (target !== page) change(target)
      state.lastX = event.clientX
      state.lastT = performance.now()
    }
    const endDrag = () => {
      const state = drag.current
      state.active = false
      if (!state.started) {
        setOffset(0)
        return
      }
      suppressClick.current = true
      // 甩动补页：速度越大翻得越多（横放转盘的手感），上限 3 页
      const dt = Math.max(1, performance.now() - state.lastT)
      const speed = ((state.dragged - (state.lastX - state.startX)) / dt) * 1000
      const fling = Math.max(-3, Math.min(3, Math.round(-speed / 900)))
      const to = Math.min(Math.max(page + fling, 0), count - 1)
      if (to !== page) change(to)
      // 吸附回原位：从当前零头缓出到 0
      setSnap(true)
      setOffset(0)
      window.setTimeout(() => setSnap(false), 220)
    }
    handlers.onPointerUp = endDrag
    handlers.onPointerCancel = () => {
      drag.current.active = false
      setSnap(false)
      setOffset(0)
    }
    handlers.onClickCapture = (event) => {
      if (!suppressClick.current) return
      suppressClick.current = false
      event.preventDefault()
      event.stopPropagation()
    }
  }

  return (
    <nav
      role="navigation"
      aria-label="pagination"
      data-slot="pagination"
      data-dragging={offset !== 0 || snap || undefined}
      className={cn("mx-auto flex w-full justify-center", className)}
      {...handlers}
      {...props}
    >
      <PaginationDragContext.Provider value={{ offset, snap }}>
        {children}
      </PaginationDragContext.Provider>
    </nav>
  )
}

/**
 * 转盘式页码（横放的 picker）：同时最多 `visible` 个，中间是当前页，左右越远越小越淡。
 * 拖动只改连续页位 pos，页码跟着滚；松手按甩动速度补页并吸附到整数页。
 */
function PickerPager({
  page,
  count,
  change,
  dragStep,
  visible,
  className,
  ...props
}: {
  page: number
  count: number
  change: (page: number) => void
  dragStep: number
  visible: number
} & React.ComponentProps<"nav">) {
  const half = Math.floor((visible - 1) / 2)
  const [pos, setPos] = React.useState(page)
  const posRef = React.useRef(pos)
  posRef.current = pos
  const raf = React.useRef<number | null>(null)
  const drag = React.useRef({ active: false, started: false, x0: 0, base: 0, lastX: 0, lastT: 0 })

  /** 缓出动画到某个页位（松手吸附 / 外部换页） */
  const glide = React.useCallback((from: number, to: number, dur = SNAP_MS) => {
    if (raf.current) cancelAnimationFrame(raf.current)
    const t0 = performance.now()
    const step = (t: number) => {
      const p = Math.min(1, (t - t0) / dur)
      const e = 1 - Math.pow(1 - p, 3)
      setPos(from + (to - from) * e)
      if (p < 1) raf.current = requestAnimationFrame(step)
    }
    raf.current = requestAnimationFrame(step)
  }, [])

  // 外部换页（上一页 / 下一页）时对齐过去；拖动中不打断
  React.useEffect(() => {
    if (!drag.current.active && Math.abs(posRef.current - page) > 0.01) {
      glide(posRef.current, page)
    }
  }, [page, glide])

  const endDrag = (withFling: boolean) => {
    const d = drag.current
    d.active = false
    if (!d.started) {
      setPos(page)
      return
    }
    // 甩动补页：速度越大翻得越多（上限 3 页）
    const dt = Math.max(1, performance.now() - d.lastT)
    const speed = ((d.lastX - d.x0) / dt) * 1000
    const fling = withFling ? Math.max(-3, Math.min(3, Math.round(-speed / 900))) : 0
    const target = Math.min(Math.max(Math.round(posRef.current) + fling, 0), count - 1)
    if (target !== page) change(target)
    glide(posRef.current, target)
  }

  const center = Math.round(pos)
  const items: React.ReactNode[] = []
  for (let p = center - half - 1; p <= center + half + 1; p++) {
    if (p < 0 || p >= count) continue
    const d = p - pos
    const t = Math.min(1, Math.abs(d) / (half + 0.5))
    if (t >= 1) continue
    const active = p === center
    items.push(
      <button
        key={p}
        type="button"
        tabIndex={-1}
        onClick={() => {
          if (p === page) return
          change(p)
          glide(posRef.current, p)
        }}
        className={cn(
          "absolute top-1/2 left-1/2 flex items-center justify-center rounded-md text-[15px] outline-none transition-colors",
          active
            ? "font-semibold text-foreground"
            : "font-normal text-muted-foreground hover:bg-accent hover:text-accent-foreground"
        )}
        style={{
          width: PAGER_SLOT,
          height: PAGER_H,
          // 先摆到中心，再按「距当前页位的格数」横移并缩放
          transform: `translate(-50%,-50%) translateX(${d * PAGER_SLOT}px) scale(${1 - 0.42 * t * t})`,
          opacity: 1 - 0.6 * t,
        }}
      >
        {p + 1}
      </button>
    )
  }

  return (
    <nav
      role="navigation"
      aria-label="pagination"
      data-slot="pagination"
      data-picker
      className={cn("mx-auto flex w-full items-center justify-center gap-2", className)}
      {...props}
    >
      <PaginationPrevious
        disabled={page <= 0}
        onClick={(e) => {
          e.preventDefault()
          change(Math.max(0, page - 1))
        }}
      />
      <div
        className="relative select-none touch-none"
        style={{ width: PAGER_SLOT * visible, height: PAGER_H }}
        onPointerDown={(e) => {
          drag.current = {
            active: true,
            started: false,
            x0: e.clientX,
            base: posRef.current,
            lastX: e.clientX,
            lastT: performance.now(),
          }
        }}
        onPointerMove={(e) => {
          const d = drag.current
          if (!d.active) return
          const dx = e.clientX - d.x0
          if (!d.started && Math.abs(dx) > DRAG_CLICK_SLOP) {
            d.started = true
            e.currentTarget.setPointerCapture(e.pointerId)
          }
          if (!d.started) return
          d.lastX = e.clientX
          d.lastT = performance.now()
          // 左拖 = 下一页：页位随位移往右走
          const next = Math.min(Math.max(d.base - dx / dragStep, 0), count - 1)
          setPos(next)
          const nearest = Math.round(next)
          if (nearest !== page) change(nearest)
        }}
        onPointerUp={() => endDrag(true)}
        onPointerCancel={() => endDrag(false)}
      >
        {/* 当前页的方框：跟着当前页连续移动（拖动中不跳格） */}
        <span
          aria-hidden
          className="pointer-events-none absolute top-1/2 left-1/2 rounded-md border border-border bg-background shadow-xs"
          style={{
            width: PAGER_SLOT,
            height: PAGER_H,
            transform: `translate(-50%,-50%) translateX(${(center - pos) * PAGER_SLOT}px)`,
          }}
        />
        {items}
      </div>
      <PaginationNext
        disabled={page >= count - 1}
        onClick={(e) => {
          e.preventDefault()
          change(Math.min(count - 1, page + 1))
        }}
      />
    </nav>
  )
}

function PaginationContent({
  className,
  style,
  ...props
}: React.ComponentProps<"ul">) {
  const drag = React.useContext(PaginationDragContext)
  return (
    <ul
      data-slot="pagination-content"
      className={cn("flex flex-row items-center gap-1", className)}
      style={
        drag.offset === 0 && !drag.snap
          ? style
          : {
              ...style,
              transform: `translateX(${drag.offset}px)`,
              // 松手吸附才加过渡；拖动中必须跟手（有过渡会拖影）
              transition: drag.snap ? "transform 180ms cubic-bezier(0.33,1,0.68,1)" : undefined,
            }
      }
      {...props}
    />
  )
}

function PaginationItem({ ...props }: React.ComponentProps<"li">) {
  return <li data-slot="pagination-item" {...props} />
}

type PaginationLinkProps = {
  isActive?: boolean
  /** 不可用（首页的上一页 / 末页的下一页）：置灰且不接收点击 */
  disabled?: boolean
} & Pick<React.ComponentProps<typeof Button>, "size"> &
  React.ComponentProps<"a">

function PaginationLink({
  className,
  isActive,
  disabled,
  size = "icon",
  ...props
}: PaginationLinkProps) {
  return (
    <a
      aria-current={isActive ? "page" : undefined}
      aria-disabled={disabled || undefined}
      data-slot="pagination-link"
      data-active={isActive}
      className={cn(
        buttonVariants({
          variant: isActive ? "outline" : "ghost",
          size,
        }),
        disabled && "pointer-events-none opacity-50",
        className
      )}
      {...props}
    />
  )
}

function PaginationPrevious({
  className,
  ...props
}: React.ComponentProps<typeof PaginationLink>) {
  return (
    <PaginationLink
      aria-label="Go to previous page"
      size="default"
      className={cn("gap-1 px-2.5 sm:pl-2.5", className)}
      {...props}
    >
      <ChevronLeftIcon />
      <span className="hidden sm:block">Previous</span>
    </PaginationLink>
  )
}

function PaginationNext({
  className,
  ...props
}: React.ComponentProps<typeof PaginationLink>) {
  return (
    <PaginationLink
      aria-label="Go to next page"
      size="default"
      className={cn("gap-1 px-2.5 sm:pr-2.5", className)}
      {...props}
    >
      <span className="hidden sm:block">Next</span>
      <ChevronRightIcon />
    </PaginationLink>
  )
}

function PaginationEllipsis({
  className,
  ...props
}: React.ComponentProps<"span">) {
  return (
    <span
      aria-hidden
      data-slot="pagination-ellipsis"
      className={cn("flex size-9 items-center justify-center", className)}
      {...props}
    >
      <MoreHorizontalIcon className="size-4" />
      <span className="sr-only">More pages</span>
    </span>
  )
}

export {
  Pagination,
  PaginationContent,
  PaginationLink,
  PaginationItem,
  PaginationPrevious,
  PaginationNext,
  PaginationEllipsis,
}
