"use client"

import { createContext, useCallback, useContext, useMemo, type ReactNode } from 'react'
import { useToast as useShadcnToast } from '@/hooks/use-toast'

interface ToastContextValue {
  addToast: (message: string, type?: 'info' | 'success' | 'error' | 'warning') => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

export const ToastProvider = ({ children }: { children: ReactNode }) => {
  const { toast } = useShadcnToast()

  const addToast = useCallback((message: string, type: 'info' | 'success' | 'error' | 'warning' = 'info') => {
    const variant = type === 'error' ? 'destructive' : 'default'
    toast({
      description: message,
      variant,
    })
  }, [toast])

  const value = useMemo(() => ({ addToast }), [addToast])

  return (
    <ToastContext.Provider value={value}>
      {children}
    </ToastContext.Provider>
  )
}

export const useAppToast = () => {
  const context = useContext(ToastContext)
  if (!context) {
    throw new Error('useAppToast must be used within ToastProvider')
  }
  return context
}
