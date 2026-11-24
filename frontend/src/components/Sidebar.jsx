import { NavLink } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { useAuth } from '../context/AuthContext'
import { selectCartCount } from '../store/slices/cartSlice'
import { selectAllListings } from '../store/slices/marketplaceSlice'
import Brand from './Brand'

export default function Sidebar() {
  const { currentUser, logout } = useAuth()
  const cartCount = useSelector(selectCartCount)
  const allListings = useSelector(selectAllListings)

  if (!currentUser) return null

  const myListings = allListings.filter((l) => l.ownerId === currentUser.id)
  const myRequests = myListings.flatMap((l) => l.buyerRequests || [])

  const initials = (currentUser.name || 'U')
    .split(' ')
    .map((part) => part.charAt(0))
    .join('')
    .slice(0, 2)
    .toUpperCase()

  const navLinkItem = (to, symbol, label, count) => (
    <NavLink
      to={to}
      className={({ isActive }) => (isActive ? 'active' : '')}
      end={to === '/marketplace'}
    >
      <span className="nav-symbol">{symbol}</span>
      <span className="nav-text">{label}</span>
      {count !== undefined && count > 0 ? (
        <span className="nav-count">{count}</span>
      ) : null}
    </NavLink>
  )

  return (
    <aside className="sidebar">
      <Brand />
      <nav className="side-nav" aria-label="Main navigation">
        <p className="nav-label">Marketplace</p>
        {navLinkItem('/marketplace', '⌂', 'Browse Items')}
        {navLinkItem('/cart', '🛒', 'Shopping Cart', cartCount)}

        {currentUser.mode === 'seller' ? (
          <>
            <p className="nav-label seller-label">Seller Studio</p>
            {navLinkItem('/sell', '＋', 'Add New Item')}
            {navLinkItem('/my-listings', '□', 'My Listings', myListings.length)}
            {navLinkItem('/requests', '↗', 'Buyer Inquiries', myRequests.length)}
          </>
        ) : null}

        <p className="nav-label seller-label">Account</p>
        {navLinkItem('/profile', '○', 'My Profile')}
        {navLinkItem('/purchases', '▣', 'Purchase History')}

        {currentUser.role === 'admin' ? (
          <>
            <p className="nav-label seller-label">Administration</p>
            {navLinkItem('/admin', '⚙', 'Admin Controls')}
          </>
        ) : null}
      </nav>

      <div className="sidebar-user">
        <div className="avatar">{initials}</div>
        <div className="user-details">
          <strong>{currentUser.name}</strong>
          <span>{currentUser.mode} mode</span>
        </div>
        <button
          type="button"
          className="logout-icon"
          aria-label="Logout"
          title="Logout"
          onClick={logout}
        >
          ↪
        </button>
      </div>
    </aside>
  )
}
