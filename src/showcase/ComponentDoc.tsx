import type { ComponentDoc } from "@/showcase/docs";
import { CodeBlock } from "@/showcase/CodeBlock";
import { cn } from "@/lib/utils";

function DocHeading({
  id,
  children,
}: {
  id: string;
  children: React.ReactNode;
}) {
  return (
    <h2
      id={id}
      className="scroll-mt-20 text-base font-semibold tracking-tight"
    >
      {children}
    </h2>
  );
}

/**
 * 组件文档块：组件说明 / 调用方式 / 调用示例。
 * 渲染在每个组件页面的展示区之后，锚点 id 与右侧「本页目录」对应。
 */
export function ComponentDoc({ doc }: { doc: ComponentDoc }) {
  return (
    <section className="mt-10 space-y-8 border-t pt-8">
      <div className="space-y-3">
        <DocHeading id="doc-about">组件说明</DocHeading>
        <p className="max-w-2xl text-sm leading-relaxed text-muted-foreground">
          {doc.summary}
        </p>
        <CodeBlock code={doc.import} lang="tsx" className="max-w-2xl" />
      </div>

      <div className="space-y-3">
        <DocHeading id="doc-api">调用方式</DocHeading>
        <div className="overflow-hidden rounded-lg border">
          <table className="w-full text-left text-[13px]">
            <thead>
              <tr className="border-b bg-muted/50 text-muted-foreground">
                <th className="px-3 py-2 font-medium">参数</th>
                <th className="px-3 py-2 font-medium">类型</th>
                <th className="px-3 py-2 font-medium">默认值</th>
                <th className="px-3 py-2 font-medium">说明</th>
              </tr>
            </thead>
            <tbody>
              {doc.api.map((row) => (
                <tr key={row.name} className="border-b last:border-0">
                  <td className="px-3 py-2 align-top font-mono text-xs font-medium whitespace-nowrap text-foreground">
                    {row.name}
                  </td>
                  <td className="px-3 py-2 align-top font-mono text-xs text-muted-foreground">
                    {row.type}
                  </td>
                  <td className="px-3 py-2 align-top font-mono text-xs whitespace-nowrap text-muted-foreground">
                    {row.default ?? "—"}
                  </td>
                  <td className="px-3 py-2 align-top text-muted-foreground">
                    {row.desc}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="space-y-5">
        <DocHeading id="doc-usage">调用示例</DocHeading>
        {doc.examples.map((example) => (
          <div key={example.title} className="space-y-2">
            <h3 className="text-[13px] font-medium text-foreground">
              {example.title}
            </h3>
            <CodeBlock code={example.code} />
          </div>
        ))}
      </div>
    </section>
  );
}

/** 「本页目录」的小标题样式，总览页与组件页共用 */
export function TocTitle() {
  return (
    <div className="mb-2 text-[11px] font-medium tracking-wider text-muted-foreground uppercase">
      本页目录
    </div>
  );
}

/** 目录项：左侧竖线指示当前节 */
export function TocLink({
  active,
  href,
  children,
}: {
  active: boolean;
  href: string;
  children: React.ReactNode;
}) {
  return (
    <a
      href={href}
      className={cn(
        "-ml-px block border-l-2 py-1 pl-3 text-[13px] transition-colors",
        active
          ? "border-primary font-medium text-foreground"
          : "border-transparent text-muted-foreground hover:text-foreground"
      )}
    >
      {children}
    </a>
  );
}
