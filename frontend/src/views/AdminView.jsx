import { useDispatch, useSelector } from 'react-redux'
import { useEffect, useState } from 'react'
import { selectAllListings, removeListing } from '../store/slices/marketplaceSlice'
import { getStats } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { formatInr, formatStamp } from '../lib/formatters'

export default function AdminView({
  users: propUsers,
  listings: propListings,
  onDeleteListing: propDelete,
  onToggleBlock: propToggleBlock,
}) {
  const dispatch = useDispatch()
  const { users: authUsers, toggleUserBlock, showToast } = useAuth()
  const allListings = useSelector(selectAllListings)
  const [stats, setStats] = useState(null)

  // Live numbers from the backend (stays hidden when the backend is offline)
  useEffect(() => {
    getStats().then(setStats).catch(() => setStats(null))
  }, [allListings])

  const users = propUsers || authUsers || []
  const listings = propListings || allListings

  const handleDeleteListing = async (id) => {
    if (propDelete) {
      propDelete(id)
    } else {
      const result = await dispatch(removeListing(id))
      if (result.success) showToast('Listing removed by administrator.', 'info')
      else showToast(result.error, 'error')
    }
  }

  const handleToggleBlock = (userId) => {
    if (propToggleBlock) {
      propToggleBlock(userId)
    } else {
      toggleUserBlock(userId)
    }
  }

  return (
    <section className="admin-layout">
      {stats && (
        <div className="summary-row" style={{ gridColumn: '1 / -1', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))' }}>
          <article><span>Listings</span><strong>{stats.totalListings}</strong></article>
          <article><span>Available</span><strong>{stats.available}</strong></article>
          <article><span>Sold</span><strong>{stats.sold}</strong></article>
          <article><span>Users</span><strong>{stats.totalUsers}</strong></article>
          <article><span>Orders</span><strong>{stats.ordersProcessed}</strong></article>
          <article><span>Revenue</span><strong>{formatInr(stats.revenue)}</strong></article>
        </div>
      )}
      <div className="panel admin-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Access control</p>
            <h2>All Campus Users</h2>
          </div>
          <span className="result-count">{users.length} accounts</span>
        </div>
        <div className="admin-list">
          {users.map((user) => (
            <article className="admin-row" key={user.id}>
              <div className="mini-avatar">{(user.name || 'U').charAt(0)}</div>
              <div className="admin-row-copy">
                <strong>{user.name}</strong>
                <span>
                  {user.email} · {user.mode} {user.role === 'admin' ? '· admin' : ''}
                </span>
              </div>
              <button
                type="button"
                className={user.isBlocked ? 'primary-btn' : 'secondary-btn'}
                onClick={() => handleToggleBlock(user.id)}
                disabled={user.role === 'admin'}
              >
                {user.role === 'admin' ? 'Admin' : user.isBlocked ? 'Unblock' : 'Block'}
              </button>
            </article>
          ))}
        </div>
      </div>

      <div className="panel admin-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Moderation</p>
            <h2>All Active Listings</h2>
          </div>
          <span className="result-count">{listings.length} listings</span>
        </div>
        <div className="admin-list">
          {listings.map((listing) => (
            <article className="admin-row" key={listing.id}>
              <img className="admin-thumb" src={listing.imageUrl} alt={listing.title} />
              <div className="admin-row-copy">
                <strong>{listing.title} · {formatInr(listing.price)}</strong>
                <span>{listing.status} · {formatStamp(listing.createdAt)}</span>
              </div>
              <button
                type="button"
                className="danger-btn admin-delete"
                onClick={() => handleDeleteListing(listing.id)}
              >
                Delete
              </button>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}
