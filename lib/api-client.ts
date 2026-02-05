import { getApiUrl } from './api'

const accessTokenKey = 'accessToken'
const refreshTokenKey = 'refreshToken'

// Flag to prevent multiple simultaneous redirects to login
let redirectingToLogin = false

const getAccessToken = () => typeof window !== 'undefined' ? localStorage.getItem(accessTokenKey) : null
const getRefreshToken = () => typeof window !== 'undefined' ? localStorage.getItem(refreshTokenKey) : null

interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: User
}

export interface User {
  id: number
  name: string
  email: string
}

export const storeSession = (authResponse: AuthResponse) => {
  if (typeof window === 'undefined' || !authResponse) return
  localStorage.setItem(accessTokenKey, authResponse.accessToken)
  localStorage.setItem(refreshTokenKey, authResponse.refreshToken)
  // Reset redirect flag when new session is established
  redirectingToLogin = false
}

export const clearSession = () => {
  if (typeof window === 'undefined') return
  localStorage.removeItem(accessTokenKey)
  localStorage.removeItem(refreshTokenKey)
  localStorage.removeItem('user')
}

export const hasStoredSession = () => {
  return typeof window !== 'undefined' && !!localStorage.getItem(accessTokenKey)
}

interface RequestOptions {
  skipAuth?: boolean
  skipRefresh?: boolean
  isForm?: boolean
  headers?: Record<string, string>
  _retry?: boolean
}

const buildHeaders = (options: RequestOptions = {}, method: string, hasBody: boolean): Record<string, string> => {
  const headers: Record<string, string> = { ...options.headers }
  if (!options.skipAuth) {
    const token = getAccessToken()
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }
  }
  if (!options.isForm && method !== 'GET' && hasBody) {
    headers['Content-Type'] = 'application/json'
  }
  return headers
}

const handleResponse = async (response: Response) => {
  const contentType = response.headers.get('content-type') || ''

  if (!response.ok) {
    let message = ''

    if (contentType.includes('application/json')) {
      try {
        const errorBody: any = await response.json()
        if (typeof errorBody === 'string') {
          message = errorBody
        } else if (errorBody) {
          message =
            errorBody.message ||
            errorBody.error ||
            (typeof errorBody === 'object' ? JSON.stringify(errorBody) : '')
        }
      } catch {
        message = await response.text()
      }
    } else {
      message = await response.text()
    }

    throw new Error(message || 'Erro na requisição')
  }

  if (response.status === 204) {
    return null
  }
  return response.json()
}

const refreshSession = async (): Promise<AuthResponse> => {
  try {
    const refreshToken = getRefreshToken()
    if (!refreshToken) {
      // Erro mais descritivo quando o refresh token não está disponível
      throw new Error('Refresh token ausente. Não foi possível atualizar a sessão.')
    }
    const response = await fetch(getApiUrl('/api/auth/refresh'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ refreshToken })
    })
    const data = await handleResponse(response)
    storeSession(data)
    return data
  } catch (error) {
    const message =
      error instanceof Error && error.message
        ? `Falha ao atualizar a sessão: ${error.message}`
        : 'Falha ao atualizar a sessão por motivo desconhecido.'
    throw new Error(message)
  }
}

let refreshPromise: Promise<AuthResponse> | null = null

const shouldRedirectToLogin = (): boolean => {
  if (typeof window === 'undefined' || window.location.pathname.startsWith('/login')) {
    return false
  }
  
  // Set flag atomically - if it's already true, don't redirect again
  if (redirectingToLogin) {
    return false
  }
  
  // Set flag before redirecting to prevent race conditions
  redirectingToLogin = true
  return true
}

const request = async <T>(method: string, path: string, body?: unknown, options: RequestOptions = {}): Promise<T> => {
  // Check for authentication token before making the request
  if (!options.skipAuth && !getAccessToken()) {
    // No token available - clear session and redirect to login
    clearSession()
    if (shouldRedirectToLogin()) {
      window.location.assign('/login')
    }
    throw new Error('Authentication required. Please log in.')
  }

  const config: RequestInit = {
    method,
    headers: buildHeaders(options, method, body !== undefined),
    body: options.isForm ? (body as BodyInit) : body !== undefined ? JSON.stringify(body) : undefined
  }
  const response = await fetch(getApiUrl(path), config)
  if (response.status === 401 && !options.skipRefresh && !options._retry) {
    try {
      if (!refreshPromise) {
        refreshPromise = refreshSession().finally(() => {
          refreshPromise = null
        })
      }
      await refreshPromise
      return request<T>(method, path, body, { ...options, _retry: true })
    } catch {
      clearSession()
      // Auto-logout on 401: clear session and redirect to login
      if (shouldRedirectToLogin()) {
        window.location.assign('/login')
      }
      throw new Error('Session expired')
    }
  }
  return handleResponse(response)
}

export const apiClient = {
  get<T>(path: string, options: RequestOptions = {}): Promise<T> {
    return request<T>('GET', path, undefined, options)
  },
  post<T>(path: string, body?: unknown, options: RequestOptions = {}): Promise<T> {
    return request<T>('POST', path, body, options)
  },
  put<T>(path: string, body?: unknown, options: RequestOptions = {}): Promise<T> {
    return request<T>('PUT', path, body, options)
  },
  patch<T>(path: string, body?: unknown, options: RequestOptions = {}): Promise<T> {
    return request<T>('PATCH', path, body, options)
  },
  delete<T>(path: string, options: RequestOptions = {}): Promise<T> {
    return request<T>('DELETE', path, undefined, options)
  }
}
