# SYNC.md — 双语言同步台账

> 规则：令牌/样式/组件任何修改，web 与 kotlin 同一批提交都改，commit 注明 `web+kt`，并更新本表。
> 命名对应：`--foo-bar` ↔ `FooBar`/`fooBar`（Kotlin 常量/参数），`button.tsx` ↔ `MuiButton.kt`。

## 一、令牌映射

| 语义 | web（CSS 变量） | kotlin（Kotlin 常量） | 状态 |
| --- | --- | --- | --- |
| 颜色（亮/暗） | `tokens/colors.css` 全部变量 | `MuiColors.kt`（oklch→sRGB 脚本生成） | 待生成 |
| 圆角 | `tokens/radius.css` | `MuiRadius.kt` | 待生成 |
| 阴影 | `tokens/shadow.css`（`--elevation-*`） | `MuiElevation.kt` | 待生成 |
| 字体 | `tokens/typography.css` | `MuiTypography.kt` | 待生成 |
| 玻璃 | `tokens/glass.css`（jade/gray 双通道、lens-chroma 等） | `GlassTokens.kt` | 待生成 |
| 玻璃引擎 | `surfaces/liquid-glass.tsx`（WebGL SDF） | `glass/GlassEngine.kt`（AGSL） | 待移植 |
| 实色面 | `surfaces/classical.ts` | `glass/Classical.kt` | 待移植 |

## 二、组件映射

状态说明：✅ 已同步 / ⏳ 两侧有差异待对齐。

| 组件 | web | kotlin | 状态 |
| --- | --- | --- | --- |
| Button | `button.tsx` | `MuiButton.kt` | 待移植 |
| Input | `input.tsx` | `MuiInput.kt` | 待移植 |
| Textarea | `textarea.tsx` | `MuiTextarea.kt` | 待移植 |
| Label | `label.tsx` | `MuiLabel.kt` | 待移植 |
| Badge | `badge.tsx` | `MuiBadge.kt` | 待移植 |
| Card | `card.tsx` | `MuiCard.kt` | 待移植 |
| Separator | `separator.tsx` | `MuiSeparator.kt` | 待移植 |
| Avatar | `avatar.tsx` | `MuiAvatar.kt` | 待移植 |
| Skeleton | `skeleton.tsx` | `MuiSkeleton.kt` | 待移植 |
| Spinner | `spinner.tsx` | `MuiSpinner.kt` | 待移植 |
| Switch | `switch.tsx` | `MuiSwitch.kt` | 待移植 |
| Toggle | `toggle.tsx` | `MuiToggle.kt` | 待移植 |
| ToggleGroup | `toggle-group.tsx` | `MuiToggleGroup.kt` | 待移植 |
| Checkbox | `checkbox.tsx` | `MuiCheckbox.kt` | 待移植 |
| RadioGroup | `radio-group.tsx` | `MuiRadioGroup.kt` | 待移植 |
| Slider | `slider.tsx` | `MuiSlider.kt` | 待移植 |
| Tabs | `tabs.tsx` | `MuiTabs.kt` | 待移植 |
| Select | `select.tsx` | `MuiSelect.kt` | 待移植 |
| Combobox | `combobox.tsx` | `MuiCombobox.kt` | 待移植 |
| Picker | `picker.tsx` | `MuiPicker.kt` | 待移植 |
| Dialog | `dialog.tsx` | `MuiDialog.kt` | 待移植 |
| AlertDialog | `alert-dialog.tsx` | `MuiAlertDialog.kt` | 待移植 |
| Sheet | `sheet.tsx` | `MuiSheet.kt` | 待移植 |
| Drawer | `drawer.tsx` | `MuiDrawer.kt` | 待移植 |
| Popover | `popover.tsx` | `MuiPopover.kt` | 待移植 |
| Tooltip | `tooltip.tsx` | `MuiTooltip.kt` | 待移植 |
| HoverCard | `hover-card.tsx` | `MuiHoverCard.kt` | 待移植 |
| DropdownMenu | `dropdown-menu.tsx` | `MuiDropdownMenu.kt` | 待移植 |
| ContextMenu | `context-menu.tsx` | `MuiContextMenu.kt` | 待移植 |
| Menubar | `menubar.tsx` | `MuiMenubar.kt` | 待移植 |
| Alert | `alert.tsx` | `MuiAlert.kt` | 待移植 |
| AlertDialog 以外的确认 | `alert-dialog.tsx` | 同上 | — |
| Progress | `progress.tsx` | `MuiProgress.kt` | 待移植 |
| Accordion | `accordion.tsx` | `MuiAccordion.kt` | 待移植 |
| Collapsible | `collapsible.tsx` | `MuiCollapsible.kt` | 待移植 |
| Breadcrumb | `breadcrumb.tsx` | `MuiBreadcrumb.kt` | 待移植 |
| Pagination | `pagination.tsx` | `MuiPagination.kt` | 待移植 |
| NavigationMenu | `navigation-menu.tsx` | `MuiNavigationMenu.kt` | 待移植 |
| Command | `command.tsx` | `MuiCommand.kt` | 待移植 |
| Table | `table.tsx` | `MuiTable.kt` | 待移植 |
| ScrollArea | `scroll-area.tsx` | `MuiScrollArea.kt` | 待移植 |
| Carousel | `carousel.tsx` | `MuiCarousel.kt` | 待移植 |
| Chart | `chart.tsx` | `MuiChart.kt` | 待移植 |
| Resizable | `resizable.tsx` | `MuiResizable.kt` | 待移植 |
| Sidebar | `sidebar.tsx` | `MuiSidebar.kt` | 待移植 |
| AspectRatio | `aspect-ratio.tsx` | `MuiAspectRatio.kt` | 待移植 |
| ButtonGroup | `button-group.tsx` | `MuiButtonGroup.kt` | 待移植 |
| Item | `item.tsx` | `MuiItem.kt` | 待移植 |
| Empty | `empty.tsx` | `MuiEmpty.kt` | 待移植 |
| Kbd | `kbd.tsx` | `MuiKbd.kt` | 待移植 |
| Field | `field.tsx` | `MuiField.kt` | 待移植 |
| Form | `form.tsx` | `MuiForm.kt` | 待移植 |
| InputGroup | `input-group.tsx` | `MuiInputGroup.kt` | 待移植 |
| InputOTP | `input-otp.tsx` | `MuiInputOtp.kt` | 待移植 |
| Calendar | `calendar.tsx` | `MuiCalendar.kt` | 待移植 |
| Attachment | `attachment.tsx` | `MuiAttachment.kt` | 待移植 |
| Bubble | `bubble.tsx` | `MuiBubble.kt` | 待移植 |
| Message | `message.tsx` | `MuiMessage.kt` | 待移植 |
| MessageScroller | `message-scroller.tsx` | `MuiMessageScroller.kt` | 待移植 |
| Marker | `marker.tsx` | `MuiMarker.kt` | 待移植 |
| Direction | `direction.tsx` | `MuiDirection.kt` | 待移植 |
| Sonner(Toast) | `sonner.tsx` | `MuiToast.kt` | 待移植 |
| 像素猫 | `mibu-cat.tsx` | `MibuCat.kt` | 待移植 |
| 红绿灯 | `traffic-lights.tsx` | `TrafficLights.kt` | 待移植 |

## 三、修改日志

| 日期 | 内容 | commit |
| --- | --- | --- |
| 2026-10-26 | 仓库重构为 web/ 双文件夹结构 | 待填 |
