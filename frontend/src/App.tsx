import { Link, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { RequireAuth } from './components/guards'
import { Empty } from './components/ui'
import { Home } from './pages/Home'
import { Shop } from './pages/Shop'
import { ProductPage } from './pages/ProductPage'
import { CartPage } from './pages/CartPage'
import { CheckoutPage } from './pages/CheckoutPage'
import { OrderPage, OrdersPage } from './pages/OrdersPage'
import { LoginPage, RegisterPage } from './pages/AuthPages'
import { AdminLayout } from './pages/admin/AdminLayout'
import { AdminDashboard } from './pages/admin/AdminDashboard'
import { AdminProducts } from './pages/admin/AdminProducts'
import { AdminOrders } from './pages/admin/AdminOrders'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Home />} />
        <Route path="shop" element={<Shop />} />
        <Route path="product/:slug" element={<ProductPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="cart" element={<RequireAuth><CartPage /></RequireAuth>} />
        <Route path="checkout" element={<RequireAuth><CheckoutPage /></RequireAuth>} />
        <Route path="orders" element={<RequireAuth><OrdersPage /></RequireAuth>} />
        <Route path="orders/:id" element={<RequireAuth><OrderPage /></RequireAuth>} />
        <Route path="admin" element={<RequireAuth admin><AdminLayout /></RequireAuth>}>
          <Route index element={<AdminDashboard />} />
          <Route path="products" element={<AdminProducts />} />
          <Route path="orders" element={<AdminOrders />} />
        </Route>
        <Route path="*" element={<Empty icon="🧭" title="Page not found"><Link to="/" className="font-semibold text-brand-600">Go home</Link></Empty>} />
      </Route>
    </Routes>
  )
}
