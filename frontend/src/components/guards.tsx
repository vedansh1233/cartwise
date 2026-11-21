import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth'
import { Spinner } from './ui'

export function RequireAuth({ children, admin }: { children: ReactNode; admin?: boolean }) {
  const { user, ready } = useAuth()
  const location = useLocation()
  if (!ready) return <Spinner />
  if (!user) return <Navigate to={`/login?next=${encodeURIComponent(location.pathname)}`} replace />
  if (admin && user.role !== 'ADMIN') return <Navigate to="/" replace />
  return <>{children}</>
}
