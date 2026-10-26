import { useEffect, useRef, useState, type ReactNode } from "react";

import { cn } from "@/lib/utils";

/**
 * 液态玻璃引擎（WebGL）。这是玻璃的唯一实现——折射 / 色散都在这里。
 *
 * 对每个像素求圆角矩形 SDF，轮廓内依次做五件事：
 *   折射 —— 采样点沿轮廓法线内收 |v|·(1 − 1/m) 像素，w 从轮廓取 1、往里衰减到 0；
 *   色散 —— R 与 B 沿同一条法线错开 uChroma 个像素采样同一背面；
 *   本体 —— 透明档是一层竖向的灰（浅 → 深 → 浅，最深在中偏上），玉白档是纯白（bool）；
 *   边缘 —— 顶/底轻微高光、两侧暗边，权重按轮廓法线分给上下与左右；
 *   背面 —— 由调用方画进 2D canvas（backdrop 回调），上传成纹理，shader 逐像素采样。
 *
 * 运动惯性（motion 0..5）：整块玻璃在页面上位移时，坐标按一张**保体积**的应变伸缩——
 * 沿运动轴尺寸 ×(1+ε)、垂直轴 ×1/(1+ε)（行列式恒为 1），急停时横拖被挤窄、高度鼓高。
 * 应变由沿运动方向的**加速度**驱动（加速度 → 目标应变 → 阻尼弹簧），匀速平移不形变：
 * 起步被甩长、急停被挤扁，随后弹簧过冲一下再定。主轴只可能是横或纵（斜轴缩放在矩形上
 * 会把直边一起拧斜），shader 只做这个仿射，所以形状 / 法线 / 折射 / 本体自然一起变形。
 *
 * 两条不能改坏的约束（改了等于没有色散）：
 *   1) uChroma 是像素量，不是倍率。倍率下位移 = v·2o/m²，小件上不到 1 设备像素，
 *      被 LINEAR 采样平均掉。
 *   2) R/B 的采样点必须夹在轮廓内。越界会采到轮廓外未折射的像素，沿轮廓折出
 *      一圈均匀彩环（接缝伪影，不是色散）。
 *
 * 轮廓外一个像素都不画（预乘 alpha = 0）：画布只是玻璃那一层，DOM 原样透出来；
 * 只有 fillOutside 的舞台自己当背景时才铺满背面。没有 WebGL 时给一层平色
 * （fallback），不冒充玻璃。参数从 --glass-lens-* 读取，在
 * canvas 元素上读——调用方在哪一层改令牌，引擎都跟着变。
 */

const SHAPE_MAX = 3;

const VERT = `
attribute vec2 aPos;
varying vec2 vPos;
void main() {
  vPos = aPos;
  gl_Position = vec4(aPos, 0.0, 1.0);
}
`;

