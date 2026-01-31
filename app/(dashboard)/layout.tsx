"use client"

import { useEffect, useState, createContext, useContext } from "react"
import { useRouter } from "next/navigation"
import { AuthProvider, useAuth } from "@/contexts/auth-context"
import { ToastProvider } from "@/contexts/toast-context"
import { Sidebar } from "@/components/layout/sidebar"
import { Topbar } from "@/components/layout/topbar"
import { hasStoredSession } from "@/lib/api-client"
import { Loader2 } from "lucide-react"

// Context for sidebar state
const SidebarContext = createContext<{
  isOpen: boolean
  toggle: () => void
} | null>(null)

export const useSidebar = () => {
  const context = useContext(SidebarContext)
  if (!context) {
    throw new Error('useSidebar must be used within SidebarProvider')
  }
  return context
}

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
          <p className="text-muted-foreground">Carregando sessão...</p>
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
  const [sidebarOpen, setSidebarOpen] = useState(false)

  return (
    <AuthProvider>
      <ToastProvider>
        <AuthGuard>
          <SidebarContext.Provider value={{ isOpen: sidebarOpen, toggle: () => setSidebarOpen(!sidebarOpen) }}>
            <div className="flex h-screen bg-background overflow-hidden">
              <Sidebar />
              <div className="flex-1 flex flex-col overflow-hidden w-full lg:w-auto">
                <Topbar />
                <main className="flex-1 overflow-y-auto p-4 sm:p-6 scrollbar-thin">
                  {children}
                </main>
              </div>
            </div>
          </SidebarContext.Provider>
        </AuthGuard>
      </ToastProvider>
    </AuthProvider>
  )
}
