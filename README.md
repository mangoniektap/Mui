# 禁止非开发者修改

## 此项目包括基于以下技术栈的Mui视觉组件

| 目录 | 技术栈 | 内容 |
| --- | --- | --- |
| `web/` | React · TypeScript · Tailwind · Vite · radix-ui · @base-ui/react | oklch 令牌 + 焦点环 + lucide 图标 |
| `kotlin/` | Compose · Compose BOM · AGP · Gradle | Android library `mui-ui`（compileSdk / minSdk） |

```
Mui/
├── README.md               # 产品说明
├── git.bat/.ps1            # git管理工具
├── start.bat               # 启动web预览
├── web/                    # React 实现
└── kotlin/                 # Kotlin 实现
```

### 如何管理

#### 组件库仓库第一次上传

```bash
git init                              # 把当前目录变成 git 仓库（已有 .git 就跳过）
git remote add origin <https://github.com/mangoniektap/Mui.git>        # 记住远端地址（https 或 git@ 都行）
git branch -M main                    # 把当前分支改名为 main
git add -A                            # 把所有文件放进暂存区（先确认 .gitignore 排掉了 node_modules / build）
git commit -m "init: Mui 组件库"        # 在本地记一个版本
git push -u origin main               # 推上去；-u 让以后直接 git push 就行
```

#### 组件库日常改完上传

```bash
git status                            # 看改了哪些，检查有没有被误标删除的文件
git add -A                            # 暂存全部改动（只想提交部分就把 -A 换成具体路径）
git commit -m "feat: 说明改了什么"     # 本地记账
git push origin main                  # 推到 GitHub，消费方才拉得到
```

#### 登记子模块

在接入项目的 Git 根目录执行一次。将本仓库的 [同步脚本](integrations/sync-mui.sh) 保存为接入项目的 `scripts/sync-mui.sh`，并随项目提交。Windows 使用 Git for Windows 随附的 shell。

```bash
git submodule add -b main https://github.com/mangoniektap/Mui.git libs/mui
git -C libs/mui checkout --detach
git add .gitmodules libs/mui scripts/sync-mui.sh .gitattributes
```

在接入项目的 `.gitattributes` 中添加 `scripts/sync-mui.sh text eol=lf`。已接入的项目可以沿用现有子模块路径 `libs/mui`。旧配置如果使用了 sparse checkout 或浅克隆，请执行 `git -C libs/mui sparse-checkout disable`，并仅在浅克隆时执行 `git -C libs/mui fetch --unshallow origin`。

构建会自动初始化普通克隆中尚未检出的子模块，使用 Git SHA 检查 `origin/main`，有更新时快进到新提交。脚本会显示实际使用的 SHA。子模块含本地改动、检出开发分支、提交无法快进或 Git 操作失败时，构建会报错。需要记录已验证的版本时，执行 `git add libs/mui` 并提交。发布到 Mui 的 `main` 前，请验证 Web 和 Android 构建，以便接入项目直接使用最新提交。

### Android 接入

让 `ANDROID_HOME` 指向安装了 Mui 所需 compileSdk 的 Android SDK，供接入项目和包含构建使用。

将下方代码添加到接入项目的 `settings.gradle.kts`。放在 `pluginManagement` 和 `plugins` 块之后、引用 Mui 的位置之前。Android Studio 和 Gradle 命令行共用这个设置。

```kotlin
val muiSync = providers.exec {
    workingDir(settingsDir)
    commandLine("git", "-c", "alias.mui-sync=!sh ./scripts/sync-mui.sh", "mui-sync", "android")
    isIgnoreExitValue = true
}
val muiRevision = muiSync.standardOutput.asText.get().trim()
if (muiSync.result.get().exitValue != 0) {
    throw GradleException(muiSync.standardError.asText.get().trim())
}
println("[Mui] $muiRevision")

includeBuild("libs/mui/kotlin") {
    dependencySubstitution {
        substitute(module("com.mui:mui-ui")).using(project(":mui-ui"))
    }
}
```

在应用模块的 `build.gradle.kts` 中添加依赖。

```kotlin
dependencies {
    implementation("com.mui:mui-ui:0.1.0")
}
```

### Web 接入

在接入项目的 `package.json` 中配置构建钩子。

```json
{
  "scripts": {
    "mui:sync": "git -c \"alias.mui-sync=!sh ./scripts/sync-mui.sh\" mui-sync web",
    "predev": "npm run mui:sync",
    "dev": "vite",
    "prebuild": "npm run mui:sync",
    "build": "tsc && vite build"
  }
}
```

首次运行或 Mui 的 npm 依赖配置更新时，脚本执行 `npm ci` 安装 Mui Web 的依赖。Vite 从子模块读取组件，和应用共用 React 与 React DOM。精确别名需要放在应用的 `@` 别名前。

```ts
import { fileURLToPath, URL } from "node:url"

const mui = fileURLToPath(new URL("./libs/mui/web/src", import.meta.url))

// vite.config.ts 中的 resolve
const resolve = {
  dedupe: ["react", "react-dom"],
  alias: [
    { find: /^@\/components\/ui\//, replacement: mui + "/components/ui/" },
    { find: /^@\/styles\//, replacement: mui + "/styles/" },
    { find: /^@\/hooks\//, replacement: mui + "/hooks/" },
    { find: /^@\/lib\/utils$/, replacement: mui + "/lib/utils.ts" },
    { find: "@", replacement: fileURLToPath(new URL("./src", import.meta.url)) },
  ],
}
```

在 `tsconfig.json` 的 `compilerOptions.paths` 中配置相同的路径。应用如有自己的 hooks 或业务控件，请使用应用内的相对导入或单独的别名。

```json
{
  "@/components/ui/*": ["./libs/mui/web/src/components/ui/*"],
  "@/styles/*": ["./libs/mui/web/src/styles/*"],
  "@/hooks/*": ["./libs/mui/web/src/hooks/*"],
  "@/lib/utils": ["./libs/mui/web/src/lib/utils.ts"],
  "@/*": ["./src/*"]
}
```

在应用的 `src/index.css` 开头载入 Mui 样式，并显式扫描组件源码。

```css
@import "../libs/mui/web/src/index.css";
@source "../libs/mui/web/src/components/ui";
@source "../libs/mui/web/src/styles/surfaces";
```
