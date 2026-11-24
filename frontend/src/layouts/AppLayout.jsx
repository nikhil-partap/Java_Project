import { Outlet } from 'react-router-dom'
import Sidebar from '../components/Sidebar'
import Topbar from '../components/Topbar'
import { useAuth } from '../context/AuthContext'

export default function AppLayout() {
  const { toast } = useAuth()

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="workspace">
        <Topbar />
        {toast?.message && (
          <div className={`flash-message flash-${toast.type || 'info'}`} role="status">
            {toast.message}
          </div>
        )}
        <div className="workspace-content">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
