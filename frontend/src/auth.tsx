import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { api, tokenStore, type User } from './api'

type AuthState = {
  user: User | null
  ready: boolean
  login: (email: string, password: string) => Promise<User>
  register: (name: string, email: string, password: string) => Promise<User>
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [ready, setReady] = useState(false)
  const qc = useQueryClient()

  useEffect(() => {
    if (!tokenStore.get()) { setReady(true); return }
    api.me().then(setUser).catch(() => tokenStore.set(null)).finally(() => setReady(true))
  }, [])

  useEffect(() => {
    const onLogout = () => { setUser(null); qc.clear() }
    window.addEventListener('cartwise:logout', onLogout)
    return () => window.removeEventListener('cartwise:logout', onLogout)
  }, [qc])

  const accept = useCallback((res: { token: string; user: User }) => {
    tokenStore.set(res.token)
    setUser(res.user)
    qc.invalidateQueries()
    return res.user
  }, [qc])

  const value: AuthState = {
    user,
    ready,
    login: async (email, password) => accept(await api.login(email, password)),
    register: async (name, email, password) => accept(await api.register(name, email, password)),
    logout: () => { tokenStore.set(null); setUser(null); qc.clear() },
  }
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
