import { useEffect, useState } from "react";

import { TocLink, TocTitle } from "@/showcase/ComponentDoc";

export type TocItem = { id: string; label: string };

/**
 * 右侧「本页目录」：锚点跳转 + 滚动高亮当前节。
 * 判定规则：视口顶端（预留顶栏高度）之前最后一个可见的节为当前节；
 * 滚到页底时强制高亮最后一节。
 */
export function TocRail({ items }: { items: TocItem[] }) {
  const [active, setActive] = useState(items[0]?.id ?? "");

  useEffect(() => {
    setActive(items[0]?.id ?? "");

    const compute = () => {
      const els = items
        .map((item) => document.getElementById(item.id))
        .filter((el): el is HTMLElement => el !== null);
      if (els.length === 0) return;

      let current = els[0];
      for (const el of els) {
        if (el.getBoundingClientRect().top < 140) current = el;
      }
      // 页底：最后一节可能永远够不到 140px 线，兜底高亮它
      if (
        window.innerHeight + window.scrollY >=
        document.documentElement.scrollHeight - 4
      ) {
        current = els[els.length - 1];
      }
      setActive(current.id);
    };

    compute();
    window.addEventListener("scroll", compute, { passive: true });
    window.addEventListener("resize", compute);
    return () => {
      window.removeEventListener("scroll", compute);
      window.removeEventListener("resize", compute);
    };
  }, [items]);

  if (items.length === 0) return null;

  return (
    <aside className="hidden xl:block">
      <div className="scrollbar-thin sticky top-20 max-h-[calc(100vh-6rem)] overflow-y-auto">
        <TocTitle />
        <nav className="border-l">
          {items.map((item) => (
            <TocLink
              key={item.id}
              href={`#${item.id}`}
              active={active === item.id}
            >
              {item.label}
            </TocLink>
          ))}
        </nav>
      </div>
    </aside>
  );
}
