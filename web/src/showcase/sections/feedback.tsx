import { useEffect, useState } from "react";
import {
  CircleCheck,
  Info,
  TriangleAlert,
  Trash,
  Check,
  X,
} from "lucide-react";
import { toast } from "sonner";

import {
  Drawer,
  DrawerClose,
  DrawerContent,
  DrawerDescription,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
  DrawerTrigger,
} from "@/components/ui/drawer";
import { Toaster } from "@/components/ui/sonner";

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogMedia,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Progress } from "@/components/ui/progress";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { Demo } from "@/showcase/Demo";

export function AlertSection() {
  return (
    <Demo title="提示条" hint="default / destructive" layout="col">
      <Alert>
        <Info />
        <AlertTitle>组件已更新</AlertTitle>
        <AlertDescription>
          刚拉取了最新的 shadcn new-york 组件，可以直接在源码上修改。
        </AlertDescription>
      </Alert>
      <Alert variant="destructive">
        <TriangleAlert />
        <AlertTitle>删除后不可恢复</AlertTitle>
        <AlertDescription>
          该操作会永久移除本地数据，请确认已备份。
        </AlertDescription>
      </Alert>
      <Alert>
        <CircleCheck />
        <AlertTitle>构建通过</AlertTitle>
        <AlertDescription>TypeScript 与 Vite 构建均无报错。</AlertDescription>
      </Alert>
    </Demo>
  );
}

export function ProgressSection() {
  return (
    <Demo title="进度" hint="value" layout="col" className="max-w-md">
      {[25, 60, 100].map((value) => (
        <div key={value} className="flex items-center gap-4">
          <Progress value={value} className="flex-1" />
          <span className="w-10 text-right font-mono text-xs text-muted-foreground">
            {value}%
          </span>
        </div>
      ))}
    </Demo>
  );
}

export function DialogSection() {
  return (
    <Demo title="对话框" hint="Dialog">
      <Dialog>
        <DialogTrigger asChild>
          <Button variant="outline">打开对话框</Button>
        </DialogTrigger>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>编辑资料</DialogTitle>
            <DialogDescription>
              修改会立即保存到本地，下次启动仍然生效。
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3">
            <div className="grid gap-1.5">
              <Label htmlFor="dialog-name">昵称</Label>
              <Input id="dialog-name" defaultValue="Mui" />
            </div>
            <div className="grid gap-1.5">
              <Label htmlFor="dialog-mail">邮箱</Label>
              <Input id="dialog-mail" placeholder="you@example.com" />
            </div>
          </div>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">取消</Button>
            </DialogClose>
            <DialogClose asChild>
              <Button>保存</Button>
            </DialogClose>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Demo>
  );
}

export function AlertDialogSection() {
  return (
    <Demo title="确认框" hint="AlertDialog">
      <AlertDialog>
        <AlertDialogTrigger asChild>
          <Button variant="destructive">
            <Trash />
            删除项目
          </Button>
        </AlertDialogTrigger>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogMedia>
              <TriangleAlert className="text-destructive" />
            </AlertDialogMedia>
            <AlertDialogTitle>确认删除这个项目？</AlertDialogTitle>
            <AlertDialogDescription>
              项目下的 24 个组件与全部本地修改都会被移除，此操作不可撤销。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction className="bg-destructive text-white hover:bg-destructive/90">
              确认删除
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Demo>
  );
}

