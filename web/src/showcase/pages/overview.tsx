import { CircleCheck, FileCode2, Layers, Palette, Sparkles } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { CodeBlock } from "@/showcase/CodeBlock";

const stack = [
  "React 19.3.1",
  "TypeScript 7",
  "Tailwind CSS 4",
  "Vite 8",
  "Radix UI",
  "cva",
  "lucide-react",
];

const deps = [
  { name: "react / react-dom", version: "^19.3", usage: "运行时" },
  { name: "tailwindcss", version: "^4", usage: "样式引擎与设计令牌" },
  { name: "radix-ui", version: "^1.8", usage: "无头行为层（绝大多数组件）" },
  { name: "@base-ui/react", version: "^1.9", usage: "Combobox" },
  { name: "class-variance-authority", version: "^0.7", usage: "变体 / 尺寸 → class 查表" },
  { name: "lucide-react", version: "^1.61", usage: "图标" },
  {
    name: "按需依赖",
    version: "—",
    usage: "sonner / vaul / embla-carousel / recharts / react-day-picker / input-otp / react-hook-form + zod / cmdk 等，组件用到再装",
  },
];

export default function Overview() {
  return (
    <div className="space-y-6">
      <Card id="sec-intro" className="scroll-mt-20">
        <CardHeader>
          <div className="flex items-center gap-3">
            <div className="grid size-11 place-items-center rounded-xl bg-primary text-lg font-semibold text-primary-foreground">
              M
            </div>
            <div>
              <CardTitle className="text-lg">Mui 组件库</CardTitle>
              <p className="mt-0.5 text-sm text-muted-foreground">
                基于 shadcn/ui new-york 的 M 代号组件与设计令牌
              </p>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap gap-1.5">
            {stack.map((item) => (
              <Badge key={item} variant="secondary">
                {item}
              </Badge>
            ))}
          </div>
          <Separator />
          <div className="grid gap-3 text-sm text-muted-foreground sm:grid-cols-3">
            <div className="flex items-start gap-2">
              <CircleCheck className="mt-0.5 size-4 text-success" />
              <span>
                <span className="text-foreground">61 个组件</span>
                <br />
                全部来自官方 registry
              </span>
            </div>
            <div className="flex items-start gap-2">
              <Palette className="mt-0.5 size-4 text-primary" />
              <span>
                <span className="text-foreground">oklch 令牌</span>
                <br />
                亮 / 暗双主题
              </span>
            </div>
            <div className="flex items-start gap-2">
              <Layers className="mt-0.5 size-4 text-warning" />
              <span>
                <span className="text-foreground">左侧导航</span>
                <br />
                按类别快速定位
              </span>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card id="sec-install" className="scroll-mt-20">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <FileCode2 className="size-4 text-primary" />
            如何引入
          </CardTitle>
          <p className="text-sm text-muted-foreground">
            Mui 不是 npm 包，是仓库里的源码库：组件直接放在{" "}
            <code className="rounded bg-muted px-1.5 py-0.5 font-mono text-xs text-foreground">
              src/components/ui/
            </code>
            ，随项目一起编译。在本工作区用路径别名引入；搬到其他项目就按 shadcn
            的方式拷源码。
          </p>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="space-y-2">
            <h3 className="text-[13px] font-medium">环境要求</h3>
            <div className="overflow-hidden rounded-lg border">
              <table className="w-full text-left text-[13px]">
                <thead>
                  <tr className="border-b bg-muted/50 text-muted-foreground">
                    <th className="px-3 py-2 font-medium">依赖</th>
                    <th className="px-3 py-2 font-medium">版本</th>
                    <th className="px-3 py-2 font-medium">用途</th>
                  </tr>
                </thead>
                <tbody>
                  {deps.map((row) => (
                    <tr key={row.name} className="border-b last:border-0">
                      <td className="px-3 py-2 align-top font-mono text-xs font-medium whitespace-nowrap text-foreground">
                        {row.name}
                      </td>
                      <td className="px-3 py-2 align-top font-mono text-xs text-muted-foreground">
                        {row.version}
                      </td>
                      <td className="px-3 py-2 align-top text-muted-foreground">
                        {row.usage}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="space-y-2">
            <h3 className="text-[13px] font-medium">在本工作区引入组件</h3>
            <p className="text-sm text-muted-foreground">
              别名 <code className="rounded bg-muted px-1 py-0.5 font-mono text-xs text-foreground">@</code>{" "}
              指向 <code className="rounded bg-muted px-1 py-0.5 font-mono text-xs text-foreground">src/</code>
              ，每个组件一个文件，按需 import：
            </p>
            <CodeBlock
              lang="tsx"
              code={`import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { toast } from "sonner"`}
            />
          </div>

          <div className="space-y-2">
            <h3 className="text-[13px] font-medium">复制到其他项目</h3>
            <p className="text-sm text-muted-foreground">
              组件是纯源码（无内部包依赖），拷文件 + 装依赖即可用：
            </p>
            <CodeBlock
              lang="bash"
              code={`# 1. 拷贝组件源码与工具函数
cp src/components/ui/button.tsx <你的项目>/components/ui/
cp src/lib/utils.ts            <你的项目>/lib/

# 2. 安装依赖（按组件实际用到的装）
npm i radix-ui class-variance-authority clsx tailwind-merge lucide-react

# 3. 把 src/index.css 与 src/styles/ 下的令牌一并拷过去`}
            />
          </div>

          <div className="space-y-2">
            <h3 className="text-[13px] font-medium">引入设计令牌与主题</h3>
            <p className="text-sm text-muted-foreground">
              颜色、圆角、阴影、字体全部是 CSS 变量，集中在 src/index.css
              引入的令牌文件里；亮暗主题靠{" "}
              <code className="rounded bg-muted px-1 py-0.5 font-mono text-xs text-foreground">
                &lt;html class="dark"&gt;
              </code>{" "}
              切换，展示站顶栏右侧可以实时预览。
            </p>
            <CodeBlock
              lang="css"
              code={`@import "tailwindcss";
@import "./styles/tokens/colors.css";   /* oklch 颜色令牌 */
@import "./styles/tokens/radius.css";
@import "./styles/tokens/typography.css";
@import "./styles/tokens/shadow.css";`}
            />
          </div>

          <div className="space-y-2">
            <h3 className="text-[13px] font-medium">cn 工具函数</h3>
            <p className="text-sm text-muted-foreground">
              合并类名（clsx + tailwind-merge，后写的 Tailwind 类胜出）：
            </p>
            <CodeBlock
              lang="tsx"
              code={`import { cn } from "@/lib/utils"

<div className={cn("px-2 text-sm", isActive && "bg-primary text-white")} />`}
            />
          </div>
        </CardContent>
      </Card>

      <Card id="sec-usage" className="scroll-mt-20">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Sparkles className="size-4 text-primary" />
            怎么改
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm text-muted-foreground">
          <p>
            1. 组件源码在{" "}
            <code className="rounded bg-muted px-1.5 py-0.5 font-mono text-xs text-foreground">
              src/components/ui/
            </code>
            ，直接改。
          </p>
          <p>
            2. 颜色、圆角、字体在{" "}
            <code className="rounded bg-muted px-1.5 py-0.5 font-mono text-xs text-foreground">
              src/index.css
            </code>{" "}
            的{" "}
            <code className="rounded bg-muted px-1.5 py-0.5 font-mono text-xs text-foreground">
              :root
            </code>{" "}
            与{" "}
            <code className="rounded bg-muted px-1.5 py-0.5 font-mono text-xs text-foreground">
              .dark
            </code>{" "}
            里，全是 oklch 变量。
          </p>
          <p>3. 保存后 Vite 热更新，右侧展示区立刻反映效果，不用重启。</p>
        </CardContent>
      </Card>
    </div>
  );
}
