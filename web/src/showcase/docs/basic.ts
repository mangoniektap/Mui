import type { ComponentDoc } from "./index";

/** 基础分组（17 个） */
export default {
  button: {
    import: 'import { Button } from "@/components/ui/button"',
    summary:
      "按钮。六种变体覆盖主次与危险操作，六种常规尺寸加四种图标尺寸；asChild 可把按钮样式与行为合并到唯一子元素上（如路由 Link），不再渲染 button 标签。",
    api: [
      {
        name: "variant",
        type: '"default" | "secondary" | "outline" | "ghost" | "link" | "destructive"',
        default: '"default"',
        desc: "视觉样式：主色 / 次色 / 描边 / 幽灵 / 链接 / 危险",
      },
      {
        name: "size",
        type: '"default" | "xs" | "sm" | "lg" | "icon" | "icon-xs" | "icon-sm" | "icon-lg"',
        default: '"default"',
        desc: "尺寸；icon 系列为正方形图标按钮",
      },
      { name: "asChild", type: "boolean", default: "false", desc: "把样式与行为合并到唯一子元素" },
      { name: "disabled", type: "boolean", default: "false", desc: "禁用；原生 button 属性" },
      { name: "className", type: "string", desc: "追加类名，与内置样式合并" },
    ],
    examples: [
      {
        title: "变体与尺寸",
        code: `import { Button } from "@/components/ui/button"

<Button>默认</Button>
<Button variant="secondary">次要</Button>
<Button variant="outline">描边</Button>
<Button variant="ghost">幽灵</Button>
<Button variant="link">链接</Button>
<Button variant="destructive">删除</Button>

<Button size="sm">小</Button>
<Button size="lg">大</Button>`,
      },
      {
        title: "图标按钮与 asChild",
        code: `import { Mail } from "lucide-react"
import { Button } from "@/components/ui/button"

<Button>
  <Mail />
  登录邮箱
</Button>

<Button size="icon" aria-label="新增">
  <Plus />
</Button>

// 套到路由组件上：样式与焦点行为保留，渲染出的是 <a>
<Button asChild>
  <a href="/docs">查看文档</a>
</Button>`,
      },
    ],
  },

  badge: {
    import: 'import { Badge } from "@/components/ui/badge"',
    summary:
      "徽章。四种变体的状态小标签，可内嵌 lucide 图标；asChild 可把徽章样式套到链接等元素上。",
    api: [
      {
        name: "variant",
        type: '"default" | "secondary" | "outline" | "destructive"',
        default: '"default"',
        desc: "主色 / 次色 / 描边 / 危险",
      },
      { name: "asChild", type: "boolean", default: "false", desc: "把样式合并到唯一子元素" },
      { name: "className", type: "string", desc: "追加类名" },
    ],
    examples: [
      {
        title: "变体与图标",
        code: `import { Check, Star } from "lucide-react"
import { Badge } from "@/components/ui/badge"

<Badge>默认</Badge>
<Badge variant="secondary">次要</Badge>
<Badge variant="outline">描边</Badge>
<Badge variant="destructive">危险</Badge>

<Badge variant="secondary">
  <Star />
  精选
</Badge>`,
      },
    ],
  },

  card: {
    import:
      'import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"',
    summary:
      "卡片。内容容器：Header（标题 / 描述 / 角标操作）、Content、Footer 三段式组合，圆角、边框与阴影全部走设计令牌。",
    api: [
      { name: "Card", type: "React.ComponentProps<\"div\">", desc: "卡片本体" },
      { name: "CardHeader", type: "React.ComponentProps<\"div\">", desc: "头部区域" },
      { name: "CardTitle", type: "React.ComponentProps<\"div\">", desc: "标题" },
      { name: "CardDescription", type: "React.ComponentProps<\"div\">", desc: "描述文字" },
      { name: "CardAction", type: "React.ComponentProps<\"div\">", desc: "头部右侧的角标操作区" },
      { name: "CardContent", type: "React.ComponentProps<\"div\">", desc: "内容区" },
      { name: "CardFooter", type: "React.ComponentProps<\"div\">", desc: "底部操作区" },
    ],
    examples: [
      {
        title: "账单卡片",
        code: `import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"

<Card className="max-w-sm">
  <CardHeader>
    <CardTitle>本月账单</CardTitle>
    <CardDescription>共 12 笔支出，环比下降 8%</CardDescription>
    <CardAction>
      <Badge variant="secondary">进行中</Badge>
    </CardAction>
  </CardHeader>
  <CardContent>
    <div className="text-3xl font-semibold">¥ 3,280.00</div>
  </CardContent>
  <CardFooter>
    <Button size="sm">查看明细</Button>
  </CardFooter>
</Card>`,
      },
    ],
  },

  avatar: {
    import: 'import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"',
    summary:
      "头像。图片加载失败自动回退到文字；AvatarGroup 叠放成组，AvatarGroupCount 显示溢出计数，AvatarBadge 挂状态点。",
    api: [
      { name: "AvatarImage", type: 'React.ComponentProps<"img">', desc: "图片；src / alt 走原生属性" },
      { name: "AvatarFallback", type: 'React.ComponentProps<"span">', desc: "回退内容，通常放文字或图标" },
      { name: "AvatarGroup", type: 'React.ComponentProps<"div"> & { size?: "default" | "sm" | "lg" }', desc: "叠放组；size 控制整体大小" },
      { name: "AvatarGroupCount", type: 'React.ComponentProps<"div">', desc: "溢出计数，如 +3" },
      { name: "AvatarBadge", type: 'React.ComponentProps<"span">', desc: "右下角状态点" },
    ],
    examples: [
      {
        title: "回退与头像组",
        code: `import {
  Avatar,
  AvatarFallback,
  AvatarGroup,
  AvatarGroupCount,
  AvatarImage,
} from "@/components/ui/avatar"

<Avatar>
  <AvatarImage src="https://github.com/shadcn.png" alt="shadcn" />
  <AvatarFallback>CN</AvatarFallback>
</Avatar>

<AvatarGroup>
  <Avatar><AvatarFallback>甲</AvatarFallback></Avatar>
  <Avatar><AvatarFallback>乙</AvatarFallback></Avatar>
  <AvatarGroupCount>+3</AvatarGroupCount>
</AvatarGroup>`,
      },
    ],
  },

  separator: {
    import: 'import { Separator } from "@/components/ui/separator"',
    summary:
      "分隔线。基于 Radix Separator，水平 / 垂直两种方向；默认 decorative（纯装饰，不进入无障碍树）。",
    api: [
      { name: "orientation", type: '"horizontal" | "vertical"', default: '"horizontal"', desc: "方向" },
      { name: "decorative", type: "boolean", default: "true", desc: "false 时按 role=\"separator\" 暴露给读屏" },
      { name: "className", type: "string", desc: "追加类名；垂直分隔需父容器给高度" },
    ],
    examples: [
      {
        title: "水平与垂直",
        code: `import { Separator } from "@/components/ui/separator"

<div>
  <p>上一段</p>
  <Separator className="my-4" />
  <p>下一段</p>
</div>

<div className="flex h-5 items-center gap-4 text-sm">
  <span>博客</span>
  <Separator orientation="vertical" />
  <span>文档</span>
</div>`,
      },
    ],
  },

  skeleton: {
    import: 'import { Skeleton } from "@/components/ui/skeleton"',
    summary:
      "骨架屏。一个带脉冲动画的占位灰块，尺寸与形状全部由 className 决定，常与 Suspense 搭配。",
    api: [{ name: "className", type: "string", desc: "控制宽高与圆角，如 h-4 w-[250px]" }],
    examples: [
      {
        title: "卡片骨架",
        code: `import { Skeleton } from "@/components/ui/skeleton"

<div className="flex items-center gap-4">
  <Skeleton className="size-12 rounded-full" />
  <div className="space-y-2">
    <Skeleton className="h-4 w-[250px]" />
    <Skeleton className="h-4 w-[200px]" />
  </div>
</div>`,
      },
    ],
  },

  kbd: {
    import: 'import { Kbd, KbdGroup } from "@/components/ui/kbd"',
    summary:
      "键盘按键提示。Kbd 渲染单个键帽，KbdGroup 把多个键排成一组（如 ⌘K）；在 Tooltip 内会自动换成适配深色的配色。",
    api: [
      { name: "Kbd", type: 'React.ComponentProps<"kbd">', desc: "单个键帽，子元素为键名" },
      { name: "KbdGroup", type: 'React.ComponentProps<"div">', desc: "按键组，横向排列" },
    ],
    examples: [
      {
        title: "快捷键提示",
        code: `import { Kbd, KbdGroup } from "@/components/ui/kbd"

<KbdGroup>
  <Kbd>⌘</Kbd>
  <Kbd>K</Kbd>
</KbdGroup>
<span className="text-muted-foreground">打开命令面板</span>

<Kbd>Esc</Kbd>`,
      },
    ],
  },

  toggle: {
    import: 'import { Toggle } from "@/components/ui/toggle"',
    summary:
      "切换按钮。按下 / 弹起两种状态，可作图标开关或文字开关；按压态由 Radix 管理（pressed / defaultPressed / onPressedChange）。",
    api: [
      { name: "variant", type: '"default" | "outline"', default: '"default"', desc: "无底色 / 描边" },
      { name: "size", type: '"default" | "sm" | "lg"', default: '"default"', desc: "尺寸" },
      { name: "pressed", type: "boolean", desc: "受控按压态" },
      { name: "defaultPressed", type: "boolean", default: "false", desc: "非受控初始态" },
      { name: "onPressedChange", type: "(pressed: boolean) => void", desc: "按压态变化回调" },
      { name: "disabled", type: "boolean", default: "false", desc: "禁用" },
    ],
    examples: [
      {
        title: "图标开关与文字开关",
        code: `import { Star } from "lucide-react"
import { Toggle } from "@/components/ui/toggle"

<Toggle aria-label="收藏">
  <Star />
</Toggle>

<Toggle variant="outline" size="sm">
  <Star />
  已收藏
</Toggle>

<Toggle variant="outline" defaultPressed>
  跟随主题
</Toggle>`,
      },
    ],
  },

  "toggle-group": {
    import:
      'import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"',
    summary:
      "切换按钮组。单选组（type=\"single\"）是胶囊底座 + 液态玻璃滑块的分段控件，支持 items 数据驱动、可拖动、按住有按压动画；多选组（type=\"multiple\"）仍是普通按钮组，手写 ToggleGroupItem。",
    api: [
      { name: "type", type: '"single" | "multiple"', desc: "单选走分段控件，多选走按钮组" },
      { name: "value / defaultValue", type: "string | string[]", desc: "选中值；单选为 string，多选为 string[]" },
      { name: "onValueChange", type: "(value: string | string[]) => void", desc: "选中变化回调" },
      { name: "items", type: "{ value: string; label: string; icon?: ReactNode }[]", desc: "仅单选组：栏目清单，给了就不写 ToggleGroupItem；各栏 label 请等长" },
      { name: "activeColor", type: "string", desc: "仅单选组：被玻璃盖住部分的文字 / 图标颜色，支持 var(--x)" },
      { name: "variant / size", type: "toggleVariants", desc: "仅多选组生效，传给每个 ToggleGroupItem" },
      { name: "spacing", type: "number", default: "0", desc: "仅多选组：按钮之间的间距" },
    ],
    examples: [
      {
        title: "单选分段控件（数据驱动）",
        code: `import { useState } from "react"
import { ToggleGroup } from "@/components/ui/toggle-group"

const [align, setAlign] = useState("left")

<ToggleGroup
  type="single"
  value={align}
  onValueChange={(value) => value && setAlign(value)}
  activeColor="var(--primary)"
  items={[
    { value: "left", label: "靠左" },
    { value: "center", label: "居中" },
    { value: "right", label: "靠右" },
  ]}
/>`,
      },
      {
        title: "多选按钮组",
        code: `import { Bold, Italic, Underline } from "lucide-react"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"

<ToggleGroup type="multiple" variant="outline" defaultValue={["bold"]}>
  <ToggleGroupItem value="bold" aria-label="加粗">
    <Bold />
  </ToggleGroupItem>
  <ToggleGroupItem value="italic" aria-label="斜体">
    <Italic />
  </ToggleGroupItem>
  <ToggleGroupItem value="underline" aria-label="下划线">
    <Underline />
  </ToggleGroupItem>
</ToggleGroup>`,
      },
    ],
  },

  "button-group": {
    import:
      'import { ButtonGroup, ButtonGroupSeparator } from "@/components/ui/button-group"',
    summary:
      "按钮组。横 / 纵排列，子按钮自动首尾圆角；ButtonGroupSeparator 插分隔线，ButtonGroupText 放文字段，也可直接包 Input 组成搜索框。",
    api: [
      { name: "orientation", type: '"horizontal" | "vertical"', default: '"horizontal"', desc: "排列方向" },
      { name: "ButtonGroupSeparator", type: "React.ComponentProps<\"div\">", desc: "组内分隔线" },
      { name: "ButtonGroupText", type: "React.ComponentProps<\"div\">", desc: "组内文字段" },
    ],
    examples: [
      {
        title: "横纵排列与组合输入",
        code: `import { Button } from "@/components/ui/button"
import { ButtonGroup, ButtonGroupSeparator } from "@/components/ui/button-group"
import { Input } from "@/components/ui/input"

<ButtonGroup>
  <Button variant="outline">日</Button>
  <Button variant="outline">周</Button>
  <Button variant="outline">月</Button>
</ButtonGroup>

<ButtonGroup>
  <Button variant="outline">保存</Button>
  <ButtonGroupSeparator />
  <Button variant="outline">另存为</Button>
</ButtonGroup>

<ButtonGroup orientation="vertical">
  <Button variant="outline" size="sm">上移</Button>
  <Button variant="outline" size="sm">下移</Button>
</ButtonGroup>`,
      },
    ],
  },

  item: {
    import:
      'import { Item, ItemActions, ItemContent, ItemMedia, ItemTitle } from "@/components/ui/item"',
    summary:
      "列表项。媒体 + 内容 + 操作区的标准行结构；asChild 可整行变成链接或按钮。",
    api: [
      { name: "variant", type: '"default" | "outline" | "muted"', default: '"default"', desc: "无底 / 描边 / 灰底" },
      { name: "size", type: '"default" | "sm"', default: '"default"', desc: "内边距与字号" },
      { name: "asChild", type: "boolean", default: "false", desc: "把整行样式合并到子元素（如 a）" },
      { name: "ItemMedia", type: 'React.ComponentProps<"div"> & { variant?: "default" | "icon" | "image" }', desc: "左侧媒体区" },
      { name: "ItemContent / ItemTitle / ItemDescription", type: "div / div / p", desc: "内容区 / 标题 / 描述" },
      { name: "ItemActions", type: "div", desc: "右侧操作区" },
      { name: "ItemGroup / ItemSeparator / ItemHeader / ItemFooter", type: "div / div / div / div", desc: "列表容器 / 项间分隔 / 头部 / 底部" },
    ],
    examples: [
      {
        title: "文件列表项",
        code: `import { Ellipsis, FileText } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Item,
  ItemActions,
  ItemContent,
  ItemDescription,
  ItemGroup,
  ItemMedia,
  ItemTitle,
} from "@/components/ui/item"

<ItemGroup className="max-w-lg">
  <Item variant="outline">
    <ItemMedia variant="icon">
      <FileText />
    </ItemMedia>
    <ItemContent>
      <ItemTitle>月度报告.pdf</ItemTitle>
      <ItemDescription>2.4 MB · 昨天更新</ItemDescription>
    </ItemContent>
    <ItemActions>
      <Button variant="ghost" size="icon-sm" aria-label="更多">
        <Ellipsis />
      </Button>
    </ItemActions>
  </Item>
</ItemGroup>`,
      },
    ],
  },

  empty: {
    import:
      'import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from "@/components/ui/empty"',
    summary:
      "空状态。图标 + 标题 + 描述 + 操作区的居中引导占位，适合列表无数据、搜索无结果等场景。",
    api: [
      { name: "Empty", type: "div", desc: "容器，通常给边框与内边距" },
      { name: "EmptyHeader", type: "div", desc: "上半部分：图标与文字" },
      { name: "EmptyMedia", type: 'div & { variant?: "default" | "icon" }', desc: "图标 / 插图区" },
      { name: "EmptyTitle", type: "h5 语义", desc: "标题" },
      { name: "EmptyDescription", type: "p", desc: "描述文字" },
      { name: "EmptyContent", type: "div", desc: "操作区，放按钮" },
    ],
    examples: [
      {
        title: "无数据引导",
        code: `import { Inbox, Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"

<Empty className="border">
  <EmptyHeader>
    <EmptyMedia variant="icon">
      <Inbox />
    </EmptyMedia>
    <EmptyTitle>暂无消息</EmptyTitle>
    <EmptyDescription>收到的新消息会显示在这里。</EmptyDescription>
  </EmptyHeader>
  <EmptyContent>
    <Button size="sm">
      <Plus />
      新建会话
    </Button>
  </EmptyContent>
</Empty>`,
      },
    ],
  },

  spinner: {
    import: 'import { Spinner } from "@/components/ui/spinner"',
    summary:
      "加载指示。旋转图标，默认 size-4 并带 role=\"status\"；大小与颜色用 className 控制。",
    api: [{ name: "className", type: "string", desc: "尺寸（size-*）与颜色（text-*）" }],
    examples: [
      {
        title: "按钮内加载",
        code: `import { Button } from "@/components/ui/button"
import { Spinner } from "@/components/ui/spinner"

<Button disabled>
  <Spinner />
  加载中
</Button>

<Button variant="outline" disabled>
  <Spinner className="text-primary" />
  同步数据
</Button>`,
      },
    ],
  },

  "aspect-ratio": {
    import: 'import { AspectRatio } from "@/components/ui/aspect-ratio"',
    summary:
      "固定比例容器。按 ratio 撑高，内部随意布局；适合图片位、视频位与卡片封面。",
    api: [
      { name: "ratio", type: "number", desc: "宽 / 高，如 16 / 9、1、4 / 5" },
      { name: "className", type: "string", desc: "追加类名" },
    ],
    examples: [
      {
        title: "图片占位",
        code: `import { AspectRatio } from "@/components/ui/aspect-ratio"

<div className="w-full max-w-xs overflow-hidden rounded-lg">
  <AspectRatio ratio={16 / 9} className="grid place-items-center bg-muted">
    16 : 9
  </AspectRatio>
</div>`,
      },
    ],
  },

  direction: {
    import: 'import { DirectionProvider, useDirection } from "@/components/ui/direction"',
    summary:
      "RTL 方向提供者。包住它的 Radix 浮层（菜单、弹层、提示）会自动跟随从右到左布局；useDirection 读当前方向。",
    api: [
      { name: "dir / direction", type: '"ltr" | "rtl"', default: '"ltr"', desc: "文本方向" },
      { name: "useDirection()", type: "() => \"ltr\" | \"rtl\"", desc: "读当前方向" },
    ],
    examples: [
      {
        title: "RTL 子树",
        code: `import { DirectionProvider } from "@/components/ui/direction"

<DirectionProvider direction="rtl">
  <div dir="rtl">مرحبا بك في Mui</div>
</DirectionProvider>`,
      },
    ],
  },

  "traffic-lights": {
    import:
      'import { TrafficLights, type TrafficLight } from "@/components/ui/traffic-lights"',
    summary:
      "红绿灯。一排圆点开关，每盏灯各用自身颜色（不读主题）；点击后光斑以该灯颜色从灯心扩散铺满视口，再淡出到目标内容——切换是被颜色盖过去，不是硬跳。光斑用 portal 挂到 body，不被任何 overflow 容器裁剪；铺满的瞬间才同步触发切换，避免闪出旧内容。",
    api: [
      {
        name: "items",
        type: "TrafficLight[]",
        desc: "灯列表：value 唯一值 / label 无障碍名称 / color 自身颜色；顺序即左到右",
      },
      { name: "value", type: "string", desc: "受控：当前点亮的灯" },
      { name: "defaultValue", type: "string", desc: "非受控：默认点亮的灯，默认取第一盏" },
      {
        name: "onValueChange",
        type: "(value, origin) => void",
        desc: "切换回调；origin 给出灯心坐标与颜色，在光斑铺满视口时触发",
      },
      { name: "size", type: '"sm" | "md" | "lg"', default: '"md"', desc: "灯直径：10 / 12 / 16 px" },
      { name: "disabled", type: "boolean", default: "false", desc: "禁用整组灯" },
      { name: "className", type: "string", desc: "追加类名，与内置样式合并" },
    ],
    examples: [
      {
        title: "三色灯 + 页面切换",
        code: `import { TrafficLights, type TrafficLight } from "@/components/ui/traffic-lights"

const lights: TrafficLight[] = [
  { value: "library", label: "资料库", color: "#ff5f57" },
  { value: "wordbook", label: "生词本", color: "#febc2e" },
  { value: "settings", label: "设置", color: "#28c840" },
]

const [page, setPage] = useState("library")

<TrafficLights items={lights} value={page} onValueChange={setPage} />`,
      },
      {
        title: "非受控",
        code: `<TrafficLights items={lights} defaultValue="wordbook" />`,
      },
    ],
  },

  "mibu-cat": {
    import: 'import { MibuCat, MibuCatMark } from "@/components/ui/mibu-cat"',
    summary:
      "像素猫。24 格 viewBox 里全用整格矩形拼出的小猫，配合 crispEdges 保持像素边缘；五种动作（常态 / 招手 / 摇尾 / 打盹 / 蹦跶 / 警觉）之间是姿态数值的连续过渡，没有关键帧跳变。三种颜色（毛色 / 浅色 / 墨色）定义在 styles/mibu-cat.css，取值与 MREAD 原版一致，不区分亮暗主题。MibuCatMark 是去掉按钮的纯图形，act / className 两个参数。",
    api: [
      {
        name: "act",
        type: 'MibuCatAct = "idle" | "wave" | "wag" | "sleep" | "hop" | "alert"',
        desc: "受控动作；不传则内部从 defaultAct 起步",
      },
      { name: "defaultAct", type: "MibuCatAct", default: '"idle"', desc: "非受控初始动作" },
      { name: "idleActs", type: "boolean", default: "true", desc: "待机是否随机做小动作" },
      { name: "onClick", type: "() => void", desc: "点击回调；原生 button 属性" },
      { name: "disabled", type: "boolean", default: "false", desc: "禁用；原生 button 属性" },
      { name: "className", type: "string", desc: "追加类名，与内置样式合并" },
    ],
    examples: [
      {
        title: "待机的猫（随机小动作）",
        code: `import { MibuCat } from "@/components/ui/mibu-cat"

<MibuCat aria-label="待机的猫" />`,
      },
      {
        title: "指定动作",
        code: `import { useState } from "react"
import { MibuCat } from "@/components/ui/mibu-cat"
import type { MibuCatAct } from "@/components/ui/cat-pose"

const [act, setAct] = useState<MibuCatAct>("idle")

<MibuCat act={act} onClick={() => setAct(act === "alert" ? "idle" : "alert")} />`,
      },
      {
        title: "静态标记",
        code: `import { MibuCatMark } from "@/components/ui/mibu-cat"

<MibuCatMark act="sleep" className="size-12" />`,
      },
    ],
  },
} satisfies Record<string, ComponentDoc>;
