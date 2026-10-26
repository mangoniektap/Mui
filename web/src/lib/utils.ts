import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/** 合并 className（shadcn/ui 同款实现） */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
