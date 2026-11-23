/** @file Kotlin 液态玻璃的 AGSL 着色器与共享几何类型。 */
package com.mui.glass

import android.graphics.RuntimeShader

/** 本体档位：透明 / 玉白 / 平灰（与 web 版 body prop 一一对应） */
enum class GlassBody {
    /** 透明档：竖向灰层次 + 边缘明暗 */
    TRANSPARENT,

    /** 玉白档：整块纯白不透明 */
    JADE,

    /** 平灰档：均匀灰（磨砂），折射/色散/边缘高光全关 */
    GRAY,
}

/** 玻璃影子档位：与 shadow.css 的 --shadow-glass-* 一一对应 */
enum class GlassShadowLevel(val key: String) {
    NONE("none"),
    XS("xs"),
    SM("sm"),
    MD("md"),
    LG("lg"),
    XL("xl"),
}

/**
 * 一块玻璃的几何：中心 / 半宽 / 半高 / 圆角（均为 dp，相对画布左上角）。
 *
 * @param cx 中心 x（画布坐标）
 * @param cy 中心 y（画布坐标）
 * @param hw 半宽
 * @param hh 半高
 * @param radius 圆角半径（胶囊 = 半高）
 * @param bulge 常驻外鼓量（dp，四边等量）：自己算好进度时用
 * @param swell 按压外鼓系数 0..1：引擎乘 press-swell × 短边 × 按压进度
 */
data class LensShape(
    val cx: Float,
    val cy: Float,
    val hw: Float,
    val hh: Float,
    val radius: Float,
    val bulge: Float = 0f,
    val swell: Float = 0f,
)

/** 单块玻璃上限（与 web 版 SHAPE_MAX 一致） */
const val GLASS_SHAPE_MAX = 3

/**
 * 液态玻璃 AGSL 片元着色器：web 版 liquid-glass.tsx 的逐行移植。
 *
 * 对每个像素求圆角矩形 SDF，轮廓内依次：折射（边缘凸透镜 + 按压中心凹透镜）、
 * 色散（R/B 沿法线错开像素量）、本体（透明档竖向灰 / 玉白 / 平灰）、
 * 边缘（顶底高光 + 两侧暗边）。轮廓外画影子（SDF 高斯近似 = drop-shadow 语义）。
 *
 * 两条硬约束（改了等于没有色散）：
 *   1) uChroma 是像素量，不是倍率；
 *   2) R/B 只夹「相对 q 额外」的越界：q 自己越界是凹透镜缩小所需（放行），
 *      色散多出来的越界不夹会折出接缝彩环。
 *
 * 档位：只有 TRANSPARENT 是玻璃（opt = 1）；JADE / GRAY 是磨砂，整套光学关掉。
 * 斜面带宽沿轮廓弧度分布（uSpan 给保持满值的固定段，之后收到顶底 0），
 * 凹透镜（全域）由 uCompress 常驻控制，透明档静止也能把背景导轨吸窄。
 */
