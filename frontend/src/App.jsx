import { Routes, Route, Navigate } from 'react-router-dom'
import './App.css'
import AppLayout from './layouts/AppLayout'
import ProtectedRoute from './components/routing/ProtectedRoute'
import AdminRoute from './components/routing/AdminRoute'
import GuestPage from './views/GuestPage'
import BuyerMarketplace from './views/BuyerMarketplace'
import ProductDetailView from './views/ProductDetailView'
import CartView from './views/CartView'
import SellItemView from './views/SellItemView'
import MyListings from './views/MyListings'
import BuyerRequests from './views/BuyerRequests'
import ProfileView from './views/ProfileView'
import PurchaseHistory from './views/PurchaseHistory'
import AdminView from './views/AdminView'
import NotFoundView from './views/NotFoundView'

export default function App() {
  return (
    <Routes>
      {/* Public Guest / Auth Route */}
      <Route path="/guest" element={<GuestPage />} />

      {/* Redirect root to marketplace */}
      <Route path="/" element={<Navigate to="/marketplace" replace />} />

      {/* Protected App Routes inside persistent AppLayout */}
      <Route
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/marketplace" element={<BuyerMarketplace />} />
        <Route path="/item/:id" element={<ProductDetailView />} />
        <Route path="/cart" element={<CartView />} />
        <Route path="/sell" element={<SellItemView />} />
        <Route path="/my-listings" element={<MyListings />} />
        <Route path="/requests" element={<BuyerRequests />} />
        <Route path="/profile" element={<ProfileView />} />
        <Route path="/purchases" element={<PurchaseHistory />} />

        {/* Role-Protected Route for Admin */}
        <Route
          path="/admin"
          element={
            <AdminRoute>
              <AdminView />
            </AdminRoute>
          }
        />
      </Route>

      {/* 404 Fallback */}
      <Route path="*" element={<NotFoundView />} />
    </Routes>
  )
}