const FRAG = `
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
#define N ${SHAPE_MAX}

varying vec2 vPos;
uniform sampler2D uTex;
uniform vec2 uRes;

// ── 每块玻璃（设备像素）──
uniform vec2 uCenter[N]; // 中心
uniform vec2 uHalf[N]; // 半宽 / 半高
uniform float uRadius[N]; // 圆角半径
uniform float uBulge[N]; // 按压外鼓量

// ── 光学 + 外观（令牌 → uniform）──
uniform float uBendBand; // --glass-lens-band    斜面宽度系数
uniform float uBend; // --glass-lens-bend      静止态放大率 - 1
uniform float uCompress; // --glass-lens-compress 按压态压缩率：m = 1 - 本值，越大球里越窄
uniform float uLens; // 按压进度 0..1：0 = 放大（静止）/ 1 = 压缩（按住）
uniform float uChroma; // --glass-lens-chroma   色散宽度（像素，非倍率）
uniform float uBody; // 玉白权重 0..1（本体档位 0→1 段）
uniform float uGray; // 平灰权重 0..1（本体档位 1→2 段）：满灰时把全部光学关掉
uniform vec3 uGrayColor; // --glass-gray-color 平灰档的灰（亮/暗两套对基线等距）
uniform vec3 uBodyColor; // --glass-body-color 灰本体的颜色
uniform float uBodyShade; // --glass-body-shade 最厚处的浓度：0 = 完全透明
uniform float uBodyPeak; // --glass-body-peak  最厚处在纵向上的位置：0 = 顶 / 1 = 底
uniform float uBodyFadeX; // --glass-body-fade-x 左右衰减量：沿斜面往里收，0 = 不衰减
uniform vec3 uRimLight; // --glass-rim-light  顶/底高光的颜色
uniform vec3 uRimDark; // --glass-rim-dark   两侧暗边的颜色
uniform float uRimTop; // --glass-rim-top    顶边高光强度
uniform float uRimBottom; // --glass-rim-bottom 底边高光强度
uniform float uRimSide; // --glass-rim-side   两侧暗边强度
uniform float uRimWidth; // --glass-rim-width  边缘窗口宽度（CSS 像素，贴轮廓往里算）
uniform vec2 uMotionAxis; // 应变轴：(1,0) 横 / (0,1) 纵——只允许屏幕轴，胶囊才不会被拧斜
uniform float uMotionAmp; // 沿应变轴的应变 ε：> 0 拉长（起步），< 0 挤扁（急停），另一轴按 1/(1+ε) 反向补偿
uniform float uDpr; // 设备像素比：抗锯齿宽度按 CSS 像素换算
uniform float uFill; // 轮廓外画什么：0 = 透明（玻璃贴在 DOM 上）/ 1 = 铺背面（画布本身就是背景）
uniform float uAlpha; // 轮廓内不透明度：1 = 全盖住 / <1 = 磨砂半透明，让背面 DOM 透出来

// 圆角矩形 SDF：h = 半宽半高，r = 圆角半径（胶囊 r = 半高）
float sdRoundRect(vec2 p, vec2 h, float r) {
  vec2 q = abs(p) - h + r;
  return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

// 轮廓外法线：SDF 的中心差分梯度（圆角处也连续）。
// 深处的 SDF 是平的、梯度为 0，那里 w = 0 用不到，给个兜底方向即可。
vec2 sdfNormal(vec2 p, vec2 h, float r) {
  const float e = 1.0;
  float gx = sdRoundRect(p + vec2(e, 0.0), h, r) - sdRoundRect(p - vec2(e, 0.0), h, r);
  float gy = sdRoundRect(p + vec2(0.0, e), h, r) - sdRoundRect(p - vec2(0.0, e), h, r);
  return normalize(vec2(gx, gy) + vec2(1e-5));
}

void main() {
  // 裁剪空间 → 画布像素（y 向下）
  vec2 p = vec2((vPos.x * 0.5 + 0.5) * uRes.x, (0.5 - vPos.y * 0.5) * uRes.y);
  vec2 uv = p / uRes;
  vec3 col = texture2D(uTex, uv).rgb;

  // 覆盖度：轮廓内 1、轮廓外 0。抗锯齿带宽按 CSS 像素给（× dpr 换设备像素）——
  // 写死 1 设备像素会让 retina 上只剩 0.5 CSS px，小球边缘锯齿明显。
  // 玻璃贴在 DOM 上时画布比玻璃大（球要外鼓），轮廓外必须留空：
  // 否则画布把自己那张不透明的方片盖在页面上，玻璃就成了一个方块。
  float a = 0.0;

  for (int i = 0; i < N; i++) {
    vec2 v = p - uCenter[i];
    // 运动惯性：应变锁在**屏幕轴**上——应变轴只可能是横 (1,0) 或纵 (0,1)，另一轴按
    // 1/(1+ε) 反向补偿。行列式恒为 1（玻璃厚度不变 ⇒ 面积守恒即体积守恒）。
    // ε > 0 = 沿轴拉长（起步被甩开）；ε < 0 = 沿轴挤扁、另一轴鼓高（急停被挤住）。
    // 为什么不用速度方向当轴：矩形沿**斜轴**做各向异性缩放时，直边会被一起拧斜
    // （只有沿轴 / 垂直轴的直线方向不变），看起来像整块转了几度。轴由 JS 按主分量选，
    // 带滞回，斜拖时不会来回跳。
    vec2 vv = v;
    if (abs(uMotionAmp) > 0.0005) {
      vv = v * mix(
        vec2(1.0 + uMotionAmp), // 垂直于应变轴（缩放因子：> 1 = 变小）
        vec2(1.0 / (1.0 + uMotionAmp)), // 沿应变轴
        uMotionAxis
      );
    }
    float bw = uHalf[i].x + uBulge[i];
    float bh = uHalf[i].y + uBulge[i];
    float br = uRadius[i] + uBulge[i];
    float sd = sdRoundRect(vv, vec2(bw, bh), br);
    float cov = 1.0 - smoothstep(-0.6 * uDpr, 0.6 * uDpr, sd);
    if (cov <= 0.0) continue;

    // 轮廓外一个像素都不动
    float depth = max(-sd, 0.0);
    float mn = max(min(bw, bh), 0.001);
    float bevel = mn * uBendBand; // 斜面宽度
    // 平灰档（uGray）把光学整套关掉：它只是一层均匀的灰——没有斜面、没有折射、
    // 没有边缘高光。下面各强度都乘 opt 归零，最后由 ⑥ 补上那层灰。
    float opt = 1.0 - uGray;

    // ① 折射：w 在轮廓上 = 1、往里衰减到 0。同一块玻璃上叠两种透镜：
    //    边缘那圈是凸的（放大），静止态就有，带色散；
    //    中心平面是凹的（把背面吸窄），强度由 uLens（按压进度）淡入。
    //    凸透镜的位移沿**轮廓法线**走，不是从中心发散——直边上处处同向，
    //    位移才会沿整条边铺开；按径向算的话只有两端看得见，边上永远只有一个突点。
    float w = clamp(1.0 - depth / bevel, 0.0, 1.0);
    w = w * w * (3.0 - 2.0 * w);
    float m = 1.0 + uBend * w * opt; // 边缘放大率
    float mc = 1.0 - uCompress * (1.0 - w) * uLens * opt; // 中心凹透镜的径向缩放
    mc = max(mc, 0.25); // 压缩再狠也不许除以 0
    float shift = length(vv) * (1.0 - 1.0 / m); // 放大率换算成该点的像素位移
    vec2 n = sdfNormal(vv, vec2(bw, bh), br); // 轮廓外法线

    // ② 色散：只发生在斜面内；斜面是等斜率切角，窗口取平顶，只在内沿收尾。
    //    错开方向也用同一条法线，彩边才贴着轮廓走。
    float o = uChroma * opt * smoothstep(0.0, 0.35, clamp(1.0 - depth / bevel, 0.0, 1.0));
    vec2 q = vv / mc - n * shift;
    // 夹取只对边缘那圈做（约束 2，防接缝彩环）。中心是凹透镜，采样点必须能跑出球外，
    // 夹回来的话"缩小"就没了；而中心 o = 0、tap 就等于 q，乘 w 正好把它放出去。
    vec2 tapR = q + n * (o * 0.5);
    vec2 tapB = q - n * (o * 0.5);
    vec2 hv = vec2(bw, bh);
    tapR -= n * max(sdRoundRect(tapR, hv, br), 0.0) * w;
    tapB -= n * max(sdRoundRect(tapB, hv, br), 0.0) * w;
    vec3 sc;
    sc.r = texture2D(uTex, (uCenter[i] + tapR) / uRes).r;
    sc.g = texture2D(uTex, (uCenter[i] + q) / uRes).g;
    sc.b = texture2D(uTex, (uCenter[i] + tapB) / uRes).b;

    // ③ 本体：透明档不是空的，是一层灰，浓度由"厚度"给——
    //    顶为 0、往中偏上增到最厚、到底再回到 0。两端厚度归零 = 背面原样透出，
    //    那两条最亮的地方就是顶/底高光；中间偏上最厚，所以那一带最深（浅 → 深 → 浅）。
    //    峰值位置 uBodyPeak 不写死在中点：玻璃的厚处偏上，才不像一条对称的横带。
    //    玉白档（uBody = 1）随后整块盖成纯白，两档之间由 CSS 过渡插值。
    float vt = clamp((vv.y + bh) / max(2.0 * bh, 0.001), 0.0, 1.0);
    float pk = clamp(uBodyPeak, 0.05, 0.95);
    float thick = mix(
      smoothstep(0.0, pk, vt),
      1.0 - smoothstep(pk, 1.0, vt),
      step(pk, vt)
    );
    // 左右：沿轮廓法线往里收——带宽就是斜面那一圈，等值线因此贴着左右的弧度走，
    // 半圆端头与圆角都跟着弧，不是被一条竖线切。朝向权重取 |n.x|：正左右最强、
    // 走到上下为 0，所以它只作用在左右两条边上，且只降一档（不归零）。
    float sideIn = clamp(1.0 - depth / max(bevel, 0.001), 0.0, 1.0);
    sideIn *= sideIn;
    thick *= 1.0 - uBodyFadeX * sideIn * abs(n.x);
    sc = mix(sc, uBodyColor, uBodyShade * thick * opt);

    // ④ 边缘：顶/底轻微高光，两侧暗边。窗口只贴轮廓一条（--glass-rim-width），
    //    方向权重用外法线分：直边上整条铺开，圆角处上下与左右自然过渡。
    float rim = 1.0 - smoothstep(0.0, max(uRimWidth, 0.5) * uDpr, depth);
    rim *= rim;
    float nUp = max(-n.y, 0.0); // 朝上 = 顶边
    float nDn = max(n.y, 0.0); // 朝下 = 底边
    float nSide = abs(n.x); // 朝左右 = 两侧
    sc = mix(sc, uRimLight, clamp(rim * nUp * uRimTop * opt, 0.0, 1.0));
    sc = mix(sc, uRimLight, clamp(rim * nDn * uRimBottom * opt, 0.0, 1.0));
    sc = mix(sc, uRimDark, clamp(rim * nSide * uRimSide * opt, 0.0, 1.0));

    // ⑤ 玉白档：整块纯白不透明，盖掉本体与边缘。
    sc = mix(sc, vec3(1.0), uBody);

    // ⑥ 平灰档：整块换成一层灰，**不吃背面纹理**——磨砂是"蒙一层"，不是"折一层"。
    //    透明感交给 uAlpha（半透明 ⇒ 底下的 DOM 字照样透出来），
    //    所以灰的明度可以按主题对称地给，不会被背面（白页面 / 深页面）带跑偏。
    sc = mix(sc, uGrayColor, uGray);

    col = mix(col, sc, cov);
    a = max(a, cov);
  }

  // uFill = 1：画布自己就是这一幕的背景（舞台），轮廓外继续铺背面。
  // 否则输出预乘 alpha，轮廓外 alpha = 0，DOM 原样透出来。
  // uAlpha：轮廓内的不透明度。平灰档是磨砂半透明，压到 0.5 上下，底下的 DOM（字、图标）
  // 才会真的透出来——否则画布整块盖住段，字就没了。
  float aa = a * uAlpha;
  gl_FragColor = uFill > 0.5 ? vec4(col, 1.0) : vec4(col * aa, aa);
}
`;

