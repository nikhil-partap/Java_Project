import { useDispatch, useSelector } from 'react-redux'
import { selectAllListings, deleteListing } from '../store/slices/marketplaceSlice'
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

  const users = propUsers || authUsers || []
  const listings = propListings || allListings

  const handleDeleteListing = (id) => {
    if (propDelete) {
      propDelete(id)
    } else {
      dispatch(deleteListing(id))
      showToast('Listing removed by administrator.', 'info')
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
