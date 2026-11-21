// Typed client for the Cartwise REST API.

export type Role = 'CUSTOMER' | 'ADMIN'
export type User = { id: number; name: string; email: string; role: Role }
export type Category = { id: number; name: string; slug: string; icon: string | null }
export type Product = {
  id: number; name: string; slug: string; description: string; price: number; mrp: number | null
  discountPercent: number; stock: number; inStock: boolean; category: Category; imageUrl: string | null
  emoji: string | null; accent: string | null; rating: number; active: boolean
}
export type Page<T> = { items: T[]; page: number; size: number; totalItems: number; totalPages: number }
export type CartLine = { productId: number; product: Product; quantity: number; lineTotal: number; available: boolean }
export type Cart = { items: CartLine[]; itemCount: number; subtotal: number; shipping: number; total: number; freeShippingThreshold: number }
export type OrderStatus = 'PLACED' | 'PACKED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'
export type PaymentMethod = 'COD' | 'UPI_DEMO'
export type Address = { fullName: string; phone: string; line1: string; city: string; state: string; pincode: string }
export type OrderLine = { productId: number; productName: string; emoji: string | null; unitPrice: number; quantity: number; lineTotal: number }
export type Order = {
  id: number; status: OrderStatus; nextStatuses: OrderStatus[]; paymentMethod: PaymentMethod; address: Address
  items: OrderLine[]; itemCount: number; subtotal: number; shipping: number; total: number
  createdAt: string; updatedAt: string; customerEmail: string | null
}
export type OrderSummary = { id: number; status: OrderStatus; itemCount: number; total: number; createdAt: string; customerEmail: string | null; customerName: string | null }
export type Stats = {
  revenue: number; orders: number; customers: number; products: number; lowStockProducts: number
  ordersByStatus: Record<OrderStatus, number>; topProducts: { name: string; unitsSold: number; revenue: number }[]
}
export type ProductInput = {
  name: string; description: string; price: number; mrp: number | null; stock: number; categoryId: number
  imageUrl: string | null; emoji: string | null; accent: string | null; active: boolean
}

export class ApiError extends Error {
  status: number
  fields: Record<string, string>
  constructor(status: number, message: string, fields: Record<string, string> = {}) {
    super(message)
    this.status = status
    this.fields = fields
  }
}

const TOKEN_KEY = 'cartwise.token'

export const tokenStore = {
  get(): string | null {
    try { return localStorage.getItem(TOKEN_KEY) } catch { return null }
  },
  set(token: string | null) {
    try {
      if (token) localStorage.setItem(TOKEN_KEY, token)
      else localStorage.removeItem(TOKEN_KEY)
    } catch { /* storage unavailable: stay logged in for this tab only */ }
  },
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const token = tokenStore.get()
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(`/api${path}`, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) })
  if (res.status === 401 && token) {
    tokenStore.set(null)
    window.dispatchEvent(new Event('cartwise:logout'))
  }
  if (!res.ok) {
    let message = res.status === 401 ? 'Please log in to continue' : 'Something went wrong'
    let fields: Record<string, string> = {}
    try {
      const err = await res.json()
      message = err.message ?? message
      fields = err.fields ?? {}
    } catch { /* empty body */ }
    throw new ApiError(res.status, message, fields)
  }
  return res.status === 204 ? (undefined as T) : res.json()
}

const qs = (params: Record<string, string | number | boolean | undefined | null>) => {
  const s = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') s.set(k, String(v)) })
  const str = s.toString()
  return str ? `?${str}` : ''
}

export const api = {
  login: (email: string, password: string) => request<{ token: string; user: User }>('POST', '/auth/login', { email, password }),
  register: (name: string, email: string, password: string) => request<{ token: string; user: User }>('POST', '/auth/register', { name, email, password }),
  me: () => request<User>('GET', '/auth/me'),

  categories: () => request<Category[]>('GET', '/categories'),
  products: (p: { q?: string; category?: string; minPrice?: number; maxPrice?: number; inStock?: boolean; sort?: string; page?: number; size?: number }) =>
    request<Page<Product>>('GET', `/products${qs(p)}`),
  product: (slug: string) => request<Product>('GET', `/products/${encodeURIComponent(slug)}`),

  cart: () => request<Cart>('GET', '/cart'),
  addToCart: (productId: number, quantity = 1) => request<Cart>('POST', '/cart/items', { productId, quantity }),
  updateCart: (productId: number, quantity: number) => request<Cart>('PUT', `/cart/items/${productId}`, { quantity }),
  removeFromCart: (productId: number) => request<Cart>('DELETE', `/cart/items/${productId}`),

  checkout: (address: Address, paymentMethod: PaymentMethod) => request<Order>('POST', '/orders', { address, paymentMethod }),
  myOrders: (page = 0) => request<Page<OrderSummary>>('GET', `/orders${qs({ page })}`),
  myOrder: (id: number) => request<Order>('GET', `/orders/${id}`),
  cancelOrder: (id: number) => request<Order>('POST', `/orders/${id}/cancel`),

  adminStats: () => request<Stats>('GET', '/admin/stats'),
  adminProducts: (q?: string, page = 0) => request<Page<Product>>('GET', `/admin/products${qs({ q, page, size: 50 })}`),
  adminCreateProduct: (p: ProductInput) => request<Product>('POST', '/admin/products', p),
  adminUpdateProduct: (id: number, p: ProductInput) => request<Product>('PUT', `/admin/products/${id}`, p),
  adminOrders: (status?: OrderStatus | '', page = 0) => request<Page<OrderSummary>>('GET', `/admin/orders${qs({ status, page, size: 50 })}`),
  adminOrder: (id: number) => request<Order>('GET', `/admin/orders/${id}`),
  adminSetStatus: (id: number, status: OrderStatus) => request<Order>('PATCH', `/admin/orders/${id}/status`, { status }),
}

const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 })
export const money = (n: number) => inr.format(n)
export const items = (n: number) => `${n} item${n === 1 ? '' : 's'}`
export const date = (iso: string) => new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
