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
 * 运动惯性（motion 0..5）：整块玻璃位移时按应变伸缩——放大轴 ×s、另一轴 ×(1/s)。
 * 画布余量充足时 s = 1+ε（行列式为 1，保体积）；余量不足时 s 按该块玻璃的真实
 * 空间封顶、收缩侧仍按原始幅度，形变由「拉长」转为「变窄」，绝不越出画布。
 * 应变来自两个时间常数不同的跟随点之差（带通位移），
 * 不是速度差分：dt 只进指数平滑、不进除数，位置量化 / 手速微抖 / 帧率波动都不会被放大。
 * 应变量只取 |lag|（运动强度），**与运动方向无关**：左右 / 上下拖动都沿应变轴
 * 拉长、另一轴收窄。target 已两级平滑，strain 只做一阶跟随：不再串欠阻尼弹簧
 * （会过冲，把外形顶出画布的固定 margin、还拖出慢动作）。
 * 主轴只可能是横或纵（斜轴缩放会把直边拧斜），shader 只做这个仿射，
 * 所以形状 / 法线 / 折射 / 本体自然一起变形。
 *
 * 两条不能改坏的约束（改了等于没有色散）：
 *   1) uChroma 是像素量，不是倍率。倍率下位移 = v·2o/m²，小件上不到 1 设备像素，
 *      被 LINEAR 采样平均掉。
 *   2) R/B 只夹「相对 q 额外」的越界。q 自己越出轮廓是凹透镜缩小所需
 *      （全域压缩时斜面圈同样会越界），整体夹回会让 R/B 停原位、只有 G 缩小；
 *      但色散多出来的那点越界不夹，仍会沿轮廓折出一圈均匀彩环。
 *
 * 轮廓外一个像素都不画（预乘 alpha = 0）：画布只是玻璃那一层，DOM 原样透出来；
 * 只有 fillOutside 的舞台自己当背景时才铺满背面。没有 WebGL 时给一层平色
 * （fallback），不冒充玻璃。参数从 --glass-lens-* 读取，在
 * canvas 元素上读——调用方在哪一层改令牌，引擎都跟着变。
 *
 * 档位：只有 transparent 是玻璃（opt = 1）；jade / gray 是磨砂，整套光学关掉。
 * 斜面带宽沿轮廓弧度分布（--glass-lens-span 给保持满值的固定段，之后收到顶底 0），
 * 凹透镜（全域）由 --glass-lens-compress 常驻控制，透明档静止也能把背景导轨吸窄。
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
uniform sampler2D uInk; // 内容层（文字 / 图标，透明底）：只折射，不参与凹透镜缩小
uniform float uHasInk; // 0 = 调用方没给 ink，别采这张纹理
uniform vec2 uRes;

// ── 每块玻璃（设备像素）──
uniform vec2 uCenter[N]; // 中心
uniform vec2 uHalf[N]; // 半宽 / 半高
uniform float uRadius[N]; // 圆角半径
uniform float uBulge[N]; // 按压外鼓量

