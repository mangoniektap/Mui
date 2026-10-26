import { useState } from "react";
import {
  Bold,
  Bookmark,
  Ellipsis,
  FileText,
  House,
  Image as ImageIcon,
  Inbox,
  Italic,
  Mail,
  Plus,
  Settings,
  Star,
  Underline,
} from "lucide-react";

import { AspectRatio } from "@/components/ui/aspect-ratio";
import { Button } from "@/components/ui/button";
import {
  ButtonGroup,
  ButtonGroupSeparator,
} from "@/components/ui/button-group";
import type { MibuCatAct } from "@/components/ui/cat-pose";
import { DirectionProvider } from "@/components/ui/direction";
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty";
import { Input } from "@/components/ui/input";
import {
  Item,
  ItemActions,
  ItemContent,
  ItemDescription,
  ItemGroup,
  ItemMedia,
  ItemTitle,
} from "@/components/ui/item";
import { Kbd, KbdGroup } from "@/components/ui/kbd";
import { MibuCat, MibuCatMark } from "@/components/ui/mibu-cat";
import { Spinner } from "@/components/ui/spinner";
import { Toggle } from "@/components/ui/toggle";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { TrafficLights, type TrafficLight } from "@/components/ui/traffic-lights";
import { Demo } from "@/showcase/Demo";

export function KbdSection() {
  return (
    <Demo title="按键提示" hint="Kbd / KbdGroup" layout="col">
      <div className="flex items-center gap-3 text-sm">
        <KbdGroup>
          <Kbd>⌘</Kbd>
          <Kbd>K</Kbd>
        </KbdGroup>
        <span className="text-muted-foreground">打开命令面板</span>
      </div>
      <div className="flex items-center gap-3 text-sm">
        <KbdGroup>
          <Kbd>Ctrl</Kbd>
          <Kbd>Shift</Kbd>
          <Kbd>P</Kbd>
        </KbdGroup>
        <span className="text-muted-foreground">切换主题</span>
      </div>
      <div className="flex items-center gap-3 text-sm">
        <Kbd>Esc</Kbd>
        <span className="text-muted-foreground">关闭浮层</span>
      </div>
    </Demo>
  );
}

export function ToggleSection() {
  return (
    <>
      <Demo title="切换按钮" hint="variant / size">
        <Toggle aria-label="收藏">
          <Star />
        </Toggle>
        <Toggle variant="outline" aria-label="收藏">
          <Star />
        </Toggle>
        <Toggle variant="outline" size="sm">
          <Star />
          已收藏
        </Toggle>
        <Toggle disabled aria-label="禁用">
          <Star />
        </Toggle>
      </Demo>
      <Demo title="文字按钮" hint="按下状态">
        <Toggle variant="outline" defaultPressed>
          跟随主题
        </Toggle>
        <Toggle variant="outline">自动保存</Toggle>
      </Demo>
    </>
  );
}

export function ToggleGroupSection() {
  const [align, setAlign] = useState("left");
  const [view, setView] = useState("home");
  return (
    <>
      <Demo title="单选组" hint="type=single · 纯文字" layout="col">
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
        />
        <span className="font-mono text-xs text-muted-foreground">
          align = {align}
        </span>
      </Demo>
      <Demo title="单选组" hint="type=single · 图标 + 文字" layout="col">
        <ToggleGroup
          type="single"
          value={view}
          onValueChange={(value) => value && setView(value)}
          activeColor="var(--primary)"
          items={[
            { value: "home", label: "首页", icon: <House /> },
            { value: "saved", label: "收藏", icon: <Bookmark /> },
            { value: "settings", label: "设置", icon: <Settings /> },
          ]}
        />
        <span className="font-mono text-xs text-muted-foreground">
          view = {view}
        </span>
      </Demo>
      <Demo title="多选组" hint="type=multiple">
        <ToggleGroup type="multiple" variant="outline" defaultValue={["bold", "italic"]}>
          <ToggleGroupItem value="bold" aria-label="加粗">
            <Bold />
          </ToggleGroupItem>
          <ToggleGroupItem value="italic" aria-label="斜体">
            <Italic />
          </ToggleGroupItem>
          <ToggleGroupItem value="underline" aria-label="下划线">
            <Underline />
          </ToggleGroupItem>
        </ToggleGroup>
      </Demo>
    </>
  );
}

export function ButtonGroupSection() {
  return (
    <>
      <Demo title="按钮组" hint="horizontal / vertical">
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
        </ButtonGroup>
      </Demo>
      <Demo title="输入 + 按钮" hint="组合" layout="col" className="max-w-md">
        <ButtonGroup className="w-full">
          <Input placeholder="输入邮箱订阅" />
          <Button>
            <Mail />
            订阅
          </Button>
        </ButtonGroup>
      </Demo>
    </>
  );
}

export function ItemSection() {
  return (
    <Demo title="列表项" hint="media / content / actions" layout="col">
      <ItemGroup className="w-full max-w-lg">
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
        <Item variant="outline">
          <ItemMedia variant="image">
            <div className="grid size-10 place-items-center bg-muted">
              <ImageIcon className="size-4 text-muted-foreground" />
            </div>
          </ItemMedia>
          <ItemContent>
            <ItemTitle>封面图.png</ItemTitle>
            <ItemDescription>860 KB · 上周</ItemDescription>
          </ItemContent>
          <ItemActions>
            <Button variant="outline" size="sm">使用</Button>
          </ItemActions>
        </Item>
      </ItemGroup>
    </Demo>
  );
}

