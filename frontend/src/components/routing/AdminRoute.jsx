import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

/**
 * Route Guard for Admin role
 */
export default function AdminRoute({ children }) {
  const { isAuthenticated, isAdmin } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/guest" state={{ from: location }} replace />
  }

  if (!isAdmin) {
    return <Navigate to="/marketplace" replace />
  }

  return children
}
