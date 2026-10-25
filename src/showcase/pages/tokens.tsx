import { useEffect, useRef, useState, type PointerEvent } from "react";

import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";
import {
  animateGlassPress,
  GlassLens,
  paintPageBackdrop,
  type LensShape,
} from "@/styles/surfaces/liquid-glass";
import { Demo } from "@/showcase/Demo";

const RADIUS = [
  ["rounded-sm", "--radius-sm"],
  ["rounded-md", "--radius-md"],
  ["rounded-lg", "--radius-lg"],
  ["rounded-xl", "--radius-xl"],
] as const;

/** 两排卡牌共用一份档位：classical 用 box-shadow 工具类，liquidglass 用同名的 drop-shadow 档 */
const SHADOWS = [
  ["shadow-xs", "xs"],
  ["shadow-sm", "sm"],
  ["shadow-md", "md"],
  ["shadow-lg", "lg"],
  ["shadow-xl", "xl"],
] as const;

const GLASS_TOKENS = [
  ["--glass-jade / --glass-gray", "本体两条通道（各 0..1）：都为 0 = 透明档（竖向的灰层次）；jade = 1 玉白（纯白不透明）；gray = 1 平灰（磨砂感、无折射无色散无边缘高光）。拆两条是为了让「灰 ↔ 透明」不绕经玉白"],
  ["--glass-gray-color / alpha", "平灰档的灰与不透明度：两套灰对同一条基线双向等距（白天往下、黑夜往上），alpha 越低越透明、底下的字越透"],
  ["--glass-body-color", "灰本体的颜色：浓度由厚度曲线给，这里只管色相与明度"],
  ["--glass-body-shade", "最厚处的浓度：0 = 完全透明，越大本体越实"],
  ["--glass-body-peak", "最厚处的纵向位置：0 = 顶 / 1 = 底（默认偏上）"],
  ["--glass-body-fade-x", "左右衰减量：沿轮廓法线往里收（带宽 = 斜面），只降一档；上下两端是归零"],
  ["--glass-rim-top / bottom", "顶 / 底轻微高光的强度：底比顶弱一档"],
  ["--glass-rim-side", "两侧暗边的强度"],
  ["--glass-rim-width", "边缘窗口宽度：贴轮廓往里算（CSS 像素）"],
  ["--glass-body-duration", "换档动画时长：本体实心度与球的位移共用（200ms）"],
  ["--shadow-glass-xs…xl", "玻璃的影子：五档与 shadow-* 一一对应（drop-shadow 版，无 spread）"],
  ["--glass-lens-band", "斜面宽度系数：min(半宽,半高) × 0.45，折射与色散的范围"],
  ["--glass-lens-bend", "边缘凸透镜：斜面放大率减 1（0.24），0 = 不折"],
  ["--glass-lens-compress", "中心凹透镜：按住时 m = 1 - 本值（0.45），越大球里的轨道越窄"],
  ["--glass-lens-chroma", "色散宽度：R 与 B 沿径向错开的总距离（3px）"],
  ["--glass-press-swell / duration", "按压外鼓：× 短边（0.2），时长 150ms"],
] as const;

/** 样品不铺底：直接落在 Demo 卡片的白色上，只有间距与对齐。 */
const BAND = "flex flex-wrap items-end gap-4";

/** 玻璃卡牌：一块玻璃表面，档位（圆角 / 阴影）由 className 给。
 *  本体是竖向的灰层次（浅 → 深 → 浅）加顶底高光、两侧暗边，都由液态玻璃表面
 *  （surfaces/liquid-glass）在 shader 里画，本身不加描边。
 *  背面铺当前卡纸的颜色——不另外铺底（样品规则）。
 *  几何从元素自己量（尺寸 + 计算圆角），不在调用处写死像素：换 size-* / rounded-* 玻璃跟着变。 */
