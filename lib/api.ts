/**
 * Centralized API configuration
 * 
 * This module provides the base URL configuration for all backend API calls.
 * The URL is configured via the NEXT_PUBLIC_API_URL environment variable.
 */

/**
 * Get the API base URL from environment variables with appropriate fallbacks
 */
export const getApiBaseUrl = (): string => {
  const envUrl = process.env.NEXT_PUBLIC_API_URL

  // If environment variable is set, use it
  if (envUrl) {
    return envUrl
  }

  // Fallback logic based on environment
  if (process.env.NODE_ENV === 'development') {
    // In development, default to localhost
    return 'http://localhost:8080'
  }

  // In production, if not configured, log warning but don't throw during build
  // The error will be thrown at runtime when API calls are made
  if (typeof window !== 'undefined') {
    console.error(
      'FATAL: NEXT_PUBLIC_API_URL is not configured. ' +
      'Please set this environment variable to your backend API URL. ' +
      'Example: NEXT_PUBLIC_API_URL=http://136.248.123.125:8080'
    )
  }
  
  // Return empty string for build time, will fail at runtime if actually used
  return ''
}

/**
 * The base URL for all API requests
 */
export const API_BASE_URL = getApiBaseUrl()

/**
 * Build a full API URL from a path
 * 
 * @param path - The API path (e.g., '/api/auth/login')
 * @returns The full URL (e.g., 'http://localhost:8080/api/auth/login')
 */
export const getApiUrl = (path: string): string => {
  const baseUrl = API_BASE_URL
  
  // Validate that API URL is configured (runtime check)
  if (!baseUrl && process.env.NODE_ENV === 'production' && typeof window !== 'undefined') {
    throw new Error(
      'API URL not configured. Please set NEXT_PUBLIC_API_URL environment variable. ' +
      'Example: NEXT_PUBLIC_API_URL=http://136.248.123.125:8080'
    )
  }
  
  // Ensure path starts with /
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  
  // Remove trailing slash from base URL if present
  const normalizedBaseUrl = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl
  
  return `${normalizedBaseUrl}${normalizedPath}`
}

/**
 * Helper function for making API requests with proper URL construction
 * This is a convenience wrapper around fetch that automatically prefixes the API base URL
 * 
 * @param path - The API path
 * @param options - Fetch options
 * @returns Promise with the fetch response
 */
export const apiFetch = (path: string, options?: RequestInit): Promise<Response> => {
  return fetch(getApiUrl(path), options)
}
