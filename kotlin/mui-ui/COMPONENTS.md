# kotlin/mui-ui 组件移植规范

从 `web/src/components/ui/*.tsx` 移植到 Compose 的统一规则（与根 README 的同步规则配套）。

## 文件与命名
- 每个组件一个文件：`web/src/components/ui/button.tsx` → `java/com/mui/components/MuiButton.kt`
- 包名统一 `com.mui.components`；子部件导出为同一文件内的多个 `@Composable`

## 注释
- 只注明「每个参数是什么」：名字 / 取值 / 作用，一行一个
- 头部注释只写：做什么 + 输入输出 + 硬约束；不写踩坑日记与 web 对应行号

## 令牌来源（禁止硬编码颜色/圆角/阴影）
- 颜色：`com.mui.theme.LocalMuiPalette.current`（字段与 colors.css 变量一一对应驼峰化）
- 圆角：`com.mui.token.MuiRadius`（sm 6 / md 8 / lg 10 / xl 14 dp）
- 阴影：`com.mui.theme.muiElevation(level, shape)`（2xs..2xl）；玻璃影子走 GlassLens 的 shadow
- 字体：`com.mui.token.MuiTypography`；正文 14sp（text-sm）、小字 12sp（text-xs）、中粗 500（font-medium）
- 玻璃：`com.mui.glass.GlassLens`（shapes 用 dp，坐标相对画布左上角；backdrop 在 DrawScope 画）
- 聚焦环：`com.mui.theme.muiFocusRing(interactionSource, shape, invalid)`，画 3dp 环
- 间距/尺寸：从 web 的 Tailwind 类直接换算（1 格 = 4dp：h-9 = 36dp、gap-2 = 8dp、px-3 = 12dp）

## 交互
- 无 ripple：`clickable(interactionSource, indication = null)`
- 焦点：组件建 `remember { MutableInteractionSource() }` 传给 clickable 与 muiFocusRing
- 受控/非受控模式沿用 web：`checked` + `defaultChecked` + `onXxxChange`
- hover 视觉用 `interactionSource.collectIsHoveredAsState()`（触摸设备上等效按压前态）

## 文本
- 一律用本包的 `MuiText`（BasicText 封装，默认 foreground / 14sp）

## 玻璃组件硬约束（Switch / Slider / ToggleGroup / Picker）
- GlassLens 画布比玻璃外扩 LENS_MARGIN，shapes 坐标 = 外扩后的画布左上角
- backdrop 先 `paintPageBackdrop(页面色)` 再画轨道/底座结构，每帧重画（live = true）
- 按压一律给 `pressed` 布尔，动画引擎自己跑；小件玻璃传 `lensBand = 0.3f`