function GlassCard({
  className,
  shadow = "sm",
  children,
}: {
  className?: string;
  /** 影子档位：与 shadow-* 一一对应，由引擎用 drop-shadow 画 */
  shadow?: "none" | "xs" | "sm" | "md" | "lg" | "xl";
  children?: React.ReactNode;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const [box, setBox] = useState({ w: 80, h: 80, r: 10 });

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const read = () => {
      const w = el.offsetWidth;
      const h = el.offsetHeight;
      // 圆角读计算值；rounded-full 给的是天文数字，按短边一半夹住
      const r = parseFloat(getComputedStyle(el).borderTopLeftRadius) || 0;
      const next = { w, h, r: Math.min(r, Math.min(w, h) / 2) };
      setBox((b) =>
        b.w === next.w && b.h === next.h && b.r === next.r ? b : next
      );
    };
    const ro = new ResizeObserver(read);
    ro.observe(el);
    read();
    return () => ro.disconnect();
  }, []);

  return (
    <div ref={ref} className={cn("relative", className)}>
      <GlassLens
        shadow={shadow}
        shapes={[
          {
            cx: box.w / 2,
            cy: box.h / 2,
            hw: box.w / 2,
            hh: box.h / 2,
            radius: box.r,
            bulge: 0,
          },
        ]}
        backdrop={(ctx, w, h) => paintPageBackdrop(ctx, w, h, ref.current)}
        className="absolute inset-0"
      />
      <span className="relative grid h-full place-items-center text-[10px] text-foreground/70">
        {children}
      </span>
    </div>
  );
}

function tokenNum(name: string, fallback: number) {
  const v = parseFloat(
    getComputedStyle(document.documentElement).getPropertyValue(name)
  );
  return Number.isFinite(v) ? v : fallback;
}

/**
 * 舞台的背面内容：极光 + 网格 + 文字。
 * 必须由画布自己画——backdrop-filter 位移不了背面像素，透镜也就无从折射。
 * 网格给透镜一组高对比直边（否则“弯”看不出来）；下半铺三行衬字，
 * 玻璃拖到哪儿，底下都有东西可折。
 */
