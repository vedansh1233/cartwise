import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api, money, type OrderStatus } from '../../api'
import { ErrorNote, Spinner, StatusBadge } from '../../components/ui'

export function AdminDashboard() {
  const stats = useQuery({ queryKey: ['admin', 'stats'], queryFn: api.adminStats })
  const recent = useQuery({ queryKey: ['admin', 'orders', ''], queryFn: () => api.adminOrders('') })
  if (stats.isLoading) return <Spinner />
  if (stats.isError) return <ErrorNote message={(stats.error as Error).message} />
  const s = stats.data!
  const statusTotal = Object.values(s.ordersByStatus).reduce((a, b) => a + b, 0) || 1

  const tiles = [
    { label: 'Revenue', value: money(s.revenue), hint: 'excluding cancelled orders' },
    { label: 'Orders', value: s.orders, hint: `${s.ordersByStatus.PLACED} waiting to be packed` },
    { label: 'Customers', value: s.customers, hint: 'registered accounts' },
    { label: 'Low stock', value: s.lowStockProducts, hint: `of ${s.products} products (< 5 left)`, warn: s.lowStockProducts > 0 },
  ]

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {tiles.map(t => (
          <div key={t.label} className="card p-5">
            <p className="text-sm text-slate-500">{t.label}</p>
            <p className={`mt-1 text-3xl font-bold ${t.warn ? 'text-amber-600' : ''}`}>{t.value}</p>
            <p className="mt-1 text-xs text-slate-400">{t.hint}</p>
          </div>
        ))}
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <div className="card p-5">
          <h2 className="mb-4 font-semibold">Orders by status</h2>
          <div className="space-y-3">
            {(Object.entries(s.ordersByStatus) as [OrderStatus, number][]).map(([status, n]) => (
              <div key={status} className="grid grid-cols-[96px_1fr_32px] items-center gap-3 text-sm">
                <StatusBadge status={status} />
                <div className="h-2.5 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-brand-500" style={{ width: `${(n / statusTotal) * 100}%` }} /></div>
                <span className="text-right font-semibold">{n}</span>
              </div>
            ))}
          </div>
        </div>
        <div className="card p-5">
          <h2 className="mb-4 font-semibold">Best sellers</h2>
          {s.topProducts.length === 0 ? <p className="text-sm text-slate-500">No sales yet.</p> : (
            <table className="w-full text-sm">
              <thead><tr className="text-left text-slate-500"><th className="pb-2 font-medium">Product</th><th className="pb-2 text-right font-medium">Units</th><th className="pb-2 text-right font-medium">Revenue</th></tr></thead>
              <tbody>{s.topProducts.map(p => <tr key={p.name} className="border-t border-slate-100"><td className="py-2">{p.name}</td><td className="py-2 text-right">{p.unitsSold}</td><td className="py-2 text-right font-semibold">{money(p.revenue)}</td></tr>)}</tbody>
            </table>
          )}
        </div>
      </div>
      <div className="card p-5">
        <div className="mb-3 flex items-center justify-between"><h2 className="font-semibold">Recent orders</h2><Link to="/admin/orders" className="text-sm font-semibold text-brand-600">Manage →</Link></div>
        <div className="divide-y divide-slate-100 text-sm">
          {recent.data?.items.slice(0, 5).map(o => (
            <div key={o.id} className="flex items-center justify-between gap-3 py-2.5">
              <span className="font-medium">#{o.id} · {o.customerName}</span>
              <div className="flex items-center gap-3"><StatusBadge status={o.status} /><span className="w-20 text-right font-semibold">{money(o.total)}</span></div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
