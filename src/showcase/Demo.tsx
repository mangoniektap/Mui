import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/lib/utils";

const demoVariants = cva("flex flex-wrap gap-3", {
  variants: {
    layout: {
      row: "items-center",
      col: "flex-col items-stretch",
      wrap: "items-start",
      grid: "grid grid-cols-1 gap-4 sm:grid-cols-2",
    },
  },
  defaultVariants: {
    layout: "row",
  },
});

export function Demo({
  title,
  hint,
  layout,
  overflow = "hidden",
  className,
  children,
}: {
  title: string;
  hint?: string;
  layout?: VariantProps<typeof demoVariants>["layout"];
  /**
   * 卡片默认裁剪溢出，避免圆角处的尖角内容。
   * 少数“弹出层长在自身文档流里”的组件（如 NavigationMenu 的 viewport）
   * 会被 overflow-hidden 切掉，这类 demo 传 "visible"。
   */
  overflow?: "hidden" | "visible";
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <section
      className={cn(
        "mb-6 rounded-xl border bg-card shadow-xs",
        overflow === "hidden" ? "overflow-hidden" : "overflow-visible"
      )}
    >
      <div className="flex items-baseline justify-between gap-4 border-b px-4 py-2.5">
        <h2 className="text-[13px] font-medium">{title}</h2>
        {hint ? (
          <span className="font-mono text-[11px] text-muted-foreground">
            {hint}
          </span>
        ) : null}
      </div>
      <div className={cn("p-5", demoVariants({ layout }), className)}>
        {children}
      </div>
    </section>
  );
}

export function Label({ children }: { children: React.ReactNode }) {
  return (
    <span className="mb-1.5 block font-mono text-[11px] text-muted-foreground">
      {children}
    </span>
  );
}