function drawStageBackdrop(ctx: CanvasRenderingContext2D, w: number, h: number) {
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  const px = (v: number) => v * dpr;
  ctx.fillStyle = "#f5f5f7";
  ctx.fillRect(0, 0, w, h);
  const glow = (x: number, y: number, r: number, c0: string, c55: string) => {
    const g = ctx.createRadialGradient(px(x * (w / dpr)), px(y * (h / dpr)), 0, px(x * (w / dpr)), px(y * (h / dpr)), px(r * (w / dpr)));
    g.addColorStop(0, c0);
    g.addColorStop(0.55, c55);
    g.addColorStop(1, "oklch(0.7 0.16 184 / 0)");
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, w, h);
  };
  glow(0.12, 0.1, 1.0, "oklch(0.7 0.16 184 / 0.28)", "oklch(0.7 0.16 184 / 0.1)");
  glow(0.88, 0.18, 0.92, "oklch(0.75 0.12 275 / 0.24)", "oklch(0.75 0.12 275 / 0.085)");
  glow(0.78, 0.88, 0.8, "oklch(0.68 0.14 85 / 0.2)", "oklch(0.68 0.14 85 / 0.07)");
  const v = ctx.createLinearGradient(0, 0, 0, h);
  v.addColorStop(0, "oklch(1 0 0 / 0.55)");
  v.addColorStop(1, "oklch(0.92 0.01 184 / 0.45)");
  ctx.fillStyle = v;
  ctx.fillRect(0, 0, w, h);

  // 网格：透镜要有一组高对比的直边才看得出“弯”——玻璃演示里的那块格子纸
  ctx.strokeStyle = "oklch(0.4 0.02 184 / 0.1)";
  ctx.lineWidth = Math.max(1, dpr * 0.75);
  const gap = px(30);
  ctx.beginPath();
  for (let x = gap; x < w; x += gap) {
    ctx.moveTo(x, 0);
    ctx.lineTo(x, h);
  }
  for (let y = gap; y < h; y += gap) {
    ctx.moveTo(0, y);
    ctx.lineTo(w, y);
  }
  ctx.stroke();

  ctx.textBaseline = "middle";
  const sans = 'system-ui, -apple-system, "PingFang SC", sans-serif';
  const maxW = w - px(40);
  /** 一行字塞不进舞台就等比缩——窄屏宁可小一点，也不裁字 */
  const line = (text: string, weight: number, size: number, y: number, color: string) => {
    ctx.fillStyle = color;
    ctx.font = `${weight} ${px(size)}px ${sans}`;
    const wid = ctx.measureText(text).width;
    if (wid > maxW) ctx.font = `${weight} ${px((size * maxW) / wid)}px ${sans}`;
    ctx.fillText(text, px(20), y);
  };
  const H = h / dpr;
  line("液态玻璃 · 三块样品都能拖", 500, 14, px(26), "oklch(0.32 0.02 184 / 0.82)");
  line(
    "按住任意一块再走——跟手拖，松手停在原地（带一点惯性滑行）。",
    400,
    12,
    px(50),
    "oklch(0.32 0.02 184 / 0.58)"
  );
  line(
    "底下这几行字是画布画的：玻璃压上去，字会被折射，边缘裂出一线色散。",
    400,
    12,
    px(70),
    "oklch(0.32 0.02 184 / 0.58)"
  );

  // 衬字：拖到哪都有高对比边缘可折
  line(
    "REFRACTION · DISPERSION · INERTIA",
    600,
    15,
    px(H * 0.56),
    "oklch(0.35 0.03 184 / 0.52)"
  );
  line("轮廓外一个像素不动，轮廓内按深度放大", 500, 13, px(H * 0.72), "oklch(0.35 0.03 184 / 0.52)");
  line("R 与 B 用不同放大率采样，贴内沿裂出一线色散", 500, 13, px(H * 0.87), "oklch(0.35 0.03 184 / 0.52)");
}

type PieceSpec = {
  id: string;
  label: string;
  /** 宽 / 高 / 圆角（CSS 像素） */
  w: number;
  h: number;
  r: number;
  /** 按住会鼓的只有液滴（按钮）；承载内容的表面不鼓 */
  swell: boolean;
  /** 初始位置（0..1，相对舞台） */
  x: number;
  y: number;
};

const PIECES: PieceSpec[] = [
  { id: "button", label: "玻璃按钮", w: 132, h: 40, r: 10, swell: true, x: 0.2, y: 0.68 },
  { id: "card", label: "玻璃卡片", w: 104, h: 72, r: 14, swell: false, x: 0.47, y: 0.66 },
  { id: "pill", label: "胶囊", w: 68, h: 28, r: 14, swell: false, x: 0.73, y: 0.7 },
];

const PAD = 12;

/** 窄舞台上按比例缩：三块并排还得留得下（只缩尺寸，不缩文字） */
function dims(spec: PieceSpec, size: { w: number }) {
  const s = size.w < 520 ? 0.78 : 1;
  return { w: spec.w * s, h: spec.h * s, r: spec.r * s };
}

/** 归一化位置 → 舞台内像素中心，并夹在边界里（尺寸变了也跟着合法） */
function place(
  p: { x: number; y: number },
  spec: PieceSpec,
  size: { w: number; h: number }
) {
  const d = dims(spec, size);
  const hw = d.w / 2;
  const hh = d.h / 2;
  const cx = Math.min(
    Math.max(p.x * size.w, hw + PAD),
    Math.max(hw + PAD, size.w - hw - PAD)
  );
  const cy = Math.min(
    Math.max(p.y * size.h, hh + PAD),
    Math.max(hh + PAD, size.h - hh - PAD)
  );
  return { cx, cy };
}

