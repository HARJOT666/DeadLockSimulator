import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'
import { FullScreenLoader } from '../../components/ui/Feedback'

/** Redirects unauthenticated visitors to /login, remembering where they wanted to go. */
export function ProtectedRoute() {
  const { user, loading, sessionExpired } = useAuth()
  const location = useLocation()
  if (loading) return <FullScreenLoader label="Checking your session" />
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname, expired: sessionExpired }} />
  return <Outlet />
}

/** Sends already-authenticated users away from /login and /register. */
export function GuestRoute() {
  const { user, loading } = useAuth()
  if (loading) return <FullScreenLoader label="Checking your session" />
  return user ? <Navigate to="/dashboard" replace /> : <Outlet />
}
