import { ArrowRight, Check, LoaderCircle, Mail, Plus, Star } from "lucide-react";

import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { Demo, Label } from "@/showcase/Demo";

export function ButtonSection() {
  return (
    <>
      <Demo title="变体" hint="variant">
        <Button>默认</Button>
        <Button variant="secondary">次要</Button>
        <Button variant="outline">描边</Button>
        <Button variant="ghost">幽灵</Button>
        <Button variant="link">链接</Button>
        <Button variant="destructive">危险</Button>
      </Demo>

      <Demo title="尺寸" hint="size">
        <Button size="xs">超小 xs</Button>
        <Button size="sm">小 sm</Button>
        <Button>默认 default</Button>
        <Button size="lg">大 lg</Button>
        <Button size="icon" aria-label="新增">
          <Plus />
        </Button>
        <Button size="icon-sm" aria-label="收藏">
          <Star />
        </Button>
      </Demo>

      <Demo title="图标与状态" hint="disabled / loading">
        <Button>
          <Mail />
          登录邮箱
        </Button>
        <Button variant="outline">
          下一步
          <ArrowRight />
        </Button>
        <Button disabled>不可用</Button>
        <Button disabled>
          <LoaderCircle className="animate-spin" />
          提交中
        </Button>
      </Demo>
    </>
  );
}

export function BadgeSection() {
  return (
    <>
      <Demo title="变体" hint="variant">
        <Badge>默认</Badge>
        <Badge variant="secondary">次要</Badge>
        <Badge variant="outline">描边</Badge>
        <Badge variant="destructive">危险</Badge>
      </Demo>
      <Demo title="带图标" hint="icon">
        <Badge>
          <Check />
          已完成
        </Badge>
        <Badge variant="secondary">
          <Star />
          精选
        </Badge>
        <Badge variant="outline">
          <Mail />
          已订阅
        </Badge>
      </Demo>
    </>
  );
}

export function CardSection() {
  return (
    <Demo title="卡片组成" hint="header / content / footer" layout="wrap">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>本月账单</CardTitle>
          <CardDescription>共 12 笔支出，环比下降 8%</CardDescription>
          <CardAction>
            <Badge variant="secondary">进行中</Badge>
          </CardAction>
        </CardHeader>
        <CardContent>
          <div className="text-3xl font-semibold tracking-tight">¥ 3,280.00</div>
          <p className="mt-1 text-xs text-muted-foreground">
            预算剩余 ¥ 1,720.00
          </p>
        </CardContent>
        <CardFooter className="gap-2">
          <Button size="sm">查看明细</Button>
          <Button size="sm" variant="outline">
            导出
          </Button>
        </CardFooter>
      </Card>

      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>快捷操作</CardTitle>
          <CardDescription>常用入口</CardDescription>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-2">
          <Button variant="outline" className="justify-start">
            <Plus />
            新建账单
          </Button>
          <Button variant="outline" className="justify-start">
            <Star />
            收藏夹
          </Button>
        </CardContent>
      </Card>
    </Demo>
  );
}

export function AvatarSection() {
  return (
    <>
      <Demo title="尺寸与回退" hint="image / fallback">
        <Avatar className="size-6">
          <AvatarFallback className="text-[10px]">M</AvatarFallback>
        </Avatar>
        <Avatar>
          <AvatarImage src="https://github.com/shadcn.png" alt="用户" />
          <AvatarFallback>CN</AvatarFallback>
        </Avatar>
        <Avatar className="size-10">
          <AvatarFallback>11</AvatarFallback>
        </Avatar>
        <Avatar className="size-12">
          <AvatarFallback>BU</AvatarFallback>
        </Avatar>
      </Demo>
      <Demo title="头像组" hint="AvatarGroup">
        <div className="flex items-center">
          <Avatar className="-mr-2 ring-2 ring-background">
            <AvatarFallback>A</AvatarFallback>
          </Avatar>
          <Avatar className="-mr-2 ring-2 ring-background">
            <AvatarFallback>B</AvatarFallback>
          </Avatar>
          <Avatar className="-mr-2 ring-2 ring-background">
            <AvatarFallback>C</AvatarFallback>
          </Avatar>
          <Avatar className="ring-2 ring-background">
            <AvatarFallback>+3</AvatarFallback>
          </Avatar>
        </div>
      </Demo>
    </>
  );
}

export function SeparatorSection() {
  return (
    <>
      <Demo title="水平" hint="orientation=horizontal" layout="col">
        <div className="text-sm">Mui 组件库</div>
        <Separator />
        <div className="text-sm text-muted-foreground">
          用 Tailwind 4 + Radix UI 构建
        </div>
      </Demo>
      <Demo title="垂直" hint="orientation=vertical">
        <div className="flex h-5 items-center gap-3 text-sm">
          <span>博客</span>
          <Separator orientation="vertical" />
          <span>文档</span>
          <Separator orientation="vertical" />
          <span>源码</span>
        </div>
      </Demo>
    </>
  );
}

export function SkeletonSection() {
  return (
    <Demo title="加载占位" hint="animate-pulse" layout="wrap">
      <div className="flex w-full max-w-sm items-center gap-4">
        <Skeleton className="size-12 rounded-full" />
        <div className="flex-1 space-y-2">
          <Skeleton className="h-3.5 w-2/3" />
          <Skeleton className="h-3 w-1/3" />
        </div>
      </div>
      <div className="w-full max-w-sm space-y-2">
        <Skeleton className="h-24 w-full rounded-lg" />
        <div className="flex gap-2">
          <Skeleton className="h-8 flex-1" />
          <Skeleton className="h-8 w-20" />
        </div>
      </div>
    </Demo>
  );
}