/**
 * 液态玻璃舞台：三块样品 = 三块真透镜，都可拖。
 *
 * 拖拽是"抓住再走"：按下时记住抓取点相对中心的偏移，移动只带这个偏移；
 * 松手停在原地，残余速度被摩擦吃掉。
 *
 * 块件不变形（界面件，不是液滴）：按住外鼓（仅按钮）+ 松手惯性，不旋转、不拉伸。
 */
function GlassStage() {
  const stageRef = useRef<HTMLDivElement>(null);
  const [size, setSize] = useState({ w: 720, h: 208 });
  const [pos, setPos] = useState<Record<string, { x: number; y: number }>>(() =>
    Object.fromEntries(PIECES.map((p) => [p.id, { x: p.x, y: p.y }]))
  );
  const [held, setHeld] = useState<string | null>(null);
  /** 按压进度 0..1：鼓起来不是一步到位，按缓出曲线走 */
  const [press, setPress] = useState<Record<string, number>>({});
  const drag = useRef<{
    id: string;
    ox: number;
    oy: number;
    lx: number;
    ly: number;
    lt: number;
    vx: number;
    vy: number;
  } | null>(null);
  const raf = useRef(0);
  const glide = useRef<{ vx: number; vy: number } | null>(null);
  const pressRaf = useRef<(() => void) | null>(null);
  const pressVal = useRef<Record<string, number>>({});

  useEffect(() => {
    const el = stageRef.current;
    if (!el) return;
    const measure = () => setSize({ w: el.clientWidth, h: el.clientHeight });
    const ro = new ResizeObserver(measure);
    ro.observe(el);
    measure();
    return () => {
      ro.disconnect();
      cancelAnimationFrame(raf.current);
      pressRaf.current?.();
    };
  }, []);

  /**
   * 按压进度：曲线与时长走引擎那条（animateGlassPress），舞台只负责按块记进度——
   * 一个 GlassLens 里有三块玻璃，各自按各自的，所以不能用 GlassLens 的 pressed。
   */
  function animatePress(id: string, to: number) {
    pressRaf.current?.();
    pressRaf.current = animateGlassPress(pressVal.current[id] ?? 0, to, (v) => {
      pressVal.current[id] = v;
      setPress((p) => ({ ...p, [id]: v }));
    });
  }

  /** 松手后的滑行：摩擦吃掉残余速度（总滑行 ≈ 松手速度 × 0.03s，快甩也就几十像素） */
  function glideTo(id: string) {
    cancelAnimationFrame(raf.current);
    let last = performance.now();
    const tick = (now: number) => {
      const dt = Math.min((now - last) / 1000, 0.032);
      last = now;
      const g = glide.current;
      if (!g) {
        raf.current = 0;
        return;
      }
      g.vx *= Math.exp(-dt / 0.03);
      g.vy *= Math.exp(-dt / 0.03);
      const speed = Math.hypot(g.vx, g.vy);
      const stage = stageRef.current;
      if (stage && speed > 6) {
        const w = stage.clientWidth || 1;
        const h = stage.clientHeight || 1;
        setPos((p) => ({
          ...p,
          [id]: {
            x: p[id].x + (g.vx * dt) / w,
            y: p[id].y + (g.vy * dt) / h,
          },
        }));
      }
      if (speed > 6) {
        raf.current = requestAnimationFrame(tick);
      } else {
        raf.current = 0;
        glide.current = null;
      }
    };
    raf.current = requestAnimationFrame(tick);
  }

  function onDown(e: PointerEvent<HTMLDivElement>, spec: PieceSpec) {
    const stage = stageRef.current;
    if (!stage) return;
    const rect = stage.getBoundingClientRect();
    const { cx, cy } = place(pos[spec.id], spec, size);
    cancelAnimationFrame(raf.current);
    raf.current = 0;
    glide.current = null;
    drag.current = {
      id: spec.id,
      // 抓取点相对玻璃中心的偏移：整个拖动过程它不变
      ox: e.clientX - rect.left - cx,
      oy: e.clientY - rect.top - cy,
      lx: e.clientX,
      ly: e.clientY,
      lt: performance.now(),
      vx: 0,
      vy: 0,
    };
    setHeld(spec.id);
    if (spec.swell) animatePress(spec.id, 1);
    // 抓住指针：拖到舞台外也继续跟手（合成事件下可能不被允许，忽略即可）
    try {
      e.currentTarget.setPointerCapture(e.pointerId);
    } catch {
      /* noop */
    }
  }

  function onMove(e: PointerEvent<HTMLDivElement>) {
    const d = drag.current;
    const stage = stageRef.current;
    if (!d || !stage) return;
    const rect = stage.getBoundingClientRect();
    const now = performance.now();
    const dt = Math.max(now - d.lt, 1);
    d.vx = (e.clientX - d.lx) / dt;
    d.vy = (e.clientY - d.ly) / dt;
    d.lx = e.clientX;
    d.ly = e.clientY;
    d.lt = now;
    const nx = e.clientX - rect.left - d.ox;
    const ny = e.clientY - rect.top - d.oy;
    setPos((p) => ({ ...p, [d.id]: { x: nx / rect.width, y: ny / rect.height } }));
  }

  function onUp() {
    const d = drag.current;
    if (!d) return;
    drag.current = null;
    setHeld(null);
    animatePress(d.id, 0);
    glide.current = { vx: d.vx * 1000, vy: d.vy * 1000 };
    glideTo(d.id);
  }

  const swell = tokenNum("--glass-press-swell", 0.15);
  const shapes: LensShape[] = PIECES.map((spec) => {
    const { cx, cy } = place(pos[spec.id], spec, size);
    const d = dims(spec, size);
    return {
      cx,
      cy,
      hw: d.w / 2,
      hh: d.h / 2,
      radius: Math.min(d.r, d.h / 2),
      bulge: spec.swell ? Math.min(d.w, d.h) * swell * (press[spec.id] ?? 0) : 0,
    };
  });

  return (
    <div
      ref={stageRef}
      className="relative h-52 w-full touch-none overflow-hidden rounded-xl border select-none"
    >
      <GlassLens
        className="absolute inset-0"
        backdrop={drawStageBackdrop}
        shapes={shapes}
        fillOutside
      />
      {PIECES.map((spec, i) => (
        <div
          key={spec.id}
          role="button"
          tabIndex={0}
          aria-label={`拖动${spec.label}`}
          onPointerDown={(e) => onDown(e, spec)}
          onPointerMove={onMove}
          onPointerUp={onUp}
          onPointerCancel={onUp}
          className={cn(
            "absolute flex cursor-grab items-center justify-center text-xs font-medium text-foreground/80 active:cursor-grabbing",
            spec.id === "button" && "text-sm",
            held === spec.id ? "z-10" : "z-0"
          )}
          style={{
            left: shapes[i].cx - dims(spec, size).w / 2,
            top: shapes[i].cy - dims(spec, size).h / 2,
            width: dims(spec, size).w,
            height: dims(spec, size).h,
          }}
        >
          {spec.label}
        </div>
      ))}
    </div>
  );
}

