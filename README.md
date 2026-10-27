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

#### 使用时拉取代码

```bash
# ============ Android 工程 ============
git submodule add --depth 1 <https://github.com/mangoniektap/Mui.git> libs/mui    # 挂子模块，只拉最新一层历史
git -C libs/mui sparse-checkout init --cone         # 开启按目录过滤
git -C libs/mui sparse-checkout set kotlin          # 只摊开 kotlin/，web/ 与 tools/ 收起

git submodule update --remote libs/mui              # 更新：拉组件库最新版本
git -C libs/mui sparse-checkout set kotlin          # 更新后重套过滤

# ============ Web 工程 ============
git submodule add --depth 1 <https://github.com/mangoniektap/Mui.git> libs/mui    # 挂子模块，只拉最新一层历史
git -C libs/mui sparse-checkout init --cone         # 开启按目录过滤
git -C libs/mui sparse-checkout set web/src         # 只摊开 web/src/，kotlin/ 与 tools/ 收起

git submodule update --remote libs/mui              # 更新：拉组件库最新版本
git -C libs/mui sparse-checkout set web/src         # 更新后重套过滤
```

### 如何使用

#### KT侧

`settings.gradle.kts`（放在 `include(":app")` 之后）：

```kotlin
includeBuild("libs/mui/kotlin") {        // 指向子模块里的 kotlin 目录
    dependencySubstitution {
        substitute(module("com.mui:mui-ui")).using(project(":mui-ui"))
    }
}   // 把对外部坐标 com.mui:mui-ui 的依赖请求，换成编译 libs/mui/kotlin 这个源码工程。
```

`app/build.gradle.kts`：

```kotlin
dependencies {
    implementation("com.mui:mui-ui:0.1.0")   // 版本号随便写，会被上面的 substitution 替换掉
}
```

#### React侧

`vite.config.ts`（精确匹配放前面，catch-all `@` 放最后）：

```ts
import { fileURLToPath, URL } from "node:url"

const mui = fileURLToPath(new URL("./libs/mui/web/src", import.meta.url))

export default defineConfig({
  resolve: {
    alias: [
      { find: /^@\/components\/ui\//, replacement: mui + "/components/ui/" },
      { find: /^@\/styles\//,         replacement: mui + "/styles/" },
      { find: /^@\/hooks\//,          replacement: mui + "/hooks/" },
      { find: /^@\/lib\/utils$/,      replacement: mui + "/lib/utils" },
      { find: "@", replacement: fileURLToPath(new URL("./src", import.meta.url)) },
    ],
  },
})  // 你工程若已有 @ → ./src 的别名，保留它、把前四条加在它前面即可。
```

`tsconfig.json`（同理，具体的放前面）：

```json
{
  "compilerOptions": {
    "paths": {
      "@/components/ui/*": ["./libs/mui/web/src/components/ui/*"],
      "@/styles/*":        ["./libs/mui/web/src/styles/*"],
      "@/hooks/*":         ["./libs/mui/web/src/hooks/*"],
      "@/lib/utils":       ["./libs/mui/web/src/lib/utils.ts"],
      "@/*":               ["./src/*"]
    }
  }
}
```

##### 入口 CSS

（你的 `src/index.css`，路径按你入口文件位置调整——下面假设入口在 `src/` 下）：

```css
@import "tailwindcss";
@import "tw-animate-css";

/* ↓ 抄自 Mui 的 index.css 第 7-14 行，去掉了它自带的 tailwind/tw-animate 两行 */
@import "../libs/mui/web/src/styles/tokens/colors.css";
@import "../libs/mui/web/src/styles/tokens/radius.css";
@import "../libs/mui/web/src/styles/tokens/typography.css";
@import "../libs/mui/web/src/styles/tokens/shadow.css";
@import "../libs/mui/web/src/styles/tokens/glass.css";
@import "../libs/mui/web/src/styles/mibu-cat.css";
@import "../libs/mui/web/src/styles/animations.css";
@import "../libs/mui/web/src/styles/base.css";

/* 让 Tailwind 扫到子模块里的 className，否则组件全掉样式 */
@source "../libs/mui/web/src";
```

kt：以后库更新只要 `submodule update`，gradle 自动重编，配置不动。
web：Mui 若新增了顶层 CSS 文件（比如再加一个 `foo.css`），你这条 import 列表要补一行；若只是改组件/令牌内容，不动配置。这就是 web 端唯一的「维护点」。
