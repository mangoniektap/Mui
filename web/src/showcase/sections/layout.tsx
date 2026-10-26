import {
  Bar,
  BarChart,
  CartesianGrid,
  XAxis,
} from "recharts";
import { House, Layers, Palette, Settings } from "lucide-react";

import {
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
} from "@/components/ui/carousel";
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart";
import {
  ResizableHandle,
  ResizablePanel,
  ResizablePanelGroup,
} from "@/components/ui/resizable";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
} from "@/components/ui/sidebar";
import {
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Demo } from "@/showcase/Demo";

export function ScrollAreaSection() {
  return (
    <Demo title="滚动区域" hint="ScrollArea" layout="col">
      <ScrollArea className="h-56 w-full max-w-md rounded-lg border">
        <div className="space-y-1 p-3">
          {Array.from({ length: 24 }, (_, i) => (
            <div
              key={i}
              className="flex items-center justify-between rounded-md px-2 py-1.5 text-sm hover:bg-accent"
            >
              <span>列表项 {String(i + 1).padStart(2, "0")}</span>
              <span className="font-mono text-xs text-muted-foreground">
                #{1000 + i}
              </span>
            </div>
          ))}
        </div>
      </ScrollArea>
    </Demo>
  );
}

export function ResizableSection() {
  return (
    <Demo title="可拖拽分栏" hint="Resizable" layout="col">
      <div className="h-52 w-full max-w-xl overflow-hidden rounded-lg border">
        <ResizablePanelGroup orientation="horizontal">
          <ResizablePanel defaultSize={40} minSize={20}>
            <div className="grid h-full place-items-center bg-muted/40 text-sm">
              左侧面板
            </div>
          </ResizablePanel>
          <ResizableHandle withHandle />
          <ResizablePanel defaultSize={60}>
            <ResizablePanelGroup orientation="vertical">
              <ResizablePanel defaultSize={55}>
                <div className="grid h-full place-items-center text-sm">
                  右上面板
                </div>
              </ResizablePanel>
              <ResizableHandle />
              <ResizablePanel defaultSize={45}>
                <div className="grid h-full place-items-center bg-muted/40 text-sm">
                  右下面板
                </div>
              </ResizablePanel>
            </ResizablePanelGroup>
          </ResizablePanel>
        </ResizablePanelGroup>
      </div>
    </Demo>
  );
}

export function TableSection() {
  return (
    <Demo title="数据表格" hint="Table" layout="col">
      <Table>
        <TableCaption>最近 4 条账单记录</TableCaption>
        <TableHeader>
          <TableRow>
            <TableHead className="w-[100px]">日期</TableHead>
            <TableHead>项目</TableHead>
            <TableHead>分类</TableHead>
            <TableHead className="text-right">金额</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {[
            ["10-21", "午餐", "餐饮", "- ¥ 38.00"],
            ["10-20", "地铁", "交通", "- ¥ 6.00"],
            ["10-19", "订阅", "服务", "- ¥ 25.00"],
            ["10-18", "报销", "收入", "+ ¥ 320.00"],
          ].map(([date, item, cat, amount]) => (
            <TableRow key={date}>
              <TableCell className="font-mono text-xs text-muted-foreground">
                {date}
              </TableCell>
              <TableCell>{item}</TableCell>
              <TableCell>
                <span className="rounded-md bg-muted px-1.5 py-0.5 text-xs">
                  {cat}
                </span>
              </TableCell>
              <TableCell className="text-right font-mono text-xs">
                {amount}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Demo>
  );
}

export function CarouselSection() {
  return (
    <Demo title="轮播" hint="Carousel · embla">
      <Carousel className="w-full max-w-sm">
        <CarouselContent>
          {Array.from({ length: 5 }, (_, i) => (
            <CarouselItem key={i}>
              <div className="grid h-40 place-items-center rounded-lg border bg-muted/40">
                <div className="text-center">
                  <div className="text-2xl font-semibold">{i + 1}</div>
                  <div className="text-xs text-muted-foreground">第 {i + 1} 张</div>
                </div>
              </div>
            </CarouselItem>
          ))}
        </CarouselContent>
        <CarouselPrevious />
        <CarouselNext />
      </Carousel>
    </Demo>
  );
}

const chartConfig = {
  desktop: { label: "桌面端", color: "var(--chart-1)" },
  mobile: { label: "移动端", color: "var(--chart-2)" },
} satisfies ChartConfig;

const chartData = [
  { month: "1 月", desktop: 186, mobile: 80 },
  { month: "2 月", desktop: 305, mobile: 200 },
  { month: "3 月", desktop: 237, mobile: 120 },
  { month: "4 月", desktop: 273, mobile: 190 },
  { month: "5 月", desktop: 209, mobile: 130 },
  { month: "6 月", desktop: 314, mobile: 240 },
];

export function ChartSection() {
  return (
    <Demo title="柱状图" hint="Chart · recharts" layout="col">
      <ChartContainer config={chartConfig} className="h-64 w-full max-w-2xl">
        <BarChart data={chartData}>
          <CartesianGrid vertical={false} />
          <XAxis
            dataKey="month"
            tickLine={false}
            axisLine={false}
            tickMargin={8}
          />
          <ChartTooltip content={<ChartTooltipContent />} />
          <Bar dataKey="desktop" fill="var(--color-desktop)" radius={4} />
          <Bar dataKey="mobile" fill="var(--color-mobile)" radius={4} />
        </BarChart>
      </ChartContainer>
      <div className="flex gap-4 text-xs text-muted-foreground">
        <span className="flex items-center gap-1.5">
          <span className="size-2 rounded-sm bg-chart-1" />
          桌面端
        </span>
        <span className="flex items-center gap-1.5">
          <span className="size-2 rounded-sm bg-chart-2" />
          移动端
        </span>
      </div>
    </Demo>
  );
}

export function SidebarSection() {
  return (
    <Demo title="侧边栏菜单" hint="SidebarMenu（片段）" layout="col">
      <SidebarProvider className="min-h-0 w-full">
        <div className="flex h-72 w-full max-w-xl overflow-hidden rounded-xl border">
          <aside className="w-52 shrink-0 border-r bg-sidebar p-2 text-sidebar-foreground">
            <div className="px-2 pt-1.5 pb-3 text-xs font-medium text-muted-foreground">
              Mui 应用
            </div>
            <SidebarMenu>
              <SidebarMenuItem>
                <SidebarMenuButton isActive>
                  <House />
                  总览
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton>
                  <Layers />
                  组件
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton>
                  <Palette />
                  配色
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton>
                  <Settings />
                  设置
                </SidebarMenuButton>
              </SidebarMenuItem>
            </SidebarMenu>
          </aside>
          <div className="flex-1 bg-background p-4 text-sm text-muted-foreground">
            内容区
          </div>
        </div>
      </SidebarProvider>
      <p className="text-xs text-muted-foreground">
        完整 Sidebar 需要整页布局（fixed + SidebarInset），这里展示菜单片段。
      </p>
    </Demo>
  );
}
