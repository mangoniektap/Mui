/**
 * classical 表面配方（surfaces 层）
 *
 * 职责：实色风格（当前默认外观）的公共表面规则，收拢为唯一出处。
 *
 * 颜色映射（本风格的规定）：
 *   按钮主色   → --primary        （bg-primary text-primary-foreground）
 *   卡牌纸面   → --card           （bg-card text-card-foreground）
 *   浮层纸面   → --popover        （bg-popover text-popover-foreground）
 *   模态对话框 → --background     （bg-background，shadow-lg）
 *   表单控件   → transparent      （边框 --input，暗色 dark:bg-input/30）
 *
 * 本风格不引入新令牌，参数全部复用：
 *   颜色→tokens/colors.css，圆角→tokens/radius.css，阴影→tokens/shadow.css。
 *
 * 消费方式：cn(base, classicalXxx, className)
 * ——tailwind-merge 保证调用方传入的 className 能干净覆盖配方。
 *
 * 切换进度：全库匹配组件已切换完毕——input / button / card / dialog / popover /
 * textarea / select / combobox / toggle / tabs / accordion / badge /
 * checkbox / radio-group / switch / item / hover-card / dropdown-menu / context-menu /
 * menubar / alert-dialog，共 22 个。本文件即这些表面的唯一出处。
 *
 * 未切换（写法与常量不一致，保持原样）：navigation-menu / scroll-area（无 border-ring，
 * 用 focus-visible:outline-1）、bubble（[button,a]: 前缀选择器）、toggle 的 outline 变体
 * （无 dark:bg-input/30）、combobox popup（ring-1 无 border）、command（无 border/shadow）、
 * 各 Sub 菜单（shadow-lg 而非 md）、sheet（无 border）、alert / attachment（无阴影）。
 */

/**
 * 聚焦环配方：全库交互控件统一（3px + ring-ring/50 + 边框转 --ring）。
 * 已核实使用方：button、input、textarea、badge、item、radio-group、switch、
 * accordion、navigation-menu、scroll-area 等绝大多数控件；
 * 日历用 has-focus:、组合框/输入组用 focus-within: 变体，不适用此常量。
 */
export const classicalFocusRing =
  "focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50";

/**
 * 校验失败态配套：aria-invalid 时边框与聚焦环转 destructive。
 * 已核实使用方：button、input、textarea、radio-group、switch、badge。
 */
export const classicalInvalidRing =
  "aria-invalid:border-destructive aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40";

/**
 * 表单控件表面：边框走 --input、底色透明、xs 阴影、暗色下沉 bg-input/30。
 * 已核实使用方：input、textarea、combobox；
 * input-group 少一个 bg-transparent，切换时按其实际为准。
 */
export const classicalControlSurface =
  "border border-input bg-transparent shadow-xs dark:bg-input/30";

/**
 * 卡片表面：纸面 --card + 前景 --card-foreground + sm 阴影。
 * 已核实使用方：card。
 */
export const classicalCardSurface =
  "border bg-card text-card-foreground shadow-sm";

/**
 * 浮层表面：纸面 --popover + md 阴影。
 * 已核实使用方：popover、hover-card、menubar 内容区及同族浮层。
 */
export const classicalOverlaySurface =
  "border bg-popover text-popover-foreground shadow-md";

/**
 * 模态表面：页面底 --background + lg 阴影。
 * 已核实使用方：dialog、alert-dialog；sheet 无边框，单独处理。
 */
export const classicalDialogSurface = "border bg-background shadow-lg";
