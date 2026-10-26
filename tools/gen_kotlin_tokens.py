#!/usr/bin/env python3
# gen_kotlin_tokens.py
# 做什么：读 web/src/styles/tokens/*.css，生成 Kotlin 令牌常量文件。
# 输入：web/src/styles/tokens/{colors,radius,shadow,glass}.css
# 输出：kotlin/mui-ui/src/main/java/com/mui/{token,glass}/ 下的 MuiColors.kt / MuiRadius.kt / MuiElevation.kt / GlassTokens.kt
# 硬约束：oklch 超出 sRGB 色域时按降色度做色域映射（保 L 与 H）；本文件是令牌唯一生成出处，产物勿手改。

import re
import math
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
CSS = ROOT / "web" / "src" / "styles" / "tokens"
OUT_TOKEN = ROOT / "kotlin" / "mui-ui" / "src" / "main" / "java" / "com" / "mui" / "token"
OUT_GLASS = ROOT / "kotlin" / "mui-ui" / "src" / "main" / "java" / "com" / "mui" / "glass"


def strip_comments(text: str) -> str:
    return re.sub(r"/\*.*?\*/", "", text, flags=re.S)


def extract_block(text: str, selector: str) -> str:
    """取 `selector { ... }` 的花括号内容（不含嵌套块）"""
    m = re.search(re.escape(selector) + r"\s*\{", text)
    if not m:
        return ""
    depth = 1
    i = m.end()
    start = i
    while depth > 0 and i < len(text):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
        i += 1
    return text[start : i - 1]


def parse_vars(block: str) -> dict:
    """`--name: value;` → {name: value}"""
    out = {}
    for m in re.finditer(r"--([\w-]+)\s*:\s*([^;]+);", block):
        out[m.group(1)] = m.group(2).strip()
    return out


# ── oklch → sRGB（色域映射：降色度，保 L/H）─────────────────────────────


def oklch_to_linear_srgb(L, C, H):
    h = math.radians(H)
    a = C * math.cos(h)
    b = C * math.sin(h)
    l_ = L + 0.3963377774 * a + 0.2158037573 * b
    m_ = L - 0.1055613458 * a - 0.0638541728 * b
    s_ = L - 0.0894841775 * a - 1.2914855480 * b
    l, m, s = l_ ** 3, m_ ** 3, s_ ** 3
    r = +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
    g = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
    bb = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
    return r, g, bb


def gamut_fit(L, C, H):
    """C 太大出 sRGB 色域时，二分降色度（L/H 不动），与浏览器 oklch 的色域映射一致方向"""
    r, g, b = oklch_to_linear_srgb(L, C, H)
    if in_gamut(r, g, b) or C <= 0:
        return C
    lo, hi = 0.0, C
    for _ in range(24):
        mid = (lo + hi) / 2
        r, g, b = oklch_to_linear_srgb(L, mid, H)
        if in_gamut(r, g, b):
            lo = mid
        else:
            hi = mid
    return lo


def in_gamut(r, g, b, eps=1e-4):
    return all(-eps <= v <= 1 + eps for v in (r, g, b))


def linear_to_srgb(v):
    v = min(max(v, 0.0), 1.0)
    return 12.92 * v if v <= 0.0031308 else 1.055 * (v ** (1 / 2.4)) - 0.055


OKLCH_RE = re.compile(r"oklch\(\s*([\d.]+%?)\s+([\d.]+%?)\s+([\d.]+)(?:\s*/\s*([\d.]+%?))?\s*\)")


def parse_color(value: str):
    """oklch(...) → (r, g, b, a)，0..1；解析失败返回 None"""
    value = value.strip()
    m = OKLCH_RE.match(value)
    if not m:
        return None
    def num(x):
        return float(x[:-1]) / 100 if x.endswith("%") else float(x)
    L, C, H = num(m.group(1)), num(m.group(2)), num(m.group(3))
    a = num(m.group(4)) if m.group(4) else 1.0
    C = gamut_fit(L, C, H)
    r, g, b = oklch_to_linear_srgb(L, C, H)
    return linear_to_srgb(r), linear_to_srgb(g), linear_to_srgb(b), a


def hex_argb(r, g, b, a):
    ai = round(a * 255)
    ri, gi, bi = round(r * 255), round(g * 255), round(b * 255)
    return f"0x{ai:02X}{ri:02X}{gi:02X}{bi:02X}"


def camel(name: str) -> str:
    parts = name.split("-")
    head = parts[0]
    rest = [p[:1].upper() + p[1:] if p[0].isdigit() is False else p for p in parts[1:]]
    out = head + "".join(rest)
    # 数字开头段（如 mui-50 → mui50）已被并入，无需再处理
    return out


# ── 1. colors.css → MuiColors.kt ─────────────────────────────────────

