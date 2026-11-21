import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError, money, type Product, type ProductInput } from '../../api'
import { ErrorNote, ProductArt, Spinner } from '../../components/ui'

const blank: ProductInput = { name: '', description: '', price: 0, mrp: null, stock: 0, categoryId: 1, imageUrl: null, emoji: '🛍️', accent: '#6366f1', active: true }

export function AdminProducts() {
  const [q, setQ] = useState('')
  const [editing, setEditing] = useState<{ id: number | null; form: ProductInput } | null>(null)
  const products = useQuery({ queryKey: ['admin', 'products', q], queryFn: () => api.adminProducts(q) })

  const edit = (p: Product) => setEditing({ id: p.id, form: { name: p.name, description: p.description, price: p.price, mrp: p.mrp, stock: p.stock, categoryId: p.category.id, imageUrl: p.imageUrl, emoji: p.emoji, accent: p.accent, active: p.active } })

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <input className="input max-w-xs" placeholder="Search products" value={q} onChange={e => setQ(e.target.value)} aria-label="Search products" />
        <button className="btn-primary ml-auto" onClick={() => setEditing({ id: null, form: blank })}>+ New product</button>
      </div>
      {products.isLoading ? <Spinner /> : (
        <div className="card overflow-x-auto">
          <table className="w-full min-w-[640px] text-sm">
            <thead className="bg-slate-50 text-left text-slate-500">
              <tr><th className="px-4 py-3 font-medium">Product</th><th className="px-4 py-3 font-medium">Category</th><th className="px-4 py-3 text-right font-medium">Price</th><th className="px-4 py-3 text-right font-medium">Stock</th><th className="px-4 py-3 font-medium">Status</th><th /></tr>
            </thead>
            <tbody>
              {products.data?.items.map(p => (
                <tr key={p.id} className="border-t border-slate-100">
                  <td className="px-4 py-2.5"><div className="flex items-center gap-3"><div className="h-10 w-10 overflow-hidden rounded-lg"><ProductArt product={p} size="xs" /></div><span className="font-medium">{p.name}</span></div></td>
                  <td className="px-4 py-2.5 text-slate-600">{p.category.name}</td>
                  <td className="px-4 py-2.5 text-right">{money(p.price)}</td>
                  <td className={`px-4 py-2.5 text-right font-semibold ${p.stock === 0 ? 'text-rose-600' : p.stock < 5 ? 'text-amber-600' : ''}`}>{p.stock}</td>
                  <td className="px-4 py-2.5">{p.active ? <span className="text-emerald-600">Live</span> : <span className="text-slate-400">Hidden</span>}</td>
                  <td className="px-4 py-2.5 text-right"><button className="font-semibold text-brand-600 hover:underline" onClick={() => edit(p)}>Edit</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {editing && <ProductDialog initial={editing} onClose={() => setEditing(null)} />}
    </div>
  )
}

function ProductDialog({ initial, onClose }: { initial: { id: number | null; form: ProductInput }; onClose: () => void }) {
  const qc = useQueryClient()
  const [form, setForm] = useState<ProductInput>(initial.form)
  const categories = useQuery({ queryKey: ['categories'], queryFn: api.categories })
  const save = useMutation({
    mutationFn: () => initial.id ? api.adminUpdateProduct(initial.id, form) : api.adminCreateProduct(form),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin'] }); qc.invalidateQueries({ queryKey: ['products'] }); onClose() },
  })
  const fe = save.error instanceof ApiError ? save.error.fields : {}
  const set = <K extends keyof ProductInput>(k: K, v: ProductInput[K]) => setForm(f => ({ ...f, [k]: v }))
  const submit = (e: FormEvent) => { e.preventDefault(); save.mutate() }
  const err = (k: string) => fe[k] && <p className="mt-1 text-xs text-rose-600">{fe[k]}</p>

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-slate-900/50 p-0 sm:items-center sm:p-4" role="dialog" aria-modal="true" aria-label={initial.id ? 'Edit product' : 'New product'} onClick={onClose}>
      <form onSubmit={submit} onClick={e => e.stopPropagation()} className="max-h-[92vh] w-full max-w-2xl space-y-4 overflow-y-auto rounded-t-3xl bg-white p-6 shadow-2xl sm:rounded-3xl" noValidate>
        <div className="flex items-center justify-between"><h2 className="text-lg font-bold">{initial.id ? 'Edit product' : 'New product'}</h2><button type="button" onClick={onClose} className="text-slate-400 hover:text-slate-700" aria-label="Close">✕</button></div>
        <div className="grid gap-4 sm:grid-cols-[1fr_120px]">
          <div className="space-y-4">
            <div><label className="label" htmlFor="pname">Name</label><input id="pname" className="input" value={form.name} onChange={e => set('name', e.target.value)} />{err('name')}</div>
            <div><label className="label" htmlFor="pdesc">Description</label><textarea id="pdesc" rows={3} className="input" value={form.description} onChange={e => set('description', e.target.value)} />{err('description')}</div>
          </div>
          <div className="group aspect-square overflow-hidden rounded-2xl border border-slate-200"><ProductArt product={{ name: form.name, imageUrl: form.imageUrl, emoji: form.emoji, accent: form.accent }} size="sm" /></div>
        </div>
        <div className="grid gap-4 sm:grid-cols-3">
          <div><label className="label" htmlFor="pprice">Price (₹)</label><input id="pprice" type="number" min="1" className="input" value={form.price || ''} onChange={e => set('price', Number(e.target.value))} />{err('price')}</div>
          <div><label className="label" htmlFor="pmrp">MRP (₹, optional)</label><input id="pmrp" type="number" min="1" className="input" value={form.mrp ?? ''} onChange={e => set('mrp', e.target.value ? Number(e.target.value) : null)} />{err('mrp')}</div>
          <div><label className="label" htmlFor="pstock">Stock</label><input id="pstock" type="number" min="0" className="input" value={form.stock} onChange={e => set('stock', Number(e.target.value))} />{err('stock')}</div>
          <div><label className="label" htmlFor="pcat">Category</label><select id="pcat" className="input" value={form.categoryId} onChange={e => set('categoryId', Number(e.target.value))}>{categories.data?.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></div>
          <div><label className="label" htmlFor="pemoji">Emoji</label><input id="pemoji" className="input" value={form.emoji ?? ''} onChange={e => set('emoji', e.target.value)} /></div>
          <div><label className="label" htmlFor="paccent">Accent colour</label><input id="paccent" className="input" value={form.accent ?? ''} placeholder="#6366f1" onChange={e => set('accent', e.target.value)} />{err('accent')}</div>
        </div>
        <div><label className="label" htmlFor="pimg">Image URL (optional)</label><input id="pimg" className="input" value={form.imageUrl ?? ''} placeholder="https://…" onChange={e => set('imageUrl', e.target.value || null)} />{err('imageUrl')}</div>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" className="h-4 w-4 accent-brand-600" checked={form.active} onChange={e => set('active', e.target.checked)} /> Visible in the store</label>
        <ErrorNote message={save.isError && Object.keys(fe).length === 0 ? (save.error as Error).message : null} />
        <div className="flex justify-end gap-2"><button type="button" className="btn-ghost" onClick={onClose}>Cancel</button><button className="btn-primary" disabled={save.isPending}>{save.isPending ? 'Saving…' : 'Save product'}</button></div>
      </form>
    </div>
  )
}