export function SheetSection() {
  return (
    <Demo title="抽屉" hint="side=right / left">
      <Sheet>
        <SheetTrigger asChild>
          <Button variant="outline">右侧抽屉</Button>
        </SheetTrigger>
        <SheetContent>
          <SheetHeader>
            <SheetTitle>组件设置</SheetTitle>
            <SheetDescription>调整当前组件库的默认表现。</SheetDescription>
          </SheetHeader>
          <div className="grid gap-3 px-4">
            {["启用玻璃拟态", "显示图标", "紧凑模式"].map((label) => (
              <div
                key={label}
                className="flex items-center justify-between rounded-lg border px-3 py-2 text-sm"
              >
                <span>{label}</span>
                <span className="text-xs text-muted-foreground">默认</span>
              </div>
            ))}
          </div>
          <SheetFooter>
            <Button>应用设置</Button>
          </SheetFooter>
        </SheetContent>
      </Sheet>

      <Sheet>
        <SheetTrigger asChild>
          <Button variant="outline">左侧抽屉</Button>
        </SheetTrigger>
        <SheetContent side="left">
          <SheetHeader>
            <SheetTitle>导航</SheetTitle>
            <SheetDescription>移动端可直接复用这层抽屉。</SheetDescription>
          </SheetHeader>
          <nav className="grid gap-1 px-4 text-sm">
            {["总览", "组件", "配色", "图标", "令牌"].map((item) => (
              <span
                key={item}
                className="rounded-md px-2 py-1.5 text-muted-foreground hover:bg-accent"
              >
                {item}
              </span>
            ))}
          </nav>
        </SheetContent>
      </Sheet>
    </Demo>
  );
}

export function TooltipSection() {
  return (
    <Demo title="工具提示" hint="side">
      {(["top", "right", "bottom", "left"] as const).map((side) => (
        <Tooltip key={side}>
          <TooltipTrigger asChild>
            <Button variant="outline">提示 · {side}</Button>
          </TooltipTrigger>
          <TooltipContent side={side}>
            这是 {side} 方向的提示
          </TooltipContent>
        </Tooltip>
      ))}
    </Demo>
  );
}

export function DrawerSection() {
  return (
    <Demo title="抽屉（移动端）" hint="Drawer · vaul">
      <Drawer>
        <DrawerTrigger asChild>
          <Button variant="outline">打开底部抽屉</Button>
        </DrawerTrigger>
        <DrawerContent>
          <div className="mx-auto w-full max-w-sm">
            <DrawerHeader>
              <DrawerTitle>选择主题</DrawerTitle>
              <DrawerDescription>
                抽屉适合移动端的轻量操作，可向下拖动关闭。
              </DrawerDescription>
            </DrawerHeader>
            <div className="grid gap-2 p-4 pt-0">
              {["浅色", "深色", "跟随系统"].map((item) => (
                <div
                  key={item}
                  className="flex items-center justify-between rounded-lg border px-3 py-2 text-sm"
                >
                  <span>{item}</span>
                  <span className="text-xs text-muted-foreground">可选</span>
                </div>
              ))}
            </div>
            <DrawerFooter>
              <Button>应用</Button>
              <DrawerClose asChild>
                <Button variant="outline">取消</Button>
              </DrawerClose>
            </DrawerFooter>
          </div>
        </DrawerContent>
      </Drawer>
    </Demo>
  );
}

function useIsDark() {
  const [dark, setDark] = useState(() =>
    document.documentElement.classList.contains("dark")
  );
  useEffect(() => {
    const observer = new MutationObserver(() =>
      setDark(document.documentElement.classList.contains("dark"))
    );
    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ["class"],
    });
    return () => observer.disconnect();
  }, []);
  return dark;
}

export function SonnerSection() {
  const dark = useIsDark();
  return (
    <Demo title="轻提示" hint="Sonner">
      <Toaster theme={dark ? "dark" : "light"} position="bottom-right" />
      <Button
        variant="outline"
        onClick={() => toast.success("组件已保存", { description: "已同步到组件库" })}
      >
        <Check />
        成功提示
      </Button>
      <Button
        variant="outline"
        onClick={() => toast.error("上传失败", { description: "请检查网络后重试" })}
      >
        <X />
        错误提示
      </Button>
      <Button
        variant="outline"
        onClick={() => toast.info("已复制到剪贴板")}
      >
        <Info />
        信息提示
      </Button>
      <Button
        variant="outline"
        onClick={() =>
          toast.promise(new Promise((resolve) => setTimeout(resolve, 1500)), {
            loading: "同步中…",
            success: "同步完成",
            error: "同步失败",
          })
        }
      >
        异步任务
      </Button>
    </Demo>
  );
}
