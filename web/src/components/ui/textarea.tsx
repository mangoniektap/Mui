import * as React from "react"
import { cn } from "@/lib/utils"
import {
  classicalControlSurface,
  classicalFocusRing,
} from "@/styles/surfaces/classical"

function Textarea({ className, ...props }: React.ComponentProps<"textarea">) {
  return (
    <textarea
      data-slot="textarea"
      className={cn(
        "flex field-sizing-content min-h-16 w-full rounded-md px-3 py-2 text-base transition-[color,box-shadow] outline-none placeholder:text-muted-foreground disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-destructive/20 md:text-sm dark:aria-invalid:ring-destructive/40",
        classicalControlSurface,
        classicalFocusRing,
        className
      )}
      {...props}
    />
  )
}

export { Textarea }
