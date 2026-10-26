import { useEffect, useMemo, useState } from "react";

import { Sidebar } from "@/showcase/Sidebar";
import { TopBar } from "@/showcase/TopBar";
import { TocRail, type TocItem } from "@/showcase/TocRail";
import { ComponentDoc } from "@/showcase/ComponentDoc";
import { componentDocs } from "@/showcase/docs";
import { cn } from "@/lib/utils";
import { GROUP_ORDER, sections } from "@/showcase/registry";

export default function App() {
  const [activeId, setActiveId] = useState("overview");
  const [query, setQuery] = useState("");

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return sections;
    return sections.filter((s) =>
      `${s.label} ${s.en} ${s.id} ${s.group}`.toLowerCase().includes(q)
    );
  }, [query]);

  const groups = useMemo(
    () =>
      GROUP_ORDER.map((group) => ({
        group,
        items: filtered.filter((s) => s.group === group),
      })).filter((g) => g.items.length > 0),
    [filtered]
  );

  const active = sections.find((s) => s.id === activeId) ?? sections[0];
  const Active = active.render;

  /** 组件页才有文档块（总览 / 配色 / 图标 / 令牌不是组件） */
  const doc = componentDocs[active.id];

  const toc = useMemo<TocItem[]>(() => {
    if (active.id === "overview") {
      return [
        { id: "sec-intro", label: "库信息" },
        { id: "sec-install", label: "如何引入" },
        { id: "sec-usage", label: "怎么改" },
      ];
    }
    if (!doc) return [];
    return [
      { id: "sec-demo", label: "展示" },
      { id: "doc-about", label: "组件说明" },
      { id: "doc-api", label: "调用方式" },
      { id: "doc-usage", label: "调用示例" },
    ];
  }, [active.id, doc]);

  useEffect(() => {
    document.title = `Mui · ${active.label}`;
    window.scrollTo({ top: 0 });
  }, [active]);

  const hasRail = toc.length > 0;

  return (
    <div className="min-h-screen">
      <TopBar query={query} onQueryChange={setQuery} />
      <div className="flex">
        <Sidebar
          groups={groups}
          activeId={active.id}
          onSelect={setActiveId}
        />
        <main className="min-w-0 flex-1">
          <div className="mx-auto w-full max-w-[68rem] px-6 py-10 md:px-10">
            <div
              className={cn(
                hasRail && "xl:grid xl:grid-cols-[minmax(0,1fr)_10rem] xl:gap-12"
              )}
            >
              <div className={cn("min-w-0", !hasRail && "max-w-4xl")}>
                <header className="mb-8 flex flex-col gap-2">
                  <div className="flex items-center gap-3">
                    <h1 className="text-2xl font-semibold tracking-tight">
                      {active.label}
                    </h1>
                    <span className="rounded-md border bg-muted/50 px-1.5 py-0.5 font-mono text-[11px] text-muted-foreground">
                      {active.en}
                    </span>
                  </div>
                  <p className="max-w-2xl text-sm text-muted-foreground">
                    {active.desc}
                  </p>
                </header>
                <div id="sec-demo" className="scroll-mt-20">
                  <Active />
                </div>
                {doc ? <ComponentDoc doc={doc} /> : null}
              </div>
              <TocRail items={toc} />
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
