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
 *   2) R/B 采样点必须夹回轮廓内，越界会折出接缝彩环。
 */
object GlassShaderSource {
    val agsl: String = """
uniform shader uTex;
uniform float2 uRes;

uniform float2 uCenter[3];
uniform float2 uHalf[3];
uniform float uRadius[3];
uniform float uBulge[3];

uniform float uBendBand;
uniform float uBend;
uniform float uCompress;
uniform float uLens;
uniform float uChroma;
uniform float uBody;
uniform float uGray;
uniform float3 uGrayColor;
uniform float3 uBodyColor;
uniform float uBodyShade;
uniform float uBodyPeak;
uniform float uBodyFadeX;
uniform float3 uRimLight;
uniform float3 uRimDark;
uniform float uRimTop;
uniform float uRimBottom;
uniform float uRimSide;
uniform float uRimWidth;
uniform float2 uMotionAxis;
uniform float uMotionAmp;
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

half4 main(float2 fragCoord) {
    float2 p = float2(fragCoord.x, fragCoord.y);
    /* ⚠️ uTex.eval() 收的是**像素坐标**，不是 0..1 的 uv。
       web 版是 texture2D(sampler, uv)，GLSL 的 sampler 才吃归一化 uv；
       AGSL 的输入 shader 用自己的像素坐标系 —— 传 uv 进来等于整屏只采左上角一个像素
       （折射/色散都会变成"什么都没有"，因为整张图是单色）。 */
    half4 texel = uTex.eval(p);
    float3 col = float3(texel.r, texel.g, texel.b);

    float a = 0.0;
    float shadowAcc = 0.0;
    float sigma = max(uShadowBlur, 0.001) * 0.5;

    for (int i = 0; i < 3; i++) {
        float2 v = p - uCenter[i];
        float2 vv = v;
        if (abs(uMotionAmp) > 0.0005) {
            vv = v * mix(
                float2(1.0 + uMotionAmp),
                float2(1.0 / (1.0 + uMotionAmp)),
                uMotionAxis
            );
        }
        float bw = uHalf[i].x + uBulge[i];
        float bh = uHalf[i].y + uBulge[i];
        float br = uRadius[i] + uBulge[i];
        float sd = sdRoundRect(vv, float2(bw, bh), br);

        // 影子：与玻璃同形（含外鼓 / 应变），高斯衰减近似 drop-shadow，画在轮廓外
        float sdShadow = sdRoundRect(vv - uShadowOffset, float2(bw, bh), br);
        float dOut = max(sdShadow, 0.0);
        shadowAcc = max(shadowAcc, uShadowColor.a * 0.5 * exp(-0.5 * (dOut / sigma) * (dOut / sigma)));

        float cov = 1.0 - smoothstep(-0.6 * uDpr, 0.6 * uDpr, sd);
        if (cov > 0.0) {
            float depth = max(-sd, 0.0);
            float mn = max(min(bw, bh), 0.001);
            float bevel = mn * uBendBand;
            float opt = 1.0 - uGray;

            float w = clamp(1.0 - depth / bevel, 0.0, 1.0);
            w = w * w * (3.0 - 2.0 * w);
            float m = 1.0 + uBend * w * opt;
            float mc = 1.0 - uCompress * (1.0 - w) * uLens * opt;
            mc = max(mc, 0.25);
            float shift = length(vv) * (1.0 - 1.0 / m);
            float2 n = sdfNormal(vv, float2(bw, bh), br);

            float o = uChroma * opt * smoothstep(0.0, 0.35, clamp(1.0 - depth / bevel, 0.0, 1.0));
            float2 q = vv / mc - n * shift;
            float2 tapR = q + n * (o * 0.5);
            float2 tapB = q - n * (o * 0.5);
            float2 hv = float2(bw, bh);
            tapR -= n * max(sdRoundRect(tapR, hv, br), 0.0) * w;
            tapB -= n * max(sdRoundRect(tapB, hv, br), 0.0) * w;
            float3 sc;
            sc.r = uTex.eval(uCenter[i] + tapR).r;
            sc.g = uTex.eval(uCenter[i] + q).g;
            sc.b = uTex.eval(uCenter[i] + tapB).b;

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
            sc = mix(sc, uBodyColor, uBodyShade * thick * opt);

            float rim = 1.0 - smoothstep(0.0, max(uRimWidth, 0.5) * uDpr, depth);
            rim = rim * rim;
            float nUp = max(-n.y, 0.0);
            float nDn = max(n.y, 0.0);
            float nSide = abs(n.x);
            sc = mix(sc, uRimLight, clamp(rim * nUp * uRimTop * opt, 0.0, 1.0));
            sc = mix(sc, uRimLight, clamp(rim * nDn * uRimBottom * opt, 0.0, 1.0));
            sc = mix(sc, uRimDark, clamp(rim * nSide * uRimSide * opt, 0.0, 1.0));

            sc = mix(sc, float3(1.0), uBody);
            sc = mix(sc, uGrayColor, uGray);

            col = mix(col, sc, cov);
            a = max(a, cov);
        }
    }

    if (uFill > 0.5) {
        return half4(float4(col, 1.0));
    }
    // 影子垫底、玻璃盖上面（over 合成，premultiplied）
    float glassA = a * uAlpha;
    float outA = glassA + shadowAcc * (1.0 - glassA);
    float3 outRGB = col * glassA + (uShadowColor.rgb * shadowAcc) * (1.0 - glassA);
    return half4(float4(outRGB, outA));
}
""".trimIndent()

    /** 编译一次（API 33+）；uniform 之后逐帧热更新 */
    fun compile(): RuntimeShader = RuntimeShader(agsl)
}
