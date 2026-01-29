"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { AuthProvider, useAuth } from "@/contexts/auth-context"
import { ToastProvider } from "@/contexts/toast-context"
import { Sidebar } from "@/components/layout/sidebar"
import { Topbar } from "@/components/layout/topbar"
import { hasStoredSession } from "@/lib/api-client"
import { Loader2 } from "lucide-react"

function AuthGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter()
  const { user } = useAuth()
  const [checking, setChecking] = useState(true)

  useEffect(() => {
    if (!hasStoredSession()) {
      router.replace("/login")
    } else {
      setChecking(false)
    }
  }, [router])

  if (checking || !user) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-background">
        <div className="flex flex-col items-center gap-4">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
          <p className="text-muted-foreground">Carregando sessao...</p>
        </div>
      </div>
    )
  }

  return <>{children}</>
}

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <AuthProvider>
      <ToastProvider>
        <AuthGuard>
          <div className="flex h-screen bg-background">
            <Sidebar />
            <div className="flex-1 flex flex-col overflow-hidden">
              <Topbar />
              <main className="flex-1 overflow-y-auto p-6 scrollbar-thin">
                {children}
              </main>
            </div>
          </div>
        </AuthGuard>
      </ToastProvider>
    </AuthProvider>
  )
}
