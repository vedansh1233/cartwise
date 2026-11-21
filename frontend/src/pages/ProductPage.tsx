import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError } from '../api'
import { useAuth } from '../auth'
import { Empty, ErrorNote, Price, ProductArt, Spinner, Stars } from '../components/ui'

export function ProductPage() {
  const { slug = '' } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [qty, setQty] = useState(1)
  const [added, setAdded] = useState(false)
  const product = useQuery({ queryKey: ['product', slug], queryFn: () => api.product(slug) })

  const add = useMutation({
    mutationFn: () => api.addToCart(product.data!.id, qty),
    onSuccess: cart => { qc.setQueryData(['cart'], cart); setAdded(true) },
  })

  if (product.isLoading) return <Spinner />
  if (product.isError) {
    return (product.error as ApiError).status === 404
      ? <Empty icon="🤷" title="Product not found"><Link to="/shop" className="font-semibold text-brand-600">Back to the shop</Link></Empty>
      : <ErrorNote message={(product.error as Error).message} />
  }
  const p = product.data!
  const maxQty = Math.min(10, p.stock)

  const onAdd = () => {
    if (!user) { navigate(`/login?next=/product/${p.slug}`); return }
    setAdded(false)
    add.mutate()
  }

  return (
    <div>
      <nav className="mb-4 text-sm text-slate-500">
        <Link to="/shop" className="hover:text-slate-800">Shop</Link> <span aria-hidden>/</span>{' '}
        <Link to={`/shop?category=${p.category.slug}`} className="hover:text-slate-800">{p.category.name}</Link>
      </nav>
      <div className="grid gap-8 lg:grid-cols-2">
        <div className="group card aspect-square overflow-hidden"><ProductArt product={p} size="lg" /></div>
        <div className="flex flex-col gap-5">
          <div>
            <p className="text-sm font-medium uppercase tracking-wide text-slate-400">{p.category.name}</p>
            <h1 className="mt-1 text-3xl font-bold leading-tight">{p.name}</h1>
            <div className="mt-2"><Stars rating={p.rating} /></div>
          </div>
          <Price price={p.price} mrp={p.mrp} discount={p.discountPercent} large />
          <p className="leading-relaxed text-slate-600">{p.description}</p>

          {p.inStock ? (
            <div className="card space-y-4 p-5">
              <p className={`text-sm font-semibold ${p.stock <= 5 ? 'text-amber-600' : 'text-emerald-600'}`}>
                {p.stock <= 5 ? `Hurry, only ${p.stock} left in stock` : 'In stock, ready to ship'}
              </p>
              <div className="flex flex-wrap items-center gap-3">
                <div className="flex items-center rounded-xl border border-slate-200">
                  <button className="px-3 py-2 text-lg disabled:opacity-30" disabled={qty <= 1} onClick={() => setQty(q => q - 1)} aria-label="Decrease quantity">−</button>
                  <span className="w-8 text-center font-semibold" aria-live="polite">{qty}</span>
                  <button className="px-3 py-2 text-lg disabled:opacity-30" disabled={qty >= maxQty} onClick={() => setQty(q => q + 1)} aria-label="Increase quantity">+</button>
                </div>
                <button className="btn-primary flex-1" onClick={onAdd} disabled={add.isPending}>{add.isPending ? 'Adding…' : 'Add to cart'}</button>
              </div>
              {added && <p className="text-sm text-emerald-700">Added to your cart. <Link to="/cart" className="font-semibold underline">View cart →</Link></p>}
              <ErrorNote message={add.isError ? (add.error as Error).message : null} />
            </div>
          ) : (
            <div className="card p-5 font-semibold text-rose-600">Currently out of stock</div>
          )}
          <ul className="grid grid-cols-3 gap-3 text-center text-xs text-slate-500">
            <li className="card p-3"><div className="text-xl" aria-hidden>🚚</div>Free delivery over ₹999</li>
            <li className="card p-3"><div className="text-xl" aria-hidden>↩️</div>Cancel before packing</li>
            <li className="card p-3"><div className="text-xl" aria-hidden>🔒</div>Secure checkout</li>
          </ul>
        </div>
      </div>
    </div>
  )
}
