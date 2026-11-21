import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, date, items, money, type OrderStatus } from '../../api'
import { ErrorNote, Spinner, StatusBadge } from '../../components/ui'
import { OrderTimeline } from '../OrdersPage'

const FILTERS: (OrderStatus | '')[] = ['', 'PLACED', 'PACKED', 'SHIPPED', 'DELIVERED', 'CANCELLED']

export function AdminOrders() {
  const [status, setStatus] = useState<OrderStatus | ''>('')
  const [openId, setOpenId] = useState<number | null>(null)
  const orders = useQuery({ queryKey: ['admin', 'orders', status], queryFn: () => api.adminOrders(status) })

  return (
    <div className="grid gap-4 lg:grid-cols-[1fr_380px]">
      <div className="space-y-3">
        <div className="flex flex-wrap gap-2">
          {FILTERS.map(f => (
            <button key={f || 'all'} onClick={() => setStatus(f)} className={`rounded-full px-3 py-1.5 text-sm font-medium ${status === f ? 'bg-slate-900 text-white' : 'bg-white text-slate-600 ring-1 ring-slate-200 hover:bg-slate-50'}`}>
              {f ? f.charAt(0) + f.slice(1).toLowerCase() : 'All'}
            </button>
          ))}
        </div>
        {orders.isLoading ? <Spinner /> : (
          <div className="card divide-y divide-slate-100">
            {orders.data?.items.length === 0 && <p className="p-6 text-center text-sm text-slate-500">No orders with this status.</p>}
            {orders.data?.items.map(o => (
              <button key={o.id} onClick={() => setOpenId(o.id)} className={`flex w-full items-center justify-between gap-3 px-4 py-3 text-left text-sm hover:bg-slate-50 ${openId === o.id ? 'bg-brand-50' : ''}`}>
                <div><p className="font-semibold">#{o.id} · {o.customerName}</p><p className="text-slate-500">{date(o.createdAt)} · {items(o.itemCount)}</p></div>
                <div className="flex items-center gap-3"><StatusBadge status={o.status} /><span className="w-20 text-right font-semibold">{money(o.total)}</span></div>
              </button>
            ))}
          </div>
        )}
      </div>
      {openId ? <OrderPanel id={openId} /> : <div className="card hidden h-fit p-6 text-center text-sm text-slate-500 lg:block">Select an order to see details and update its status.</div>}
    </div>
  )
}

function OrderPanel({ id }: { id: number }) {
  const qc = useQueryClient()
  const order = useQuery({ queryKey: ['admin', 'order', id], queryFn: () => api.adminOrder(id) })
  const move = useMutation({
    mutationFn: (s: OrderStatus) => api.adminSetStatus(id, s),
    onSuccess: o => { qc.setQueryData(['admin', 'order', id], o); qc.invalidateQueries({ queryKey: ['admin', 'orders'] }); qc.invalidateQueries({ queryKey: ['admin', 'stats'] }) },
  })
  if (order.isLoading) return <div className="card"><Spinner /></div>
  if (!order.data) return null
  const o = order.data
  return (
    <aside className="card h-fit space-y-4 p-5 lg:sticky lg:top-24">
      <div className="flex items-center justify-between"><h2 className="font-bold">Order #{o.id}</h2><StatusBadge status={o.status} /></div>
      <OrderTimeline status={o.status} />
      <div className="text-sm"><p className="font-medium">{o.address.fullName} · {o.address.phone}</p><p className="text-slate-500">{o.customerEmail}</p><p className="text-slate-600">{o.address.line1}, {o.address.city}, {o.address.state} {o.address.pincode}</p></div>
      <ul className="space-y-1 text-sm">{o.items.map(i => <li key={i.productId} className="flex justify-between gap-2"><span className="truncate">{i.emoji} {i.quantity} × {i.productName}</span><span>{money(i.lineTotal)}</span></li>)}</ul>
      <div className="flex justify-between border-t border-slate-100 pt-2 font-bold"><span>Total</span><span>{money(o.total)}</span></div>
      <ErrorNote message={move.isError ? (move.error as Error).message : null} />
      {o.nextStatuses.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {o.nextStatuses.map(s => (
            <button key={s} disabled={move.isPending} onClick={() => move.mutate(s)} className={s === 'CANCELLED' ? 'btn-ghost text-rose-600' : 'btn-primary'}>
              {s === 'CANCELLED' ? 'Cancel order' : `Mark as ${s.toLowerCase()}`}
            </button>
          ))}
        </div>
      )}
    </aside>
  )
}
