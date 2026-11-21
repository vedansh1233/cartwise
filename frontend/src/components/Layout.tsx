import { useState, type FormEvent } from 'react'
import { Link, NavLink, Outlet, useNavigate, useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api'
import { useAuth } from '../auth'

export function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [q, setQ] = useState(params.get('q') ?? '')
  const cart = useQuery({ queryKey: ['cart'], queryFn: api.cart, enabled: !!user })

  const search = (e: FormEvent) => {
    e.preventDefault()
    navigate(q.trim() ? `/shop?q=${encodeURIComponent(q.trim())}` : '/shop')
  }

  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-30 border-b border-slate-200/80 bg-white/90 backdrop-blur">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center gap-x-3 gap-y-2 px-4 py-3 sm:flex-nowrap sm:gap-6">
          <Link to="/" className="flex items-center gap-2 text-lg font-extrabold tracking-tight">
            <img src="/favicon.svg" alt="" className="h-8 w-8" /> Cartwise
          </Link>
          <form onSubmit={search} className="order-3 w-full sm:order-none sm:flex-1" role="search">
            <input value={q} onChange={e => setQ(e.target.value)} className="input bg-slate-50" placeholder="Search for earbuds, books, shoes…" aria-label="Search products" />
          </form>
          <nav className="ml-auto flex items-center gap-0.5 text-sm font-medium sm:ml-0 sm:gap-1">
            <NavLink to="/shop" className="rounded-lg px-2 py-2 text-slate-600 hover:bg-slate-100 sm:px-3">Shop</NavLink>
            {user?.role === 'ADMIN' && <NavLink to="/admin" className="rounded-lg px-2 py-2 text-brand-700 hover:bg-brand-50 sm:px-3">Admin</NavLink>}
            {user ? (
              <>
                <NavLink to="/orders" className="rounded-lg px-2 py-2 text-slate-600 hover:bg-slate-100 sm:px-3">Orders</NavLink>
                <button onClick={() => { logout(); window.location.assign('/') }} className="rounded-lg px-2 py-2 text-slate-600 hover:bg-slate-100 sm:px-3">Log out</button>
              </>
            ) : (
              <NavLink to="/login" className="rounded-lg px-2 py-2 text-slate-600 hover:bg-slate-100 sm:px-3">Log in</NavLink>
            )}
            <NavLink to="/cart" className="relative ml-1 rounded-xl bg-slate-900 px-3.5 py-2 text-white hover:bg-slate-700" aria-label="Cart">
              🛒<span className="ml-1.5 hidden sm:inline">Cart</span>
              {!!cart.data?.itemCount && (
                <span className="absolute -right-2 -top-2 flex h-5 min-w-5 items-center justify-center rounded-full bg-rose-500 px-1 text-[11px] font-bold">{cart.data.itemCount}</span>
              )}
            </NavLink>
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-6 sm:py-8"><Outlet /></main>
      <footer className="border-t border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl flex-col gap-1 px-4 py-6 text-sm text-slate-500 sm:flex-row sm:justify-between">
          <p>Cartwise · demo store built with Spring Boot and React</p>
          <p>Payments are simulated; no real money is charged.</p>
        </div>
      </footer>
    </div>
  )
}
