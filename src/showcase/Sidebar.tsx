import { cn } from "@/lib/utils";
import type { Section } from "@/showcase/registry";

export function Sidebar({
  groups,
  activeId,
  onSelect,
}: {
  groups: { group: string; items: Section[] }[];
  activeId: string;
  onSelect: (id: string) => void;
}) {
  return (
    <aside className="scrollbar-thin sticky top-14 hidden h-[calc(100vh-3.5rem)] w-60 shrink-0 overflow-y-auto border-r bg-sidebar px-3 py-4 md:block">
      {groups.length === 0 ? (
        <p className="px-2 py-6 text-xs text-muted-foreground">
          没有匹配的条目
        </p>
      ) : null}
      {groups.map(({ group, items }) => (
        <div key={group} className="mb-5 last:mb-0">
          <div className="px-2 pb-1.5 text-[11px] font-medium tracking-wider text-muted-foreground uppercase">
            {group}
          </div>
          <div className="space-y-0.5">
            {items.map((item) => {
              const active = item.id === activeId;
              return (
                <button
                  key={item.id}
                  onClick={() => onSelect(item.id)}
                  className={cn(
                    "flex w-full items-center justify-between gap-2 rounded-md px-2 py-1.5 text-sm transition-colors",
                    active
                      ? "bg-sidebar-accent font-medium text-foreground"
                      : "text-muted-foreground hover:bg-sidebar-accent/60 hover:text-foreground"
                  )}
                >
                  <span>{item.label}</span>
                  <span
                    className={cn(
                      "font-mono text-[11px] font-normal",
                      active ? "text-muted-foreground" : "text-muted-foreground/70"
                    )}
                  >
                    {item.en}
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      ))}
    </aside>
  );
}
