import { useState } from "react";
import { FileText, Image as ImageIcon, Calendar, Bot } from "lucide-react";

import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  Attachment,
  AttachmentAction,
  AttachmentActions,
  AttachmentContent,
  AttachmentDescription,
  AttachmentGroup,
  AttachmentMedia,
  AttachmentTitle,
} from "@/components/ui/attachment";
import {
  Bubble,
  BubbleContent,
  BubbleGroup,
  BubbleReactions,
} from "@/components/ui/bubble";
import {
  Marker,
  MarkerContent,
  MarkerIcon,
} from "@/components/ui/marker";
import {
  Message,
  MessageAvatar,
  MessageContent,
  MessageFooter,
  MessageGroup,
  MessageHeader,
} from "@/components/ui/message";
import {
  MessageScroller,
  MessageScrollerButton,
  MessageScrollerContent,
  MessageScrollerItem,
  MessageScrollerProvider,
  MessageScrollerViewport,
} from "@/components/ui/message-scroller";
import { Button } from "@/components/ui/button";
import { Demo } from "@/showcase/Demo";

export function BubbleSection() {
  return (
    <Demo title="消息气泡" hint="Bubble" layout="col">
      <BubbleGroup className="w-full max-w-md">
        <Bubble variant="muted">
          <BubbleContent>默认的次要气泡，适合对方消息。</BubbleContent>
        </Bubble>
        <Bubble variant="default" align="end">
          <BubbleContent>这是一条己方消息，使用主色。</BubbleContent>
        </Bubble>
        <Bubble variant="tinted">
          <BubbleContent>浅色强调气泡 tinted。</BubbleContent>
        </Bubble>
        <Bubble variant="outline">
          <BubbleContent>描边气泡 outline。</BubbleContent>
        </Bubble>
        <Bubble variant="destructive">
          <BubbleContent>危险状态，例如发送失败。</BubbleContent>
        </Bubble>
        <Bubble variant="secondary" align="start">
          <BubbleContent>带表情反馈的消息</BubbleContent>
          <BubbleReactions>👍 2 · ❤️ 1</BubbleReactions>
        </Bubble>
      </BubbleGroup>
    </Demo>
  );
}

export function MessageSection() {
  return (
    <Demo title="完整消息行" hint="Message" layout="col">
      <MessageGroup className="w-full max-w-lg">
        <Message align="start">
          <MessageAvatar>
            <Avatar className="size-8">
              <AvatarFallback>
                <Bot className="size-4" />
              </AvatarFallback>
            </Avatar>
          </MessageAvatar>
          <MessageContent>
            <MessageHeader>
              <span className="text-xs font-medium">Mui 助手</span>
              <span className="text-[11px] text-muted-foreground">10:24</span>
            </MessageHeader>
            <Bubble variant="muted">
              <BubbleContent>
                已经把这批组件整理好了，左侧按类别可以直接跳转。
              </BubbleContent>
            </Bubble>
            <MessageFooter>
              <span className="text-[11px] text-muted-foreground">已送达</span>
            </MessageFooter>
          </MessageContent>
        </Message>

        <Message align="end">
          <MessageAvatar>
            <Avatar className="size-8">
              <AvatarFallback>我</AvatarFallback>
            </Avatar>
          </MessageAvatar>
          <MessageContent>
            <MessageHeader className="justify-end">
              <span className="text-[11px] text-muted-foreground">10:25</span>
            </MessageHeader>
            <Bubble variant="default" align="end">
              <BubbleContent>好，我先看看图标和配色。</BubbleContent>
            </Bubble>
          </MessageContent>
        </Message>
      </MessageGroup>
    </Demo>
  );
}

export function AttachmentSection() {
  return (
    <Demo title="附件卡片" hint="Attachment" layout="col">
      <AttachmentGroup className="w-full">
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
            <ImageIcon />
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
      </AttachmentGroup>
    </Demo>
  );
}

export function MarkerSection() {
  return (
    <Demo title="分隔标记" hint="Marker" layout="col">
      <Marker variant="separator">
        <MarkerIcon>
          <Calendar />
        </MarkerIcon>
        <MarkerContent>今天</MarkerContent>
      </Marker>
      <Marker>
        <MarkerIcon>
          <FileText />
        </MarkerIcon>
        <MarkerContent>加载了 24 个组件，共 61 个文件</MarkerContent>
      </Marker>
      <Marker variant="border">
        <MarkerContent>已读回执已开启</MarkerContent>
      </Marker>
    </Demo>
  );
}

export function MessageScrollerSection() {
  const [count, setCount] = useState(8);
  return (
    <Demo title="消息滚动容器" hint="MessageScroller" layout="col">
      <div className="h-72 w-full max-w-lg overflow-hidden rounded-xl border">
        <MessageScrollerProvider>
          <MessageScroller>
            <MessageScrollerViewport>
              <MessageScrollerContent className="gap-3 p-3">
                {Array.from({ length: count }, (_, i) => (
                  <MessageScrollerItem key={i}>
                    <Bubble
                      variant={i % 3 === 0 ? "default" : "muted"}
                      align={i % 3 === 0 ? "end" : "start"}
                    >
                      <BubbleContent>第 {i + 1} 条消息</BubbleContent>
                    </Bubble>
                  </MessageScrollerItem>
                ))}
              </MessageScrollerContent>
            </MessageScrollerViewport>
            <MessageScrollerButton />
          </MessageScroller>
        </MessageScrollerProvider>
      </div>
      <Button
        variant="outline"
        size="sm"
        onClick={() => setCount((n) => n + 3)}
      >
        追加 3 条
      </Button>
    </Demo>
  );
}
