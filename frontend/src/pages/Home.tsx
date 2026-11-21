import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api'
import { ProductCard } from '../components/ProductCard'
import { Spinner } from '../components/ui'

export function Home() {
  const categories = useQuery({ queryKey: ['categories'], queryFn: api.categories })
  const deals = useQuery({ queryKey: ['products', 'home-deals'], queryFn: () => api.products({ sort: 'rating', size: 8, inStock: true }) })

  return (
    <div className="space-y-12">
      <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-brand-700 via-brand-600 to-fuchsia-500 px-6 py-12 text-white shadow-xl sm:px-12 sm:py-16">
        <div className="relative z-10 max-w-xl">
          <p className="mb-3 inline-flex rounded-full bg-white/15 px-3 py-1 text-xs font-semibold uppercase tracking-wider">Free delivery over ₹999</p>
          <h1 className="text-4xl font-extrabold leading-tight sm:text-5xl">Everything you need, delivered fast.</h1>
          <p className="mt-4 text-lg text-white/85">Electronics, fashion, books and more, at prices that make sense.</p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link to="/shop" className="btn bg-white text-brand-700 hover:bg-brand-50">Start shopping</Link>
            <Link to="/shop?sort=price_asc" className="btn border border-white/40 text-white hover:bg-white/10">Browse deals</Link>
          </div>
        </div>
        <div className="pointer-events-none absolute -right-6 top-1/2 hidden -translate-y-1/2 select-none text-[11rem] leading-none opacity-90 md:block" aria-hidden>🛍️</div>
      </section>

      <section>
        <h2 className="mb-4 text-xl font-bold">Shop by category</h2>
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-6">
          {categories.data?.map(c => (
            <Link key={c.id} to={`/shop?category=${c.slug}`} className="card flex flex-col items-center gap-2 px-2 py-5 text-center transition hover:-translate-y-0.5 hover:border-brand-200 hover:shadow-md">
              <span className="text-4xl" aria-hidden>{c.icon}</span>
              <span className="text-sm font-semibold">{c.name}</span>
            </Link>
          ))}
        </div>
      </section>

      <section>
        <div className="mb-4 flex items-end justify-between">
          <h2 className="text-xl font-bold">Top rated right now</h2>
          <Link to="/shop?sort=rating" className="text-sm font-semibold text-brand-600 hover:underline">See all →</Link>
        </div>
        {deals.isLoading ? <Spinner /> : (
          <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
            {deals.data?.items.map(p => <ProductCard key={p.id} product={p} />)}
          </div>
        )}
      </section>
    </div>
  )
}