export function EmptySection() {
  return (
    <Demo title="空状态" hint="Empty" layout="col">
      <Empty className="w-full border">
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
      </Empty>
    </Demo>
  );
}

export function SpinnerSection() {
  return (
    <>
      <Demo title="加载指示" hint="size" layout="row">
        <Spinner className="size-3" />
        <Spinner className="size-4" />
        <Spinner className="size-6" />
        <Spinner className="size-8 text-primary" />
      </Demo>
      <Demo title="按钮内加载" hint="搭配 Button">
        <Button disabled>
          <Spinner className="size-4" />
          加载中
        </Button>
        <Button variant="outline" disabled>
          <Spinner className="size-4" />
          同步数据
        </Button>
      </Demo>
    </>
  );
}

export function AspectRatioSection() {
  return (
    <Demo title="固定比例" hint="ratio" layout="wrap">
      <div className="w-full max-w-xs overflow-hidden rounded-lg">
        <AspectRatio
          ratio={16 / 9}
          className="grid place-items-center bg-muted text-xs text-muted-foreground"
        >
          16 : 9
        </AspectRatio>
      </div>
      <div className="w-full max-w-[160px] overflow-hidden rounded-lg">
        <AspectRatio
          ratio={1}
          className="grid place-items-center bg-accent text-xs text-accent-foreground"
        >
          1 : 1
        </AspectRatio>
      </div>
      <div className="w-full max-w-[160px] overflow-hidden rounded-lg">
        <AspectRatio
          ratio={4 / 5}
          className="grid place-items-center bg-secondary text-xs text-secondary-foreground"
        >
          4 : 5
        </AspectRatio>
      </div>
    </Demo>
  );
}

export function DirectionSection() {
  return (
    <Demo title="RTL 方向" hint="DirectionProvider" layout="col">
      <DirectionProvider dir="rtl">
        <div dir="rtl" className="w-full max-w-sm rounded-lg border p-4 text-sm">
          <p className="font-medium">مرحبا بك في Mui</p>
          <p className="mt-1 text-muted-foreground">
            容器内的 Radix 浮层会自动跟随从右到左的布局。
          </p>
        </div>
      </DirectionProvider>
      <p className="text-xs text-muted-foreground">
        需要整棵树都是 RTL 时，把 DirectionProvider 提到应用根部。
      </p>
    </Demo>
  );
}

/** 三盏灯各用自身颜色：红 / 黄 / 绿，不读主题 */
const LIGHTS: TrafficLight[] = [
  { value: "library", label: "资料库", color: "#ff5f57" },
  { value: "wordbook", label: "生词本", color: "#febc2e" },
  { value: "settings", label: "设置", color: "#28c840" },
];

export function TrafficLightsSection() {
  const [page, setPage] = useState("library");
  return (
    <>
      <Demo title="红绿灯" hint="items / onValueChange" layout="col">
        <TrafficLights items={LIGHTS} value={page} onValueChange={setPage} />
        <div className="w-full rounded-lg border p-4 text-sm">
          当前页面：
          <span className="font-medium">
            {LIGHTS.find((light) => light.value === page)?.label}
          </span>
        </div>
      </Demo>
      <Demo title="尺寸" hint="sm / md / lg">
        <TrafficLights size="sm" items={LIGHTS} defaultValue="library" />
        <TrafficLights items={LIGHTS} defaultValue="wordbook" />
        <TrafficLights size="lg" items={LIGHTS} defaultValue="settings" />
      </Demo>
      <Demo title="禁用" hint="disabled">
        <TrafficLights items={LIGHTS} defaultValue="library" disabled />
      </Demo>
    </>
  );
}

const CAT_ACTS: { act: MibuCatAct; label: string }[] = [
  { act: "idle", label: "常态" },
  { act: "wave", label: "招手" },
  { act: "wag", label: "摇尾" },
  { act: "sleep", label: "打盹" },
  { act: "hop", label: "蹦跶" },
  { act: "alert", label: "警觉" },
];

export function MibuCatSection() {
  const [act, setAct] = useState<MibuCatAct>("idle");
  return (
    <>
      <Demo title="待机的猫" hint="idleActs" layout="col">
        <div className="flex items-center gap-4">
          <MibuCat aria-label="待机的猫" />
          <p className="text-sm text-muted-foreground">
            不传 act 时待机随机做小动作：招手、摇尾、打盹、蹦跶，动作之间平滑过渡。
          </p>
        </div>
      </Demo>
      <Demo title="指定动作" hint="act" layout="col">
        <div className="flex flex-wrap items-center gap-2">
          {CAT_ACTS.map((item) => (
            <Button
              key={item.act}
              variant={act === item.act ? "default" : "outline"}
              size="sm"
              onClick={() => setAct(item.act)}
            >
              {item.label}
            </Button>
          ))}
        </div>
        <MibuCat act={act} />
      </Demo>
      <Demo title="静态标记" hint="MibuCatMark" layout="row">
        <MibuCatMark act="idle" className="size-12" />
        <MibuCatMark act="wave" className="size-12" />
        <MibuCatMark act="wag" className="size-12" />
        <MibuCatMark act="sleep" className="size-12" />
        <MibuCatMark act="hop" className="size-12" />
        <MibuCatMark act="alert" className="size-12" />
      </Demo>
    </>
  );
}
