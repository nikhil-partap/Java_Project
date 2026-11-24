import { Link } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { addToCart, selectIsInCart } from '../store/slices/cartSlice'
import { formatInr, formatStamp } from '../lib/formatters'
import { useAuth } from '../context/AuthContext'

export default function ListingCard({
  listing,
  currentUser,
  onDelete,
  onEdit,
  onRequest,
  onMarkSold,
}) {
  const dispatch = useDispatch()
  const { showToast } = useAuth()
  const isInCart = useSelector(selectIsInCart(listing.id))

  const isOwner = currentUser?.id === listing.ownerId

  const handleAddToCart = (e) => {
    e.preventDefault()
    e.stopPropagation()
    dispatch(addToCart(listing))
    showToast?.(`Added "${listing.title}" to cart!`, 'success')
  }

  return (
    <article className="market-card">
      <div className="listing-image-wrap">
        <Link to={`/item/${listing.id}`} className="listing-image-link" tabIndex={-1}>
          <img
            className="listing-image"
            src={listing.imageUrl}
            alt={listing.title}
            loading="lazy"
          />
        </Link>
        {isOwner && onEdit ? (
          <button
            type="button"
            className="edit-badge-btn"
            onClick={() => onEdit(listing)}
            title="Edit item"
          >
            ✎ Edit
          </button>
        ) : null}
        <span className={`status-pill status-${listing.status?.toLowerCase()}`}>
          {listing.status}
        </span>
      </div>

      <div className="listing-body">
        <div className="listing-heading">
          <div>
            <p className="category">{listing.category}</p>
            <h3>
              <Link to={`/item/${listing.id}`} className="listing-title-link">
                {listing.title}
              </Link>
            </h3>
          </div>
          <strong className="listing-price">{formatInr(listing.price)}</strong>
        </div>

        <p className="description">{listing.description}</p>
        <div className="listing-meta">
          <span>{listing.condition}</span>
          <span>{listing.location}</span>
        </div>

        <div className="seller-row">
          <div className="mini-avatar" aria-hidden="true">
            {(listing.owner?.name ?? 'Student').charAt(0)}
          </div>
          <div>
            <span>Listed by {listing.owner?.name ?? 'Student seller'}</span>
            <small>{formatStamp(listing.createdAt)}</small>
          </div>
        </div>

        {currentUser ? (
          <div className="card-actions">
            {isOwner ? (
              <div className="owner-actions">
                {listing.status === 'Sold' ? (
                  <span className="status-note">Sold</span>
                ) : (
                  <button
                    type="button"
                    className="secondary-btn"
                    onClick={() => onMarkSold?.(listing.id)}
                  >
                    Mark as sold
                  </button>
                )}
                <button
                  type="button"
                  className="danger-btn"
                  onClick={() => onDelete?.(listing.id)}
                >
                  Delete listing
                </button>
              </div>
            ) : (
              <div className="buyer-actions">
                {listing.status === 'Available' ? (
                  <>
                    <button
                      type="button"
                      className="primary-btn"
                      onClick={() => onRequest?.(listing.id)}
                    >
                      Quick Inquire
                    </button>
                    <button
                      type="button"
                      className={`cart-action-btn ${isInCart ? 'in-cart' : ''}`}
                      onClick={handleAddToCart}
                      disabled={isInCart}
                      title={isInCart ? 'Already in cart' : 'Add to cart'}
                    >
                      {isInCart ? '✓ In Cart' : '+ Cart'}
                    </button>
                  </>
                ) : (
                  <span className="status-note">{listing.status}</span>
                )}
              </div>
            )}
          </div>
        ) : (
          <div className="card-actions guest-actions">
            <Link to={`/item/${listing.id}`} className="secondary-btn full-button">
              View Details
            </Link>
          </div>
        )}
      </div>
    </article>
  )
}
