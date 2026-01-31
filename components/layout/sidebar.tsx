"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import Image from "next/image"
import { cn } from "@/lib/utils"
import { useSidebar } from "@/app/(dashboard)/layout"
import {
  LayoutDashboard,
  ArrowLeftRight,
  Wallet,
  Building2,
  Tags,
  PiggyBank,
  Wand2,
  Upload,
  Target,
  Bell,
  ChevronLeft,
  ChevronRight,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { useState, useEffect } from "react"

const navItems = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { href: "/transacoes", label: "Transações", icon: ArrowLeftRight },
  { href: "/contas", label: "Contas", icon: Wallet },
  { href: "/instituicoes", label: "Instituições", icon: Building2 },
  { href: "/categorias", label: "Categorias", icon: Tags },
  { href: "/orcamentos", label: "Orçamentos", icon: PiggyBank },
  { href: "/regras", label: "Regras", icon: Wand2 },
  { href: "/importar", label: "Importar", icon: Upload },
  { href: "/metas", label: "Metas", icon: Target },
  { href: "/alertas", label: "Alertas", icon: Bell },
]

export function Sidebar() {
  const pathname = usePathname()
  const { isOpen, toggle } = useSidebar()
  const [collapsed, setCollapsed] = useState(false)
  const [isMobile, setIsMobile] = useState(false)
  
  // Track viewport size reactively
  useEffect(() => {
    const handleResize = () => {
      const mobile = window.innerWidth < 1024
      setIsMobile(mobile)
      // Close mobile sidebar when resizing to desktop
      if (!mobile && isOpen) {
        toggle()
      }
    }
    
    // Set initial state
    handleResize()
    
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [isOpen, toggle])

  const handleOverlayClick = () => {
    toggle()
  }
  
  const handleOverlayKeyDown = (event: React.KeyboardEvent) => {
    if (event.key === "Escape" || event.key === "Enter" || event.key === " ") {
      event.preventDefault()
      toggle()
    }
  }

  return (
    <>
      {/* Mobile overlay */}
      {isOpen && (
        <div 
          className="fixed inset-0 bg-black/50 z-40 lg:hidden"
          role="button"
          aria-label="Close menu"
          tabIndex={0}
          onClick={handleOverlayClick}
          onKeyDown={handleOverlayKeyDown}
        />
      )}
      
      <aside
        className={cn(
          "flex flex-col h-screen bg-sidebar border-r border-sidebar-border transition-all duration-300 z-50",
          "fixed lg:relative",
          // Mobile: show/hide based on isOpen
          "lg:translate-x-0",
          isOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0",
          // Desktop: collapse functionality
          collapsed && "lg:w-16",
          !collapsed && "w-64"
        )}
      >
      <div className="flex items-center justify-between h-16 px-4 border-b border-sidebar-border">
        {!collapsed && (
          <div className="flex flex-col">
            <Image
              src="/brand/logo.png"
              alt="Moneta"
              width={160}
              height={40}
              className="h-8 w-auto"
              priority
            />
            <span className="text-xs text-muted-foreground">Gestão Financeira</span>
          </div>
        )}
        {collapsed && (
          <Image
            src="/brand/icon.png"
            alt="Moneta"
            width={32}
            height={32}
            className="h-8 w-8"
          />
        )}
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8 text-muted-foreground hover:text-foreground"
          onClick={() => setCollapsed(!collapsed)}
        >
          {collapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
        </Button>
      </div>

      <nav className="flex-1 py-4 overflow-y-auto scrollbar-thin">
        <ul className="space-y-1 px-2">
          {navItems.map((item) => {
            const isActive = pathname === item.href || pathname?.startsWith(item.href + "/")
            return (
              <li key={item.href}>
                <Link
                  href={item.href}
                  onClick={() => {
                    // Close mobile sidebar when navigating
                    if (isMobile && isOpen) {
                      toggle()
                    }
                  }}
                  className={cn(
                    "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors",
                    isActive
                      ? "bg-primary/10 text-primary"
                      : "text-muted-foreground hover:text-foreground hover:bg-secondary"
                  )}
                  title={collapsed ? item.label : undefined}
                >
                  <item.icon className="h-5 w-5 flex-shrink-0" />
                  {!collapsed && <span>{item.label}</span>}
                </Link>
              </li>
            )
          })}
        </ul>
      </nav>
    </aside>
    </>
  )
}