OUT_TOKEN.mkdir(parents=True, exist_ok=True)
OUT_GLASS.mkdir(parents=True, exist_ok=True)

colors_text = strip_comments((CSS / "colors.css").read_text(encoding="utf-8"))
light_vars = parse_vars(extract_block(colors_text, ":root"))
dark_vars = parse_vars(extract_block(colors_text, ".dark"))

names = list(light_vars.keys())
lines = []
for n in names:
    c = parse_color(light_vars[n])
    assert c, f"无法解析亮色 --{n}: {light_vars[n]}"
    lines.append(f"        {camel(n)} = Color({hex_argb(*c)}),")
light_body = "\n".join(lines)

lines = []
for n in names:
    raw = dark_vars.get(n, light_vars[n])  # 暗色没覆盖的名字沿用亮色
    c = parse_color(raw)
    assert c, f"无法解析暗色 --{n}: {raw}"
    lines.append(f"        {camel(n)} = Color({hex_argb(*c)}),")
dark_body = "\n".join(lines)

props = ",\n".join(f"    val {camel(n)}: Color" for n in names)

mui_colors = f"""package com.mui.token

import androidx.compose.ui.graphics.Color

/** 由 tools/gen_kotlin_tokens.py 从 web/src/styles/tokens/colors.css 生成，勿手改。 */
data class MuiPalette(
{props}
)

/** 亮 / 暗两套调色板：亮 = :root，暗 = .dark（未覆盖的名字沿用亮色值）。 */
object MuiColors {{
    val Light = MuiPalette(
{light_body}
    )

    val Dark = MuiPalette(
{dark_body}
    )
}}
"""
(OUT_TOKEN / "MuiColors.kt").write_text(mui_colors, encoding="utf-8")
print(f"MuiColors.kt: {len(names)} colors x2")

# ── 2. radius.css → MuiRadius.kt ─────────────────────────────────────

radius_text = strip_comments((CSS / "radius.css").read_text(encoding="utf-8"))
radius_vars = parse_vars(extract_block(radius_text, ":root"))
base_px = float(radius_vars["radius"].replace("rem", "")) * 16  # 0.625rem = 10px
R = {"sm": base_px - 4, "md": base_px - 2, "lg": base_px, "xl": base_px + 4}

mui_radius = f"""package com.mui.token

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 由 tools/gen_kotlin_tokens.py 从 web/src/styles/tokens/radius.css 生成（--radius = {base_px:g}px），勿手改。 */
object MuiRadius {{
    val sm: Dp = {R['sm']:g}.dp
    val md: Dp = {R['md']:g}.dp
    val lg: Dp = {R['lg']:g}.dp
    val xl: Dp = {R['xl']:g}.dp
}}
"""
(OUT_TOKEN / "MuiRadius.kt").write_text(mui_radius, encoding="utf-8")
print(f"MuiRadius.kt: base={base_px:g}px")

# ── 3. shadow.css → MuiElevation.kt ──────────────────────────────────

shadow_text = strip_comments((CSS / "shadow.css").read_text(encoding="utf-8"))
shadow_light = parse_vars(extract_block(shadow_text, ":root"))
shadow_dark = parse_vars(extract_block(shadow_text, ".dark"))


def parse_rgb(text: str):
    """rgb(0 0 0 / 0.1) → (r,g,b,a) 0..1"""
    m = re.match(r"rgb\(\s*([\d.]+)\s+([\d.]+)\s+([\d.]+)(?:\s*/\s*([\d.]+%?))?\s*\)", text.strip())
    if not m:
        return None
    def num(x):
        return float(x[:-1]) / 100 if x.endswith("%") else float(x)
    a = num(m.group(4)) if m.group(4) else 1.0
    return (int(m.group(1)) / 255, int(m.group(2)) / 255, int(m.group(3)) / 255, a)


def parse_px(x: str) -> float:
    x = x.strip()
    return float(x[:-2]) if x.endswith("px") else float(x)


def parse_shadows(value: str, ink_vars: dict):
    """一条 box-shadow 列表 → [(dx, dy, blur, color)]（spread 已在 CSS 设计层折进 blur）"""
    out = []
    for part in value.split(","):
        part = part.strip()
        toks = part.split()
        # 颜色是末尾一段（rgb(...) 可能含空格），前面全是几何：dx dy blur [spread]
        color_start = next(i for i, t in enumerate(toks) if t.startswith("rgb(") or t.startswith("var("))
        geom, color_toks = toks[:color_start], toks[color_start:]
        color_raw = " ".join(color_toks)
        if color_raw.startswith("var("):
            ref = color_raw[4:-1].strip()
            color_raw = ink_vars[ref.lstrip("-")]
        c = parse_rgb(color_raw)
        assert c, f"无法解析阴影颜色: {color_raw}"
        dx, dy = parse_px(geom[0]), parse_px(geom[1])
        blur = parse_px(geom[2]) if len(geom) > 2 else 0.0
        spread = parse_px(geom[3]) if len(geom) > 3 else 0.0
        out.append((dx, dy, blur, c))
    return out


