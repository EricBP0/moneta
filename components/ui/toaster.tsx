"use client"

import { useToast } from "@/hooks/use-toast"
import { X } from "lucide-react"
import { cn } from "@/lib/utils"

export function Toaster() {
  const { toasts, dismiss } = useToast()

  return (
    <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={cn(
            "pointer-events-auto relative flex w-full max-w-md items-center justify-between gap-4 overflow-hidden rounded-lg border p-4 shadow-lg transition-all",
            toast.variant === "destructive"
              ? "border-destructive bg-destructive text-destructive-foreground"
              : "border-border bg-card text-card-foreground"
          )}
        >
          <div className="flex flex-col gap-1">
            {toast.title && (
              <div className="text-sm font-semibold">{toast.title}</div>
            )}
            {toast.description && (
              <div className="text-sm opacity-90">{toast.description}</div>
            )}
          </div>
          {toast.action}
          <button
            onClick={() => dismiss(toast.id)}
            className="absolute right-2 top-2 rounded-md p-1 opacity-70 transition-opacity hover:opacity-100"
          >
            <X className="h-4 w-4" />
            <span className="sr-only">Fechar</span>
          </button>
        </div>
      ))}
    </div>
  )
}
