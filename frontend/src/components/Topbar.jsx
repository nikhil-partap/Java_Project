import { useLocation, Link } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { useAuth } from '../context/AuthContext'
import { selectCartCount } from '../store/slices/cartSlice'

const ROUTE_TITLES = {
  '/marketplace': ['Campus Marketplace', 'Discover textbooks, cycles, electronics, and study essentials.'],
  '/cart': ['Shopping Cart', 'Review selected items and submit peer purchase requests.'],
  '/sell': ['Post New Item', 'Share details and photos to publish your listing.'],
  '/my-listings': ['My Listings', 'Manage, edit, or mark your active campus items as sold.'],
  '/requests': ['Buyer Inquiries', 'See campus students interested in purchasing your items.'],
  '/profile': ['Your Profile', 'Manage your student details, year, and campus contact.'],
  '/purchases': ['Purchase History', 'Review items bought from fellow students.'],
  '/admin': ['Administration Panel', 'Moderate campus users and manage active listings.'],
}

export default function Topbar() {
  const location = useLocation()
  const { currentUser, toggleMode, backendOnline } = useAuth()
  const cartCount = useSelector(selectCartCount)

  const pathname = location.pathname
  const matchedRoute = Object.keys(ROUTE_TITLES).find(
    (route) => pathname === route || (route !== '/' && pathname.startsWith(route)),
  )

  const [title, subtitle] = matchedRoute
    ? ROUTE_TITLES[matchedRoute]
    : pathname.startsWith('/item/')
    ? ['Item Details', 'Inspect item description, photos, and seller campus location.']
    : ['Campus Marketplace', 'Connecting campus students for peer-to-peer deals.']

  const mode = currentUser?.mode || 'buyer'

  return (
    <header className="topbar">
      <div className="topbar-heading">
        <h1 className="topbar-title">{title}</h1>
        <p className="topbar-subtitle">{subtitle}</p>
      </div>

      <div className="topbar-actions">
        {backendOnline !== null && (
          <small
            title={backendOnline ? 'Connected to the Java backend' : 'Backend unreachable - using saved local data'}
            style={{ color: backendOnline ? '#15803d' : '#b45309', fontSize: '0.75rem', whiteSpace: 'nowrap' }}
          >
            ● Backend: {backendOnline ? 'online' : 'offline'}
          </small>
        )}
        <Link to="/cart" className="topbar-cart-link" aria-label="Shopping Cart">
          <span className="cart-icon" aria-hidden="true">🛒</span>
          <span className="cart-label">Cart</span>
          {cartCount > 0 && <span className="topbar-cart-badge">{cartCount}</span>}
        </Link>

        <button
          type="button"
          className="mode-switch"
          onClick={toggleMode}
          title="Switch between Buyer and Seller modes"
        >
          <span className={`mode-indicator ${mode}`} />
          <span className="mode-text">
            {mode === 'seller' ? 'Seller Mode' : 'Buyer Mode'}
          </span>
          <small className="mode-hint">Switch</small>
        </button>
      </div>
    </header>
  )
}
