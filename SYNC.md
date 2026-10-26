# SYNC.md — 双语言同步台账

> 规则：令牌/样式/组件任何修改，web 与 kotlin 同一批提交都改，commit 注明 `web+kt`，并更新本表。
> 命名对应：`--foo-bar` ↔ `fooBar`（Kotlin 常量/参数），`button.tsx` ↔ `MuiButton.kt`。

## 一、令牌映射

| 语义 | web | kotlin | 状态 |
| --- | --- | --- | --- |
| 颜色（亮/暗） | `tokens/colors.css` | `token/MuiColors.kt`（MuiColors.Light / Dark，45 色 ×2，脚本生成） | ✅ |
| 圆角 | `tokens/radius.css`（--radius 10px） | `token/MuiRadius.kt`（sm6/md8/lg10/xl14） | ✅ |
| 阴影 | `tokens/shadow.css`（`--elevation-*` 双主题 + `--shadow-glass-*`） | `token/MuiElevation.kt`（lightXs..dark2xl + glassShadowLight/Dark） | ✅ |
| 字体 | `tokens/typography.css` | `token/MuiTypography.kt`（sans=Default / mono=Monospace，Android 无同一套栈） | ✅ |
| 玻璃令牌 | `tokens/glass.css` | `glass/GlassTokens.kt`（Light/Dark GlassPalette + 全部标量） | ✅ |
| 玻璃引擎 | `surfaces/liquid-glass.tsx`（WebGL SDF） | `glass/GlassShader.kt`（AGSL 源）+ `glass/GlassLens.kt`（Composable：按压动画 / 惯性应变 / 影子） | ✅ |
| 实色面配方 | `surfaces/classical.ts` | 逐组件落在 components 里（聚焦环 = theme/MuiFocusRing.kt，阴影 = theme/MuiShadow.kt） | ✅ |
| 令牌生成 | — | `tools/gen_kotlin_tokens.py`（改 CSS 后重跑，产物勿手改） | ✅ |

## 二、组件映射（63/63 全覆盖）

状态说明：✅ 已移植并同步。

