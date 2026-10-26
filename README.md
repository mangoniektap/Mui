# Mui

统一 M11BU / MREAD / Mcoc 前端视觉的自建 UI 库，**双语言实现，视觉同源**：

| 目录 | 语言 | 形态 |
| --- | --- | --- |
| `web/` | React 19 + TS + Tailwind 4 + Vite | shadcn 组件 + oklch 令牌 + WebGL 液态玻璃引擎 |
| `kotlin/` | Kotlin + Jetpack Compose | Android library（`mui-ui`）+ 演示 app（`showcase`） |

两套实现共享同一套设计语言：同一组令牌、同一批组件、同一种玻璃质感。

## 仓库结构

```
Mui/
├── README.md          # 本文件：双套说明 + 同步规则
├── SYNC.md            # 同步台账：令牌/组件映射表 + 状态
├── git_tool.bat/.ps1  # git 快捷操作（双击出菜单）
├── 启动.bat           # 双击启动 web 版 dev server
├── web/               # React 实现（dev server 在 web/ 下启动）
└── kotlin/            # Kotlin 实现（mui-ui 库 + showcase 演示 app）
```

## 同步规则（重要）

1. **任何令牌 / 样式 / 组件的修改，web 与 kotlin 两侧必须同一批提交都改**，commit message 注明 `web+kt`。
2. 改动后更新 `SYNC.md` 里对应条目的状态与备注。
3. 命名一一对应：CSS 变量 `--brand-strong` ↔ Kotlin `MuiColors.brandStrong`；web 组件 `switch.tsx` ↔ Kotlin `MuiSwitch.kt`。对应关系以 `SYNC.md` 为准。
4. 组件视觉形态以现有定稿为准（Switch 胶囊拇指、ToggleGroup 白胶囊底座 + 透明玻璃滑块、像素猫三专用色、红绿灯自发光色等），两侧都不重新设计。

## 常用命令

```bash
# web
cd web && npm run dev        # 开发
cd web && npm run build      # 构建产物

# kotlin（需 Android Studio 或本机 JDK+SDK）
cd kotlin && ./gradlew :showcase:installDebug
```
