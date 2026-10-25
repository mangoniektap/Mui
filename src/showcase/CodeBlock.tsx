import { useEffect, useRef, useState } from "react";
import { Check, Copy } from "lucide-react";

import { cn } from "@/lib/utils";

/**
 * 代码块：等宽显示 + 一键复制。
 * 剪贴板 API 在非安全上下文（http / 本地 file）会拒绝，
 * 这时退回 textarea + execCommand 的老办法，两条路都不可用就静默失败。
 */
export function CodeBlock({
  code,
  lang = "tsx",
  className,
  maxHeight,
}: {
  code: string;
  lang?: string;
  className?: string;
  maxHeight?: string;
}) {
  const [copied, setCopied] = useState(false);
  const timerRef = useRef<number | null>(null);

  useEffect(() => {
    return () => {
      if (timerRef.current) window.clearTimeout(timerRef.current);
    };
  }, []);

  async function copy() {
    let ok = false;
    try {
      await navigator.clipboard.writeText(code);
      ok = true;
    } catch {
      const ta = document.createElement("textarea");
      ta.value = code;
      ta.style.position = "fixed";
      ta.style.opacity = "0";
      document.body.appendChild(ta);
      ta.select();
      try {
        ok = document.execCommand("copy");
      } catch {
        ok = false;
      }
      document.body.removeChild(ta);
    }
    if (!ok) return;
    setCopied(true);
    if (timerRef.current) window.clearTimeout(timerRef.current);
    timerRef.current = window.setTimeout(() => setCopied(false), 1600);
  }

  return (
    <div
      className={cn(
        "group/code relative overflow-hidden rounded-lg border bg-muted/40",
        className
      )}
    >
      <div className="flex items-center justify-between border-b bg-muted/60 px-3 py-1.5">
        <span className="font-mono text-[11px] text-muted-foreground">{lang}</span>
        <button
          type="button"
          onClick={copy}
          className="flex items-center gap-1 rounded-md px-1.5 py-1 text-[11px] text-muted-foreground transition-colors hover:bg-background hover:text-foreground"
          aria-label={copied ? "已复制" : "复制代码"}
        >
          {copied ? (
            <>
              <Check className="size-3.5 text-success" />
              已复制
            </>
          ) : (
            <>
              <Copy className="size-3.5" />
              复制
            </>
          )}
        </button>
      </div>
      <pre
        className="scrollbar-thin overflow-auto p-3 text-xs leading-relaxed"
        style={maxHeight ? { maxHeight } : undefined}
      >
        <code className="font-mono">{code}</code>
      </pre>
    </div>
  );
}
