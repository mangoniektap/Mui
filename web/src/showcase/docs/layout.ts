import type { ComponentDoc } from "./index";

/** 布局分组（6 个） */
export default {
  "scroll-area": {
    import: 'import { ScrollArea } from "@/components/ui/scroll-area"',
    summary:
      "滚动区域。用自定义滚动条替换系统滚动条，支持横竖两向；内容超长时容器自身给出高度约束。",
    api: [
      { name: "ScrollArea", type: "div", desc: "容器；需要给高度（h-*）或 max-h-*" },
      { name: "ScrollBar", type: "div", desc: "滚动条；orientation: horizontal | vertical" },
      { name: "type", type: '"auto" | "always" | "scroll" | "hover"', default: '"hover"', desc: "滚动条显示时机（Radix 约定）" },
    ],
    examples: [
      {
        title: "纵向滚动列表",
        code: `import { ScrollArea } from "@/components/ui/scroll-area"

<ScrollArea className="h-48 w-64 rounded-lg border">
  <div className="space-y-2 p-3">
    {items.map((item) => (
      <div key={item} className="text-sm">{item}</div>
    ))}
  </div>
</ScrollArea>`,
      },
    ],
  },

  resizable: {
    import:
      'import { ResizableHandle, ResizablePanel, ResizablePanelGroup } from "@/components/ui/resizable"',
    summary: "可拖拽分栏。按住中间把手调整各面板宽度，支持横纵双向嵌套。",
    api: [
      { name: "ResizablePanelGroup", type: 'Props & { direction?: "horizontal" | "vertical" }', desc: "分栏容器" },
      { name: "ResizablePanel", type: "Props", desc: "面板；defaultSize / minSize / maxSize / order" },
      { name: "ResizableHandle", type: 'Props & { withHandle?: boolean }', desc: "拖拽把手；withHandle 显示 grip 图标" },
    ],
    examples: [
      {
        title: "左右分栏",
        code: `import {
  ResizableHandle,
  ResizablePanel,
  ResizablePanelGroup,
} from "@/components/ui/resizable"

<ResizablePanelGroup direction="horizontal" className="min-h-48 rounded-lg border">
  <ResizablePanel defaultSize={30} minSize={20}>
    <div className="p-4 text-sm">侧栏</div>
  </ResizablePanel>
  <ResizableHandle withHandle />
  <ResizablePanel defaultSize={70}>
    <div className="p-4 text-sm">主区</div>
  </ResizablePanel>
</ResizablePanelGroup>`,
      },
    ],
  },

  table: {
    import:
      'import { Table, TableBody, TableCaption, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"',
    summary:
      "数据表格。原生 table 的语义化封装：表头、表体、表尾与标题齐全，容器外套可滚动 div 处理横向溢出。",
    api: [
      { name: "Table", type: "table", desc: "表格本体" },
      { name: "TableHeader / TableBody / TableFooter", type: "thead / tbody / tfoot", desc: "三段结构" },
      { name: "TableRow", type: "tr", desc: "行；data-state=selected 高亮" },
      { name: "TableHead / TableCell", type: "th / td", desc: "表头格 / 内容格" },
      { name: "TableCaption", type: "caption", desc: "表格标题" },
    ],
    examples: [
      {
        title: "基础表格",
        code: `import {
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"

<div className="overflow-x-auto rounded-lg border">
  <Table>
    <TableCaption>最近三笔订单</TableCaption>
    <TableHeader>
      <TableRow>
        <TableHead>订单号</TableHead>
        <TableHead className="text-right">金额</TableHead>
      </TableRow>
    </TableHeader>
    <TableBody>
      <TableRow>
        <TableCell className="font-medium">#1024</TableCell>
        <TableCell className="text-right">¥ 128.00</TableCell>
      </TableRow>
    </TableBody>
  </Table>
</div>`,
      },
    ],
  },

  carousel: {
    import:
      'import { Carousel, CarouselContent, CarouselItem, CarouselNext, CarouselPrevious } from "@/components/ui/carousel"',
    summary:
      "轮播。基于 embla-carousel：支持横竖方向、循环、多屏与自动播放；CarouselApi 可在外部控制滚动。",
    api: [
      { name: "orientation", type: '"horizontal" | "vertical"', default: '"horizontal"', desc: "滚动方向" },
      { name: "opts", type: "EmblaOptionsType", desc: "embla 原生配置：loop / align / slidesToScroll" },
      { name: "setApi", type: "(api: CarouselApi) => void", desc: "拿到 embla 实例，用于外部控制" },
      { name: "CarouselContent", type: "div", desc: "轨道；默认 -ml-4，项配 pl-4" },
      { name: "CarouselItem", type: "div", desc: "单个轮播项，用 basis-* 控制每屏数量" },
      { name: "CarouselPrevious / CarouselNext", type: "button", desc: "上 / 下一张按钮" },
    ],
    examples: [
      {
        title: "图片轮播",
        code: `import {
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
} from "@/components/ui/carousel"

<Carousel className="w-full max-w-sm">
  <CarouselContent>
    {slides.map((slide, i) => (
      <CarouselItem key={i}>
        <img src={slide} className="aspect-video w-full rounded-lg object-cover" />
      </CarouselItem>
    ))}
  </CarouselContent>
  <CarouselPrevious />
  <CarouselNext />
</Carousel>`,
      },
    ],
  },

  chart: {
    import:
      'import { ChartContainer, ChartLegend, ChartLegendContent, ChartTooltip, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart"',
    summary:
      "图表。recharts 的封装：ChartConfig 定义数据键与颜色（支持 CSS 变量），Tooltip / Legend 自动跟随主题色。",
    api: [
      { name: "ChartContainer", type: 'Props & { config: ChartConfig }', desc: "图表容器，必传 config" },
      { name: "ChartTooltip", type: "Tooltip", desc: "浮层提示；content 配 ChartTooltipContent" },
      { name: "ChartTooltipContent", type: 'Props & { hideLabel?: boolean; hideIndicator?: boolean; indicator?: "line" | "dot" | "dashed"; nameKey?: string; labelKey?: string }', desc: "提示内容定制" },
      { name: "ChartLegend / ChartLegendContent", type: "Legend", desc: "图例；Content 可配 hideIcon / nameKey" },
      { name: "ChartConfig", type: "Record<string, { label: string; color?: string }>", desc: "数据键 → 标签 / 颜色（可写 var(--chart-1)）" },
      { name: "ChartStyle", type: "div", desc: "按 config 注入 CSS 变量的辅助组件" },
    ],
    examples: [
      {
        title: "柱状图",
        code: `import { Bar, BarChart, XAxis } from "recharts"
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart"

const config: ChartConfig = {
  visits: { label: "访问量", color: "var(--chart-1)" },
}

const data = [
  { month: "1 月", visits: 320 },
  { month: "2 月", visits: 480 },
]

<ChartContainer config={config} className="min-h-48 w-full">
  <BarChart data={data}>
    <XAxis dataKey="month" />
    <Bar dataKey="visits" fill="var(--color-visits)" radius={4} />
    <ChartTooltip content={<ChartTooltipContent />} />
  </BarChart>
</ChartContainer>`,
      },
    ],
  },

  sidebar: {
    import:
      'import { Sidebar, SidebarContent, SidebarFooter, SidebarGroup, SidebarGroupLabel, SidebarHeader, SidebarInset, SidebarMenu, SidebarMenuButton, SidebarMenuItem, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar"',
    summary:
      "应用侧边栏。折叠 / 展开 / 移动端抽屉三态齐全，菜单按钮带 tooltip 与激活态；useSidebar 在任意子组件里读状态。",
    api: [
      { name: "SidebarProvider", type: 'Props & { defaultOpen?: boolean; open?: boolean; onOpenChange?: (open: boolean) => void }', desc: "根容器，管折叠状态" },
      { name: "Sidebar", type: 'Props & { side?: "left" | "right"; variant?: "sidebar" | "floating" | "inset"; collapsible?: "offcanvas" | "icon" | "none" }', desc: "侧边栏本体" },
      { name: "SidebarHeader / Content / Footer / Rail / Inset", type: "div", desc: "头部 / 滚动内容 / 底部 / 折叠把手 / 主内容区" },
      { name: "SidebarGroup / GroupLabel / GroupContent / GroupAction", type: "div", desc: "分组 / 组标题 / 组内容 / 组操作" },
      { name: "SidebarMenu / MenuItem / MenuButton", type: "ul / li / button", desc: "菜单；Button 有 size（sm | default | lg）、isActive、tooltip" },
      { name: "SidebarMenuSub / SubItem / SubButton", type: "ul / li / a", desc: "二级菜单" },
      { name: "SidebarMenuAction / Badge / Skeleton", type: "button / div / div", desc: "行内操作 / 角标 / 加载骨架" },
      { name: "useSidebar()", type: "() => { state; open; setOpen; … }", desc: "读 / 写折叠状态" },
    ],
    examples: [
      {
        title: "应用框架",
        code: `import { Sidebar, SidebarContent, SidebarGroup, SidebarGroupLabel, SidebarHeader, SidebarInset, SidebarMenu, SidebarMenuButton, SidebarMenuItem, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar"

<SidebarProvider>
  <Sidebar>
    <SidebarHeader>Logo</SidebarHeader>
    <SidebarContent>
      <SidebarGroup>
        <SidebarGroupLabel>导航</SidebarGroupLabel>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton isActive>总览</SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarGroup>
    </SidebarContent>
  </Sidebar>
  <SidebarInset>
    <SidebarTrigger />
    <main className="p-4">页面内容</main>
  </SidebarInset>
</SidebarProvider>`,
      },
    ],
  },
} satisfies Record<string, ComponentDoc>;