// ── 光学 + 外观（令牌 → uniform）──
uniform float uBendBand; // --glass-lens-band    斜面宽度系数
uniform float uBend; // --glass-lens-bend      静止态放大率 - 1
uniform float uCompress; // --glass-lens-compress 中心凹透镜压缩率（常驻）：m = 1 - 本值
// 凹透镜常驻：强度只有 uCompress 一个来源，按压进度不参与光学（只管外鼓 / 换档）
uniform float uChroma; // --glass-lens-chroma   色散宽度（像素，非倍率）
uniform float uSpan; // --glass-lens-span    斜面固定带宽段的半长 / 半高
uniform float uDecay; // --glass-lens-decay   斜面归零位置：0.5 = 顶/底，越大越早归零
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
uniform float uMotionAmp; // 沿应变轴的应变 ε ≥ 0：放大轴恒为应变轴、另一轴恒收窄，无符号分支
uniform float uDpr; // 设备像素比：抗锯齿宽度按 CSS 像素换算
uniform float uFill; // 轮廓外画什么：0 = 透明（玻璃贴在 DOM 上）/ 1 = 铺背面（画布本身就是背景）
uniform float uAlpha; // 轮廓内不透明度：1 = 全盖住 / <1 = 磨砂半透明，让背面 DOM 透出来
uniform float uClear; // --glass-clear 1 = 轮廓内 alpha 只由「玻璃改变量」给出（全透明本体）

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
  vec3 back = texture2D(uTex, uv).rgb;
  vec3 col = back;

  // 覆盖度：轮廓内 1、轮廓外 0。抗锯齿带宽按 CSS 像素给（× dpr 换设备像素）——
  // 写死 1 设备像素会让 retina 上只剩 0.5 CSS px，小球边缘锯齿明显。
  // 玻璃贴在 DOM 上时画布比玻璃大（球要外鼓），轮廓外必须留空：
  // 否则画布把自己那张不透明的方片盖在页面上，玻璃就成了一个方块。
  float a = 0.0;

  for (int i = 0; i < N; i++) {
    vec2 v = p - uCenter[i];
    // 运动惯性：应变锁在**屏幕轴**上——应变轴只可能是横 (1,0) 或纵 (0,1)。
    // ε 恒 ≥ 0（CPU 侧取 |lag|）：放大轴**恒等于应变轴**、另一轴恒收窄，
    // 所以左右 / 上下拖动都变细，不会一边变粗一边变细。
    // 余量充足时 s·(1/s) = 1 保体积；余量不足时放大侧封顶、只保留收缩
    // （见下面 room 的计算），形状绝不越出画布。
    // 为什么不用速度方向当轴：矩形沿**斜轴**做各向异性缩放时，直边会被一起拧斜
    // （只有沿轴 / 垂直轴的直线方向不变），看起来像整块转了几度。轴由 JS 按主分量选，
    // 带滞回，斜拖时不会来回跳。
    float bw = uHalf[i].x + uBulge[i];
    float bh = uHalf[i].y + uBulge[i];
    vec2 vv = v;
    if (uMotionAmp > 0.0005) {
      // 保体积应变 s·t = 1：应变轴 ×s、另一轴 ×(1/s)。s 必须由**这块玻璃在
      // 画布里的真实余量**封顶：画布外扩只按按压外鼓预算，uBulge 已经吃掉大半，
      // 惯性再放大就会把轮廓顶到画布边界、被硬切成直线。放大侧按 s 封顶，
      // 收缩侧仍按**原始 sWant** 走：余量不足时形变不会整个消失，只是由
      // 「拉长」变成「变窄」——不越界，流体感也还在。
      vec2 hv = vec2(bw, bh);
      vec2 growAxis = uMotionAxis; // ε ≥ 0，放大轴恒为应变轴
      float sWant = 1.0 + uMotionAmp;
      vec2 room = max(min(uCenter[i], uRes - uCenter[i]) - hv, vec2(0.0));
      float halfGrow = dot(growAxis, hv);
      float roomGrow = dot(growAxis, room);
      float s = min(sWant, 1.0 + roomGrow / max(halfGrow, 0.001));
      vv = v * mix(vec2(sWant), vec2(1.0 / s), growAxis);
    }
    float br = uRadius[i] + uBulge[i];
    float sd = sdRoundRect(vv, vec2(bw, bh), br);
    float cov = 1.0 - smoothstep(-0.6 * uDpr, 0.6 * uDpr, sd);
    if (cov <= 0.0) continue;

    // 轮廓外一个像素都不动
    float depth = max(-sd, 0.0);
    float mn = max(min(bw, bh), 0.001);
    float bevel = mn * uBendBand; // 斜面宽度
    // 只有透明档是玻璃；玉白 / 平灰是磨砂，整套光学都不参与。
    // 下面各强度都乘 opt 归零，最后由 ⑤⑥ 补上整块白 / 灰。
    float opt = 1.0 - max(uBody, uGray);
    vec2 n = sdfNormal(vv, vec2(bw, bh), br); // 轮廓外法线

    // ① 折射：w 在轮廓上 = 1、往里衰减到 0。斜面带宽沿**轮廓弧度**分布：
    //    固定段（在 y 轴上的投影长度 = uSpan × 玻璃全高，用来对齐导轨高度）保持
    //    lens-band 满值，之后按轮廓法线角度平滑收到 0 —— 顶 / 底不折射。
    //    用角度而不是 y 做参数，内边界才是等距弧线，不会被一条直线切出方块。
    //    单参数 atan：GLSL 与 AGSL 都可靠，两参数 atan(y,x) 在 SkSL 上不保险。
    float th = atan(abs(n.y) / max(abs(n.x), 0.0001));
    float th0 = asin(clamp(uSpan, 0.0, 1.0));
    // 收窄进度 t：0 = 固定段末尾（仍是满带宽），1 = 顶 / 底那个点
    float t = clamp((th - th0) / max(1.5707963 - th0, 0.0001), 0.0, 1.0);
    // 归零位置由 uDecay 控制，不固定在 t = 1：
    //   k = 0.5 → tEnd = 1，正好在顶 / 底归零；
    //   k > 0.5 → 更早归零（离顶底更远）；k < 0.5 → 顶底仍留一点斜面。
    // tEnd = (1-k)/k 让 k = 0.5 精确复现原来的 smoothstep 收窄曲线。
    float k = clamp(uDecay, 0.05, 0.95);
    float tEnd = (1.0 - k) / k;
    float s = clamp(t / tEnd, 0.0, 1.0);
    float taper = 1.0 - s * s * (3.0 - 2.0 * s);
    float wRaw = clamp(1.0 - depth / bevel, 0.0, 1.0);
    float w = wRaw * wRaw * (3.0 - 2.0 * wRaw) * taper;
    float m = 1.0 + uBend * w * opt; // 边缘放大率
    // 中心凹透镜：强度只有 uCompress 一个来源，常驻（透明档静止也压缩）。
    // 范围必须是**整个玻璃内部**：若按 (1-wRaw) 只给中心，斜面圈 mc = 1，
    // 凸透镜就在那一圈独占、把轨道撑得比玻璃外还宽，凹透镜便只是在
    // "比放大后的窄一点"。全域压缩后 q = vv/mc - n*shift 的净采样半径始终
    // 大于原半径，玻璃下的轨道整体比玻璃外窄；凸透镜只负责斜面折射 / 彩边。
    float mc = 1.0 - uCompress * opt;
    mc = max(mc, 0.25); // 压缩再狠也不许除以 0
    float shift = length(vv) * (1.0 - 1.0 / m); // 放大率换算成该点的像素位移

    // ② 色散：只发生在斜面内；斜面是等斜率切角，窗口取平顶，只在内沿收尾。
    //    错开方向也用同一条法线，彩边才贴着轮廓走；上下端 taper = 0 时一并消失。
    float o = uChroma * opt * taper * smoothstep(0.0, 0.35, wRaw);
    vec2 q = vv / mc - n * shift;
    // 只夹「色散相对 q 额外造成的越界」，保留 q 本身跑出轮廓的部分。
    // 凹透镜是全域压缩，斜面圈的 q 同样会越界；若照旧把 tapR/tapB 整体夹回轮廓，
    // R/B 就停在原位置、只有 G 缩小 —— 轨道只剩一圈彩边，宽度看不出变化。
    // overQ 是 q 自己越界的量（缩小所需，放行），色散多出来的那点才夹回去防彩环。
    vec2 tapR = q + n * (o * 0.5);
    vec2 tapB = q - n * (o * 0.5);
    vec2 hv = vec2(bw, bh);
    float overQ = max(sdRoundRect(q, hv, br), 0.0);
    tapR -= n * max(max(sdRoundRect(tapR, hv, br), 0.0) - overQ, 0.0) * w;
    tapB -= n * max(max(sdRoundRect(tapB, hv, br), 0.0) - overQ, 0.0) * w;
    vec3 sc;
    sc.r = texture2D(uTex, (uCenter[i] + tapR) / uRes).r;
    sc.g = texture2D(uTex, (uCenter[i] + q) / uRes).g;
    sc.b = texture2D(uTex, (uCenter[i] + tapB) / uRes).b;

    // 内容层只走凸透镜位移，不除 mc：文字 / 图标被折射但尺寸不变。
    // 背景轨道仍由 q = vv/mc - n*shift 压缩采样，两层互不干扰。
    // 色散必须两层都做：文字是最高对比的边缘，彩边主要靠它显形；
    // 只给背景层分通道的话，字搬到 uInk 后色散就整块看不见了。
    if (uHasInk > 0.5) {
      vec2 qInk = vv - n * shift;
      vec2 inkR = qInk + n * (o * 0.5);
      vec2 inkB = qInk - n * (o * 0.5);
      float overInk = max(sdRoundRect(qInk, hv, br), 0.0);
      inkR -= n * max(max(sdRoundRect(inkR, hv, br), 0.0) - overInk, 0.0) * w;
      inkB -= n * max(max(sdRoundRect(inkB, hv, br), 0.0) - overInk, 0.0) * w;
      // 三个通道各自完整合成：R/B 必须用**自己错位位置**的 ink 覆盖度。
      // 三通道共用中心 alpha 的话，错位造成的覆盖差异会被统一裁掉，色散就没了；
      // 而且 uInk 透明底 RGB=0、青绿字 R≈0，只换 RGB 不换 alpha 也看不出差别。
      vec4 inkR4 = texture2D(uInk, (uCenter[i] + inkR) / uRes);
      vec4 inkG4 = texture2D(uInk, (uCenter[i] + qInk) / uRes);
      vec4 inkB4 = texture2D(uInk, (uCenter[i] + inkB) / uRes);
      sc.r = mix(sc.r, inkR4.r, inkR4.a);
      sc.g = mix(sc.g, inkG4.g, inkG4.a);
      sc.b = mix(sc.b, inkB4.b, inkB4.a);
    }

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
    // uClear：不画任何线、也不手调颜色。这一圈玻璃有多实，由凸透镜真实算出的
    // 采样位移 shift（放大率 m 换算的像素偏移）决定；sc 仍是页面底色，不会发灰。
    // 色散继续走上面原生的 sc.r/g/b 三通道错位采样，没有任何模拟。
    float ca = cov;
    if (uClear > 0.5) {
      ca = cov * clamp(length(sc - back) * 3.0 + shift / max(bevel, 0.001) * 0.6, 0.0, 1.0);
    }
    a = max(a, ca);
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
  ink,
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
  /**
   * 可选的内容层（文字 / 图标，透明底），单独上传成第二张纹理。
   * backdrop 用 q = vv/mc - n*shift 采样（轨道被压窄）；
   * ink 用 q = vv - n*shift 采样（只被凸透镜折射，不缩小）。
   */
  ink?: (
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
   * 应变由两级时间常数跟随的位移差驱动，按保体积的比例缩放：起步时沿运动方向被甩长，
   * 急停时沿运动方向被挤扁、垂直方向鼓高，随后一阶跟随回 0（无过冲、无回弹）。
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
   * 进度只驱动 shapes[].swell 与换档，不参与光学。所有玻璃共用这一条，
   * 别在调用方再写一份；凹透镜由 --glass-lens-compress 常驻控制。
   */
  pressed?: boolean;
  /**
   * 手动指定按压进度 0..1，覆盖 pressed 的动画：一个 GlassLens 里有几块玻璃
   * 各自按压时（展示站的舞台）由调用方算进度，用 shapes[].bulge 给外鼓。
   * 只驱动外鼓与换档，不再参与光学（凹透镜由 --glass-lens-compress 常驻控制）。
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
    inkTex: WebGLTexture;
    u: Record<string, WebGLUniformLocation | null>;
    w: number;
    h: number;
    dpr: number;
  } | null>(null);
  /** 离屏画布：背景层 + 内容层两张纹理的来源；live 时每帧重画 */
  const texRef = useRef<{
    off: HTMLCanvasElement;
    ctx: CanvasRenderingContext2D | null;
    inkOff: HTMLCanvasElement;
    inkCtx: CanvasRenderingContext2D | null;
  } | null>(null);
  /** 背面纹理脏了（换主题 / 换令牌）：下一次 draw 先重画再上传 */
  const dirty = useRef(true);
  const latest = useRef({ shapes, backdrop, ink, live, fillOutside, lens, motion });
  latest.current.shapes = shapes;
  latest.current.backdrop = backdrop;
  latest.current.ink = ink;
  latest.current.live = live;
  latest.current.fillOutside = fillOutside;
  latest.current.lens = lens;
  latest.current.motion = Math.min(5, Math.max(0, motion));

  /**
   * 运动惯性状态：两个时间常数不同的跟随点做带通，取差值当"惯性位移"。
   * 不做数值微分——dt 只出现在指数平滑里，位置量化 / 手速波动 / 帧率抖动都不会被放大。
   * 只量画布自己：拇指件靠 CSS 位移动，props 里的坐标是跳变的。
   */
  const inertia = useRef({
    fx: 0, // 快跟随点（τ = 20ms）
    fy: 0,
    sx: 0, // 慢跟随点（τ = 55ms）
    sy: 0,
    t: 0, // 上次时间（毫秒）
    ready: false, // 首帧把两个跟随点放到当前位置
    axis: 0, // 应变轴：0 横 / 1 纵
    strain: 0, // 一阶跟随 target 的应变 ε ≥ 0（运动强度，方向无关），无过冲
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
    const inkTex = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, inkTex);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    const u: Record<string, WebGLUniformLocation | null> = {};
    for (const n of [
      "uTex", "uInk", "uHasInk", "uRes", "uCenter", "uHalf", "uRadius", "uBulge",
      "uBendBand", "uBend", "uCompress", "uChroma", "uSpan", "uDecay",
      "uBody", "uBodyColor", "uBodyShade", "uBodyPeak", "uBodyFadeX",
      "uGray", "uGrayColor",
      "uRimLight", "uRimDark", "uRimTop", "uRimBottom", "uRimSide", "uRimWidth",
      "uMotionAxis", "uMotionAmp",
      "uDpr", "uFill", "uAlpha", "uClear",
    ]) {
      u[n] = gl.getUniformLocation(prog, n);
    }
    glRef.current = { gl, prog, tex, inkTex, u, w: 0, h: 0, dpr: 1 };

    /** 背景层 + 内容层 → 两张离屏 2D 画布 → 两张纹理 */
    function paintTexture() {
      const g = glRef.current;
      const t = texRef.current;
      if (!g || !t || !t.ctx) return;
      latest.current.backdrop(t.ctx, t.off.width, t.off.height, g.dpr);
      g.gl.activeTexture(g.gl.TEXTURE0);
      g.gl.bindTexture(g.gl.TEXTURE_2D, g.tex);
      g.gl.texImage2D(
        g.gl.TEXTURE_2D, 0, g.gl.RGBA, g.gl.RGBA, g.gl.UNSIGNED_BYTE, t.off
      );
      if (t.inkCtx) {
        t.inkCtx.clearRect(0, 0, t.inkOff.width, t.inkOff.height);
        latest.current.ink?.(t.inkCtx, t.inkOff.width, t.inkOff.height, g.dpr);
        g.gl.activeTexture(g.gl.TEXTURE1);
        g.gl.bindTexture(g.gl.TEXTURE_2D, g.inkTex);
        g.gl.texImage2D(
          g.gl.TEXTURE_2D, 0, g.gl.RGBA, g.gl.RGBA, g.gl.UNSIGNED_BYTE, t.inkOff
        );
      }
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
      const inkOff = document.createElement("canvas");
      inkOff.width = w;
      inkOff.height = h;
      texRef.current = {
        off, ctx: off.getContext("2d"),
        inkOff, inkCtx: inkOff.getContext("2d"),
      };
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
    const { gl, prog, tex, inkTex, u, w, h, dpr } = g;
    // 令牌在 canvas 元素上读：调用方在哪一层改令牌，引擎都跟着变
    const cs = getComputedStyle(canvasRef.current ?? document.documentElement);
    /** 读数字令牌（"0.45" / "3px" 都取到数值部分）；不给兜底就是 0，令牌才是唯一来源 */
    const num = (name: string, fb = 0) => {
      const v = parseFloat(cs.getPropertyValue(name));
      return Number.isFinite(v) ? v : fb;
    };
    /** 读颜色令牌：任意 CSS 颜色 → sRGB 0..1，读不到就用兜底色 */
    const color = (name: string, fb: string) =>
      parseColor(cs.getPropertyValue(name)) ?? parseColor(fb) ?? [0, 0, 0];

    // ── 运动惯性：双时间常数跟随 → 带通位移 → 一阶跟随 ────────────────────
    // 不做速度差分：a = (v - pv)/dt 会把位置量化、手速微抖、帧率波动放大 1/dt²，
    // 人手永远无法匀速，弹簧于是被持续激励 → 抖动。
    // 这里 dt 只进指数平滑的指数，不在除数里：
    //   fast 紧跟（20ms）、slow 慢跟（55ms），lag = fast - slow
    //   起步 lag > 0（拉长）｜匀速 lag 恒定（稳定形变，不抖）｜急停 lag 反号（挤扁）
    // mo = 0 时整段跳过：静止的卡牌连一次 layout 读都不做。
    const mo = latest.current.motion;
    const im = inertia.current;
    if (mo > 0 && canvasRef.current) {
      const r = canvasRef.current.getBoundingClientRect();
      const px = r.left + window.scrollX; // 文档坐标：页面滚动不算玻璃在动
      const py = r.top + window.scrollY;
      const now = performance.now();
      if (!im.ready) {
        // 首帧把两个跟随点都放到当前位置，避免从原点"飞"过来产生一次假冲量
        im.fx = im.sx = px;
        im.fy = im.sy = py;
        im.t = now;
        im.ready = true;
        im.strain = 0;
        im.axis = 0;
      } else {
        const dt = Math.min(Math.max((now - im.t) / 1000, 0.001), 0.05);
        const kf = 1 - Math.exp(-dt / 0.02);
        const ks = 1 - Math.exp(-dt / 0.055);
        im.fx += (px - im.fx) * kf;
        im.fy += (py - im.fy) * kf;
        im.sx += (px - im.sx) * ks;
        im.sy += (py - im.sy) * ks;
        const lx = im.fx - im.sx;
        const ly = im.fy - im.sy;
        // 轴只取横 / 纵，用平滑后的 lag 判，带 25% 滞回（不会在阈值附近横跳）
        const ax = Math.abs(lx);
        const ay = Math.abs(ly);
        if (im.axis === 0 && ay > ax * 1.25) im.axis = 1;
        else if (im.axis === 1 && ax > ay * 1.25) im.axis = 0;
        // 匀速时 lag ≈ v·(τs−τf)，8px 吃满一档；τs−τf 由 60ms 缩到 35ms，
        // 除数同比缩小才保持上一轮定好的灵敏度。
        // 取 |lag|：符号只是运动方向，不是「起步 / 急停」相位。当相位用会让
        // 向左走 ε<0、shader 把放大轴翻到垂直轴 ⇒ 玻璃反而变粗。方向只用于
        // 选轴（上面已用 abs 比主分量），形变强度必须与方向无关。
        const along = im.axis === 0 ? lx : ly;
        const target = Math.min(1, Math.abs(along) / 8) * 0.05 * mo;
        // target 已由 fast/slow 两级指数平滑，这里只做一阶跟随：二阶弹簧会过冲
        // 并拖出慢动作；一阶输出恒在 target 包络内。20/55/28ms 三条时间常数
        // 让起步跟手、回落干脆，不再有粘稠的拖尾。
        im.strain += (target - im.strain) * (1 - Math.exp(-dt / 0.028));
        im.t = now;
      }
    } else if (im.ready) {
      // 档位被关掉：下一轮重新起算
      im.ready = false;
      im.strain = 0;
    }

    const list = latest.current.shapes;
    /** 按压外鼓量：系数 × --glass-press-swell × 短边，按本玻璃的进度走 */
    const pressSwell = num("--glass-press-swell", 0.3) * latest.current.lens;
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
      gl.activeTexture(gl.TEXTURE0);
      gl.bindTexture(gl.TEXTURE_2D, tex);
      gl.texImage2D(
        gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, texRef.current.off
      );
      const t = texRef.current;
      if (t.inkCtx) {
        t.inkCtx.clearRect(0, 0, t.inkOff.width, t.inkOff.height);
        latest.current.ink?.(t.inkCtx, t.inkOff.width, t.inkOff.height, dpr);
        gl.activeTexture(gl.TEXTURE1);
        gl.bindTexture(gl.TEXTURE_2D, inkTex);
        gl.texImage2D(
          gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, t.inkOff
        );
      }
      dirty.current = false;
    }
    gl.viewport(0, 0, w, h);
    gl.useProgram(prog);
    gl.activeTexture(gl.TEXTURE0);
    gl.bindTexture(gl.TEXTURE_2D, tex);
    gl.uniform1i(u.uTex, 0);
    gl.activeTexture(gl.TEXTURE1);
    gl.bindTexture(gl.TEXTURE_2D, inkTex);
    gl.uniform1i(u.uInk, 1);
    gl.uniform1f(u.uHasInk, latest.current.ink ? 1 : 0);
    gl.uniform2f(u.uRes, w, h);
    gl.uniform2fv(u.uCenter, cen);
    gl.uniform2fv(u.uHalf, hal);
    gl.uniform1fv(u.uRadius, rad);
    gl.uniform1fv(u.uBulge, bul);
    gl.uniform1f(u.uBendBand, num("--glass-lens-band", 0.35));
    gl.uniform1f(u.uBend, num("--glass-lens-bend", 0.35));
    gl.uniform1f(u.uCompress, num("--glass-lens-compress", 0.35));
    gl.uniform1f(u.uChroma, num("--glass-lens-chroma") * dpr); // px → 设备像素
    gl.uniform1f(u.uSpan, Math.min(1, Math.max(0, num("--glass-lens-span", 0.6))));
    gl.uniform1f(u.uDecay, Math.min(0.95, Math.max(0.05, num("--glass-lens-decay", 0.5))));
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
    gl.uniform1f(u.uClear, num("--glass-clear", 0));
    gl.uniform3fv(u.uBodyColor, color("--glass-body-color", "oklch(0.55 0 0)"));
    gl.uniform1f(u.uBodyShade, num("--glass-body-shade", 0.24));
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
