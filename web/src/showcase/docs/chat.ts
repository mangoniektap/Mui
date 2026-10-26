import type { ComponentDoc } from "./index";

/** 对话分组（5 个） */
export default {
  bubble: {
    import:
      'import { Bubble, BubbleContent, BubbleGroup, BubbleReactions } from "@/components/ui/bubble"',
    summary:
      "消息气泡。七种变体覆盖己方 / 对方 / 强调 / 危险等语义，align 决定左右归属；BubbleContent 可 asChild 变成按钮或链接，BubbleReactions 挂表情反馈。",
    api: [
      { name: "variant", type: '"default" | "secondary" | "muted" | "tinted" | "outline" | "ghost" | "destructive"', default: '"default"', desc: "主色（己方）/ 次色 / 灰 / 浅色强调 / 描边 / 无底 / 危险" },
      { name: "align", type: '"start" | "end"', default: '"start"', desc: "左（对方）/ 右（己方）" },
      { name: "BubbleContent", type: 'Props & { asChild?: boolean }', desc: "气泡内容体；asChild 时支持内嵌 button / a" },
      { name: "BubbleGroup", type: "div", desc: "气泡组容器" },
      { name: "BubbleReactions", type: 'Props & { side?: "top" | "bottom"; align?: "start" | "end" }', desc: "表情反馈条，挂在气泡边缘" },
    ],
    examples: [
      {
        title: "会话气泡",
        code: `import {
  Bubble,
  BubbleContent,
  BubbleGroup,
  BubbleReactions,
} from "@/components/ui/bubble"

<BubbleGroup className="max-w-md">
  <Bubble variant="muted">
    <BubbleContent>默认的次要气泡，适合对方消息。</BubbleContent>
  </Bubble>
  <Bubble variant="default" align="end">
    <BubbleContent>这是一条己方消息，使用主色。</BubbleContent>
  </Bubble>
  <Bubble variant="destructive">
    <BubbleContent>发送失败，点击重试。</BubbleContent>
  </Bubble>
  <Bubble variant="secondary">
    <BubbleContent>带表情反馈的消息</BubbleContent>
    <BubbleReactions>👍 2 · ❤️ 1</BubbleReactions>
  </Bubble>
</BubbleGroup>`,
      },
    ],
  },

  message: {
    import:
      'import { Message, MessageAvatar, MessageContent, MessageFooter, MessageGroup, MessageHeader } from "@/components/ui/message"',
    summary:
      "消息行。头像 + 头部（昵称 / 时间）+ 气泡 + 底部的完整消息结构，align 决定整行左右。",
    api: [
      { name: "Message", type: 'Props & { align?: "start" | "end" }', desc: "消息行；align 默认 start" },
      { name: "MessageGroup", type: "div", desc: "消息列表容器" },
      { name: "MessageAvatar", type: "div", desc: "头像位，通常放 Avatar" },
      { name: "MessageHeader", type: "div", desc: "头部：昵称、时间等" },
      { name: "MessageContent", type: "div", desc: "内容列，放 Bubble" },
      { name: "MessageFooter", type: "div", desc: "底部：状态、操作等" },
    ],
    examples: [
      {
        title: "完整消息",
        code: `import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Bubble, BubbleContent } from "@/components/ui/bubble"
import {
  Message,
  MessageAvatar,
  MessageContent,
  MessageGroup,
  MessageHeader,
} from "@/components/ui/message"

<MessageGroup className="max-w-lg">
  <Message align="start">
    <MessageAvatar>
      <Avatar className="size-8">
        <AvatarFallback>M</AvatarFallback>
      </Avatar>
    </MessageAvatar>
    <MessageContent>
      <MessageHeader>
        <span className="text-xs font-medium">Mui 助手</span>
        <span className="text-[11px] text-muted-foreground">10:24</span>
      </MessageHeader>
      <Bubble variant="muted">
        <BubbleContent>已经把这批组件整理好了。</BubbleContent>
      </Bubble>
    </MessageContent>
  </Message>
</MessageGroup>`,
      },
    ],
  },

  attachment: {
    import:
      'import { Attachment, AttachmentActions, AttachmentAction, AttachmentContent, AttachmentDescription, AttachmentGroup, AttachmentMedia, AttachmentTitle } from "@/components/ui/attachment"',
    summary:
      "附件卡片。上传中 / 处理中 / 完成 / 失败五种状态各有视觉反馈，支持横向与竖向排布；AttachmentTrigger 可整卡变链接。",
    api: [
      { name: "state", type: '"idle" | "uploading" | "processing" | "error" | "done"', default: '"done"', desc: "附件状态； uploading / processing 有微光效果" },
      { name: "size", type: '"default" | "sm" | "xs"', default: '"default"', desc: "卡片尺寸" },
      { name: "orientation", type: '"horizontal" | "vertical"', default: '"horizontal"', desc: "横排 / 竖排" },
      { name: "AttachmentMedia", type: 'Props & { variant?: "icon" | "image" }', desc: "缩略图位" },
      { name: "AttachmentContent / Title / Description", type: "div / span / span", desc: "内容区 / 文件名 / 描述" },
      { name: "AttachmentActions / AttachmentAction", type: "div / Button", desc: "操作区 / 操作按钮（ghost 风格）" },
      { name: "AttachmentTrigger", type: 'button & { asChild?: boolean }', desc: "整卡点击区域" },
      { name: "AttachmentGroup", type: "div", desc: "附件组，横向滚动" },
    ],
    examples: [
      {
        title: "三种状态",
        code: `import { FileText } from "lucide-react"
import {
  Attachment,
  AttachmentAction,
  AttachmentActions,
  AttachmentContent,
  AttachmentDescription,
  AttachmentGroup,
  AttachmentMedia,
  AttachmentTitle,
} from "@/components/ui/attachment"

<AttachmentGroup>
  <Attachment>
    <AttachmentMedia>
      <FileText />
    </AttachmentMedia>
    <AttachmentContent>
      <AttachmentTitle>季度报告.pdf</AttachmentTitle>
      <AttachmentDescription>1.8 MB · 已上传</AttachmentDescription>
    </AttachmentContent>
    <AttachmentActions>
      <AttachmentAction size="xs">下载</AttachmentAction>
    </AttachmentActions>
  </Attachment>

  <Attachment state="uploading">
    <AttachmentMedia>
      <FileText />
    </AttachmentMedia>
    <AttachmentContent>
      <AttachmentTitle>封面图.png</AttachmentTitle>
      <AttachmentDescription>上传中 68%</AttachmentDescription>
    </AttachmentContent>
  </Attachment>

  <Attachment state="error">
    <AttachmentMedia>
      <FileText />
    </AttachmentMedia>
    <AttachmentContent>
      <AttachmentTitle>合同.docx</AttachmentTitle>
      <AttachmentDescription>上传失败，点击重试</AttachmentDescription>
    </AttachmentContent>
    <AttachmentActions>
      <AttachmentAction size="xs">重试</AttachmentAction>
    </AttachmentActions>
  </Attachment>
</AttachmentGroup>`,
      },
    ],
  },

  marker: {
    import: 'import { Marker, MarkerContent, MarkerIcon } from "@/components/ui/marker"',
    summary:
      "标记。时间分隔、系统提示等行内标记；separator 变体在文字两侧拉出分隔线，border 变体是带下边框的细条。",
    api: [
      { name: "variant", type: '"default" | "separator" | "border"', default: '"default"', desc: "普通 / 两侧分隔线 / 下边框" },
      { name: "asChild", type: "boolean", default: "false", desc: "把样式合并到唯一子元素" },
      { name: "MarkerIcon", type: "span", desc: "标记图标位" },
      { name: "MarkerContent", type: "span", desc: "标记文字" },
    ],
    examples: [
      {
        title: "时间分隔",
        code: `import { Calendar } from "lucide-react"
import { Marker, MarkerContent, MarkerIcon } from "@/components/ui/marker"

<Marker variant="separator">
  <MarkerIcon>
    <Calendar />
  </MarkerIcon>
  <MarkerContent>今天</MarkerContent>
</Marker>

<Marker>
  <MarkerContent>加载了 24 个组件，共 61 个文件</MarkerContent>
</Marker>`,
      },
    ],
  },

  "message-scroller": {
    import:
      'import { MessageScroller, MessageScrollerButton, MessageScrollerContent, MessageScrollerItem, MessageScrollerProvider, MessageScrollerViewport } from "@/components/ui/message-scroller"',
    summary:
      "消息滚动容器。贴底滚动 +「回到底部」浮动按钮：向上翻看历史时按钮淡入，新消息到达自动滚底；另导出三个 hook 给自定义布局。",
    api: [
      { name: "MessageScrollerProvider", type: "—", desc: "根容器，提供上下文" },
      { name: "MessageScroller", type: "div", desc: "滚动区外层" },
      { name: "MessageScrollerViewport", type: "div", desc: "滚动视口，给高度约束" },
      { name: "MessageScrollerContent", type: "div", desc: "消息列表" },
      { name: "MessageScrollerItem", type: "div", desc: "单条消息容器" },
      { name: "MessageScrollerButton", type: "button", desc: "回到底部按钮，离底时自动出现" },
      { name: "useMessageScroller()", type: "() => context", desc: "读上下文" },
      { name: "useMessageScrollerScrollable()", type: "() => boolean", desc: "是否可滚动" },
      { name: "useMessageScrollerVisibility()", type: "() => boolean", desc: "是否停在底部" },
    ],
    examples: [
      {
        title: "消息列表",
        code: `import { useState } from "react"
import { Bubble, BubbleContent } from "@/components/ui/bubble"
import {
  MessageScroller,
  MessageScrollerButton,
  MessageScrollerContent,
  MessageScrollerItem,
  MessageScrollerProvider,
  MessageScrollerViewport,
} from "@/components/ui/message-scroller"

const [count, setCount] = useState(8)

<div className="h-72 w-full max-w-lg overflow-hidden rounded-xl border">
  <MessageScrollerProvider>
    <MessageScroller>
      <MessageScrollerViewport>
        <MessageScrollerContent className="gap-3 p-3">
          {Array.from({ length: count }, (_, i) => (
            <MessageScrollerItem key={i}>
              <Bubble variant={i % 3 === 0 ? "default" : "muted"} align={i % 3 === 0 ? "end" : "start"}>
                <BubbleContent>第 {i + 1} 条消息</BubbleContent>
              </Bubble>
            </MessageScrollerItem>
          ))}
        </MessageScrollerContent>
      </MessageScrollerViewport>
      <MessageScrollerButton />
    </MessageScroller>
  </MessageScrollerProvider>
</div>`,
      },
    ],
  },
} satisfies Record<string, ComponentDoc>;
