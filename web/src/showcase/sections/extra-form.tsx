import { useState } from "react";
import { useForm } from "react-hook-form";
import { Mail, Search } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import {
  Combobox,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxItem,
  ComboboxList,
} from "@/components/ui/combobox";
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Picker } from "@/components/ui/picker";
import {
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
  InputGroupInput,
} from "@/components/ui/input-group";
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSeparator,
  InputOTPSlot,
} from "@/components/ui/input-otp";
import { Demo } from "@/showcase/Demo";

export function InputOTPSection() {
  const [value, setValue] = useState("123456");
  return (
    <Demo title="验证码输入" hint="InputOTP" layout="col">
      <InputOTP maxLength={6} value={value} onChange={setValue}>
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
      </InputOTP>
      <span className="font-mono text-xs text-muted-foreground">
        value = {value || "—"}
      </span>
    </Demo>
  );
}

export function InputGroupSection() {
  return (
    <Demo title="输入组合" hint="addon / button" layout="col" className="max-w-md">
      <InputGroup>
        <InputGroupAddon align="inline-start">
          <Mail />
        </InputGroupAddon>
        <InputGroupInput placeholder="you@example.com" />
        <InputGroupAddon align="inline-end">
          <InputGroupButton size="icon-xs" aria-label="搜索">
            <Search />
          </InputGroupButton>
        </InputGroupAddon>
      </InputGroup>
      <InputGroup>
        <InputGroupInput placeholder="搜索组件…" />
        <InputGroupAddon align="inline-end">
          <InputGroupButton variant="default" size="sm">
            搜索
          </InputGroupButton>
        </InputGroupAddon>
      </InputGroup>
      <InputGroup>
        <InputGroupInput placeholder="无效状态" aria-invalid />
      </InputGroup>
    </Demo>
  );
}

export function FieldSection() {
  return (
    <Demo title="字段布局" hint="Field / FieldGroup" layout="col" className="max-w-md">
      <FieldGroup>
        <Field>
          <FieldLabel htmlFor="field-email">邮箱</FieldLabel>
          <Input id="field-email" placeholder="you@example.com" />
          <FieldDescription>用于登录与接收通知。</FieldDescription>
        </Field>
        <Field>
          <FieldLabel htmlFor="field-name">昵称</FieldLabel>
          <Input id="field-name" defaultValue="Mui" />
          <FieldDescription>支持中英文，最多 20 个字符。</FieldDescription>
        </Field>
      </FieldGroup>
    </Demo>
  );
}

type Profile = { email: string };

export function FormSection() {
  const [submitted, setSubmitted] = useState<string | null>(null);
  const form = useForm<Profile>({ defaultValues: { email: "" } });

  return (
    <Demo title="表单校验" hint="react-hook-form" layout="col">
      <Form {...form}>
        <form
          onSubmit={form.handleSubmit((values) => setSubmitted(values.email))}
          className="w-full max-w-sm space-y-4"
        >
          <FormField
            control={form.control}
            name="email"
            rules={{
              required: "请输入邮箱",
              pattern: {
                value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                message: "邮箱格式不正确",
              },
            }}
            render={({ field }) => (
              <FormItem>
                <FormLabel>邮箱</FormLabel>
                <FormControl>
                  <Input placeholder="you@example.com" {...field} />
                </FormControl>
                <FormDescription>提交后会做必填与格式校验。</FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
          <div className="flex items-center gap-3">
            <Button type="submit">提交</Button>
            {submitted ? (
              <span className="text-xs text-success">已提交：{submitted}</span>
            ) : null}
          </div>
        </form>
      </Form>
    </Demo>
  );
}

const FRAMEWORKS = ["React", "Vue", "Svelte", "Solid", "Angular", "Astro"];

export function ComboboxSection() {
  const [value, setValue] = useState<string | null>("React");
  return (
    <Demo title="可搜索选择" hint="Combobox · Base UI" layout="col" className="max-w-xs">
      <Combobox items={FRAMEWORKS} value={value} onValueChange={setValue}>
        <ComboboxInput placeholder="搜索框架…" showClear />
        <ComboboxContent>
          <ComboboxList>
            {(item: string) => (
              <ComboboxItem key={item} value={item}>
                {item}
              </ComboboxItem>
            )}
          </ComboboxList>
          <ComboboxEmpty>没有匹配项</ComboboxEmpty>
        </ComboboxContent>
      </Combobox>
      <span className="font-mono text-xs text-muted-foreground">
        value = {value ?? "null"}
      </span>
    </Demo>
  );
}

export function CalendarSection() {
  const [date, setDate] = useState<Date | undefined>(new Date());
  return (
    <Demo title="日历" hint="react-day-picker" layout="wrap">
      <Calendar
        mode="single"
        selected={date}
        onSelect={setDate}
        className="rounded-md border"
      />
      <div className="w-full max-w-xs space-y-2 text-sm">
        <p className="font-medium">单选模式</p>
        <p className="text-muted-foreground">
          {date ? date.toLocaleDateString("zh-CN") : "未选择日期"}
        </p>
        <p className="text-xs text-muted-foreground">
          支持 range / multiple 模式，API 与 react-day-picker 一致。
        </p>
      </div>
    </Demo>
  );
}

export function PickerSection() {
  const [weight, setWeight] = useState(29);
  return (
    <Demo
      title="转盘选择器"
      hint="Picker · 滚轮 / 拖拽 / 中心吸附"
      layout="col"
      // 轮盘展开后比触发器高得多，卡片默认 overflow-hidden 会把上下半截切掉
      overflow="visible"
    >
      <Picker min={25} max={33} unit="kg" value={weight} onValueChange={setWeight} />
      <span className="font-mono text-xs text-muted-foreground">
        weight = {weight} kg
      </span>
    </Demo>
  );
}
