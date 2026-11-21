import { useState } from 'react'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { api } from '../api'
import { ProductCard } from '../components/ProductCard'
import { Empty, ErrorNote, Spinner } from '../components/ui'

const PRICE_RANGES = [
  { label: 'Any price', min: '', max: '' },
  { label: 'Under ₹500', min: '', max: '500' },
  { label: '₹500 – ₹1,500', min: '500', max: '1500' },
  { label: '₹1,500 – ₹3,000', min: '1500', max: '3000' },
  { label: 'Over ₹3,000', min: '3000', max: '' },
]

export function Shop() {
  const [params, setParams] = useSearchParams()
  const [showFilters, setShowFilters] = useState(false)
  const q = params.get('q') ?? ''
  const category = params.get('category') ?? ''
  const sort = params.get('sort') ?? 'newest'
  const min = params.get('minPrice') ?? ''
  const max = params.get('maxPrice') ?? ''
  const inStock = params.get('inStock') === 'true'
  const page = Number(params.get('page') ?? 0)

  const set = (changes: Record<string, string>) => {
    const next = new URLSearchParams(params)
    Object.entries(changes).forEach(([k, v]) => (v ? next.set(k, v) : next.delete(k)))
    if (!('page' in changes)) next.delete('page')
    setParams(next)
  }

  const categories = useQuery({ queryKey: ['categories'], queryFn: api.categories })
  const products = useQuery({
    queryKey: ['products', q, category, sort, min, max, inStock, page],
    queryFn: () => api.products({ q, category, sort, minPrice: min ? Number(min) : undefined, maxPrice: max ? Number(max) : undefined, inStock, page, size: 12 }),
    placeholderData: keepPreviousData,
  })
  const activeCategory = categories.data?.find(c => c.slug === category)

  return (
    <div className="grid gap-6 lg:grid-cols-[240px_1fr]">
      <button className="btn-ghost lg:hidden" onClick={() => setShowFilters(s => !s)} aria-expanded={showFilters}>
        {showFilters ? 'Hide filters' : 'Filters'}{(category || min || max || inStock) ? ' •' : ''}
      </button>
      <aside className={`space-y-6 ${showFilters ? '' : 'hidden'} lg:block`}>
        <div>
          <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">Category</h3>
          <div className="flex flex-wrap gap-2 lg:flex-col lg:gap-1">
            <button onClick={() => set({ category: '' })} className={`rounded-lg px-3 py-1.5 text-left text-sm ${!category ? 'bg-brand-50 font-semibold text-brand-700' : 'text-slate-600 hover:bg-slate-100'}`}>All products</button>
            {categories.data?.map(c => (
              <button key={c.id} onClick={() => set({ category: c.slug })} className={`rounded-lg px-3 py-1.5 text-left text-sm ${category === c.slug ? 'bg-brand-50 font-semibold text-brand-700' : 'text-slate-600 hover:bg-slate-100'}`}>
                <span aria-hidden>{c.icon}</span> {c.name}
              </button>
            ))}
          </div>
        </div>
        <div>
          <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">Price</h3>
          <div className="flex flex-wrap gap-2 lg:flex-col lg:gap-1">
            {PRICE_RANGES.map(r => (
              <button key={r.label} onClick={() => set({ minPrice: r.min, maxPrice: r.max })} className={`rounded-lg px-3 py-1.5 text-left text-sm ${min === r.min && max === r.max ? 'bg-brand-50 font-semibold text-brand-700' : 'text-slate-600 hover:bg-slate-100'}`}>{r.label}</button>
            ))}
          </div>
        </div>
        <label className="flex cursor-pointer items-center gap-2 text-sm text-slate-700">
          <input type="checkbox" checked={inStock} onChange={e => set({ inStock: e.target.checked ? 'true' : '' })} className="h-4 w-4 accent-brand-600" />
          In stock only
        </label>
      </aside>

      <section>
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold">{q ? `Results for “${q}”` : activeCategory?.name ?? 'All products'}</h1>
            {products.data && <p className="text-sm text-slate-500">{products.data.totalItems} products</p>}
          </div>
          <select value={sort} onChange={e => set({ sort: e.target.value })} className="input w-auto" aria-label="Sort by">
            <option value="newest">Newest</option>
            <option value="price_asc">Price: low to high</option>
            <option value="price_desc">Price: high to low</option>
            <option value="rating">Top rated</option>
            <option value="name">Name A–Z</option>
          </select>
        </div>
        {q && <button onClick={() => set({ q: '' })} className="mb-4 rounded-full bg-slate-100 px-3 py-1 text-sm text-slate-600 hover:bg-slate-200">Clear search ✕</button>}

        {products.isLoading ? <Spinner /> : products.isError ? <ErrorNote message={(products.error as Error).message} /> :
          products.data!.items.length === 0 ? (
            <Empty icon="🔍" title="No products match these filters">Try another search or clear a filter.</Empty>
          ) : (
            <>
              <div className={`grid grid-cols-2 gap-4 md:grid-cols-3 xl:grid-cols-4 ${products.isFetching ? 'opacity-60' : ''}`}>
                {products.data!.items.map(p => <ProductCard key={p.id} product={p} />)}
              </div>
              {products.data!.totalPages > 1 && (
                <div className="mt-8 flex items-center justify-center gap-3">
                  <button className="btn-ghost" disabled={page === 0} onClick={() => set({ page: String(page - 1) })}>← Previous</button>
                  <span className="text-sm text-slate-500">Page {page + 1} of {products.data!.totalPages}</span>
                  <button className="btn-ghost" disabled={page + 1 >= products.data!.totalPages} onClick={() => set({ page: String(page + 1) })}>Next →</button>
                </div>
              )}
            </>
          )}
      </section>
    </div>
  )
}
