import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api } from '../../api/endpoints'
import { onUnauthorized } from '../../api/client'
import type { User } from '../../api/types'

interface AuthState {
  user: User | null
  /** True until the initial /auth/me check has finished. */
  loading: boolean
  /** Set when the server rejected the session (expired/invalid token). */
  sessionExpired: boolean
  login: (email: string, password: string) => Promise<void>
  register: (name: string, email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

/**
 * Session state. The JWT lives in an HttpOnly cookie that JavaScript cannot read, so the client
 * learns who is logged in by asking /auth/me.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [sessionExpired, setSessionExpired] = useState(false)

  useEffect(() => {
    api.me().then(setUser).catch(() => setUser(null)).finally(() => setLoading(false))
    return onUnauthorized(() => {
      setUser((current) => {
        if (current) setSessionExpired(true)
        return null
      })
    })
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const res = await api.login(email, password)
    setSessionExpired(false)
    setUser(res.user)
  }, [])

  const register = useCallback(async (name: string, email: string, password: string) => {
    const res = await api.register(name, email, password)
    setSessionExpired(false)
    setUser(res.user)
  }, [])

  const logout = useCallback(async () => {
    try { await api.logout() } finally { setUser(null) }
  }, [])

  const value = useMemo(() => ({ user, loading, sessionExpired, login, register, logout }),
    [user, loading, sessionExpired, login, register, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}
