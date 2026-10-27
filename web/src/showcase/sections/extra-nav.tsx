import { useState } from "react";
import {
  Calendar,
  ChevronDown,
  ChevronRight,
  CircleCheck,
  CreditCard,
  Ellipsis,
  FileText,
  LogOut,
  Search,
  Settings,
  User,
} from "lucide-react";

import {
  Breadcrumb,
  BreadcrumbEllipsis,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb";
import { Button } from "@/components/ui/button";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import {
  Command,
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
  CommandSeparator,
  CommandShortcut,
} from "@/components/ui/command";
import {
  ContextMenu,
  ContextMenuContent,
  ContextMenuItem,
  ContextMenuLabel,
  ContextMenuSeparator,
  ContextMenuShortcut,
  ContextMenuTrigger,
} from "@/components/ui/context-menu";
import {
  HoverCard,
  HoverCardContent,
  HoverCardTrigger,
} from "@/components/ui/hover-card";
import {
  Menubar,
  MenubarContent,
  MenubarItem,
  MenubarMenu,
  MenubarSeparator,
  MenubarShortcut,
  MenubarTrigger,
} from "@/components/ui/menubar";
import {
  NavigationMenu,
  NavigationMenuContent,
  NavigationMenuItem,
  NavigationMenuLink,
  NavigationMenuList,
  NavigationMenuTrigger,
} from "@/components/ui/navigation-menu";
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination";
import { Demo } from "@/showcase/Demo";

export function BreadcrumbSection() {
  return (
    <Demo title="面包屑" hint="Breadcrumb" layout="col">
      <Breadcrumb>
        <BreadcrumbList>
          <BreadcrumbItem>
            <BreadcrumbLink href="#">首页</BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbEllipsis />
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbLink href="#">组件</BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbPage>面包屑</BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>
      <Breadcrumb>
        <BreadcrumbList>
          <BreadcrumbItem>
            <BreadcrumbLink href="#">Mui</BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator>
            <ChevronRight />
          </BreadcrumbSeparator>
          <BreadcrumbItem>
            <BreadcrumbPage>自定义分隔符</BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>
    </Demo>
  );
}

export function CommandSection() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Demo title="命令面板（内联）" hint="Command">
        <Command className="w-full max-w-md rounded-lg border shadow-xs">
          <CommandInput placeholder="输入命令或搜索…" />
          <CommandList>
            <CommandEmpty>没有匹配的命令</CommandEmpty>
            <CommandGroup heading="建议">
              <CommandItem>
                <Calendar />
                日历
                <CommandShortcut>⌘C</CommandShortcut>
              </CommandItem>
              <CommandItem>
                <Search />
                搜索
                <CommandShortcut>⌘K</CommandShortcut>
              </CommandItem>
              <CommandItem>
                <Settings />
                设置
                <CommandShortcut>⌘S</CommandShortcut>
              </CommandItem>
            </CommandGroup>
            <CommandSeparator />
            <CommandGroup heading="账户">
              <CommandItem>
                <User />
                个人资料
              </CommandItem>
            </CommandGroup>
          </CommandList>
        </Command>
      </Demo>

      <Demo title="命令弹窗" hint="CommandDialog">
        <Button variant="outline" onClick={() => setOpen(true)}>
          打开命令面板
        </Button>
        <CommandDialog open={open} onOpenChange={setOpen} title="命令面板">
          <CommandInput placeholder="搜索组件…" />
          <CommandList>
            <CommandEmpty>没有结果</CommandEmpty>
            <CommandGroup heading="组件">
              <CommandItem onSelect={() => setOpen(false)}>
                <CircleCheck />
                按钮
              </CommandItem>
              <CommandItem onSelect={() => setOpen(false)}>
                <CircleCheck />
                对话框
              </CommandItem>
            </CommandGroup>
          </CommandList>
        </CommandDialog>
      </Demo>
    </>
  );
}

export function ContextMenuSection() {
  return (
    <Demo title="右键菜单" hint="ContextMenu" layout="col">
      <ContextMenu>
        <ContextMenuTrigger asChild>
          <div className="grid h-32 w-full place-items-center rounded-lg border border-dashed text-sm text-muted-foreground">
            在这里点击右键
          </div>
        </ContextMenuTrigger>
        <ContextMenuContent className="w-48">
          <ContextMenuLabel>文件操作</ContextMenuLabel>
          <ContextMenuItem>
            复制
            <ContextMenuShortcut>⌘C</ContextMenuShortcut>
          </ContextMenuItem>
          <ContextMenuItem>
            重命名
            <ContextMenuShortcut>F2</ContextMenuShortcut>
          </ContextMenuItem>
          <ContextMenuSeparator />
          <ContextMenuItem variant="destructive">
            删除
          </ContextMenuItem>
        </ContextMenuContent>
      </ContextMenu>
    </Demo>
  );
}

export function MenubarSection() {
  return (
    <Demo title="菜单栏" hint="Menubar">
      <Menubar>
        <MenubarMenu>
          <MenubarTrigger>文件</MenubarTrigger>
          <MenubarContent>
            <MenubarItem>
              新建
              <MenubarShortcut>⌘N</MenubarShortcut>
            </MenubarItem>
            <MenubarItem>打开…</MenubarItem>
            <MenubarSeparator />
            <MenubarItem>
              退出
              <MenubarShortcut>⇧⌘Q</MenubarShortcut>
            </MenubarItem>
          </MenubarContent>
        </MenubarMenu>
        <MenubarMenu>
          <MenubarTrigger>编辑</MenubarTrigger>
          <MenubarContent>
            <MenubarItem>
              撤销
              <MenubarShortcut>⌘Z</MenubarShortcut>
            </MenubarItem>
            <MenubarItem>
              重做
              <MenubarShortcut>⇧⌘Z</MenubarShortcut>
            </MenubarItem>
          </MenubarContent>
        </MenubarMenu>
        <MenubarMenu>
          <MenubarTrigger>视图</MenubarTrigger>
          <MenubarContent>
            <MenubarItem>放大</MenubarItem>
            <MenubarItem>缩小</MenubarItem>
          </MenubarContent>
        </MenubarMenu>
      </Menubar>
    </Demo>
  );
}