def emit_shadows(vars_map: dict, ink_vars: dict, prefix: str):
    body = []
    for level in ["2xs", "xs", "sm", "md", "lg", "xl", "2xl"]:
        if f"elevation-{level}" not in vars_map:
            continue
        shadows = parse_shadows(vars_map[f"elevation-{level}"], ink_vars)
        specs = ", ".join(
            f"MuiShadowSpec(dx = {dx:g}.dp, dy = {dy:g}.dp, blur = {blur:g}.dp, color = Color({hex_argb(*c)}))"
            for dx, dy, blur, c in shadows
        )
        cap = level if level[0].isdigit() else level[0].upper() + level[1:]
        body.append(f"    /** --elevation-{level} */\n    val {prefix}{cap}: List<MuiShadowSpec> = listOf({specs})")
    return "\n\n".join(body)


ink_light = {"shadow-glass-ink": shadow_light["shadow-glass-ink"]}
ink_dark = {"shadow-glass-ink": shadow_dark.get("shadow-glass-ink", shadow_light["shadow-glass-ink"])}
light_block = emit_shadows(shadow_light, ink_light, "light")
dark_block = emit_shadows(shadow_dark, ink_dark, "dark")

ink_c_light = parse_rgb(shadow_light["shadow-glass-ink"])
ink_c_dark = parse_rgb(shadow_dark.get("shadow-glass-ink", shadow_light["shadow-glass-ink"]))

glass_shadows = []
for level in ["xs", "sm", "md", "lg", "xl"]:
    key = f"shadow-glass-{level}"
    raw = shadow_light[key].replace("var(--shadow-glass-ink)", shadow_light["shadow-glass-ink"])
    dx, dy, blur, c = parse_shadows(raw, ink_light)[0]
    glass_shadows.append(f'"{level}" to GlassShadowSpec(dx = {dx:g}.dp, dy = {dy:g}.dp, blur = {blur:g}.dp, color = Color({hex_argb(*c)}))')
glass_dark = []
for level in ["xs", "sm", "md", "lg", "xl"]:
    key = f"shadow-glass-{level}"
    raw = shadow_dark[key].replace("var(--shadow-glass-ink)", shadow_dark.get("shadow-glass-ink", shadow_light["shadow-glass-ink"]))
    dx, dy, blur, c = parse_shadows(raw, ink_dark)[0]
    glass_dark.append(f'"{level}" to GlassShadowSpec(dx = {dx:g}.dp, dy = {dy:g}.dp, blur = {blur:g}.dp, color = Color({hex_argb(*c)}))')

mui_elevation = f"""package com.mui.token

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/** 一层投影：偏移 / 模糊 / 颜色（spread 已按 shadow.css 的设计折进 blur）。 */
data class MuiShadowSpec(val dx: Dp, val dy: Dp, val blur: Dp, val color: Color)

/** 玻璃引擎的影子档（drop-shadow 语义：无 spread）。 */
data class GlassShadowSpec(val dx: Dp, val dy: Dp, val blur: Dp, val color: Color)

/** 由 tools/gen_kotlin_tokens.py 从 web/src/styles/tokens/shadow.css 生成，勿手改。 */
object MuiElevation {{
    // 亮色：黑晕向下偏移 = 投影
{light_block}

    // 暗色：零偏移白晕 = 边缘亮
{dark_block}

    /** 玻璃影子档位：与 shadow.css 的 --shadow-glass-* 一一对应（亮色）。 */
    val glassShadowLight: Map<String, GlassShadowSpec> = mapOf(
        {", ".join(glass_shadows)}
    )

    /** 玻璃影子档位（暗色：白晕零偏移）。 */
    val glassShadowDark: Map<String, GlassShadowSpec> = mapOf(
        {", ".join(glass_dark)}
    )

    /** --shadow-glass-ink */
    val glassInkLight: Color = Color({hex_argb(*ink_c_light)})
    val glassInkDark: Color = Color({hex_argb(*ink_c_dark)})
}}
"""
(OUT_TOKEN / "MuiElevation.kt").write_text(mui_elevation, encoding="utf-8")
print("MuiElevation.kt: 7+7 levels, glass 5+5")

# ── 4. glass.css → GlassTokens.kt ────────────────────────────────────

glass_text = strip_comments((CSS / "glass.css").read_text(encoding="utf-8"))
glass_light = parse_vars(extract_block(glass_text, ":root"))
glass_dark = parse_vars(extract_block(glass_text, ".dark"))


def gval(name: str, dark: bool = False) -> str:
    src = glass_dark if dark else glass_light
    return src.get(name, glass_light[name])


