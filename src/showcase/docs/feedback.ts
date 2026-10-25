import type { ComponentDoc } from "./index";

/** 反馈分组（8 个） */
export default {
  alert: {
    import: 'import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"',
    summary: "提示条。页面内的信息 / 成功 / 危险提示，标题与描述两行结构。",
    api: [
      { name: "variant", type: '"default" | "destructive"', default: '"default"', desc: "常规 / 危险（红色）" },
      { name: "AlertTitle", type: "div", desc: "标题行" },
      { name: "AlertDescription", type: "div", desc: "描述行" },
    ],
    examples: [
      {
        title: "两种变体",
        code: `import { Info } from "lucide-react"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"

<Alert>
  <Info />
  <AlertTitle>提示</AlertTitle>
  <AlertDescription>当前为预览版数据。</AlertDescription>
</Alert>

<Alert variant="destructive">
  <AlertTitle>保存失败</AlertTitle>
  <AlertDescription>网络中断，请稍后重试。</AlertDescription>
</Alert>`,
      },
    ],
  },

  progress: {
    import: 'import { Progress } from "@/components/ui/progress"',
    summary: "进度条。0-100 的完成度展示，基于 Radix Progress。",
    api: [
      { name: "value", type: "number", default: "0", desc: "当前进度，0-100" },
      { name: "max", type: "number", default: "100", desc: "最大值" },
      { name: "className", type: "string", desc: "追加类名" },
    ],
    examples: [
      {
        title: "任务进度",
        code: `import { useState, useEffect } from "react"
import { Progress } from "@/components/ui/progress"

const [value, setValue] = useState(13)

useEffect(() => {
  const timer = setInterval(() => setValue((v) => (v >= 100 ? 100 : v + 7)), 400)
  return () => clearInterval(timer)
}, [])

<Progress value={value} className="w-64" />`,
      },
    ],
  },

  dialog: {
    import:
      'import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"',
    summary:
      "模态对话框。基于 Radix Dialog：遮罩 + 居中卡片，焦点锁在弹层内，Esc 关闭；适合表单与确认。",
    api: [
      { name: "open / onOpenChange", type: "boolean / (open: boolean) => void", desc: "受控开关" },
      { name: "defaultOpen", type: "boolean", default: "false", desc: "非受控初始态" },
      { name: "DialogContent", type: 'Props & { showCloseButton?: boolean }', desc: "内容卡片；默认带右上角关闭按钮" },
      { name: "DialogHeader / DialogTitle / DialogDescription / DialogFooter", type: "div / h2 / p / div", desc: "头部 / 标题 / 描述 / 底部操作区" },
      { name: "DialogTrigger / DialogClose", type: "button", desc: "触发器 / 关闭按钮" },
      { name: "DialogPortal / DialogOverlay", type: "—", desc: "已内置在 Content 中，通常不用手动写" },
    ],
    examples: [
      {
        title: "编辑资料弹窗",
        code: `import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

<Dialog>
  <DialogTrigger asChild>
    <Button variant="outline">编辑资料</Button>
  </DialogTrigger>
  <DialogContent>
    <DialogHeader>
      <DialogTitle>编辑资料</DialogTitle>
      <DialogDescription>修改后点击保存。</DialogDescription>
    </DialogHeader>
    <div className="grid gap-2">
      <Label htmlFor="nick">昵称</Label>
      <Input id="nick" defaultValue="Mui 用户" />
    </div>
    <DialogFooter>
      <Button type="submit">保存</Button>
    </DialogFooter>
  </DialogContent>
</Dialog>`,
      },
    ],
  },

  "alert-dialog": {
    import:
      'import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogHeader, AlertDialogTitle, AlertDialogTrigger } from "@/components/ui/alert-dialog"',
    summary:
      "确认框。危险操作的二次确认：点击遮罩不关闭，必须显式选 Action / Cancel；结构与 Dialog 一致。",
    api: [
      { name: "open / onOpenChange", type: "boolean / (open: boolean) => void", desc: "受控开关" },
      { name: "AlertDialogContent", type: 'Props & { size?: "default" | "sm" }', desc: "内容卡片；sm 为紧凑尺寸" },
      { name: "AlertDialogAction / AlertDialogCancel", type: "button", desc: "确认 / 取消按钮，点击后自动关闭" },
      { name: "AlertDialogMedia", type: "div", desc: "标题左侧的图标位" },
      { name: "AlertDialogHeader / Title / Description / Footer", type: "div / h2 / p / div", desc: "结构同 Dialog" },
    ],
    examples: [
      {
        title: "删除确认",
        code: `import { Button } from "@/components/ui/button"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog"

<AlertDialog>
  <AlertDialogTrigger asChild>
    <Button variant="destructive">删除项目</Button>
  </AlertDialogTrigger>
  <AlertDialogContent>
    <AlertDialogHeader>
      <AlertDialogTitle>确定删除？</AlertDialogTitle>
      <AlertDialogDescription>该操作无法撤销。</AlertDialogDescription>
    </AlertDialogHeader>
    <AlertDialogFooter>
      <AlertDialogCancel>取消</AlertDialogCancel>
      <AlertDialogAction>删除</AlertDialogAction>
    </AlertDialogFooter>
  </AlertDialogContent>
</AlertDialog>`,
      },
    ],
  },

  sheet: {
    import:
      'import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet"',
    summary:
      "侧边抽屉。从屏幕边缘滑出的面板，适合设置、筛选与移动导航；结构与 Dialog 一致，多一个 side。",
    api: [
      { name: "open / onOpenChange", type: "boolean / (open: boolean) => void", desc: "受控开关" },
      { name: "SheetContent", type: 'Props & { side?: "top" | "right" | "bottom" | "left"; showCloseButton?: boolean }', desc: "抽屉面板；side 默认 right" },
      { name: "SheetHeader / SheetTitle / SheetDescription / SheetFooter", type: "div / h2 / p / div", desc: "结构同 Dialog" },
      { name: "SheetTrigger / SheetClose", type: "button", desc: "触发器 / 关闭按钮" },
    ],
    examples: [
      {
        title: "左侧设置面板",
        code: `import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet"

<Sheet>
  <SheetTrigger asChild>
    <Button variant="outline">打开设置</Button>
  </SheetTrigger>
  <SheetContent side="left">
    <SheetHeader>
      <SheetTitle>设置</SheetTitle>
    </SheetHeader>
    <div className="grid gap-2 px-4">
      <Label htmlFor="api">API Key</Label>
      <Input id="api" />
    </div>
  </SheetContent>
</Sheet>`,
      },
    ],
  },

  drawer: {
    import:
      'import { Drawer, DrawerContent, DrawerDescription, DrawerHeader, DrawerTitle, DrawerTrigger } from "@/components/ui/drawer"',
    summary:
      "底部抽屉。基于 vaul，从屏幕底部拖出，可向下拖动关闭；适合移动端操作面板。",
    api: [
      { name: "open / onOpenChange", type: "boolean / (open: boolean) => void", desc: "受控开关" },
      { name: "shouldScaleBackground", type: "boolean", desc: "打开时把身后页面缩小（vaul 特性）" },
      { name: "DrawerContent", type: "div", desc: "底部面板，已含拖拽把手" },
      { name: "DrawerHeader / DrawerTitle / DrawerDescription / DrawerFooter", type: "div / h2 / p / div", desc: "结构同 Dialog" },
      { name: "DrawerTrigger / DrawerClose", type: "button", desc: "触发器 / 关闭按钮" },
    ],
    examples: [
      {
        title: "底部操作面板",
        code: `import { Button } from "@/components/ui/button"
import {
  Drawer,
  DrawerContent,
  DrawerDescription,
  DrawerHeader,
  DrawerTitle,
  DrawerTrigger,
} from "@/components/ui/drawer"

<Drawer>
  <DrawerTrigger asChild>
    <Button variant="outline">选择账号</Button>
  </DrawerTrigger>
  <DrawerContent>
    <DrawerHeader>
      <DrawerTitle>切换账号</DrawerTitle>
      <DrawerDescription>选择一个账号继续。</DrawerDescription>
    </DrawerHeader>
  </DrawerContent>
</Drawer>`,
      },
    ],
  },

  tooltip: {
    import:
      'import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip"',
    summary:
      "提示气泡。悬停 / 聚焦时出现的轻量说明，四个方向自动避让；应用根部需包一个 TooltipProvider。",
    api: [
      { name: "TooltipProvider", type: "—", desc: "全局只需一个，包在应用根部" },
      { name: "side", type: '"top" | "right" | "bottom" | "left"', default: '"top"', desc: "弹出方向" },
      { name: "delayDuration", type: "number", default: "200", desc: "悬停多久后弹出（ms）" },
      { name: "TooltipContent", type: 'Props & { sideOffset?: number }', desc: "气泡内容" },
      { name: "TooltipTrigger", type: "button", desc: "触发器，通常配 asChild" },
    ],
    examples: [
      {
        title: "图标按钮提示",
        code: `import { Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip"

<TooltipProvider>
  <Tooltip>
    <TooltipTrigger asChild>
      <Button size="icon" aria-label="新增">
        <Plus />
      </Button>
    </TooltipTrigger>
    <TooltipContent>
      <p>新增组件</p>
    </TooltipContent>
  </Tooltip>
</TooltipProvider>`,
      },
    ],
  },

  sonner: {
    import: 'import { toast } from "sonner"',
    summary:
      "轻提示。页面角落的Toast：成功 / 错误 / 信息 / 加载态，支持 Promise 与操作按钮；先在根部挂一次 Toaster。",
    api: [
      { name: "Toaster", type: "React.ComponentProps<typeof Toaster>", desc: "在应用根部挂载一次；theme 传 light / dark / system" },
      { name: "toast(message)", type: "string | ReactNode", desc: "基础提示，返回 id" },
      { name: "toast.success / error / info / warning / loading", type: "同 toast", desc: "各状态变体" },
      { name: "toast.promise(p, msgs)", type: "Promise", desc: "异步任务：loading → success / error" },
      { name: "toast.dismiss(id?)", type: "—", desc: "关闭指定或全部提示" },
    ],
    examples: [
      {
        title: "触发各类提示",
        code: `import { toast } from "sonner"

toast("事件已创建")

toast.success("保存成功", {
  description: "更改将在下次同步时生效。",
})

toast.error("上传失败，请重试")

toast.promise(saveDraft(), {
  loading: "保存中…",
  success: "已保存",
  error: "保存失败",
})

// 在 App 根部（src/App.tsx）挂一次：
// import { Toaster } from "@/components/ui/sonner"
// <Toaster theme="light" />`,
      },
    ],
  },
} satisfies Record<string, ComponentDoc>;
