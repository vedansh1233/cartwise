import { Link } from 'react-router-dom'
import type { Product } from '../api'
import { Price, ProductArt, Stars } from './ui'

export function ProductCard({ product }: { product: Product }) {
  return (
    <Link to={`/product/${product.slug}`}
      className="group card flex flex-col overflow-hidden transition hover:-translate-y-0.5 hover:shadow-lg focus-visible:ring-4 focus-visible:ring-brand-100">
      <div className="relative aspect-square overflow-hidden">
        <ProductArt product={product} />
        {product.discountPercent >= 30 && (
          <span className="absolute left-3 top-3 rounded-full bg-rose-500 px-2.5 py-1 text-xs font-bold text-white shadow">-{product.discountPercent}%</span>
        )}
        {!product.inStock && (
          <span className="absolute inset-x-0 bottom-0 bg-slate-900/75 py-1.5 text-center text-xs font-semibold uppercase tracking-wide text-white">Out of stock</span>
        )}
      </div>
      <div className="flex flex-1 flex-col gap-1.5 p-4">
        <p className="text-xs font-medium uppercase tracking-wide text-slate-400">{product.category.name}</p>
        <h3 className="line-clamp-2 font-semibold leading-snug text-slate-900">{product.name}</h3>
        <div className="mt-auto flex items-end justify-between gap-2 pt-1">
          <Price price={product.price} mrp={product.mrp} discount={product.discountPercent} />
          <Stars rating={product.rating} />
        </div>
        {product.inStock && product.stock <= 5 && <p className="text-xs font-medium text-amber-600">Only {product.stock} left</p>}
      </div>
    </Link>
  )
}
