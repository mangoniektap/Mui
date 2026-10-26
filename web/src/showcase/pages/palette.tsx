import { useState } from "react";
import { Check, Copy } from "lucide-react";

import { cn } from "@/lib/utils";
import { Demo } from "@/showcase/Demo";

const BRAND_STEPS = [
  50, 100, 200, 300, 400, 500, 600, 700, 800, 900,
].map((n) => `--mui-${n}` as `--mui-${number}`);

const UI_TOKENS = [
  ["--background", "页面底色"],
  ["--foreground", "正文颜色"],
  ["--card", "卡片底色"],
  ["--muted", "次级底色"],
  ["--muted-foreground", "次级文字"],
  ["--accent", "悬浮/选中"],
  ["--border", "边框"],
  ["--input", "输入框边框"],
  ["--ring", "聚焦环"],
  ["--primary", "主色"],
  ["--secondary", "次要色"],
  ["--destructive", "危险色"],
] as const;

const SEMANTIC = [
  ["--success", "成功"],
  ["--warning", "警告"],
  ["--info", "信息"],
  ["--destructive", "错误"],
] as const;

function useCopy() {
  const [copied, setCopied] = useState<string | null>(null);
  const copy = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(text);
      window.setTimeout(() => setCopied(null), 1200);
    } catch {
      setCopied(null);
    }
  };
  return { copied, copy };
}

export default function PalettePage() {
  const { copied, copy } = useCopy();

  return (
    <>
      <Demo title="品牌色 Mui Indigo" hint="10 阶 · 点击复制变量">
        <div className="grid w-full grid-cols-5 gap-2 md:grid-cols-10">
          {BRAND_STEPS.map((token) => (
            <button
              key={token}
              onClick={() => copy(`var(${token})`)}
              className="group overflow-hidden rounded-lg border text-left transition-transform hover:-translate-y-0.5"
            >
              <div
                className="h-14 w-full"
                style={{ background: `var(${token})` }}
              />
              <div className="flex items-center justify-between px-1.5 py-1">
                <span className="font-mono text-[10px]">
                  {token.replace("--mui-", "")}
                </span>
                {copied === `var(${token})` ? (
                  <Check className="size-3 text-success" />
                ) : (
                  <Copy className="size-3 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100" />
                )}
              </div>
            </button>
          ))}
        </div>
      </Demo>

      <Demo title="语义色" hint="success / warning / info / destructive">
        <div className="grid w-full grid-cols-2 gap-3 md:grid-cols-4">
          {SEMANTIC.map(([token, label]) => (
            <button
              key={token}
              onClick={() => copy(`var(${token})`)}
              className="rounded-lg border p-3 text-left transition-transform hover:-translate-y-0.5"
              style={{
                background: `color-mix(in oklab, var(${token}) 12%, transparent)`,
                borderColor: `color-mix(in oklab, var(${token}) 35%, transparent)`,
              }}
            >
              <div
                className="text-sm font-medium"
                style={{ color: `var(${token})` }}
              >
                {label}
              </div>
              <div className="mt-1 font-mono text-[10px] text-muted-foreground">
                {token}
              </div>
            </button>
          ))}
        </div>
      </Demo>

      <Demo title="界面令牌" hint="semantic tokens">
        <div className="grid w-full grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
          {UI_TOKENS.map(([token, desc]) => (
            <button
              key={token}
              onClick={() => copy(`var(${token})`)}
              className="flex items-center gap-2.5 rounded-lg border bg-background px-2.5 py-2 text-left transition-colors hover:bg-accent"
            >
              <span
                className="size-7 shrink-0 rounded-md border"
                style={{ background: `var(${token})` }}
              />
              <span className="min-w-0">
                <span className="block truncate font-mono text-[10px]">
                  {token}
                </span>
                <span className="block truncate text-[10px] text-muted-foreground">
                  {desc}
                </span>
              </span>
            </button>
          ))}
        </div>
      </Demo>

      <Demo title="图表色" hint="chart-1 … chart-5" layout="col">
        <div className="flex w-full gap-2">
          {[1, 2, 3, 4, 5].map((n) => (
            <div key={n} className="flex-1">
              <div
                className="h-16 rounded-md"
                style={{ background: `var(--chart-${n})` }}
              />
              <div className="mt-1 text-center font-mono text-[10px] text-muted-foreground">
                chart-{n}
              </div>
            </div>
          ))}
        </div>
      </Demo>

      <Demo title="使用方式" hint="Tailwind 4" layout="col">
        <pre className="w-full overflow-x-auto rounded-lg border bg-muted/40 p-4 font-mono text-xs leading-6">
{`<div className="bg-background text-foreground">
  <button className="bg-primary text-primary-foreground">主要按钮</button>
  <p className="text-muted-foreground">说明文字</p>
  <div className="border border-border" />
</div>`}
        </pre>
        <p className={cn("text-xs text-muted-foreground")}>
          改配色只需动 index.css 里的 oklch 变量，组件不用改。
        </p>
      </Demo>
    </>
  );
}
