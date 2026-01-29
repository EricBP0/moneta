"use client"

import { AuthProvider } from "@/contexts/auth-context"
import { ToastProvider } from "@/contexts/toast-context"

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <AuthProvider>
      <ToastProvider>
        <div className="min-h-screen flex items-center justify-center bg-background p-4">
          {children}
        </div>
      </ToastProvider>
    </AuthProvider>
  )
}
