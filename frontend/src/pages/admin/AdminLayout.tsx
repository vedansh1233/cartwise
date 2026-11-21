import { NavLink, Outlet } from 'react-router-dom'

const link = ({ isActive }: { isActive: boolean }) =>
  `rounded-lg px-3 py-2 text-sm font-medium ${isActive ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-100'}`

export function AdminLayout() {
  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">Admin</h1>
        <nav className="flex gap-1 rounded-xl border border-slate-200 bg-white p-1">
          <NavLink to="/admin" end className={link}>Dashboard</NavLink>
          <NavLink to="/admin/products" className={link}>Products</NavLink>
          <NavLink to="/admin/orders" className={link}>Orders</NavLink>
        </nav>
      </div>
      <Outlet />
    </div>
  )
}
