"use client"

import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { apiClient, clearSession, storeSession, type User } from '@/lib/api-client'

interface AuthContextValue {
  user: User | null
  loading: boolean
  login: (credentials: { email: string; password: string }) => Promise<User>
  register: (data: { name: string; email: string; password: string }) => Promise<User>
  logout: () => void
  refreshProfile: () => Promise<User | null>
}

const AuthContext = createContext<AuthContextValue | null>(null)

const storageUserKey = 'user'

const readStoredUser = (): User | null => {
  if (typeof window === 'undefined') return null
  const raw = localStorage.getItem(storageUserKey)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    localStorage.removeItem(storageUserKey)
    return null
  }
}

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(false)
  const [initialized, setInitialized] = useState(false)

  useEffect(() => {
    const storedUser = readStoredUser()
    setUser(storedUser)
    setInitialized(true)
  }, [])

  const persistUser = useCallback((nextUser: User | null) => {
    if (typeof window === 'undefined') return
    if (nextUser) {
      localStorage.setItem(storageUserKey, JSON.stringify(nextUser))
    } else {
      localStorage.removeItem(storageUserKey)
    }
    setUser(nextUser)
  }, [])

  const login = useCallback(async ({ email, password }: { email: string; password: string }) => {
    setLoading(true)
    try {
      const response = await apiClient.post<{ accessToken: string; refreshToken: string; user: User }>(
        '/api/auth/login',
        { email, password },
        { skipAuth: true, skipRefresh: true }
      )
      storeSession(response)
      persistUser(response.user)
      return response.user
    } finally {
      setLoading(false)
    }
  }, [persistUser])

  const register = useCallback(async ({ name, email, password }: { name: string; email: string; password: string }) => {
    setLoading(true)
    try {
      const response = await apiClient.post<{ accessToken: string; refreshToken: string; user: User }>(
        '/api/auth/register',
        { name, email, password },
        { skipAuth: true, skipRefresh: true }
      )
      storeSession(response)
      persistUser(response.user)
      return response.user
    } finally {
      setLoading(false)
    }
  }, [persistUser])

  const logout = useCallback(() => {
    clearSession()
    persistUser(null)
  }, [persistUser])

  const refreshProfile = useCallback(async () => {
    if (typeof window === 'undefined' || !localStorage.getItem('accessToken')) {
      return null
    }
    const response = await apiClient.get<User>('/api/me')
    persistUser(response)
    return response
  }, [persistUser])

  useEffect(() => {
    if (initialized && !user && typeof window !== 'undefined' && localStorage.getItem('accessToken')) {
      refreshProfile().catch(() => {
        clearSession()
        persistUser(null)
      })
    }
  }, [initialized, refreshProfile, user, persistUser])

  const value = useMemo(() => ({
    user,
    loading,
    login,
    register,
    logout,
    refreshProfile
  }), [user, loading, login, register, logout, refreshProfile])

  if (!initialized) {
    return null
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