function compileShader(gl: WebGLRenderingContext, type: number, src: string) {
  const sh = gl.createShader(type);
  if (!sh) return null;
  gl.shaderSource(sh, src);
  gl.compileShader(sh);
  if (!gl.getShaderParameter(sh, gl.COMPILE_STATUS)) {
    console.error("[GlassLens] shader compile:", gl.getShaderInfoLog(sh));
    gl.deleteShader(sh);
    return null;
  }
  return sh;
}

/** 一块玻璃的几何（全部 CSS 像素，中心相对画布左上角） */
export type LensShape = {
  cx: number;
  cy: number;
  /** 半宽 / 半高 */
  hw: number;
  hh: number;
  /** 圆角半径（胶囊 = 半高） */
  radius: number;
  /** 常驻外鼓量（像素，四边等量）：自己算好进度时用这个 */
  bulge?: number;
  /** 按压外鼓系数 0..1：引擎乘 --glass-press-swell × 短边 × 本玻璃按压进度（按下就鼓） */
  swell?: number;
};

const EMPTY: LensShape = {
  cx: -1e4,
  cy: -1e4,
  hw: 0,
  hh: 0,
  radius: 0,
  bulge: 0,
};

/** 沿祖先链找第一个不透明背景色——玻璃背面里"页面"的颜色 */
export function pageColorOf(el: HTMLElement | null): string {
  let node = el;
  while (node) {
    const bg = getComputedStyle(node).backgroundColor;
    if (bg && bg !== "rgba(0, 0, 0, 0)" && bg !== "transparent") return bg;
    node = node.parentElement;
  }
  return "#ffffff";
}

