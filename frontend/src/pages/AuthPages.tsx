import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { ApiError } from '../api'
import { useAuth } from '../auth'
import { ErrorNote } from '../components/ui'

function useNext() {
  const [params] = useSearchParams()
  const next = params.get('next') ?? '/'
  return next.startsWith('/') && !next.startsWith('//') ? next : '/'
}

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const next = useNext()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true); setError(null)
    try {
      const user = await login(email, password)
      navigate(next === '/' && user.role === 'ADMIN' ? '/admin' : next, { replace: true })
    } catch (err) {
      setError((err as Error).message)
    } finally { setBusy(false) }
  }

  return (
    <div className="mx-auto max-w-md">
      <form onSubmit={submit} className="card space-y-4 p-6 sm:p-8">
        <div><h1 className="text-2xl font-bold">Welcome back</h1><p className="text-sm text-slate-500">Log in to view your cart and orders.</p></div>
        <div><label className="label" htmlFor="email">Email</label><input id="email" type="email" autoComplete="email" className="input" value={email} onChange={e => setEmail(e.target.value)} required /></div>
        <div><label className="label" htmlFor="password">Password</label><input id="password" type="password" autoComplete="current-password" className="input" value={password} onChange={e => setPassword(e.target.value)} required /></div>
        <ErrorNote message={error} />
        <button className="btn-primary w-full" disabled={busy}>{busy ? 'Logging in…' : 'Log in'}</button>
        <p className="text-center text-sm text-slate-500">New here? <Link to={`/register?next=${encodeURIComponent(next)}`} className="font-semibold text-brand-600">Create an account</Link></p>
      </form>
      <div className="mt-4 rounded-2xl border border-dashed border-slate-300 p-4 text-sm text-slate-600">
        <p className="font-semibold text-slate-800">Demo accounts</p>
        <p>Customer: <code>demo@cartwise.dev</code> / <code>Demo@1234</code></p>
        <p>Admin: <code>admin@cartwise.dev</code> / <code>Admin@1234</code></p>
      </div>
    </div>
  )
}

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const next = useNext()
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [error, setError] = useState<ApiError | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true); setError(null)
    try {
      await register(form.name, form.email, form.password)
      navigate(next, { replace: true })
    } catch (err) {
      setError(err as ApiError)
    } finally { setBusy(false) }
  }
  const f = error?.fields ?? {}

  return (
    <form onSubmit={submit} className="card mx-auto max-w-md space-y-4 p-6 sm:p-8" noValidate>
      <div><h1 className="text-2xl font-bold">Create your account</h1><p className="text-sm text-slate-500">It takes less than a minute.</p></div>
      {(['name', 'email', 'password'] as const).map(k => (
        <div key={k}>
          <label className="label" htmlFor={k}>{k === 'name' ? 'Full name' : k === 'email' ? 'Email' : 'Password'}</label>
          <input id={k} type={k === 'name' ? 'text' : k} className="input" value={form[k]} onChange={e => setForm(s => ({ ...s, [k]: e.target.value }))}
            autoComplete={k === 'password' ? 'new-password' : k} placeholder={k === 'password' ? 'At least 8 characters' : undefined} required />
          {f[k] && <p className="mt-1 text-xs text-rose-600">{f[k]}</p>}
        </div>
      ))}
      <ErrorNote message={error && Object.keys(f).length === 0 ? error.message : null} />
      <button className="btn-primary w-full" disabled={busy}>{busy ? 'Creating account…' : 'Create account'}</button>
      <p className="text-center text-sm text-slate-500">Already have an account? <Link to="/login" className="font-semibold text-brand-600">Log in</Link></p>
    </form>
  )
}