object GlassShaderSource {
    val agsl: String = """
uniform shader uTex;
uniform float uHasInk;
uniform float2 uRes;

uniform float2 uCenter[3];
uniform float2 uHalf[3];
uniform float uRadius[3];
uniform float uBulge[3];

uniform float uBendBand;
uniform float uBend;
uniform float uCompress;
// 凹透镜常驻：强度只有 uCompress 一个来源，按压进度不参与光学
uniform float uChroma;
uniform float uSpan;
uniform float uDecay;
uniform float uBody;
uniform float uGray;
layout(color) uniform float4 uGrayColor;
layout(color) uniform float4 uBodyColor;
uniform float uBodyShade;
uniform float uBodyPeak;
uniform float uBodyFadeX;
layout(color) uniform float4 uRimLight;
layout(color) uniform float4 uRimDark;
uniform float uRimTop;
uniform float uRimBottom;
uniform float uRimSide;
uniform float uRimWidth;
uniform float2 uMotionAxis;
uniform float uMotionAmp; // ε ≥ 0：放大轴恒为应变轴、另一轴恒收窄
uniform float uDpr;
uniform float uFill;
uniform float uAlpha;
// 必须是 layout(color)：Kotlin 侧用 setColorUniform 传 ARGB，
// 少了这个修饰符运行时会抛「attempting to set a color uniform using the non-color specific APIs」。
layout(color) uniform vec4 uShadowColor;
uniform float2 uShadowOffset;
uniform float uShadowBlur;

float sdRoundRect(float2 p, float2 h, float r) {
    float2 q = abs(p) - h + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

float2 sdfNormal(float2 p, float2 h, float r) {
    float e = 1.0;
    float gx = sdRoundRect(p + float2(e, 0.0), h, r) - sdRoundRect(p - float2(e, 0.0), h, r);
    float gy = sdRoundRect(p + float2(0.0, e), h, r) - sdRoundRect(p - float2(0.0, e), h, r);
    return normalize(float2(gx, gy) + float2(1e-5));
}

// 图层上下分别录制背面和文字。夹到各自边缘的像素中心，保持与 Web 纹理夹取一致。
half4 sampleLayer(float2 p, float originY) {
    return uTex.eval(clamp(p, float2(0.5), uRes - 0.5) + float2(0.0, originY));
}

half4 main(float2 p) {
    if (p.x < 0.0 || p.y < 0.0 || p.x >= uRes.x || p.y >= uRes.y) return half4(0.0);
    float4 col = sampleLayer(p, 0.0);

    float a = 0.0;
    float shadowAcc = 0.0;
    float sigma = max(uShadowBlur, 0.001);

    for (int i = 0; i < 3; i++) {
        float bw = uHalf[i].x + uBulge[i];
        float bh = uHalf[i].y + uBulge[i];
        float2 v = p - uCenter[i];
        float2 vv = v;
        float2 motionScale = float2(1.0);
        if (uMotionAmp > 0.0005) {
            // 保体积应变 s·t=1；s 由这块玻璃在画布里的真实余量封顶。
            // 画布外扩只按按压外鼓预算，uBulge 已吃掉大半，惯性再放大就会被切平。
            // 放大侧按 s 封顶，收缩侧仍按**原始 sWant** 走：余量不足时形变不会整个
            // 消失，只是由「拉长」变成「变窄」——不越界，流体感也还在。
            float2 hv = float2(bw, bh);
            float2 growAxis = uMotionAxis; // ε ≥ 0，放大轴恒为应变轴
            float sWant = 1.0 + uMotionAmp;
            float2 room = max(min(uCenter[i], uRes - uCenter[i]) - hv, float2(0.0));
            float halfGrow = dot(growAxis, hv);
            float roomGrow = dot(growAxis, room);
            float s = min(sWant, 1.0 + roomGrow / max(halfGrow, 0.001));
            motionScale = mix(float2(sWant), float2(1.0 / s), growAxis);
            vv = v * motionScale;
        }
        float br = uRadius[i] + uBulge[i];
        float sd = sdRoundRect(vv, float2(bw, bh), br);

        // 以 SDF 近似模糊轮廓遮罩。偏移用屏幕坐标，拖动形变时阴影偏移仍保持同一距离。
        if (uShadowColor.a > 0.0) {
            float sdShadow = sdRoundRect((v - uShadowOffset) * motionScale, float2(bw, bh), br);
            float blurCoverage = 1.0 / (1.0 + exp(clamp(1.7 * sdShadow / sigma, -16.0, 16.0)));
            shadowAcc = max(shadowAcc, uShadowColor.a * blurCoverage);
        }

        float cov = 1.0 - smoothstep(-0.6 * uDpr, 0.6 * uDpr, sd);
        if (cov > 0.0) {
            float depth = max(-sd, 0.0);
            float mn = max(min(bw, bh), 0.001);
            float bevel = max(mn * uBendBand, 0.001);
            // 只有透明档是玻璃；玉白 / 平灰是磨砂，整套光学都不参与
            float opt = 1.0 - max(uBody, uGray);
            float2 n = sdfNormal(vv, float2(bw, bh), br);

            // 斜面带宽沿轮廓弧度：固定段（y 轴投影 = uSpan × 全高）保持满带宽，
            // 之后按法线角度收到 0。单参数 atan —— SkSL 没有可靠的 atan(y,x) 重载。
            float th = atan(abs(n.y) / max(abs(n.x), 0.0001));
            float th0 = asin(clamp(uSpan, 0.0, 1.0));
            // 收窄进度 t：0 = 固定段末尾，1 = 顶 / 底。归零位置由 uDecay 控制：
            // k=0.5 → 顶底归零；k>0.5 → 更早归零；k<0.5 → 顶底仍有残余
            float t = clamp((th - th0) / max(1.5707963 - th0, 0.0001), 0.0, 1.0);
            float k = clamp(uDecay, 0.05, 0.95);
            float tEnd = (1.0 - k) / k;
            float s = clamp(t / tEnd, 0.0, 1.0);
            float taper = 1.0 - s * s * (3.0 - 2.0 * s);
            float wRaw = clamp(1.0 - depth / bevel, 0.0, 1.0);
            float w = wRaw * wRaw * (3.0 - 2.0 * wRaw) * taper;
            float m = 1.0 + uBend * w * opt;
            // 凹透镜常驻，强度只有 uCompress 一个来源；范围覆盖整个玻璃内部。
            // 只给中心的话斜面圈会被凸透镜独占，轨道反而比玻璃外宽
            float mc = 1.0 - uCompress * opt;
            mc = max(mc, 0.25);
            float shift = length(vv) * (1.0 - 1.0 / m);

            float o = uChroma * opt * taper * smoothstep(0.0, 0.35, wRaw);
            float2 q = vv / mc - n * shift;
            // 只夹色散相对 q 额外造成的越界；q 自己越界是凹透镜缩小所需，
            // 全域压缩下斜面圈的 q 也会越界，整体夹回会让 R/B 停在原处、只有 G 缩小
            float2 tapR = q + n * (o * 0.5);
            float2 tapB = q - n * (o * 0.5);
            float2 hv = float2(bw, bh);
            float overQ = max(sdRoundRect(q, hv, br), 0.0);
            tapR -= n * max(max(sdRoundRect(tapR, hv, br), 0.0) - overQ, 0.0) * w;
            tapB -= n * max(max(sdRoundRect(tapB, hv, br), 0.0) - overQ, 0.0) * w;
            half4 trackR = sampleLayer(uCenter[i] + tapR, 0.0);
            half4 trackG = sampleLayer(uCenter[i] + q, 0.0);
            half4 trackB = sampleLayer(uCenter[i] + tapB, 0.0);
            float3 sc = float3(trackR.r, trackG.g, trackB.b);
            float3 opacity = float3(trackR.a, trackG.a, trackB.a);
            // 内容层只走凸透镜位移，不除 mc：文字 / 图标折射但不缩小。
            // 色散两层都要做：彩边主要来自文字这种高对比边缘，只给背景层分通道会看不见
            if (uHasInk > 0.5) {
                float2 qInk = vv - n * shift;
                float2 inkR = qInk + n * (o * 0.5);
                float2 inkB = qInk - n * (o * 0.5);
                float overInk = max(sdRoundRect(qInk, hv, br), 0.0);
                inkR -= n * max(max(sdRoundRect(inkR, hv, br), 0.0) - overInk, 0.0) * w;
                inkB -= n * max(max(sdRoundRect(inkB, hv, br), 0.0) - overInk, 0.0) * w;
                // 三通道各自合成：R/B 用自己错位位置的 ink 覆盖度。
                // 共用中心 alpha 会把错位造成的覆盖差异统一裁掉，色散就没了
                half4 inkR4 = sampleLayer(uCenter[i] + inkR, uRes.y);
                half4 inkG4 = sampleLayer(uCenter[i] + qInk, uRes.y);
                half4 inkB4 = sampleLayer(uCenter[i] + inkB, uRes.y);
                float3 inkOpacity = float3(inkR4.a, inkG4.a, inkB4.a);
                // AGSL 样本是预乘颜色。直接做 over，避免文字的半透明边缘再乘一次 alpha。
                sc = sc * (1.0 - inkOpacity) + float3(inkR4.r, inkG4.g, inkB4.b);
                opacity = mix(opacity, float3(1.0), inkOpacity);
            }

            float vt = clamp((vv.y + bh) / max(2.0 * bh, 0.001), 0.0, 1.0);
            float pk = clamp(uBodyPeak, 0.05, 0.95);
            float thick = mix(
                smoothstep(0.0, pk, vt),
                1.0 - smoothstep(pk, 1.0, vt),
                step(pk, vt)
            );
            float sideIn = clamp(1.0 - depth / max(bevel, 0.001), 0.0, 1.0);
            sideIn = sideIn * sideIn;
            thick = thick * (1.0 - uBodyFadeX * sideIn * abs(n.x));
            float tint = uBodyShade * thick * opt;
            sc = mix(sc, uBodyColor.rgb, tint);
            opacity = mix(opacity, float3(1.0), tint);

            float rim = 1.0 - smoothstep(0.0, max(uRimWidth, 0.5) * uDpr, depth);
            rim = rim * rim;
            float nUp = max(-n.y, 0.0);
            float nDn = max(n.y, 0.0);
            float nSide = abs(n.x);
            float topLight = clamp(rim * nUp * uRimTop * opt, 0.0, 1.0);
            float bottomLight = clamp(rim * nDn * uRimBottom * opt, 0.0, 1.0);
            float sideShade = clamp(rim * nSide * uRimSide * opt, 0.0, 1.0);
            sc = mix(sc, uRimLight.rgb, topLight);
            sc = mix(sc, uRimLight.rgb, bottomLight);
            sc = mix(sc, uRimDark.rgb, sideShade);

            sc = mix(sc, float3(1.0), uBody);
            sc = mix(sc, uGrayColor.rgb, uGray);
            opacity = 1.0 - (1.0 - opacity) * (1.0 - topLight) * (1.0 - bottomLight) * (1.0 - sideShade)
                * (1.0 - uBody) * (1.0 - uGray);

            col = mix(col, float4(sc, max(opacity.r, max(opacity.g, opacity.b))), cov);
            a = max(a, cov);
        }
    }

    if (uFill > 0.5) {
        return half4(col);
    }
    // 影子垫底、玻璃盖上面（over 合成，premultiplied）
    float4 glass = col * (a * uAlpha);
    float4 shadow = float4(uShadowColor.rgb * shadowAcc, shadowAcc);
    return half4(glass + shadow * (1.0 - glass.a));
}
""".trimIndent()

    /** 编译一次（API 33+）；uniform 之后逐帧热更新 */
    fun compile(): RuntimeShader = RuntimeShader(agsl)
}
