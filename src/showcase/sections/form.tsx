import { useState } from "react";
import { Mail, Search } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Slider } from "@/components/ui/slider";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { Demo } from "@/showcase/Demo";

export function InputSection() {
  return (
    <>
      <Demo title="基础输入" layout="col" className="max-w-md">
        <Input placeholder="请输入邮箱地址" />
        <Input type="password" placeholder="请输入密码" />
        <Input disabled placeholder="禁用状态" />
        <Input aria-invalid placeholder="校验失败状态" defaultValue="错误的邮箱" />
      </Demo>
      <Demo title="带图标" layout="col" className="max-w-md">
        <div className="relative">
          <Mail className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input className="pl-9" placeholder="you@example.com" />
        </div>
        <div className="flex gap-2">
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input className="pl-9" placeholder="搜索…" />
          </div>
          <Button>搜索</Button>
        </div>
      </Demo>
    </>
  );
}

export function TextareaSection() {
  return (
    <Demo title="多行文本" layout="col" className="max-w-md">
      <Textarea placeholder="写点什么…" />
      <Textarea placeholder="禁用状态" disabled />
      <Textarea aria-invalid defaultValue="超出字数限制的内容" />
    </Demo>
  );
}

export function LabelSection() {
  return (
    <Demo title="标签与控件" layout="col" className="max-w-md">
      <div className="grid gap-2">
        <Label htmlFor="demo-email">邮箱</Label>
        <Input id="demo-email" placeholder="you@example.com" />
        <p className="text-xs text-muted-foreground">
          用于接收账单与通知，不会公开。
        </p>
      </div>
      <div className="grid gap-2">
        <Label htmlFor="demo-password">密码</Label>
        <Input id="demo-password" type="password" />
      </div>
    </Demo>
  );
}

export function CheckboxSection() {
  const [checked, setChecked] = useState(true);
  return (
    <>
      <Demo title="基础" hint="checked / disabled">
        <label className="flex items-center gap-2 text-sm">
          <Checkbox defaultChecked />
          记住我
        </label>
        <label className="flex items-center gap-2 text-sm">
          <Checkbox checked={checked} onCheckedChange={(v) => setChecked(v === true)} />
          接收通知
        </label>
        <label className="flex items-center gap-2 text-sm text-muted-foreground">
          <Checkbox disabled />
          不可用
        </label>
        <label className="flex items-center gap-2 text-sm text-muted-foreground">
          <Checkbox disabled defaultChecked />
          已锁定
        </label>
      </Demo>
      <Demo title="列表选择" layout="col" className="max-w-sm">
        {["账单提醒", "每周摘要", "新功能预告"].map((item) => (
          <label key={item} className="flex items-center gap-2 text-sm">
            <Checkbox defaultChecked={item === "账单提醒"} />
            {item}
          </label>
        ))}
      </Demo>
    </>
  );
}

export function RadioGroupSection() {
  return (
    <Demo title="单选组" layout="col" className="max-w-sm">
      <RadioGroup defaultValue="standard">
        {[
          ["standard", "标准版", "适合个人使用"],
          ["pro", "专业版", "解锁全部组件"],
          ["team", "团队版", "多人协作与主题同步"],
        ].map(([value, title, desc]) => (
          <label key={value} className="flex items-start gap-3 text-sm">
            <RadioGroupItem value={value} className="mt-0.5" />
            <span className="flex flex-col gap-0.5">
              <span>{title}</span>
              <span className="text-xs text-muted-foreground">{desc}</span>
            </span>
          </label>
        ))}
      </RadioGroup>
    </Demo>
  );
}

export function SwitchSection() {
  const [notify, setNotify] = useState(true);
  return (
    <Demo
      title="开关"
      hint="点击切换 · 按住可沿轨道拖动 · sm / md / lg 三档"
      layout="col"
      className="max-w-sm"
    >
      <div className="flex items-center justify-center gap-8">
        <Switch size="sm" defaultChecked />
        <Switch size="md" defaultChecked />
        <Switch size="lg" defaultChecked />
      </div>
      <label className="flex items-center justify-between gap-4 text-sm">
        桌面通知
        <Switch checked={notify} onCheckedChange={setNotify} />
      </label>
      <label className="flex items-center justify-between gap-4 text-sm">
        自动更新
        <Switch defaultChecked />
      </label>
      <label className="flex items-center justify-between gap-4 text-sm text-muted-foreground">
        实验特性
        <Switch disabled />
      </label>
    </Demo>
  );
}

export function SliderSection() {
  const [value, setValue] = useState([42]);
  return (
    <Demo title="滑块" layout="col" className="max-w-md">
      <div className="flex items-center gap-4">
        <Slider value={value} onValueChange={setValue} className="flex-1" />
        <span className="w-10 text-right font-mono text-sm text-muted-foreground">
          {value[0]}%
        </span>
      </div>
      <Slider defaultValue={[25, 75]} />
      <Slider defaultValue={[60]} disabled />
    </Demo>
  );
}

export function SelectSection() {
  return (
    <Demo title="选择器" layout="wrap">
      <Select defaultValue="react">
        <SelectTrigger className="w-48">
          <SelectValue placeholder="选择框架" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="react">React 19</SelectItem>
          <SelectItem value="vue">Vue 3</SelectItem>
          <SelectItem value="svelte">Svelte 5</SelectItem>
          <SelectItem value="solid">Solid</SelectItem>
        </SelectContent>
      </Select>

      <Select>
        <SelectTrigger className="w-52">
          <SelectValue placeholder="按分组选择" />
        </SelectTrigger>
        <SelectContent>
          <SelectGroup>
            <SelectLabel>基础</SelectLabel>
            <SelectItem value="button">按钮</SelectItem>
            <SelectItem value="input">输入框</SelectItem>
          </SelectGroup>
          <SelectGroup>
            <SelectLabel>浮层</SelectLabel>
            <SelectItem value="dialog">对话框</SelectItem>
            <SelectItem value="popover">气泡卡片</SelectItem>
          </SelectGroup>
        </SelectContent>
      </Select>
    </Demo>
  );
}
