import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError, items, money, type Address, type PaymentMethod } from '../api'
import { useAuth } from '../auth'
import { Empty, ErrorNote, Spinner } from '../components/ui'

const FIELDS: { key: keyof Address; label: string; placeholder: string; full?: boolean; inputMode?: 'numeric' | 'tel' }[] = [
  { key: 'fullName', label: 'Full name', placeholder: 'As on your ID' },
  { key: 'phone', label: 'Mobile number', placeholder: '10-digit mobile', inputMode: 'tel' },
  { key: 'line1', label: 'Address', placeholder: 'House no., street, area', full: true },
  { key: 'city', label: 'City', placeholder: 'City' },
  { key: 'state', label: 'State', placeholder: 'State' },
  { key: 'pincode', label: 'PIN code', placeholder: '6 digits', inputMode: 'numeric' },
]

export function CheckoutPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const cart = useQuery({ queryKey: ['cart'], queryFn: api.cart })
  const [address, setAddress] = useState<Address>({ fullName: user?.name ?? '', phone: '', line1: '', city: '', state: '', pincode: '' })
  const [payment, setPayment] = useState<PaymentMethod>('UPI_DEMO')
  const place = useMutation({
    mutationFn: () => api.checkout(address, payment),
    onSuccess: order => {
      qc.invalidateQueries({ queryKey: ['cart'] })
      qc.invalidateQueries({ queryKey: ['products'] })
      navigate(`/orders/${order.id}?placed=1`)
    },
  })
  const fieldErrors = place.error instanceof ApiError ? place.error.fields : {}

  if (cart.isLoading) return <Spinner />
  if (!cart.data || cart.data.items.length === 0) {
    return <Empty icon="🛒" title="Nothing to check out"><Link to="/shop" className="font-semibold text-brand-600">Browse products</Link></Empty>
  }
  const c = cart.data
  const submit = (e: FormEvent) => { e.preventDefault(); place.mutate() }

  return (
    <form onSubmit={submit} className="grid gap-6 lg:grid-cols-[1fr_340px]" noValidate>
      <section className="space-y-6">
        <h1 className="text-2xl font-bold">Checkout</h1>
        <div className="card p-5">
          <h2 className="mb-4 font-semibold">Delivery address</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            {FIELDS.map(f => (
              <div key={f.key} className={f.full ? 'sm:col-span-2' : ''}>
                <label className="label" htmlFor={f.key}>{f.label}</label>
                <input id={f.key} className={`input ${fieldErrors[`address.${f.key}`] ? 'border-rose-400' : ''}`} placeholder={f.placeholder}
                  inputMode={f.inputMode} value={address[f.key]} required
                  onChange={e => setAddress(a => ({ ...a, [f.key]: e.target.value }))} />
                {fieldErrors[`address.${f.key}`] && <p className="mt-1 text-xs text-rose-600">{f.label} {fieldErrors[`address.${f.key}`]}</p>}
              </div>
            ))}
          </div>
        </div>
        <div className="card p-5">
          <h2 className="mb-1 font-semibold">Payment</h2>
          <p className="mb-4 text-sm text-slate-500">This is a demo store: no real payment is taken.</p>
          <div className="grid gap-3 sm:grid-cols-2">
            {([['UPI_DEMO', 'UPI (demo)', 'Simulated instant payment'], ['COD', 'Cash on delivery', 'Pay when it arrives']] as const).map(([value, title, hint]) => (
              <label key={value} className={`flex cursor-pointer items-start gap-3 rounded-xl border p-4 ${payment === value ? 'border-brand-500 bg-brand-50 ring-4 ring-brand-100' : 'border-slate-200'}`}>
                <input type="radio" name="payment" className="mt-1 accent-brand-600" checked={payment === value} onChange={() => setPayment(value)} />
                <span><span className="block font-semibold">{title}</span><span className="text-sm text-slate-500">{hint}</span></span>
              </label>
            ))}
          </div>
        </div>
      </section>
      <aside className="card h-fit space-y-3 p-5 lg:sticky lg:top-24">
        <h2 className="font-semibold">{items(c.itemCount)}</h2>
        <ul className="space-y-2 text-sm">
          {c.items.map(i => (
            <li key={i.productId} className="flex justify-between gap-3"><span className="truncate text-slate-600">{i.quantity} × {i.product.name}</span><span>{money(i.lineTotal)}</span></li>
          ))}
        </ul>
        <div className="flex justify-between border-t border-slate-100 pt-3 text-sm"><span className="text-slate-500">Delivery</span><span>{c.shipping === 0 ? 'FREE' : money(c.shipping)}</span></div>
        <div className="flex justify-between text-lg font-bold"><span>Total</span><span>{money(c.total)}</span></div>
        <ErrorNote message={place.isError ? (place.error as Error).message : null} />
        <button className="btn-primary w-full" disabled={place.isPending}>{place.isPending ? 'Placing order…' : `Place order · ${money(c.total)}`}</button>
      </aside>
    </form>
  )
}