export function NavigationMenuSection() {
  return (
    <Demo
      title="导航菜单"
      hint="NavigationMenu"
      layout="col"
      overflow="visible"
      className="min-h-64 items-start"
    >
      {/* flex-none：避免在 flex-col 里被 flex-1 拉高，导致触发器垂直居中、面板跑到卡片外 */}
      <NavigationMenu className="w-full max-w-lg flex-none justify-start">
        <NavigationMenuList>
          <NavigationMenuItem>
            <NavigationMenuTrigger>组件</NavigationMenuTrigger>
            <NavigationMenuContent>
              <div className="grid w-64 gap-1 p-2">
                {["按钮 Button", "对话框 Dialog", "卡片 Card", "表单 Form"].map(
                  (label) => (
                    <div
                      key={label}
                      className="rounded-md px-2 py-1.5 text-sm hover:bg-accent"
                    >
                      {label}
                    </div>
                  )
                )}
              </div>
            </NavigationMenuContent>
          </NavigationMenuItem>
          <NavigationMenuItem>
            <NavigationMenuTrigger>资源</NavigationMenuTrigger>
            <NavigationMenuContent>
              <div className="grid w-52 gap-1 p-2 text-sm">
                <div className="rounded-md px-2 py-1.5 hover:bg-accent">配色</div>
                <div className="rounded-md px-2 py-1.5 hover:bg-accent">图标</div>
              </div>
            </NavigationMenuContent>
          </NavigationMenuItem>
          <NavigationMenuItem>
            <NavigationMenuLink asChild>
              <a
                href="#"
                className="inline-flex h-9 items-center rounded-md px-3 text-sm font-medium hover:bg-accent"
              >
                文档
              </a>
            </NavigationMenuLink>
          </NavigationMenuItem>
        </NavigationMenuList>
      </NavigationMenu>
    </Demo>
  );
}

export function PaginationSection() {
  // 页码 0 起；转盘模式下拖动 / 甩动 / 点页码都走 onPageChange
  const [page, setPage] = useState(0);
  const count = 12;
  return (
    <>
      <Demo title="分页（横放转盘）" hint="Pagination · 拖动换页" layout="col">
        <Pagination currentPage={page} pageCount={count} onPageChange={setPage} />
        <p className="text-[12px] text-muted-foreground">
          第 {page + 1} / {count} 页 · 按住页码左右拖，松手吸附
        </p>
      </Demo>

      <Demo title="分页（自定义内容）" hint="Pagination + PaginationContent" layout="col">
        <Pagination>
          <PaginationContent>
            <PaginationItem>
              <PaginationPrevious href="#" />
            </PaginationItem>
            <PaginationItem>
              <PaginationLink href="#">1</PaginationLink>
            </PaginationItem>
            <PaginationItem>
              <PaginationLink href="#" isActive>
                2
              </PaginationLink>
            </PaginationItem>
            <PaginationItem>
              <PaginationLink href="#">3</PaginationLink>
            </PaginationItem>
            <PaginationItem>
              <PaginationEllipsis />
            </PaginationItem>
            <PaginationItem>
              <PaginationNext href="#" />
            </PaginationItem>
          </PaginationContent>
        </Pagination>
      </Demo>
    </>
  );
}

export function HoverCardSection() {
  return (
    <Demo title="悬浮卡片" hint="HoverCard">
      <HoverCard>
        <HoverCardTrigger asChild>
          <Button variant="link">@mui</Button>
        </HoverCardTrigger>
        <HoverCardContent className="w-72">
          <div className="flex gap-3">
            <div className="grid size-10 shrink-0 place-items-center rounded-full bg-primary text-sm font-semibold text-primary-foreground">
              M
            </div>
            <div className="space-y-1">
              <p className="text-sm font-medium">Mui 组件库</p>
              <p className="text-xs text-muted-foreground">
                基于 shadcn new-york 的 M 代号组件与设计令牌。
              </p>
              <p className="text-xs text-muted-foreground">2026 年 10 月加入</p>
            </div>
          </div>
        </HoverCardContent>
      </HoverCard>
    </Demo>
  );
}

export function CollapsibleSection() {
  const [open, setOpen] = useState(false);
  return (
    <Demo title="折叠区" hint="Collapsible" layout="col" className="max-w-md">
      <Collapsible open={open} onOpenChange={setOpen}>
        <CollapsibleTrigger asChild>
          <Button variant="outline" className="w-full justify-between">
            高级设置
            <ChevronDown
              className={open ? "rotate-180 transition-transform" : "transition-transform"}
            />
          </Button>
        </CollapsibleTrigger>
        <CollapsibleContent className="mt-3 space-y-2 rounded-lg border p-3 text-sm text-muted-foreground">
          <p>· 开启后会自动同步到云端</p>
          <p>· 仅对当前工作区生效</p>
          <p>· 可在设置里随时关闭</p>
        </CollapsibleContent>
      </Collapsible>
    </Demo>
  );
}