def gnum(name: str, dark: bool = False) -> float:
    return float(re.sub(r"(px|ms)$", "", gval(name, dark)))


def gcol(name: str, dark: bool = False):
    c = parse_color(gval(name, dark))
    assert c, f"无法解析玻璃颜色 --{name}"
    return hex_argb(*c)


def palette(prefix: str, dark: bool):
    return f"""    val {prefix} = GlassPalette(
        bodyColor = Color({gcol('glass-body-color', dark)}),
        grayColor = Color({gcol('glass-gray-color', dark)}),
        rimLight = Color({gcol('glass-rim-light', dark)}),
        rimDark = Color({gcol('glass-rim-dark', dark)}),
        rimTop = {gnum('glass-rim-top', dark):g}f,
        rimBottom = {gnum('glass-rim-bottom', dark):g}f,
        rimSide = {gnum('glass-rim-side', dark):g}f,
    )"""

glass_tokens = f"""package com.mui.glass

import androidx.compose.ui.graphics.Color

/** 一个主题下的玻璃外观（颜色 + 明暗差异化的边缘强度）。 */
data class GlassPalette(
    /** --glass-body-color 灰本体的颜色 */
    val bodyColor: Color,
    /** --glass-gray-color 平灰档的灰 */
    val grayColor: Color,
    /** --glass-rim-light 顶/底高光颜色 */
    val rimLight: Color,
    /** --glass-rim-dark 两侧暗边颜色 */
    val rimDark: Color,
    /** --glass-rim-top 顶边高光强度 */
    val rimTop: Float,
    /** --glass-rim-bottom 底边高光强度 */
    val rimBottom: Float,
    /** --glass-rim-side 两侧暗边强度 */
    val rimSide: Float,
)

/** 由 tools/gen_kotlin_tokens.py 从 web/src/styles/tokens/glass.css 生成，勿手改。 */
object GlassTokens {{
    val Light = GlassPalette(
        bodyColor = Color({gcol('glass-body-color')}),
        grayColor = Color({gcol('glass-gray-color')}),
        rimLight = Color({gcol('glass-rim-light')}),
        rimDark = Color({gcol('glass-rim-dark')}),
        rimTop = {gnum('glass-rim-top'):g}f,
        rimBottom = {gnum('glass-rim-bottom'):g}f,
        rimSide = {gnum('glass-rim-side'):g}f,
    )

    val Dark = GlassPalette(
        bodyColor = Color({gcol('glass-body-color', True)}),
        grayColor = Color({gcol('glass-gray-color', True)}),
        rimLight = Color({gcol('glass-rim-light', True)}),
        rimDark = Color({gcol('glass-rim-dark', True)}),
        rimTop = {gnum('glass-rim-top', True):g}f,
        rimBottom = {gnum('glass-rim-bottom', True):g}f,
        rimSide = {gnum('glass-rim-side', True):g}f,
    )

    /** --glass-body-shade 透明档最厚处的浓度 0..1 */
    const val bodyShade = {gnum('glass-body-shade'):g}f
    /** --glass-body-peak 最厚处的纵向位置 0..1（0 = 顶） */
    const val bodyPeak = {gnum('glass-body-peak'):g}f
    /** --glass-body-fade-x 左右衰减量 */
    const val bodyFadeX = {gnum('glass-body-fade-x'):g}f
    /** --glass-gray-alpha 平灰档不透明度 */
    const val grayAlpha = {gnum('glass-gray-alpha'):g}f
    /** --glass-rim-width 边缘窗口宽度（dp） */
    const val rimWidthDp = {gnum('glass-rim-width'):g}f
    /** --glass-lens-band 斜面宽度系数 */
    const val lensBand = {gnum('glass-lens-band'):g}f
    /** --glass-lens-bend 静止态放大率 - 1 */
    const val lensBend = {gnum('glass-lens-bend'):g}f
    /** --glass-lens-compress 按压压缩率 */
    const val lensCompress = {gnum('glass-lens-compress'):g}f
    /** --glass-lens-chroma 色散总宽度（dp，像素量非倍率） */
    const val lensChromaDp = {gnum('glass-lens-chroma'):g}f
    /** --glass-press-swell 按压外鼓系数 */
    const val pressSwell = {gnum('glass-press-swell'):g}f
    /** --glass-press-duration 按压动画时长（毫秒） */
    const val pressDurationMs = {gnum('glass-press-duration'):g}f
    /** --glass-body-duration 换档动画时长（毫秒） */
    const val bodyDurationMs = {gnum('glass-body-duration'):g}f
}}
"""
(OUT_GLASS / "GlassTokens.kt").write_text(glass_tokens, encoding="utf-8")
print("GlassTokens.kt: light/dark palettes + scalars")
print("DONE")
