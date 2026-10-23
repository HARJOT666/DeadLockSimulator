import axios, { AxiosError } from 'axios'
import type { ApiError } from './types'

/**
 * Axios instance. Authentication uses the HttpOnly cookie set by the backend, so no token is ever
 * read or stored by JavaScript (withCredentials makes the browser send it).
 */
export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  withCredentials: true,
  timeout: 30_000,
  headers: { 'X-Requested-With': 'XMLHttpRequest' },
})

/** Listeners notified when the session is rejected (expired/invalid token). */
const unauthorizedListeners = new Set<() => void>()
export function onUnauthorized(fn: () => void) {
  unauthorizedListeners.add(fn)
  return () => { unauthorizedListeners.delete(fn) }
}

http.interceptors.response.use(
  (r) => r,
  (error: AxiosError) => {
    const url = error.config?.url ?? ''
    if (error.response?.status === 401 && !url.includes('/auth/login') && !url.includes('/auth/me')) {
      unauthorizedListeners.forEach((fn) => fn())
    }
    return Promise.reject(error)
  },
)

/** Human-readable message (plus validation details) from any API failure. */
export function describeError(error: unknown): { message: string; details: string[] } {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as Partial<ApiError> | undefined
    if (body?.message) return { message: body.message, details: body.details ?? [] }
    if (!error.response) return { message: 'Cannot reach the server. Is the backend running on port 8080?', details: [] }
    return { message: `Request failed (${error.response.status})`, details: [] }
  }
  return { message: error instanceof Error ? error.message : 'Unexpected error', details: [] }
}
