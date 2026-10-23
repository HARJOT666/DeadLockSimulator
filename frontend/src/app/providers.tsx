import type { ReactNode } from 'react'
import { BrowserRouter } from 'react-router-dom'
import { MotionConfig } from 'framer-motion'
import { AuthProvider } from '../features/auth/AuthContext'

/** App-wide providers. MotionConfig makes every Framer animation honour prefers-reduced-motion. */
export function Providers({ children }: { children: ReactNode }) {
  return (
    <BrowserRouter>
      <MotionConfig reducedMotion="user">
        <AuthProvider>{children}</AuthProvider>
      </MotionConfig>
    </BrowserRouter>
  )
}
