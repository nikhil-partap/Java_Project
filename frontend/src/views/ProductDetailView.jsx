import { useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import {
  selectListingById,
  sendBuyerRequest,
} from '../store/slices/marketplaceSlice'
import {
  addToCart,
  selectIsInCart,
} from '../store/slices/cartSlice'
import { useAuth } from '../context/AuthContext'
import ImageCarousel from '../components/ImageCarousel'
import { formatINR, formatRelativeTime } from '../lib/formatters'
import { Button } from '../components/common'

export default function ProductDetailView() {
  const { id } = useParams()
  const navigate = useNavigate()
  const dispatch = useDispatch()
  const { currentUser, users, showToast } = useAuth()

  const listing = useSelector(selectListingById(id))
  const isInCart = useSelector(selectIsInCart(id))

  const [isRequesting, setIsRequesting] = useState(false)
  const [requestMsg, setRequestMsg] = useState('')
  const [offerAmount, setOfferAmount] = useState('')
  const [showRequestForm, setShowRequestForm] = useState(false)

  if (!listing) {
    return (
      <section className="view-panel product-detail-view">
        <div className="empty-state-card">
          <div className="empty-icon" aria-hidden="true">🔍</div>
          <h2>Item Not Found</h2>
          <p>The listing you are looking for does not exist or may have been removed.</p>
          <div className="empty-actions">
            <Link to="/marketplace" className="btn btn-primary">
              Back to Marketplace
            </Link>
          </div>
        </div>
      </section>
    )
  }

  const owner = users?.find((u) => u.id === listing.ownerId) || listing.owner || {
    name: 'Campus Student',
    department: 'General',
    year: 'N/A',
  }
  const isOwner = currentUser?.id === listing.ownerId

  const handleAddToCart = () => {
    dispatch(addToCart(listing))
    showToast(`Added "${listing.title}" to cart!`, 'success')
  }

  const handleSendRequest = (e) => {
    e.preventDefault()
    if (!currentUser) {
      showToast('Please login to send a purchase request.', 'error')
      navigate('/guest')
      return
    }

    setIsRequesting(true)
    setTimeout(async () => {
      const result = await dispatch(
        sendBuyerRequest({
          listingId: listing.id,
          request: {
            id: `req-${Date.now()}`,
            buyerId: currentUser.id,
            message: requestMsg.trim() || 'Hi! I am interested in purchasing this item.',
            offerPrice: Number(offerAmount) || listing.price,
            status: 'Pending',
            createdAt: new Date().toISOString(),
          },
        }),
      )
      setIsRequesting(false)
      if (!result.success) {
        showToast(result.error, 'error')
        return
      }
      setShowRequestForm(false)
      setRequestMsg('')
      setOfferAmount('')
      showToast('Your request has been sent to the seller!', 'success')
    }, 400)
  }

  return (
    <section className="view-panel product-detail-view">
      <div className="detail-breadcrumb">
        <Link to="/marketplace" className="breadcrumb-back">
          ← Back to Marketplace
        </Link>
        <span className="breadcrumb-separator">/</span>
        <span className="breadcrumb-category">{listing.category}</span>
        <span className="breadcrumb-separator">/</span>
        <span className="breadcrumb-current">{listing.title}</span>
      </div>

      <div className="detail-layout">
        <div className="detail-media">
          <ImageCarousel
            images={listing.images && listing.images.length > 0 ? listing.images : [listing.imageUrl]}
            title={listing.title}
          />
        </div>

        <div className="detail-content">
          <div className="detail-badges">
            <span className="category-pill">{listing.category}</span>
            <span className="condition-pill">{listing.condition}</span>
            <span className={`status-pill status-${listing.status?.toLowerCase()}`}>
              {listing.status}
            </span>
          </div>

          <h1 className="detail-title">{listing.title}</h1>
          <div className="detail-price">{formatINR(listing.price)}</div>

          <p className="detail-description">{listing.description}</p>

          <div className="detail-meta-box">
            <div className="meta-row">
              <span className="meta-label">Campus Location:</span>
              <span className="meta-val">{listing.location || 'Campus Main Grounds'}</span>
            </div>
            <div className="meta-row">
              <span className="meta-label">Posted:</span>
              <span className="meta-val">{formatRelativeTime(listing.createdAt)}</span>
            </div>
            <div className="meta-row">
              <span className="meta-label">Seller:</span>
              <span className="meta-val">
                <strong>{owner.name}</strong> ({owner.department || 'Student'} • {owner.year || ''})
              </span>
            </div>
            {owner.phone && (
              <div className="meta-row">
                <span className="meta-label">Contact:</span>
                <span className="meta-val">{owner.phone}</span>
              </div>
            )}
          </div>

          {!isOwner && listing.status === 'Available' && (
            <div className="detail-actions">
              <Button
                variant={isInCart ? 'secondary' : 'primary'}
                size="lg"
                onClick={handleAddToCart}
                disabled={isInCart}
              >
                {isInCart ? '✓ Already in Cart' : '🛒 Add to Cart'}
              </Button>

              <Button
                variant="outline"
                size="lg"
                onClick={() => setShowRequestForm(!showRequestForm)}
              >
                {showRequestForm ? 'Cancel Request' : 'Direct Inquiry'}
              </Button>
            </div>
          )}

          {isOwner && (
            <div className="seller-notice-box">
              <p>💡 This is your listing. You can manage or mark it sold in <strong>My Listings</strong>.</p>
              <Link to="/my-listings" className="btn btn-secondary btn-sm">
                Go to My Listings
              </Link>
            </div>
          )}

          {showRequestForm && !isOwner && (
            <form onSubmit={handleSendRequest} className="detail-request-form">
              <h3>Send Purchase Request to {owner.name}</h3>
              <div className="form-field">
                <label className="form-label" htmlFor="offer-price">Your Offer (₹)</label>
                <input
                  id="offer-price"
                  type="number"
                  placeholder={String(listing.price)}
                  value={offerAmount}
                  onChange={(e) => setOfferAmount(e.target.value)}
                  className="form-input"
                  min="0"
                />
              </div>

              <div className="form-field">
                <label className="form-label" htmlFor="offer-msg">Message to Seller</label>
                <textarea
                  id="offer-msg"
                  rows="3"
                  placeholder="Where and when can we meet on campus?"
                  value={requestMsg}
                  onChange={(e) => setRequestMsg(e.target.value)}
                  className="form-textarea"
                />
              </div>

              <Button type="submit" variant="primary" isLoading={isRequesting}>
                Submit Request
              </Button>
            </form>
          )}
        </div>
      </div>
    </section>
  )
}
