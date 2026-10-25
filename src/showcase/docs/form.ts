import type { ComponentDoc } from "./index";

/** 表单分组（15 个） */
export default {
  input: {
    import: 'import { Input } from "@/components/ui/input"',
    summary:
      "单行输入框。原生 input 的全量属性（value / onChange / type / placeholder / disabled…）直接透传，外观走令牌。",
    api: [
      { name: "value / defaultValue", type: "string", desc: "受控 / 非受控值" },
      { name: "onChange", type: "(e: ChangeEvent<HTMLInputElement>) => void", desc: "变化回调" },
      { name: "type", type: "string", default: '"text"', desc: "原生类型：text / email / password / number…" },
      { name: "disabled", type: "boolean", default: "false", desc: "禁用" },
      { name: "className", type: "string", desc: "追加类名" },
    ],
    examples: [
      {
        title: "基础与禁用",
        code: `import { Input } from "@/components/ui/input"

<Input placeholder="请输入邮箱" type="email" />
<Input placeholder="禁用状态" disabled />`,
      },
    ],
  },

  textarea: {
    import: 'import { Textarea } from "@/components/ui/textarea"',
    summary: "多行文本域。原生 textarea 全量属性透传。",
    api: [
      { name: "value / defaultValue", type: "string", desc: "受控 / 非受控值" },
      { name: "rows", type: "number", desc: "原生可见行数" },
      { name: "disabled", type: "boolean", default: "false", desc: "禁用" },
      { name: "className", type: "string", desc: "追加类名；默认 min-h-16，可改高度" },
    ],
    examples: [
      {
        title: "多行输入",
        code: `import { Textarea } from "@/components/ui/textarea"

<Textarea placeholder="说点什么…" rows={4} />`,
      },
    ],
  },

  label: {
    import: 'import { Label } from "@/components/ui/label"',
    summary: "表单标签。基于 Radix Label，与控件天然关联，点击标签聚焦对应控件。",
    api: [{ name: "htmlFor", type: "string", desc: "关联控件的 id" }],
    examples: [
      {
        title: "关联输入框",
        code: `import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

<div className="grid gap-2">
  <Label htmlFor="email">邮箱</Label>
  <Input id="email" type="email" />
</div>`,
      },
    ],
  },

  checkbox: {
    import: 'import { Checkbox } from "@/components/ui/checkbox"',
    summary: "复选框。基于 Radix Checkbox，支持选中 / 半选（indeterminate）与禁用。",
    api: [
      { name: "checked", type: "boolean", desc: "受控选中态" },
      { name: "defaultChecked", type: "boolean", default: "false", desc: "非受控初始态" },
      { name: "onCheckedChange", type: "(checked: boolean) => void", desc: "选中变化回调" },
      { name: "disabled", type: "boolean", default: "false", desc: "禁用" },
    ],
    examples: [
      {
        title: "受控复选框",
        code: `import { Checkbox } from "@/components/ui/checkbox"

const [checked, setChecked] = useState(false)

<div className="flex items-center gap-2">
  <Checkbox
    id="terms"
    checked={checked}
    onCheckedChange={(v) => setChecked(v === true)}
  />
  <Label htmlFor="terms">接受服务条款</Label>
</div>`,
      },
    ],
  },

  "radio-group": {
    import: 'import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"',
    summary: "单选组。一组互斥选项，带说明文字时用 Field / Label 组合。",
    api: [
      { name: "value / defaultValue", type: "string", desc: "选中值" },
      { name: "onValueChange", type: "(value: string) => void", desc: "选中变化回调" },
      { name: "RadioGroupItem", type: 'Props & { value: string }', desc: "单个选项，value 必给" },
      { name: "disabled", type: "boolean", default: "false", desc: "禁用整组或单项" },
    ],
    examples: [
      {
        title: "套餐选择",
        code: `import { Label } from "@/components/ui/label"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"

<RadioGroup defaultValue="pro">
  {[
    { value: "free", label: "免费版" },
    { value: "pro", label: "专业版" },
  ].map((item) => (
    <div key={item.value} className="flex items-center gap-2">
      <RadioGroupItem value={item.value} id={item.value} />
      <Label htmlFor={item.value}>{item.label}</Label>
    </div>
  ))}
</RadioGroup>`,
      },
    ],
  },

  switch: {
    import: 'import { Switch } from "@/components/ui/switch"',
    summary:
      "开关。即时生效的布尔开关；滑块是液态玻璃球（折射 / 按压外鼓），按住可沿轨道拖动，松手按落点过半吸附。受控与非受控均可。",
    api: [
      { name: "size", type: '"sm" | "md" | "lg"', default: '"md"', desc: "三档尺寸：24 / 32 / 40 px 轨道" },
      { name: "checked", type: "boolean", desc: "受控选中态" },
      { name: "defaultChecked", type: "boolean", default: "false", desc: "非受控初始态" },
      { name: "onCheckedChange", type: "(checked: boolean) => void", desc: "切换回调" },
      { name: "disabled", type: "boolean", default: "false", desc: "禁用" },
    ],
    examples: [
      {
        title: "受控开关",
        code: `import { useState } from "react"
import { Switch } from "@/components/ui/switch"

const [on, setOn] = useState(false)

<Switch checked={on} onCheckedChange={setOn} />
<Switch size="sm" defaultChecked />
<Switch size="lg" disabled />`,
      },
    ],
  },

  slider: {
    import: 'import { Slider } from "@/components/ui/slider"',
    summary: "滑块。单值或双值区间，基于 Radix Slider。",
    api: [
      { name: "value / defaultValue", type: "number[]", desc: "值数组；长度 2 即区间滑块" },
      { name: "onValueChange", type: "(value: number[]) => void", desc: "变化回调" },
      { name: "min / max / step", type: "number", default: "0 / 100 / 1", desc: "范围与步长" },
      { name: "disabled", type: "boolean", default: "false", desc: "禁用" },
    ],
    examples: [
      {
        title: "单值与区间",
        code: `import { Slider } from "@/components/ui/slider"

<Slider defaultValue={[50]} max={100} step={1} className="w-64" />

<Slider defaultValue={[25, 75]} max={100} step={1} className="w-64" />`,
      },
    ],
  },

  select: {
    import:
      'import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"',
    summary:
      "选择器。原生风格的折叠下拉，支持分组、占位与禁用；基于 Radix Select。",
    api: [
      { name: "value / defaultValue", type: "string", desc: "选中值" },
      { name: "onValueChange", type: "(value: string) => void", desc: "选中变化回调" },
      { name: "SelectTrigger", type: 'Props & { size?: "sm" | "default" }', desc: "触发器；size 控制高度" },
      { name: "SelectValue", type: 'Props & { placeholder?: string }', desc: "选中值展示 / 占位" },
      { name: "SelectGroup / SelectLabel / SelectSeparator", type: "—", desc: "分组 / 组标题 / 分隔" },
      { name: "SelectItem", type: 'Props & { value: string }', desc: "选项，value 必给" },
    ],
    examples: [
      {
        title: "带分组的下拉",
        code: `import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

<Select>
  <SelectTrigger className="w-48">
    <SelectValue placeholder="选择时区" />
  </SelectTrigger>
  <SelectContent>
    <SelectGroup>
      <SelectLabel>亚洲</SelectLabel>
      <SelectItem value="cst">中国标准时间</SelectItem>
      <SelectItem value="jst">日本标准时间</SelectItem>
    </SelectGroup>
  </SelectContent>
</Select>`,
      },
    ],
  },

  picker: {
    import: 'import { Picker } from "@/components/ui/picker"',
    summary:
      "转盘选择器。点击在原位展开竖向滚轮：鼠标滚轮或按住拖拽，松手吸附到最近一档；中间选中框是一条液态玻璃。调用方只给数值范围与单位，刻度由组件自己铺。",
    api: [
      { name: "min / max", type: "number", desc: "闭区间范围，从 min 起每 step 一档" },
      { name: "step", type: "number", default: "1", desc: "步长；小数步长（0.1）也支持" },
      { name: "unit", type: "string", desc: "单位后缀（如 kg），固定在数字右侧" },
      { name: "value / defaultValue", type: "number", desc: "受控 / 非受控选中值" },
      { name: "onValueChange", type: "(value: number) => void", desc: "选中变化回调" },
      { name: "format", type: "(value: number) => string", desc: "数字显示方式，默认 String(v)" },
      { name: "disabled", type: "boolean", default: "false", desc: "禁用" },
    ],
    examples: [
      {
        title: "体重选择",
        code: `import { useState } from "react"
import { Picker } from "@/components/ui/picker"

const [weight, setWeight] = useState(65)

<Picker
  min={30}
  max={150}
  unit="kg"
  value={weight}
  onValueChange={setWeight}
/>`,
      },
    ],
  },

  "input-otp": {
    import: 'import { InputOTP, InputOTPGroup, InputOTPSlot } from "@/components/ui/input-otp"',
    summary: "分段验证码输入。基于 input-otp，一格一位，支持粘贴自动分配与 onComplete。",
    api: [
      { name: "maxLength", type: "number", desc: "位数，决定渲染几个格子" },
      { name: "value / onChange", type: "string / (v: string) => void", desc: "受控值" },
      { name: "onComplete", type: "(value: string) => void", desc: "输满时的回调" },
      { name: "InputOTPSlot", type: 'Props & { index: number }', desc: "单个格子，index 必给" },
      { name: "InputOTPSeparator", type: "div", desc: "组间分隔符" },
    ],
    examples: [
      {
        title: "6 位验证码",
        code: `import {
  InputOTP,
  InputOTPGroup,
  InputOTPSeparator,
  InputOTPSlot,
} from "@/components/ui/input-otp"

<InputOTP maxLength={6} onComplete={(code) => console.log(code)}>
  <InputOTPGroup>
    <InputOTPSlot index={0} />
    <InputOTPSlot index={1} />
    <InputOTPSlot index={2} />
  </InputOTPGroup>
  <InputOTPSeparator />
  <InputOTPGroup>
    <InputOTPSlot index={3} />
    <InputOTPSlot index={4} />
    <InputOTPSlot index={5} />
  </InputOTPGroup>
</InputOTP>`,
      },
    ],
  },

  "input-group": {
    import:
      'import { InputGroup, InputGroupAddon, InputGroupButton, InputGroupInput } from "@/components/ui/input-group"',
    summary:
      "输入组合。在输入框前后挂前缀 / 后缀 / 按钮 / 文字，组成搜索框、网址输入等复合控件。",
    api: [
      { name: "InputGroup", type: "div", desc: "容器，包住输入与附加物" },
      { name: "InputGroupAddon", type: 'div & { align?: "inline-start" | "inline-end" | "block-start" | "block-end" }', desc: "附加物位置" },
      { name: "InputGroupInput", type: "input", desc: "输入框本体" },
      { name: "InputGroupTextarea", type: "textarea", desc: "多行输入本体" },
      { name: "InputGroupButton", type: "button", desc: "附加按钮" },
      { name: "InputGroupText", type: "div", desc: "附加文字" },
    ],
    examples: [
      {
        title: "搜索框",
        code: `import { Search } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
  InputGroupInput,
} from "@/components/ui/input-group"

<InputGroup className="max-w-sm">
  <InputGroupAddon>
    <Search />
  </InputGroupAddon>
  <InputGroupInput placeholder="搜索组件…" />
  <InputGroupAddon align="inline-end">
    <InputGroupButton variant="default" size="xs">搜索</InputGroupButton>
  </InputGroupAddon>
</InputGroup>`,
      },
    ],
  },

  field: {
    import:
      'import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"',
    summary:
      "字段布局。标签 + 控件 + 说明 / 错误信息的标准行结构，支持竖排、横排与响应式；与 Form、FieldError 组合出校验提示。",
    api: [
      { name: "orientation", type: '"vertical" | "horizontal" | "responsive"', default: '"vertical"', desc: "标签与控件的排列方式" },
      { name: "FieldLabel", type: "label", desc: "字段标签" },
      { name: "FieldDescription", type: "p", desc: "辅助说明" },
      { name: "FieldError", type: "p", desc: "错误信息，通常配合 aria-invalid" },
      { name: "FieldSet / FieldLegend", type: "fieldset / legend", desc: "字段集 / 集标题（variant: legend | label）" },
      { name: "FieldGroup / FieldSeparator / FieldTitle / FieldContent", type: "div / div / div / div", desc: "字段组 / 组内分隔 / 组标题 / 内容列" },
    ],
    examples: [
      {
        title: "带说明与错误的字段",
        code: `import { Input } from "@/components/ui/input"
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"

<FieldGroup>
  <Field>
    <FieldLabel htmlFor="name">昵称</FieldLabel>
    <Input id="name" placeholder="2-20 个字符" />
    <FieldDescription>将展示在评论与消息中。</FieldDescription>
  </Field>
  <Field data-invalid="true">
    <FieldLabel htmlFor="email">邮箱</FieldLabel>
    <Input id="email" aria-invalid="true" defaultValue="not-an-email" />
    <FieldError>请输入有效的邮箱地址。</FieldError>
  </Field>
</FieldGroup>`,
      },
    ],
  },

  form: {
    import:
      'import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"',
    summary:
      "表单校验。react-hook-form + zod 的桥接层：FormField 管状态与校验，FormItem 提供上下文，FormMessage 出错误文案。",
    api: [
      { name: "Form", type: "FormProvider", desc: "包住整个表单，接 useForm 返回值" },
      { name: "FormField", type: 'Props & { control: Control; name: string; render: () => ReactNode }', desc: "受控字段，render 里拿 field" },
      { name: "FormItem", type: "div", desc: "单个字段容器，提供上下文" },
      { name: "FormLabel / FormControl", type: "label / Slot", desc: "标签 / 控件包装（自动接 id 与 aria）" },
      { name: "FormDescription / FormMessage", type: "p / p", desc: "说明 / 校验错误信息" },
    ],
    examples: [
      {
        title: "登录表单（zod 校验）",
        code: `import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Button } from "@/components/ui/button"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"

const schema = z.object({
  email: z.string().email("请输入有效邮箱"),
  password: z.string().min(6, "至少 6 位"),
})

function LoginForm() {
  const form = useForm({ resolver: zodResolver(schema) })

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit((v) => console.log(v))} className="space-y-4">
        <FormField
          control={form.control}
          name="email"
          render={({ field }) => (
            <FormItem>
              <FormLabel>邮箱</FormLabel>
              <FormControl>
                <Input placeholder="you@example.com" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit">登录</Button>
      </form>
    </Form>
  )
}`,
      },
    ],
  },

  combobox: {
    import:
      'import { Combobox, ComboboxContent, ComboboxInput, ComboboxItem, ComboboxList } from "@/components/ui/combobox"',
    summary:
      "可搜索选择器。Base UI 驱动，输入即过滤；ComboboxItem 的 children 必须写成函数子节点才会参与过滤。另有多选形态（ComboboxChips）与弹窗内嵌用法。",
    api: [
      { name: "items", type: "any[]", desc: "Root 的选项数据源（Base UI 约定）" },
      { name: "value / onValueChange", type: "any", desc: "选中值 / 回调" },
      { name: "ComboboxInput", type: 'Props & { showTrigger?: boolean; showClear?: boolean }', desc: "输入框；默认带下拉箭头，可加清除按钮" },
      { name: "ComboboxList", type: "div", desc: "选项列表容器" },
      { name: "ComboboxItem", type: "div", desc: "单个选项；children 需为函数子节点才会过滤" },
      { name: "ComboboxEmpty", type: "div", desc: "无结果占位" },
      { name: "ComboboxTrigger / ComboboxValue", type: "button / span", desc: "独立触发器 / 选中值展示" },
      { name: "ComboboxChips / ComboboxChip / ComboboxChipsInput", type: "div / div / input", desc: "多选形态的标签行" },
    ],
    examples: [
      {
        title: "框架选择（输入过滤）",
        code: `import {
  Combobox,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxItem,
  ComboboxList,
} from "@/components/ui/combobox"

const frameworks = [
  { value: "react", label: "React" },
  { value: "vue", label: "Vue" },
  { value: "svelte", label: "Svelte" },
]

<Combobox items={frameworks}>
  <ComboboxInput placeholder="选择框架…" showClear />
  <ComboboxContent>
    <ComboboxList>
      {(item) => (
        <ComboboxItem key={item.value} value={item}>
          {item.label}
        </ComboboxItem>
      )}
    </ComboboxList>
    <ComboboxEmpty>没有匹配项</ComboboxEmpty>
  </ComboboxContent>
</Combobox>`,
      },
    ],
  },

  calendar: {
    import: 'import { Calendar } from "@/components/ui/calendar"',
    summary:
      "日历。基于 react-day-picker，支持单选 / 多选 / 范围三种模式，外观与按钮变体可配。",
    api: [
      { name: "mode", type: '"single" | "multiple" | "range"', default: '"single"', desc: "选择模式" },
      { name: "selected / onSelect", type: "Date | Date[] | DateRange", desc: "选中值 / 回调" },
      { name: "buttonVariant", type: '"default" | "outline" | "ghost" | …', default: '"ghost"', desc: "日期格子的按钮变体" },
      { name: "showOutsideDays", type: "boolean", default: "true", desc: "是否显示相邻月份的日期" },
      { name: "className", type: "string", desc: "追加类名" },
    ],
    examples: [
      {
        title: "日期选择",
        code: `import { useState } from "react"
import { Calendar } from "@/components/ui/calendar"

const [date, setDate] = useState<Date>()

<Calendar
  mode="single"
  selected={date}
  onSelect={setDate}
  className="rounded-lg border"
/>`,
      },
    ],
  },
} satisfies Record<string, ComponentDoc>;
