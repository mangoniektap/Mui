import { useState } from "react";
import {
  ChevronDown,
  CreditCard,
  LogOut,
  Settings,
  Share2,
  User,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuShortcut,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Popover,
  PopoverContent,
  PopoverDescription,
  PopoverHeader,
  PopoverTitle,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Separator } from "@/components/ui/separator";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Demo } from "@/showcase/Demo";

export function TabsSection() {
  return (
    <Demo title="标签页" hint="Tabs" layout="col">
      <Tabs defaultValue="account" className="w-full max-w-md">
        <TabsList>
          <TabsTrigger value="account">账户</TabsTrigger>
          <TabsTrigger value="password">密码</TabsTrigger>
          <TabsTrigger value="notify">通知</TabsTrigger>
        </TabsList>
        <TabsContent value="account">
          <div className="rounded-lg border p-4 text-sm">
            <p className="font-medium">账户信息</p>
            <p className="mt-1 text-muted-foreground">
              当前登录：mui@example.com
            </p>
          </div>
        </TabsContent>
        <TabsContent value="password">
          <div className="rounded-lg border p-4 text-sm">
            <p className="font-medium">修改密码</p>
            <p className="mt-1 text-muted-foreground">上次修改：30 天前</p>
          </div>
        </TabsContent>
        <TabsContent value="notify">
          <div className="rounded-lg border p-4 text-sm">
            <p className="font-medium">通知偏好</p>
            <p className="mt-1 text-muted-foreground">账单提醒、每周摘要</p>
          </div>
        </TabsContent>
      </Tabs>
    </Demo>
  );
}

export function AccordionSection() {
  return (
    <Demo title="折叠面板" hint="type=single / multiple" layout="col">
      <Accordion type="single" collapsible className="w-full max-w-lg">
        <AccordionItem value="item-1">
          <AccordionTrigger>怎么修改组件样式？</AccordionTrigger>
          <AccordionContent>
            直接改 src/components/ui 下的源码，保存后 Vite 热更新会立刻反映在右侧。
          </AccordionContent>
        </AccordionItem>
        <AccordionItem value="item-2">
          <AccordionTrigger>令牌在哪里定义？</AccordionTrigger>
          <AccordionContent>
            src/index.css 的 :root 与 .dark 块，全部使用 oklch 色彩空间。
          </AccordionContent>
        </AccordionItem>
        <AccordionItem value="item-3">
          <AccordionTrigger>能同步给 M11BU / MREAD 吗？</AccordionTrigger>
          <AccordionContent>
            可以，后续把 src/components/ui 与令牌文件复制过去，或打包成独立依赖。
          </AccordionContent>
        </AccordionItem>
      </Accordion>
    </Demo>
  );
}

export function PopoverSection() {
  return (
    <Demo title="气泡卡片" hint="Popover">
      <Popover>
        <PopoverTrigger asChild>
          <Button variant="outline">查看详情</Button>
        </PopoverTrigger>
        <PopoverContent align="start">
          <PopoverHeader>
            <PopoverTitle className="text-sm font-medium">标题</PopoverTitle>
            <PopoverDescription className="text-xs text-muted-foreground">
              气泡卡片适合放轻量表单或说明。
            </PopoverDescription>
          </PopoverHeader>
          <Separator className="my-3" />
          <ul className="space-y-1.5 text-xs text-muted-foreground">
            <li>· 自动避开视口边缘</li>
            <li>· 点击外部关闭</li>
            <li>· 与 Dialog 共用焦点管理</li>
          </ul>
        </PopoverContent>
      </Popover>
    </Demo>
  );
}

export function DropdownMenuSection() {
  const [showBookmarks, setShowBookmarks] = useState(true);
  const [theme, setTheme] = useState("system");

  return (
    <Demo title="下拉菜单" hint="DropdownMenu">
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="outline">
            打开菜单
            <ChevronDown />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="start" className="w-56">
          <DropdownMenuLabel>我的账户</DropdownMenuLabel>
          <DropdownMenuGroup>
            <DropdownMenuItem>
              <User />
              个人资料
              <DropdownMenuShortcut>⇧⌘P</DropdownMenuShortcut>
            </DropdownMenuItem>
            <DropdownMenuItem>
              <CreditCard />
              账单
              <DropdownMenuShortcut>⌘B</DropdownMenuShortcut>
            </DropdownMenuItem>
            <DropdownMenuItem>
              <Settings />
              设置
              <DropdownMenuShortcut>⌘S</DropdownMenuShortcut>
            </DropdownMenuItem>
          </DropdownMenuGroup>
          <DropdownMenuSeparator />
          <DropdownMenuCheckboxItem
            checked={showBookmarks}
            onCheckedChange={(value) => setShowBookmarks(value === true)}
          >
            显示收藏夹
          </DropdownMenuCheckboxItem>
          <DropdownMenuSub>
            <DropdownMenuSubTrigger>主题</DropdownMenuSubTrigger>
            <DropdownMenuSubContent>
              <DropdownMenuRadioGroup value={theme} onValueChange={setTheme}>
                <DropdownMenuRadioItem value="light">浅色</DropdownMenuRadioItem>
                <DropdownMenuRadioItem value="dark">深色</DropdownMenuRadioItem>
                <DropdownMenuRadioItem value="system">跟随系统</DropdownMenuRadioItem>
              </DropdownMenuRadioGroup>
            </DropdownMenuSubContent>
          </DropdownMenuSub>
          <DropdownMenuSeparator />
          <DropdownMenuItem>
            <Share2 />
            分享
          </DropdownMenuItem>
          <DropdownMenuItem variant="destructive">
            <LogOut />
            退出登录
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </Demo>
  );
}