/** 把"页面"铺满画布：透镜背面最底下的一层（调用方再往上画结构） */
export function paintPageBackdrop(
  ctx: CanvasRenderingContext2D,
  w: number,
  h: number,
  el: HTMLElement | null
) {
  ctx.fillStyle = pageColorOf(el);
  ctx.fillRect(0, 0, w, h);
}

/** 读数字令牌（"0.45" / "3px" 都取数值部分），令牌从 :root 读 */
function tokenNum(name: string, fallback: number) {
  const v = parseFloat(
    getComputedStyle(document.documentElement).getPropertyValue(name)
  );
  return Number.isFinite(v) ? v : fallback;
}

/**
 * 令牌 → sRGB 0..1。任意 CSS 颜色（oklch / rgb / 命名色）都吃：
 * 先在 1×1 画布上填一格再读回来，浏览器自己完成换算。结果按颜色串缓存。
 * 这样颜色仍然只写在 CSS 里（跟着亮暗主题走），shader 只管混。
 */
const colorCache = new Map<string, [number, number, number]>();
let colorProbe: CanvasRenderingContext2D | null = null;

function parseColor(css: string): [number, number, number] | null {
  const raw = css.trim();
  if (!raw || !CSS.supports("color", raw)) return null;
  const hit = colorCache.get(raw);
  if (hit) return hit;
  if (!colorProbe) {
    const c = document.createElement("canvas");
    c.width = 1;
    c.height = 1;
    colorProbe = c.getContext("2d", { willReadFrequently: true });
  }
  if (!colorProbe) return null;
  colorProbe.clearRect(0, 0, 1, 1);
  colorProbe.fillStyle = "#000";
  colorProbe.fillStyle = raw;
  colorProbe.fillRect(0, 0, 1, 1);
  const d = colorProbe.getImageData(0, 0, 1, 1).data;
  const rgb: [number, number, number] = [d[0] / 255, d[1] / 255, d[2] / 255];
  colorCache.set(raw, rgb);
  return rgb;
}

/**
 * 按压进度 0..1 的推进曲线：easeOutQuad（起步快、越走越慢），时长读 --glass-press-duration。
 *
 * 全库唯一的按压动画。GlassLens 的 pressed 走它；同时按住多块玻璃、需要各自一份进度的
 * 调用方（展示站的玻璃舞台）也走它，别再另写一条曲线。返回 cancel。
 */
export function animateGlassPress(
  from: number,
  to: number,
  onUpdate: (v: number) => void
) {
  const dur = Math.max(tokenNum("--glass-press-duration", 150), 1);
  const t0 = performance.now();
  let raf = 0;
  const step = () => {
    const t = Math.min(1, (performance.now() - t0) / dur);
    onUpdate(from + (to - from) * (1 - (1 - t) * (1 - t)));
    if (t < 1) raf = requestAnimationFrame(step);
  };
  raf = requestAnimationFrame(step);
  return () => cancelAnimationFrame(raf);
}

/** 按住状态 → 进度 0..1（GlassLens 内部用，拿到的是同一份动画） */
export function useGlassPress(active: boolean) {
  const [progress, setProgress] = useState(0);
  const cur = useRef(0);
  useEffect(() => {
    const to = active ? 1 : 0;
    if (cur.current === to) return;
    return animateGlassPress(cur.current, to, (v) => {
      cur.current = v;
      setProgress(v);
    });
  }, [active]);
  return progress;
}

/** 玻璃影子的档位：与 shadow-* 一一对应 */
export type GlassShadow = "none" | "xs" | "sm" | "md" | "lg" | "xl";

