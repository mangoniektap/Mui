import type { ComponentDoc } from "./index";

/** 导航分组（12 个） */
export default {
  tabs: {
    import: 'import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"',
    summary:
      "标签页。同一区域内的视图切换；TabsList 有 default（胶囊）与 line（下划线）两种形态，支持横竖方向。",
    api: [
      { name: "value / defaultValue", type: "string", desc: "受控 / 非受控选中页" },
      { name: "onValueChange", type: "(value: string) => void", desc: "切换回调" },
      { name: "orientation", type: '"horizontal" | "vertical"', default: '"horizontal"', desc: "排列方向" },
      { name: "TabsList", type: 'Props & { variant?: "default" | "line" }', desc: "标签栏形态" },
      { name: "TabsTrigger", type: 'Props & { value: string }', desc: "单个标签，value 与对应 Content 一致" },
      { name: "TabsContent", type: 'Props & { value: string }', desc: "页面内容，value 与对应 Trigger 一致" },
    ],
    examples: [
      {
        title: "基础标签页",
        code: `import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"

<Tabs defaultValue="account">
  <TabsList>
    <TabsTrigger value="account">账号</TabsTrigger>
    <TabsTrigger value="password">密码</TabsTrigger>
  </TabsList>
  <TabsContent value="account">账号设置内容</TabsContent>
  <TabsContent value="password">密码设置内容</TabsContent>
</Tabs>`,
      },
    ],
  },

  accordion: {
    import:
      'import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion"',
    summary: "折叠面板。成组内容的展开与收起，单选（single）或多选（multiple）可配。",
    api: [
      { name: "type", type: '"single" | "multiple"', default: '"single"', desc: "single 同时只开一项" },
      { name: "collapsible", type: "boolean", default: "false", desc: "single 下是否允许再点一次收起" },
      { name: "value / defaultValue", type: "string | string[]", desc: "展开项" },
      { name: "AccordionItem", type: 'Props & { value: string }', desc: "单个折叠项" },
      { name: "AccordionTrigger", type: "button", desc: "标题行（自带展开箭头）" },
      { name: "AccordionContent", type: "div", desc: "展开内容" },
    ],
    examples: [
      {
        title: "常见问题",
        code: `import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion"

<Accordion type="single" collapsible className="w-full">
  <AccordionItem value="q1">
    <AccordionTrigger>如何引入组件？</AccordionTrigger>
    <AccordionContent>从 @/components/ui 按需 import。</AccordionContent>
  </AccordionItem>
  <AccordionItem value="q2">
    <AccordionTrigger>支持暗色模式吗？</AccordionTrigger>
    <AccordionContent>支持，令牌按主题切换。</AccordionContent>
  </AccordionItem>
</Accordion>`,
      },
    ],
  },

  popover: {
    import:
      'import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"',
    summary: "气泡卡片。点击触发的轻量浮层，可放表单、筛选器等任意内容。",
    api: [
      { name: "open / onOpenChange", type: "boolean / (open: boolean) => void", desc: "受控开关" },
      { name: "PopoverContent", type: 'Props & { align?: "start" | "center" | "end"; side?: "top" | "right" | "bottom" | "left"; sideOffset?: number }', desc: "浮层内容" },
      { name: "PopoverTrigger", type: "button", desc: "触发器，通常配 asChild" },
      { name: "PopoverAnchor", type: "div", desc: "自定义锚点" },
    ],
    examples: [
      {
        title: "筛选浮层",
        code: `import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"

<Popover>
  <PopoverTrigger asChild>
    <Button variant="outline">筛选</Button>
  </PopoverTrigger>
  <PopoverContent className="w-64">
    <div className="grid gap-2">
      <Label htmlFor="kw">关键词</Label>
      <Input id="kw" />
    </div>
  </PopoverContent>
</Popover>`,
      },
    ],
  },

  "dropdown-menu": {
    import:
      'import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"',
    summary:
      "下拉菜单。点击触发器展开的菜单，支持分组、复选 / 单选、子菜单与快捷键提示；基于 Radix DropdownMenu。",
    api: [
      { name: "open / onOpenChange", type: "boolean / (open: boolean) => void", desc: "受控开关" },
      { name: "DropdownMenuItem", type: 'Props & { inset?: boolean; variant?: "default" | "destructive" }', desc: "菜单项；destructive 为危险样式" },
      { name: "DropdownMenuCheckboxItem / RadioGroup / RadioItem", type: "—", desc: "复选 / 单选菜单项" },
      { name: "DropdownMenuLabel / Separator / Shortcut / Group", type: "—", desc: "组标题 / 分隔 / 快捷键 / 分组" },
      { name: "DropdownMenuSub / SubTrigger / SubContent", type: "—", desc: "子菜单三级结构" },
      { name: "DropdownMenuTrigger", type: "button", desc: "触发器，通常配 asChild" },
    ],
    examples: [
      {
        title: "账号菜单",
        code: `import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

<DropdownMenu>
  <DropdownMenuTrigger asChild>
    <Button variant="outline">我的账号</Button>
  </DropdownMenuTrigger>
  <DropdownMenuContent>
    <DropdownMenuLabel>账号</DropdownMenuLabel>
    <DropdownMenuSeparator />
    <DropdownMenuItem>个人资料</DropdownMenuItem>
    <DropdownMenuItem>设置</DropdownMenuItem>
    <DropdownMenuItem variant="destructive">退出登录</DropdownMenuItem>
  </DropdownMenuContent>
</DropdownMenu>`,
      },
    ],
  },

  "context-menu": {
    import:
      'import { ContextMenu, ContextMenuContent, ContextMenuItem, ContextMenuTrigger } from "@/components/ui/context-menu"',
    summary:
      "右键菜单。在指定区域内右键唤出的上下文菜单，子组件与 DropdownMenu 完全同构。",
    api: [
      { name: "ContextMenuTrigger", type: "div", desc: "右键触发区域，包住内容" },
      { name: "ContextMenuItem", type: 'Props & { inset?: boolean; variant?: "default" | "destructive" }', desc: "菜单项" },
      { name: "CheckboxItem / RadioGroup / RadioItem / Sub / Label / Separator / Shortcut / Group / Portal", type: "—", desc: "同 DropdownMenu 对应部件" },
    ],
    examples: [
      {
        title: "右键编辑列表",
        code: `import {
  ContextMenu,
  ContextMenuContent,
  ContextMenuItem,
  ContextMenuTrigger,
} from "@/components/ui/context-menu"

<ContextMenu>
  <ContextMenuTrigger className="rounded-lg border p-8">
    右键点击这块区域
  </ContextMenuTrigger>
  <ContextMenuContent>
    <ContextMenuItem>复制</ContextMenuItem>
    <ContextMenuItem>粘贴</ContextMenuItem>
    <ContextMenuItem variant="destructive">删除</ContextMenuItem>
  </ContextMenuContent>
</ContextMenu>`,
      },
    ],
  },

  menubar: {
    import:
      'import { Menubar, MenubarContent, MenubarItem, MenubarMenu, MenubarTrigger } from "@/components/ui/menubar"',
    summary:
      "菜单栏。桌面应用式的顶部菜单：多个 MenubarMenu 横向排列，点开各自动画下拉；子组件与 DropdownMenu 同构。",
    api: [
      { name: "Menubar", type: "div", desc: "菜单栏容器" },
      { name: "MenubarMenu", type: "—", desc: "一个菜单分组" },
      { name: "MenubarTrigger", type: "button", desc: "顶层菜单按钮" },
      { name: "MenubarContent / Item / CheckboxItem / RadioItem / Sub / Label / Separator / Shortcut", type: "—", desc: "同 DropdownMenu 对应部件" },
    ],
    examples: [
      {
        title: "编辑器菜单栏",
        code: `import {
  Menubar,
  MenubarContent,
  MenubarItem,
  MenubarMenu,
  MenubarSeparator,
  MenubarTrigger,
} from "@/components/ui/menubar"

<Menubar>
  <MenubarMenu>
    <MenubarTrigger>文件</MenubarTrigger>
    <MenubarContent>
      <MenubarItem>新建文件</MenubarItem>
      <MenubarSeparator />
      <MenubarItem>保存</MenubarItem>
    </MenubarContent>
  </MenubarMenu>
  <MenubarMenu>
    <MenubarTrigger>编辑</MenubarTrigger>
    <MenubarContent>
      <MenubarItem>撤销</MenubarItem>
      <MenubarItem>重做</MenubarItem>
    </MenubarContent>
  </MenubarMenu>
</Menubar>`,
      },
    ],
  },

  "navigation-menu": {
    import:
      'import { NavigationMenu, NavigationMenuContent, NavigationMenuItem, NavigationMenuList, NavigationMenuTrigger, navigationMenuTriggerStyle } from "@/components/ui/navigation-menu"',
    summary:
      "导航菜单。带内容面板的站内导航：Trigger 下可展开一个 viewport 大面板，放分组链接与描述。",
    api: [
      { name: "value / defaultValue / onValueChange", type: "string", desc: "展开的菜单项" },
      { name: "NavigationMenuList", type: "ul", desc: "顶层列表" },
      { name: "NavigationMenuItem", type: "li", desc: "单个菜单项容器" },
      { name: "NavigationMenuTrigger", type: "button", desc: "顶层按钮，自带箭头动画" },
      { name: "NavigationMenuContent", type: "div", desc: "展开面板；不放 Content 就是纯链接（用 navigationMenuTriggerStyle）" },
      { name: "NavigationMenuLink", type: "a", desc: "面板内链接" },
      { name: "NavigationMenuIndicator / Viewport", type: "—", desc: "指示条 / 共享面板容器，已内置" },
    ],
    examples: [
      {
        title: "带面板的导航",
        code: `import {
  NavigationMenu,
  NavigationMenuContent,
  NavigationMenuItem,
  NavigationMenuList,
  NavigationMenuTrigger,
  navigationMenuTriggerStyle,
} from "@/components/ui/navigation-menu"

<NavigationMenu>
  <NavigationMenuList>
    <NavigationMenuItem>
      <NavigationMenuTrigger>组件</NavigationMenuTrigger>
      <NavigationMenuContent>
        <ul className="grid w-48 gap-2 p-2">
          <li>按钮</li>
          <li>卡片</li>
        </ul>
      </NavigationMenuContent>
    </NavigationMenuItem>
    <NavigationMenuItem>
      <NavigationMenuLink href="/docs" className={navigationMenuTriggerStyle()}>
        文档
      </NavigationMenuLink>
    </NavigationMenuItem>
  </NavigationMenuList>
</NavigationMenu>`,
      },
    ],
  },

  command: {
    import:
      'import { Command, CommandInput, CommandItem, CommandList } from "@/components/ui/command"',
    summary:
      "命令面板。可搜索的命令列表；CommandDialog 提供 ⌘K 弹窗模式，输入即过滤，分组与空态齐全。",
    api: [
      { name: "Command", type: "div", desc: "根容器" },
      { name: "CommandInput", type: "input", desc: "搜索输入框" },
      { name: "CommandList", type: "div", desc: "结果列表容器" },
      { name: "CommandEmpty", type: "div", desc: "无结果占位" },
      { name: "CommandGroup", type: 'div & { heading?: string }', desc: "分组" },
      { name: "CommandItem", type: 'div & { value?: string; onSelect?: () => void }', desc: "可选项；keywords 可加同义词" },
      { name: "CommandSeparator / CommandShortcut", type: "div / span", desc: "分隔 / 快捷键提示" },
      { name: "CommandDialog", type: 'Dialog & { title?: string; description?: string; showCloseButton?: boolean }', desc: "弹窗形态" },
    ],
    examples: [
      {
        title: "弹窗命令面板",
        code: `import {
  Command,
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command"

const [open, setOpen] = useState(false)

<CommandDialog open={open} onOpenChange={setOpen}>
  <CommandInput placeholder="输入命令…" />
  <CommandList>
    <CommandEmpty>没有找到命令</CommandEmpty>
    <CommandGroup heading="建议">
      <CommandItem onSelect={() => setOpen(false)}>新建文件</CommandItem>
      <CommandItem onSelect={() => setOpen(false)}>搜索组件</CommandItem>
    </CommandGroup>
  </CommandList>
</CommandDialog>`,
      },
    ],
  },

  breadcrumb: {
    import:
      'import { Breadcrumb, BreadcrumbItem, BreadcrumbLink, BreadcrumbList, BreadcrumbPage, BreadcrumbSeparator } from "@/components/ui/breadcrumb"',
    summary: "面包屑。展示层级路径，最后一项为当前页（Page），中间可用 Ellipsis 省略。",
    api: [
      { name: "BreadcrumbList", type: "nav", desc: "列表容器" },
      { name: "BreadcrumbItem", type: "li", desc: "单个层级" },
      { name: "BreadcrumbLink", type: "a", desc: "可点击层级；asChild 可接路由组件" },
      { name: "BreadcrumbPage", type: "span", desc: "当前页（不可点）" },
      { name: "BreadcrumbSeparator", type: "li", desc: "分隔符，默认 ChevronRight" },
      { name: "BreadcrumbEllipsis", type: "span", desc: "省略号" },
    ],
    examples: [
      {
        title: "层级路径",
        code: `import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb"

<Breadcrumb>
  <BreadcrumbList>
    <BreadcrumbItem>
      <BreadcrumbLink href="/">首页</BreadcrumbLink>
    </BreadcrumbItem>
    <BreadcrumbSeparator />
    <BreadcrumbItem>
      <BreadcrumbLink href="/components">组件</BreadcrumbLink>
    </BreadcrumbItem>
    <BreadcrumbSeparator />
    <BreadcrumbItem>
      <BreadcrumbPage>按钮</BreadcrumbPage>
    </BreadcrumbItem>
  </BreadcrumbList>
</Breadcrumb>`,
      },
    ],
  },

  pagination: {
    import:
      'import { Pagination, PaginationContent, PaginationEllipsis, PaginationItem, PaginationLink, PaginationNext, PaginationPrevious } from "@/components/ui/pagination"',
    summary:
      "分页。默认「横放转盘」：给 currentPage / pageCount / onPageChange 且不传子节点即自动接管 —— 同屏最多 5 个页码、两侧缩小淡出、按住左右拖连续换页、松手甩动吸附；传子节点则回到自定义内容（页码钮 + 上一页 / 下一页 / 省略号）。",
    api: [
      { name: "currentPage / pageCount / onPageChange", type: "number / number / (p: number) => void", desc: "转盘模式三件套；给齐且无子节点时启用" },
      { name: "visiblePages", type: "number", default: "5", desc: "转盘模式同屏页码个数" },
      { name: "dragStep", type: "number", default: "40", desc: "拖动多远算翻一页（px）" },
      { name: "PaginationContent", type: "ul", desc: "自定义内容模式的容器" },
      { name: "PaginationLink", type: 'a & { href: string; isActive?: boolean }', desc: "页码链接；asChild 可接路由组件" },
      { name: "PaginationPrevious / PaginationNext", type: "a", desc: "上一页 / 下一页" },
      { name: "PaginationEllipsis", type: "span", desc: "省略号" },
      { name: "PaginationItem", type: "li", desc: "单个条目容器" },
    ],
    examples: [
      {
        title: "基础分页",
        code: `import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination"

<Pagination>
  <PaginationContent>
    <PaginationItem>
      <PaginationPrevious href="#" />
    </PaginationItem>
    <PaginationItem>
      <PaginationLink href="#">1</PaginationLink>
    </PaginationItem>
    <PaginationItem>
      <PaginationLink href="#" isActive>2</PaginationLink>
    </PaginationItem>
    <PaginationItem>
      <PaginationEllipsis />
    </PaginationItem>
    <PaginationItem>
      <PaginationNext href="#" />
    </PaginationItem>
  </PaginationContent>
</Pagination>`,
      },
    ],
  },

  "hover-card": {
    import:
      'import { HoverCard, HoverCardContent, HoverCardTrigger } from "@/components/ui/hover-card"',
    summary: "悬浮卡片。悬停（非点击）展开的富内容卡片，适合用户 / 仓库预览。",
    api: [
      { name: "open / onOpenChange", type: "boolean / (open: boolean) => void", desc: "受控开关" },
      { name: "openDelay / closeDelay", type: "number", default: "200 / 300", desc: "悬停 / 移出延迟（ms）" },
      { name: "HoverCardContent", type: 'Props & { align?: "start" | "center" | "end"; side?: "top" | "right" | "bottom" | "left" }', desc: "浮层内容" },
      { name: "HoverCardTrigger", type: "a", desc: "触发器，通常配 asChild" },
    ],
    examples: [
      {
        title: "用户预览",
        code: `import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { HoverCard, HoverCardContent, HoverCardTrigger } from "@/components/ui/hover-card"

<HoverCard>
  <HoverCardTrigger asChild>
    <a href="#" className="text-primary underline">@mui</a>
  </HoverCardTrigger>
  <HoverCardContent className="w-64">
    <div className="flex gap-3">
      <Avatar>
        <AvatarImage src="https://github.com/shadcn.png" />
        <AvatarFallback>M</AvatarFallback>
      </Avatar>
      <div>
        <p className="font-medium">Mui</p>
        <p className="text-xs text-muted-foreground">组件库与设计令牌</p>
      </div>
    </div>
  </HoverCardContent>
</HoverCard>`,
      },
    ],
  },

  collapsible: {
    import:
      'import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"',
    summary: "折叠区。单一区域的展开与收起；需要高度动画时配 tw-animate-css 的动画类。",
    api: [
      { name: "open / onOpenChange", type: "boolean / (open: boolean) => void", desc: "受控展开态" },
      { name: "defaultOpen", type: "boolean", default: "false", desc: "非受控初始态" },
      { name: "disabled", type: "boolean", default: "false", desc: "禁用" },
      { name: "CollapsibleTrigger", type: "button", desc: "触发器" },
      { name: "CollapsibleContent", type: "div", desc: "展开内容" },
    ],
    examples: [
      {
        title: "展开更多",
        code: `import { Button } from "@/components/ui/button"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"

<Collapsible className="w-80">
  <CollapsibleTrigger asChild>
    <Button variant="outline">查看详情</Button>
  </CollapsibleTrigger>
  <CollapsibleContent className="pt-2 text-sm text-muted-foreground">
    这里是展开后的详细说明。
  </CollapsibleContent>
</Collapsible>`,
      },
    ],
  },
} satisfies Record<string, ComponentDoc>;
