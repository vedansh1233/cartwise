import { Link, useParams, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, date, items, money, type OrderStatus } from '../api'
import { Empty, ErrorNote, Spinner, StatusBadge } from '../components/ui'

export function OrdersPage() {
  const orders = useQuery({ queryKey: ['orders'], queryFn: () => api.myOrders() })
  if (orders.isLoading) return <Spinner />
  if (orders.isError) return <ErrorNote message={(orders.error as Error).message} />
  if (orders.data!.items.length === 0) {
    return <Empty icon="📦" title="No orders yet"><Link to="/shop" className="font-semibold text-brand-600">Find something you like</Link></Empty>
  }
  return (
    <div className="mx-auto max-w-3xl space-y-3">
      <h1 className="text-2xl font-bold">Your orders</h1>
      {orders.data!.items.map(o => (
        <Link key={o.id} to={`/orders/${o.id}`} className="card flex items-center justify-between gap-4 p-4 transition hover:border-brand-200 hover:shadow-md">
          <div>
            <p className="font-semibold">Order #{o.id}</p>
            <p className="text-sm text-slate-500">{date(o.createdAt)} · {items(o.itemCount)}</p>
          </div>
          <div className="flex items-center gap-4"><StatusBadge status={o.status} /><span className="font-bold">{money(o.total)}</span></div>
        </Link>
      ))}
    </div>
  )
}

const STEPS: OrderStatus[] = ['PLACED', 'PACKED', 'SHIPPED', 'DELIVERED']

export function OrderTimeline({ status }: { status: OrderStatus }) {
  if (status === 'CANCELLED') return <p className="rounded-xl bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700">This order was cancelled and the items were returned to stock.</p>
  const current = STEPS.indexOf(status)
  return (
    <ol className="grid grid-cols-4 gap-2">
      {STEPS.map((s, i) => (
        <li key={s} className="flex flex-col items-center gap-2 text-center">
          <div className={`h-2 w-full rounded-full ${i <= current ? 'bg-brand-600' : 'bg-slate-200'}`} />
          <span className={`text-xs font-semibold ${i <= current ? 'text-brand-700' : 'text-slate-400'}`}>{s.charAt(0) + s.slice(1).toLowerCase()}</span>
        </li>
      ))}
    </ol>
  )
}

export function OrderPage() {
  const id = Number(useParams().id)
  const [params] = useSearchParams()
  const qc = useQueryClient()
  const order = useQuery({ queryKey: ['order', id], queryFn: () => api.myOrder(id) })
  const cancel = useMutation({
    mutationFn: () => api.cancelOrder(id),
    onSuccess: o => { qc.setQueryData(['order', id], o); qc.invalidateQueries({ queryKey: ['orders'] }) },
  })
  if (order.isLoading) return <Spinner />
  if (order.isError) return <Empty icon="📦" title="Order not found"><Link to="/orders" className="font-semibold text-brand-600">Back to your orders</Link></Empty>
  const o = order.data!

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      {params.get('placed') && <div className="rounded-2xl bg-emerald-50 px-5 py-4 text-emerald-800"><span className="font-semibold">Order placed! 🎉</span> We'll keep you posted as it moves along.</div>}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">Order #{o.id}</h1>
          <p className="text-sm text-slate-500">Placed on {date(o.createdAt)} · {o.paymentMethod === 'COD' ? 'Cash on delivery' : 'UPI (demo)'}</p>
        </div>
        <StatusBadge status={o.status} />
      </div>
      <div className="card p-5"><OrderTimeline status={o.status} /></div>
      <div className="card divide-y divide-slate-100">
        {o.items.map(i => (
          <div key={i.productId} className="flex items-center justify-between gap-3 p-4">
            <div className="flex items-center gap-3"><span className="text-2xl" aria-hidden>{i.emoji ?? '🛍️'}</span><div><p className="font-medium">{i.productName}</p><p className="text-sm text-slate-500">{i.quantity} × {money(i.unitPrice)}</p></div></div>
            <span className="font-semibold">{money(i.lineTotal)}</span>
          </div>
        ))}
        <div className="space-y-1 p-4 text-sm">
          <div className="flex justify-between"><span className="text-slate-500">Subtotal</span><span>{money(o.subtotal)}</span></div>
          <div className="flex justify-between"><span className="text-slate-500">Delivery</span><span>{o.shipping === 0 ? 'FREE' : money(o.shipping)}</span></div>
          <div className="flex justify-between pt-1 text-base font-bold"><span>Total</span><span>{money(o.total)}</span></div>
        </div>
      </div>
      <div className="card p-5 text-sm">
        <h2 className="mb-1 font-semibold">Delivering to</h2>
        <p>{o.address.fullName} · {o.address.phone}</p>
        <p className="text-slate-600">{o.address.line1}, {o.address.city}, {o.address.state} {o.address.pincode}</p>
      </div>
      <ErrorNote message={cancel.isError ? (cancel.error as Error).message : null} />
      {o.status === 'PLACED' && <button className="btn-ghost text-rose-600" disabled={cancel.isPending} onClick={() => cancel.mutate()}>{cancel.isPending ? 'Cancelling…' : 'Cancel order'}</button>}
    </div>
  )
}