export function GlassLens({
  shapes,
  backdrop,
  className,
  live = false,
  fillOutside = false,
  motion = 0,
  body = "transparent",
  pressed = false,
  lens: lensProp,
  shadow = "sm",
  fallback,
}: {
  /** 最多 SHAPE_MAX 块，按数组顺序叠（后面的盖前面） */
  shapes: LensShape[];
  /** 画背面内容（页面 / 轨道 / 图像），透镜逐像素采样它。w/h 是设备像素 */
  backdrop: (
    ctx: CanvasRenderingContext2D,
    w: number,
    h: number,
    dpr: number
  ) => void;
  className?: string;
  /** true = rAF 常驻重绘，且每帧重画背面（球在动 / 背面在跟着变时用） */
  live?: boolean;
  /**
   * 轮廓外画什么。false（默认）= 一个像素都不画，DOM 原样透出来——玻璃贴在页面上时用这个；
   * true = 把背面铺满画布，画布自己就是这一幕的背景（透镜舞台那种整片绘制的场景）。
   */
  fillOutside?: boolean;
  /**
   * 运动惯性（水滴形变）的幅度档位 0..5：0 = 关闭（默认），5 = 最大。
   * 应变由沿运动方向的加速度驱动，按保体积的比例缩放：起步时沿运动方向被甩长，
   * 急停时沿运动方向被挤扁、垂直方向鼓高，随后由弹簧回落。
   * 只有真的会动的玻璃才给（拇指件 / 球），静止的卡牌保持 0。
   */
  motion?: number;
  /**
   * 本体档位：
   *   "transparent"（默认）= 透明档：竖向的灰层次 + 边缘明暗；
   *   "jade" = 玉白档：整块纯白不透明；
   *   "gray" = 平灰档：均匀的灰（磨砂感），折射 / 色散 / 边缘高光全部关掉，背面仍然透出。
   * 三档之间靠 CSS 过渡两条通道（--glass-jade / --glass-gray）插值，不要在这边做动画。
   */
  body?: "transparent" | "jade" | "gray";
  /**
   * 按住状态：引擎自己跑按压动画（animateGlassPress / --glass-press-duration），
   * 进度同时驱动中心凹透镜与 shapes[].swell。所有玻璃共用这一条，别在调用方再写一份。
   */
  pressed?: boolean;
  /**
   * 手动指定按压进度 0..1（0 = 中心不折，1 = 按 --glass-lens-compress 把背面吸窄），
   * 覆盖 pressed 的动画：一个 GlassLens 里有几块玻璃各自按压时（展示站的舞台）
   * 由调用方算进度，用 shapes[].bulge 给外鼓。
   */
  lens?: number;
  /**
   * 影子的档位，与 shadow-* 一一对应（xs/sm/md/lg/xl），"none" = 不投。
   * 引擎用 drop-shadow 画，所以影子跟着玻璃轮廓走；档位值见 shadow.css 的 --shadow-glass-*。
   */
  shadow?: GlassShadow;
  /** 无 WebGL 时的替代内容；不给则铺一层平色（不冒充玻璃） */
  fallback?: ReactNode;
}) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [noWebgl, setNoWebgl] = useState(false);
  /** 按压进度：pressed 时引擎自己跑那条共用动画；给了 lens 就用调用方那份 */
  const press = useGlassPress(pressed);
  const lens = lensProp ?? press;
  const glRef = useRef<{
    gl: WebGLRenderingContext;
    prog: WebGLProgram;
    tex: WebGLTexture;
    u: Record<string, WebGLUniformLocation | null>;
    w: number;
    h: number;
    dpr: number;
  } | null>(null);
  /** 离屏背面画布：纹理的来源；live 时每帧重画 */
  const texRef = useRef<{
    off: HTMLCanvasElement;
    ctx: CanvasRenderingContext2D | null;
  } | null>(null);
  /** 背面纹理脏了（换主题 / 换令牌）：下一次 draw 先重画再上传 */
  const dirty = useRef(true);
  const latest = useRef({ shapes, backdrop, live, fillOutside, lens, press, motion });
  latest.current.shapes = shapes;
  latest.current.backdrop = backdrop;
  latest.current.live = live;
  latest.current.fillOutside = fillOutside;
  latest.current.lens = lens;
  latest.current.press = press;
  latest.current.motion = Math.min(5, Math.max(0, motion));

  /**
   * 运动惯性状态：画布位置 → 速度 → 沿运动方向的加速度 → 阻尼弹簧。整块玻璃共用一份。
   * 只量画布自己——拇指件是靠 CSS 位移在动的，props 里的坐标是跳变的，不能拿来算速度。
   */
  const inertia = useRef({
    x: 0, // 上次量到的画布位置（文档坐标）
    y: 0,
    t: 0, // 上次时间（毫秒）
    ready: false, // 第一帧只记位置，不算速度
    vx: 0, // 平滑后的速度（px/s）
    vy: 0,
    pvx: 0, // 上一帧的平滑速度：加速度取它和 vx 的差分
    pvy: 0,
    axis: 0, // 应变轴 = 运动方向所在的轴：0 = 横 / 1 = 纵（锁屏幕轴，带滞回）
    dir: 1, // 沿该轴的运动方向 ±1（速度归零后锁存，急停要靠它才认得出方向）
    acc: 0, // 沿运动方向的加速度（px/s²，低通后）
    strain: 0, // 弹簧位移 = 沿轴的应变（正 = 沿轴拉长，负 = 沿轴压扁）
    vel: 0, // 弹簧速度
  });

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    // alpha：轮廓外要透明（玻璃贴在 DOM 上，画布只是玻璃那一层）
    const gl = canvas.getContext("webgl", {
      alpha: true,
      premultipliedAlpha: true,
      antialias: false,
    });
    if (!gl) {
      setNoWebgl(true);
      return;
    }
    const vs = compileShader(gl, gl.VERTEX_SHADER, VERT);
    const fs = compileShader(gl, gl.FRAGMENT_SHADER, FRAG);
    if (!vs || !fs) {
      setNoWebgl(true);
      return;
    }
    const prog = gl.createProgram();
    if (!prog) {
      setNoWebgl(true);
      return;
    }
    gl.attachShader(prog, vs);
    gl.attachShader(prog, fs);
    gl.linkProgram(prog);
    if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) {
      console.error("[GlassLens] link:", gl.getProgramInfoLog(prog));
      setNoWebgl(true);
      return;
    }
    const buf = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, buf);
    // 一个覆盖全屏的大三角形
    gl.bufferData(
      gl.ARRAY_BUFFER,
      new Float32Array([-1, -1, 3, -1, -1, 3]),
      gl.STATIC_DRAW
    );
    const aPos = gl.getAttribLocation(prog, "aPos");
    gl.enableVertexAttribArray(aPos);
    gl.vertexAttribPointer(aPos, 2, gl.FLOAT, false, 0, 0);
    const tex = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, tex);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    const u: Record<string, WebGLUniformLocation | null> = {};
    for (const n of [
      "uTex", "uRes", "uCenter", "uHalf", "uRadius", "uBulge",
      "uBendBand", "uBend", "uCompress", "uLens", "uChroma",
      "uBody", "uBodyColor", "uBodyShade", "uBodyPeak", "uBodyFadeX",
      "uGray", "uGrayColor",
      "uRimLight", "uRimDark", "uRimTop", "uRimBottom", "uRimSide", "uRimWidth",
      "uMotionAxis", "uMotionAmp",
      "uDpr", "uFill", "uAlpha",
    ]) {
      u[n] = gl.getUniformLocation(prog, n);
    }
    glRef.current = { gl, prog, tex, u, w: 0, h: 0, dpr: 1 };

    /** 背面 → 离屏 2D 画布 → 纹理 */
    function paintTexture() {
      const g = glRef.current;
      const t = texRef.current;
      if (!g || !t || !t.ctx) return;
      latest.current.backdrop(t.ctx, t.off.width, t.off.height, g.dpr);
      g.gl.bindTexture(g.gl.TEXTURE_2D, g.tex);
      g.gl.texImage2D(
        g.gl.TEXTURE_2D, 0, g.gl.RGBA, g.gl.RGBA, g.gl.UNSIGNED_BYTE, t.off
      );
    }

    const resize = () => {
      const g = glRef.current;
      if (!g) return;
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      const w = Math.max(1, Math.round(canvas.clientWidth * dpr));
      const h = Math.max(1, Math.round(canvas.clientHeight * dpr));
      if (w === g.w && h === g.h && dpr === g.dpr) return;
      canvas.width = w;
      canvas.height = h;
      g.w = w;
      g.h = h;
      g.dpr = dpr;
      const off = document.createElement("canvas");
      off.width = w;
      off.height = h;
      texRef.current = { off, ctx: off.getContext("2d") };
      paintTexture();
      draw();
    };
    const ro = new ResizeObserver(resize);
    ro.observe(canvas);
    resize();
    return () => ro.disconnect();
  }, []);

  // 参数变化 → 重绘（拖动时每个指针事件一帧）
  useEffect(() => {
    draw();
  });

  // live：常驻 rAF，每帧重画背面再重绘（球在动、背面在跟着变）
  useEffect(() => {
    if (!live) return;
    let raf = requestAnimationFrame(function loop() {
      draw();
      raf = requestAnimationFrame(loop);
    });
    return () => cancelAnimationFrame(raf);
  }, [live]);

  // 主题切换（<html> 的 class 变）→ 背面颜色可能变，纹理先标脏再重绘
  useEffect(() => {
    const mo = new MutationObserver(() => {
      dirty.current = true;
      draw();
    });
    mo.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ["class"],
    });
    return () => mo.disconnect();
  }, []);

  function draw() {
    const g = glRef.current;
    if (!g || g.w === 0) return;
    const { gl, prog, tex, u, w, h, dpr } = g;
    // 令牌在 canvas 元素上读：调用方在哪一层改令牌，引擎都跟着变
    const cs = getComputedStyle(canvasRef.current ?? document.documentElement);
    /** 读数字令牌（"0.45" / "3px" 都取到数值部分） */
    const num = (name: string, fb: number) => {
      const v = parseFloat(cs.getPropertyValue(name));
      return Number.isFinite(v) ? v : fb;
    };
    /** 读颜色令牌：任意 CSS 颜色 → sRGB 0..1，读不到就用兜底色 */
    const color = (name: string, fb: string) =>
      parseColor(cs.getPropertyValue(name)) ?? parseColor(fb) ?? [0, 0, 0];

    // ── 运动惯性：位移 → 速度 → 加速度 → 阻尼弹簧 ────────────────────
    // 形变来自惯性力（∝ 加速度），不是速度：匀速平移不形变，只有起步（沿运动方向
    // 被甩长）和急停（沿运动方向被挤扁、垂直方向鼓高，体积守恒）才有形状变化。
    // mo = 0 时整段跳过：静止的卡牌连一次 layout 读都不做。
    const mo = latest.current.motion;
    const im = inertia.current;
    if (mo > 0 && canvasRef.current) {
      const r = canvasRef.current.getBoundingClientRect();
      const px = r.left + window.scrollX; // 文档坐标：页面滚动不算玻璃在动
      const py = r.top + window.scrollY;
      const now = performance.now();
      const dt = im.ready ? Math.min((now - im.t) / 1000, 0.02) : 0;
      if (im.ready && dt > 0) {
        // 速度低通（时间常数 45ms）：跟手，但不放大手抖
        const kv = 1 - Math.exp(-dt / 0.045);
        im.vx += ((px - im.x) / dt - im.vx) * kv;
        im.vy += ((py - im.y) / dt - im.vy) * kv;
        const sp = Math.hypot(im.vx, im.vy);
        // 应变轴只可能横 / 纵：按主分量选，带 25% 滞回（斜着拖不会左右横跳）。
        // 绝不把轴交给速度方向——斜轴缩放会把胶囊的直边拧斜，看着像整块转了。
        // 速度低于 40px/s 就不判轴和方向：锁存刚才那一份——急停正需要"沿着原来的
        // 运动方向"被挤扁，那时速度已经趋零，自己指不出方向了。
        const ax = Math.abs(im.vx);
        const ay = Math.abs(im.vy);
        if (sp > 40) {
          if (im.axis === 0 && ay > ax * 1.25) im.axis = 1;
          else if (im.axis === 1 && ax > ay * 1.25) im.axis = 0;
          const along = im.axis === 0 ? im.vx : im.vy;
          if (Math.abs(along) > 40) im.dir = along > 0 ? 1 : -1;
        }
        // 沿运动方向的加速度 = 该轴速度的导数 × 运动方向。加速为正（拉长），
        // 急停为负（挤扁）——冲量 ∝ 停下之前的速度，拖得越快挤得越狠。
        const aRaw =
          ((im.axis === 0 ? im.vx - im.pvx : im.vy - im.pvy) / dt) * im.dir;
        // 再低通 25ms：急停那一下的尖峰留着，帧率抖动不放大
        im.acc += (aRaw - im.acc) * (1 - Math.exp(-dt / 0.025));
        im.pvx = im.vx;
        im.pvy = im.vy;
        // 目标应变 ∝ 加速度：3500px/s² 吃满一档，每档 3.5%（5 档 = 17.5%）
        const target = Math.max(-1, Math.min(1, im.acc / 3500)) * 0.035 * mo;
        // 刚度 700、阻尼比 ≈ 0.5：冲一下、回一次，300ms 内定住
        im.vel += (-700 * (im.strain - target) - 27 * im.vel) * dt;
        // 夹取只是兜底，不参与档位：挤扁侧 0.35（1/(1−0.35) = 1.54 倍高，再深会顶出
        // 画布外扩的余量），5 档满额 0.175 加过冲也远在其内 ⇒ 1..5 档一一对应、不被削平
        im.strain = Math.max(-0.35, Math.min(0.45, im.strain + im.vel * dt));
      }
      im.x = px;
      im.y = py;
      im.t = now;
      im.ready = true;
    } else if (im.ready) {
      // 档位被关掉：下一轮重新起算，别拿很久以前的位置和速度算加速度
      im.ready = false;
      im.pvx = 0;
      im.pvy = 0;
      im.acc = 0;
      im.strain = 0;
      im.vel = 0;
    }

    const list = latest.current.shapes;
    /** 按压外鼓量：系数 × --glass-press-swell × 短边，按本玻璃的进度走 */
    const pressSwell = num("--glass-press-swell", 0.3) * latest.current.press;
    const cen = new Float32Array(SHAPE_MAX * 2);
    const hal = new Float32Array(SHAPE_MAX * 2);
    const rad = new Float32Array(SHAPE_MAX);
    const bul = new Float32Array(SHAPE_MAX);
    for (let i = 0; i < SHAPE_MAX; i++) {
      const s = list[i] ?? EMPTY;
      cen[i * 2] = s.cx * dpr;
      cen[i * 2 + 1] = s.cy * dpr;
      hal[i * 2] = s.hw * dpr;
      hal[i * 2 + 1] = s.hh * dpr;
      rad[i] = s.radius * dpr;
      bul[i] =
        ((s.bulge ?? 0) +
          (s.swell ?? 0) * Math.min(s.hw, s.hh) * 2 * pressSwell) *
        dpr;
    }
    // 背面重画：live（跟着球/状态变，每帧一次）或脏了（换主题 / 换令牌）
    if ((latest.current.live || dirty.current) && texRef.current?.ctx) {
      latest.current.backdrop(texRef.current.ctx, w, h, dpr);
      gl.bindTexture(gl.TEXTURE_2D, tex);
      gl.texImage2D(
        gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, texRef.current.off
      );
      dirty.current = false;
    }
    gl.viewport(0, 0, w, h);
    gl.useProgram(prog);
    gl.activeTexture(gl.TEXTURE0);
    gl.bindTexture(gl.TEXTURE_2D, tex);
    gl.uniform1i(u.uTex, 0);
    gl.uniform2f(u.uRes, w, h);
    gl.uniform2fv(u.uCenter, cen);
    gl.uniform2fv(u.uHalf, hal);
    gl.uniform1fv(u.uRadius, rad);
    gl.uniform1fv(u.uBulge, bul);
    gl.uniform1f(u.uBendBand, num("--glass-lens-band", 0.45));
    gl.uniform1f(u.uBend, num("--glass-lens-bend", 0.24));
    gl.uniform1f(u.uCompress, num("--glass-lens-compress", 0.35));
    gl.uniform1f(u.uLens, Math.min(1, Math.max(0, latest.current.lens)));
    gl.uniform1f(u.uChroma, num("--glass-lens-chroma", 3) * dpr); // px → 设备像素
    // 两条独立通道（不是一条 0..2 的标量）：灰 → 透明只让 gray 那条 1→0，
    // jade 那条始终 0，所以过渡路径上不会经过"纯白"。
    const jade = Math.min(1, Math.max(0, num("--glass-jade", 0)));
    const gray = Math.min(1, Math.max(0, num("--glass-gray", 0)));
    gl.uniform1f(u.uBody, jade);
    gl.uniform1f(u.uGray, gray);
    gl.uniform3fv(u.uGrayColor, color("--glass-gray-color", "oklch(0.82 0 0)"));
    // 平灰档的不透明度：整块玻璃（含它盖住的 DOM）按这个比例与背面混合。
    // 压到 0.5 上下 ⇒ 底下的字透得出来，磨砂玻璃才是"透明"的而不是一块板。
    const grayAlpha = num("--glass-gray-alpha", 0.5);
    gl.uniform1f(u.uAlpha, 1 - (1 - grayAlpha) * gray);
    gl.uniform3fv(u.uBodyColor, color("--glass-body-color", "oklch(0.55 0 0)"));
    gl.uniform1f(u.uBodyShade, num("--glass-body-shade", 0.3));
    gl.uniform1f(u.uBodyPeak, num("--glass-body-peak", 0.22));
    gl.uniform1f(u.uBodyFadeX, num("--glass-body-fade-x", 0.35));
    gl.uniform3fv(u.uRimLight, color("--glass-rim-light", "oklch(1 0 0)"));
    gl.uniform3fv(u.uRimDark, color("--glass-rim-dark", "oklch(0.45 0 0)"));
    gl.uniform1f(u.uRimTop, num("--glass-rim-top", 0.45));
    gl.uniform1f(u.uRimBottom, num("--glass-rim-bottom", 0.22));
    gl.uniform1f(u.uRimSide, num("--glass-rim-side", 0.4));
    gl.uniform1f(u.uRimWidth, num("--glass-rim-width", 2));
    gl.uniform2f(u.uMotionAxis, im.axis === 0 ? 1 : 0, im.axis === 0 ? 0 : 1);
    gl.uniform1f(u.uMotionAmp, mo > 0 ? im.strain : 0);
    gl.uniform1f(u.uDpr, dpr);
    gl.uniform1f(u.uFill, latest.current.fillOutside ? 1 : 0);
    gl.drawArrays(gl.TRIANGLES, 0, 3);
  }

  const shadowFilter =
    shadow === "none" ? undefined : `drop-shadow(var(--shadow-glass-${shadow}))`;
  /** 本体档位 → CSS 类：类名只负责把对应那条通道置 1，过渡由使用方声明 */
  const bodyClass =
    body === "jade" ? "glass-jade" : body === "gray" ? "glass-gray" : undefined;

  // 没有 WebGL：只留一层极淡的前景色把形状标出来，不冒充折射
  if (noWebgl) {
    return (
      <div className={cn("relative", bodyClass, className)}>
        {fallback ??
          shapes.map((s, i) => (
            <div
              key={i}
              className={cn(
                "absolute",
                body === "jade"
                  ? "bg-white"
                  : body === "gray"
                    ? "bg-muted"
                    : "bg-foreground/6"
              )}
              style={{
                left: s.cx - s.hw,
                top: s.cy - s.hh,
                width: s.hw * 2,
                height: s.hh * 2,
                borderRadius: s.radius,
                filter: shadowFilter,
              }}
            />
          ))}
      </div>
    );
  }

  return (
    <div className={cn("relative", bodyClass, className)}>
      {/* 影子交给 CSS 的 drop-shadow：它跟着画布的 alpha 走，
          所以玻璃是什么形状（圆角 / 胶囊 / 圆 / 几块叠着）影子就是什么形状 */}
      <canvas
        ref={canvasRef}
        className="absolute inset-0 h-full w-full"
        style={{ filter: shadowFilter }}
      />
    </div>
  );
}