| 组件 | web | kotlin | 状态 | 备注 |
| --- | --- | --- | --- | --- |
| Button | button.tsx | MuiButton.kt | ✅ | variant/size 全档 |
| Input | input.tsx | MuiInput.kt | ✅ | |
| Textarea | textarea.tsx | MuiTextarea.kt | ✅ | |
| Label | label.tsx | MuiLabel.kt | ✅ | |
| Badge | badge.tsx | MuiBadge.kt | ✅ | |
| Card | card.tsx | MuiCard.kt | ✅ | |
| Separator | separator.tsx | MuiSeparator.kt | ✅ | |
| Avatar | avatar.tsx | MuiAvatar.kt | ✅ | |
| Skeleton | skeleton.tsx | MuiSkeleton.kt | ✅ | |
| Spinner | spinner.tsx | MuiSpinner.kt | ✅ | |
| Switch | switch.tsx | MuiSwitch.kt | ✅ | 胶囊拇指+拖动吸附+按压换档 |
| Slider | slider.tsx | MuiSlider.kt | ✅ | 玻璃拇指，单值/区间 |
| Toggle | toggle.tsx | MuiToggle.kt | ✅ | |
| ToggleGroup | toggle-group.tsx | MuiToggleGroup.kt | ✅ | single=白胶囊底座+玻璃滑块；multiple=按钮组 |
| Select | select.tsx | MuiSelect.kt | ✅ | 数据驱动（TODO: 键盘导航） |
| Tabs | tabs.tsx | MuiTabs.kt | ✅ | default/line 双变体 |
| Combobox | combobox.tsx | MuiCombobox.kt | ✅ | List<String> 起步 |
| Picker | picker.tsx | MuiPicker.kt | ✅ | 转盘曲率/吸附/gray 玻璃条（TODO: 滚轮） |
| Dialog | dialog.tsx | MuiDialog.kt | ✅ | |
| AlertDialog | alert-dialog.tsx | MuiAlertDialog.kt | ✅ | |
| Sheet | sheet.tsx | MuiSheet.kt | ✅ | 四向 |
| Drawer | drawer.tsx | MuiDrawer.kt | ✅ | |
| Popover | popover.tsx | MuiPopover.kt | ✅ | |
| Tooltip | tooltip.tsx | MuiTooltip.kt | ✅ | |
| HoverCard | hover-card.tsx | MuiHoverCard.kt | ✅ | |
| DropdownMenu | dropdown-menu.tsx | MuiDropdownMenu.kt | ✅ | |
| ContextMenu | context-menu.tsx | MuiContextMenu.kt | ✅ | 长按/右键 |
| Menubar | menubar.tsx | MuiMenubar.kt | ✅ | |
| Alert | alert.tsx | MuiAlert.kt | ✅ | |
| Progress | progress.tsx | MuiProgress.kt | ✅ | |
| Accordion | accordion.tsx | MuiAccordion.kt | ✅ | 高度动画 |
| Collapsible | collapsible.tsx | MuiCollapsible.kt | ✅ | |
| Breadcrumb | breadcrumb.tsx | MuiBreadcrumb.kt | ✅ | |
| Pagination | pagination.tsx | MuiPagination.kt | ✅ | |
| NavigationMenu | navigation-menu.tsx | MuiNavigationMenu.kt | ✅ | |
| Command | command.tsx | MuiCommand.kt | ✅ | |
| Table | table.tsx | MuiTable.kt | ✅ | |
| ScrollArea | scroll-area.tsx | MuiScrollArea.kt | ✅ | |
| Carousel | carousel.tsx | MuiCarousel.kt | ✅ | HorizontalPager |
| Chart | chart.tsx | MuiChart.kt | ✅ | Canvas 三模式（TODO: 坐标轴/tooltip） |
| Resizable | resizable.tsx | MuiResizable.kt | ✅ | |
| Sidebar | sidebar.tsx | MuiSidebar.kt | ✅ | 折叠动画 |
| AspectRatio | aspect-ratio.tsx | MuiAspectRatio.kt | ✅ | |
| ButtonGroup | button-group.tsx | MuiButtonGroup.kt | ✅ | |
| Item | item.tsx | MuiItem.kt | ✅ | |
| Empty | empty.tsx | MuiEmpty.kt | ✅ | |
| Kbd | kbd.tsx | MuiKbd.kt | ✅ | |
| Field | field.tsx | MuiField.kt | ✅ | |
| Form | form.tsx | MuiForm.kt | ✅ | RHF 状态机不移植（文件头 TODO） |
| InputGroup | input-group.tsx | MuiInputGroup.kt | ✅ | |
| InputOTP | input-otp.tsx | MuiInputOtp.kt | ✅ | |
| Calendar | calendar.tsx | MuiCalendar.kt | ✅ | 单选月历 |
| Attachment | attachment.tsx | MuiAttachment.kt | ✅ | |
| Bubble | bubble.tsx | MuiBubble.kt | ✅ | |
| Message | message.tsx | MuiMessage.kt | ✅ | |
| MessageScroller | message-scroller.tsx | MuiMessageScroller.kt | ✅ | |
| Marker | marker.tsx | MuiMarker.kt | ✅ | |
| Direction | direction.tsx | MuiDirection.kt | ✅ | |
| Sonner(Toast) | sonner.tsx | MuiToast.kt | ✅ | |
| 像素猫 | mibu-cat.tsx + cat-pose.ts + hooks/use-mibu-cat-pose.ts | MibuCat.kt + CatPose.kt | ✅ | 三专用色 #17a89a/#47d1c1/#083b38 不分主题；姿态系统逐函数移植 |
| 红绿灯 | traffic-lights.tsx | MuiTrafficLights.kt | ✅ | 点击光斑铺满（全屏 Popup）→ 铺满瞬间提交 |

各文件头部的 `// TODO:` 行 = 未移植的次要 web 行为（键盘导航、RTL、断点响应等），改动对应组件时优先补齐。

## 三、showcase

| web | kotlin | 状态 |
| --- | --- | --- |
| `src/showcase/`（registry 65 入口 + 页面） | `kotlin/showcase/`（MainActivity 分区滚动，对齐 registry 分区顺序 + Palette/Tokens 演示） | ✅（演示为代表性 demo，未逐条复刻 65 入口的全部微变体） |

## 四、修改日志

| 日期 | 内容 | commit |
| --- | --- | --- |
| 2026-10-26 | 仓库重构为 web/ 双文件夹结构 | 6d1f1b6 |
| 2026-10-26 | kotlin 骨架 + 令牌生成 + AGSL 引擎 + 主题层 | 117bd8d |
| 2026-10-26 | 基础组件批 + 玻璃样板 Switch | 2366df4 |
| 2026-10-26 | 全量组件移植（63/63） | a36c9ab |
| 2026-10-26 | showcase 演示 app + 台账定稿 | 本次 |
