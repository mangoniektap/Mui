/** 单个组件的文档数据：说明 / 调用方式 / 调用示例 */

/** 调用方式的一行：参数名、类型、默认值、说明 */
export type ApiRow = {
  name: string;
  type: string;
  default?: string;
  desc: string;
};

/** 调用示例：一段带标题的代码 */
export type DocExample = {
  title: string;
  code: string;
};

export type ComponentDoc = {
  /** 顶部 import 语句，展示在「组件说明」里 */
  import: string;
  summary: string;
  api: ApiRow[];
  examples: DocExample[];
};

import basic from "./basic";
import form from "./form";
import feedback from "./feedback";
import navigation from "./navigation";
import layout from "./layout";
import chat from "./chat";

/** key = registry 里的 section id */
export const componentDocs: Record<string, ComponentDoc> = {
  ...basic,
  ...form,
  ...feedback,
  ...navigation,
  ...layout,
  ...chat,
};