export default function TokensPage() {
  return (
    <>
      <Demo title="圆角" hint="--radius 派生 · 实色卡牌" layout="col">
        <div className={BAND}>
          {RADIUS.map(([cls, token]) => (
            <div key={cls} className="flex flex-col items-center gap-2">
              <div className={`size-24 border bg-card ${cls}`} />
              <span className="font-mono text-[10px] text-muted-foreground">
                {cls}
              </span>
              <span className="font-mono text-[9px] text-muted-foreground/70">
                {token}
              </span>
            </div>
          ))}
        </div>
      </Demo>

      <Demo title="阴影" hint="shadow-* · 两种卡牌共用同一份目录" layout="col">
        <p className="text-xs text-muted-foreground">
          classical —— 实色卡牌：亮色由阴影说厚度，暗色由边缘亮光说厚度
        </p>
        <div className={BAND}>
          {SHADOWS.map(([cls]) => (
            <div key={cls} className="flex flex-col items-center gap-2">
              <div
                className={`grid size-20 place-items-center rounded-lg border bg-card text-[10px] ${cls}`}
              >
                卡片
              </div>
              <span className="font-mono text-[10px] text-muted-foreground">
                {cls}
              </span>
            </div>
          ))}
        </div>

        <Separator />

        <p className="text-xs text-muted-foreground">
          liquidglass —— 玻璃卡牌：档位一一对应；影子由引擎 drop-shadow 沿轮廓投，暗色翻成白光晕
        </p>
        <div className={BAND}>
          {SHADOWS.map(([cls, level]) => (
            <div key={cls} className="flex flex-col items-center gap-2">
              <GlassCard shadow={level} className="size-20 rounded-lg">
                卡片
              </GlassCard>
              <span className="font-mono text-[10px] text-muted-foreground">
                {cls}
              </span>
            </div>
          ))}
        </div>
      </Demo>

      <Demo title="字体层级" hint="font-sans / font-mono" layout="col">
        <div className="space-y-3">
          <p className="text-2xl font-semibold tracking-tight">
            标题一 · 24px semibold
          </p>
          <p className="text-lg font-semibold">标题二 · 18px semibold</p>
          <p className="text-base font-medium">标题三 · 16px medium</p>
          <p className="text-sm">
            正文 · 14px regular，用于绝大多数界面文字与说明内容。
          </p>
          <p className="text-xs text-muted-foreground">
            辅助文字 · 12px muted-foreground，用于提示与元信息。
          </p>
          <p className="font-mono text-xs">const mui = "font-mono 12px";</p>
        </div>
      </Demo>

      <Demo title="间距" hint="Tailwind 4 基准 4px" layout="col">
        <div className="flex items-end gap-3">
          {[1, 2, 3, 4, 6, 8, 12].map((n) => (
            <div key={n} className="flex flex-col items-center gap-1">
              <div
                className="rounded bg-primary/15"
                style={{ width: `calc(var(--spacing) * ${n})`, height: 24 }}
              />
              <span className="font-mono text-[10px] text-muted-foreground">
                {n}
              </span>
            </div>
          ))}
        </div>
      </Demo>

      <Demo title="组件里的用法" hint="约定" layout="col">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="outline">bg-primary</Badge>
          <Badge variant="outline">text-primary-foreground</Badge>
          <Badge variant="outline">border-border</Badge>
          <Badge variant="outline">ring-ring/50</Badge>
          <Badge variant="outline">text-muted-foreground</Badge>
          <Badge variant="outline">bg-accent</Badge>
        </div>
        <Separator />
        <p className="text-xs text-muted-foreground">
          新增组件时只使用语义令牌类（bg-card、text-muted-foreground…），不要写死颜色值，切换主题才不会有漏网之鱼。
        </p>
      </Demo>

      <Demo
        title="液态玻璃"
        hint="liquidglass 专属 · 真透镜：折射 / 色散 / 惯性"
        layout="col"
      >
        <GlassStage />
        <p className="text-xs text-muted-foreground">
          三块样品走 GlassLens：WebGL 逐像素对圆角矩形求 SDF，折射与色散都是折出来的。
          拖动只做跟手与松手惯性，块件本身不变形（界面件，不是液滴）；
          唯一的手感反馈是按住按钮时向外鼓。
        </p>
        <div className="grid gap-1.5 sm:grid-cols-2">
          {GLASS_TOKENS.map(([token, desc]) => (
            <div
              key={token}
              className="flex items-baseline gap-2 font-mono text-[11px]"
            >
              <span className="text-foreground">{token}</span>
              <span className="ml-auto truncate text-[10px] font-sans text-muted-foreground/70">
                {desc}
              </span>
            </div>
          ))}
        </div>
      </Demo>
    </>
  );
}
