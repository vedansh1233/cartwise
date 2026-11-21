import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, items, money, type Cart } from '../api'
import { Empty, ErrorNote, ProductArt, Spinner } from '../components/ui'

export function CartPage() {
  const qc = useQueryClient()
  const cart = useQuery({ queryKey: ['cart'], queryFn: api.cart })
  const onCart = (c: Cart) => qc.setQueryData(['cart'], c)
  const update = useMutation({ mutationFn: ({ id, qty }: { id: number; qty: number }) => api.updateCart(id, qty), onSuccess: onCart })
  const remove = useMutation({ mutationFn: (id: number) => api.removeFromCart(id), onSuccess: onCart })

  if (cart.isLoading) return <Spinner />
  if (cart.isError) return <ErrorNote message={(cart.error as Error).message} />
  const c = cart.data!
  if (c.items.length === 0) {
    return <Empty icon="🛒" title="Your cart is empty"><Link to="/shop" className="btn-primary mt-4">Start shopping</Link></Empty>
  }
  const toFree = c.freeShippingThreshold - c.subtotal
  const blocked = c.items.some(i => !i.available)

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
      <section className="space-y-3">
        <h1 className="text-2xl font-bold">Your cart <span className="text-base font-normal text-slate-500">({items(c.itemCount)})</span></h1>
        <ErrorNote message={update.isError ? (update.error as Error).message : remove.isError ? (remove.error as Error).message : null} />
        {c.items.map(line => (
          <div key={line.productId} className="card flex gap-4 p-4">
            <Link to={`/product/${line.product.slug}`} className="group h-24 w-24 shrink-0 overflow-hidden rounded-xl"><ProductArt product={line.product} size="sm" /></Link>
            <div className="flex min-w-0 flex-1 flex-col">
              <div className="flex items-start justify-between gap-3">
                <Link to={`/product/${line.product.slug}`} className="font-semibold hover:text-brand-700">{line.product.name}</Link>
                <span className="shrink-0 font-bold">{money(line.lineTotal)}</span>
              </div>
              <span className="text-sm text-slate-500">{money(line.product.price)} each</span>
              {!line.available && <span className="text-sm font-medium text-rose-600">Only {line.product.stock} available, please reduce the quantity</span>}
              <div className="mt-auto flex items-center justify-between pt-2">
                <div className="flex items-center rounded-lg border border-slate-200 text-sm">
                  <button className="px-2.5 py-1 disabled:opacity-30" disabled={line.quantity <= 1 || update.isPending} onClick={() => update.mutate({ id: line.productId, qty: line.quantity - 1 })} aria-label="Decrease">−</button>
                  <span className="w-7 text-center font-semibold">{line.quantity}</span>
                  <button className="px-2.5 py-1 disabled:opacity-30" disabled={update.isPending} onClick={() => update.mutate({ id: line.productId, qty: line.quantity + 1 })} aria-label="Increase">+</button>
                </div>
                <button onClick={() => remove.mutate(line.productId)} className="text-sm text-slate-500 hover:text-rose-600">Remove</button>
              </div>
            </div>
          </div>
        ))}
      </section>
      <aside className="card h-fit space-y-3 p-5 lg:sticky lg:top-24">
        <h2 className="font-semibold">Order summary</h2>
        <div className="flex justify-between text-sm"><span className="text-slate-500">Subtotal</span><span>{money(c.subtotal)}</span></div>
        <div className="flex justify-between text-sm"><span className="text-slate-500">Delivery</span><span>{c.shipping === 0 ? <span className="font-semibold text-emerald-600">FREE</span> : money(c.shipping)}</span></div>
        {toFree > 0 && <p className="rounded-lg bg-brand-50 px-3 py-2 text-xs text-brand-700">Add {money(toFree)} more for free delivery</p>}
        <div className="flex justify-between border-t border-slate-100 pt-3 text-lg font-bold"><span>Total</span><span>{money(c.total)}</span></div>
        <Link to="/checkout" className={`btn-primary w-full ${blocked ? 'pointer-events-none opacity-50' : ''}`} aria-disabled={blocked}>Proceed to checkout</Link>
      </aside>
    </div>
  )
}
