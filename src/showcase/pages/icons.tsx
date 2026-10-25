import { useMemo, useState } from "react";
import { Check, Search, icons, type LucideIcon } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

const ALL = (Object.entries(icons) as [string, LucideIcon][])
  .map(([name, Icon]) => ({ name, Icon }))
  .sort((a, b) => a.name.localeCompare(b.name));

const PAGE_SIZE = 200;

export default function IconsPage() {
  const [query, setQuery] = useState("");
  const [limit, setLimit] = useState(PAGE_SIZE);
  const [copied, setCopied] = useState<string | null>(null);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return ALL;
    return ALL.filter((item) => item.name.toLowerCase().includes(q));
  }, [query]);

  const visible = filtered.slice(0, limit);

  const copy = async (name: string) => {
    try {
      await navigator.clipboard.writeText(`<${name} />`);
      setCopied(name);
      window.setTimeout(() => setCopied(null), 1200);
    } catch {
      setCopied(null);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative w-full max-w-xs">
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setLimit(PAGE_SIZE);
            }}
            placeholder="搜索图标名，如 arrow…"
            className="pl-9"
          />
        </div>
        <span className="text-xs text-muted-foreground">
          {filtered.length} / {ALL.length} 个图标 · 点击复制 JSX
        </span>
        {copied ? (
          <span className="text-xs text-success">已复制 &lt;{copied} /&gt;</span>
        ) : null}
      </div>

      <div className="grid grid-cols-4 gap-2 sm:grid-cols-6 md:grid-cols-8 xl:grid-cols-10">
        {visible.map(({ name, Icon }) => (
          <button
            key={name}
            onClick={() => copy(name)}
            title={name}
            className="group flex flex-col items-center gap-2 rounded-lg border bg-card px-1.5 py-2.5 transition-colors hover:border-ring/40 hover:bg-accent"
          >
            <Icon className="size-5 text-foreground" />
            <span className="w-full truncate text-center text-[10px] text-muted-foreground">
              {name}
            </span>
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <p className="py-10 text-center text-sm text-muted-foreground">
          没有匹配的图标
        </p>
      ) : null}

      {limit < filtered.length ? (
        <div className="flex flex-col items-center gap-2 py-4">
          <Button variant="outline" onClick={() => setLimit((n) => n + PAGE_SIZE)}>
            加载更多
          </Button>
          <span className="text-xs text-muted-foreground">
            已显示 {visible.length} / {filtered.length}
          </span>
        </div>
      ) : null}
    </div>
  );
}
