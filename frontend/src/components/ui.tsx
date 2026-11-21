import type { ReactNode } from 'react'
import { money, type OrderStatus, type Product } from '../api'

/** Product artwork: the photo if there is one, otherwise a soft gradient tile with the product emoji. */
export function ProductArt({ product, size = 'md' }: { product: Pick<Product, 'name' | 'imageUrl' | 'emoji' | 'accent'>; size?: 'xs' | 'sm' | 'md' | 'lg' }) {
  const accent = product.accent ?? '#6366f1'
  const text = { xs: 'text-xl', sm: 'text-5xl', md: 'text-6xl', lg: 'text-[9rem]' }[size]
  if (product.imageUrl) {
    return <img src={product.imageUrl} alt={product.name} className="h-full w-full object-cover" loading="lazy" />
  }
  return (
    <div className="flex h-full w-full items-center justify-center"
      style={{ background: `radial-gradient(circle at 30% 25%, ${accent}33, ${accent}12 55%, #f8fafc 100%)` }}>
      <span className={`${text} drop-shadow-sm transition-transform duration-300 group-hover:scale-110`} aria-hidden>{product.emoji ?? '🛍️'}</span>
    </div>
  )
}

export function Price({ price, mrp, discount, large }: { price: number; mrp: number | null; discount: number; large?: boolean }) {
  return (
    <div className="flex flex-wrap items-baseline gap-x-2">
      <span className={large ? 'text-3xl font-bold' : 'text-lg font-bold'}>{money(price)}</span>
      {mrp && discount > 0 && (
        <>
          <span className="text-sm text-slate-400 line-through">{money(mrp)}</span>
          <span className="text-sm font-semibold text-emerald-600">{discount}% off</span>
        </>
      )}
    </div>
  )
}

export function Stars({ rating }: { rating: number }) {
  return (
    <span className="inline-flex items-center gap-1 rounded-md bg-emerald-600 px-1.5 py-0.5 text-xs font-semibold text-white">
      {rating.toFixed(1)} <span aria-hidden>★</span>
    </span>
  )
}

const statusStyle: Record<OrderStatus, string> = {
  PLACED: 'bg-sky-50 text-sky-700 ring-sky-200',
  PACKED: 'bg-amber-50 text-amber-700 ring-amber-200',
  SHIPPED: 'bg-indigo-50 text-indigo-700 ring-indigo-200',
  DELIVERED: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  CANCELLED: 'bg-rose-50 text-rose-700 ring-rose-200',
}

export function StatusBadge({ status }: { status: OrderStatus }) {
  return <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset ${statusStyle[status]}`}>{status.charAt(0) + status.slice(1).toLowerCase()}</span>
}

export function Empty({ icon, title, children }: { icon: string; title: string; children?: ReactNode }) {
  return (
    <div className="card flex flex-col items-center px-6 py-16 text-center">
      <div className="mb-3 text-5xl" aria-hidden>{icon}</div>
      <h2 className="text-lg font-semibold">{title}</h2>
      <div className="mt-2 text-sm text-slate-500">{children}</div>
    </div>
  )
}

export function Spinner() {
  return <div className="flex justify-center py-20"><div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-100 border-t-brand-600" /></div>
}

export function ErrorNote({ message }: { message?: string | null }) {
  if (!message) return null
  return <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{message}</div>
}
