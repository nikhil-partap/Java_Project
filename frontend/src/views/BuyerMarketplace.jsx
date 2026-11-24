import { useState, useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import {
  selectAvailableListings,
  removeListing,
  sendBuyerRequest,
  saveListingEdit,
} from '../store/slices/marketplaceSlice'
import { useAuth } from '../context/AuthContext'
import ListingCard from '../components/ListingCard'
import MarketplaceFilters from '../components/MarketplaceFilters'
import ProductDetailModal from '../components/ProductDetailModal'
import EditListingModal from '../components/EditListingModal'
import './BuyerMarketplace.css'

export default function BuyerMarketplace({
  currentUser: propUser,
  listings: propListings,
  requestItem: propRequestItem,
  onDelete: propOnDelete,
  onEdit: propOnEdit,
}) {
  const dispatch = useDispatch()
  const { currentUser: authUser, users, showToast } = useAuth()
  const currentUser = propUser || authUser

  const reduxListings = useSelector(selectAvailableListings)
  const baseListings = propListings || reduxListings

  // Attach owner objects to listings
  const listings = baseListings.map((item) => {
    if (item.owner) return item
    const owner = users?.find((u) => u.id === item.ownerId)
    return { ...item, owner }
  })

  const [searchTerm, setSearchTerm] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('All')
  const [maxPrice, setMaxPrice] = useState(50000)
  const [sortBy, setSortBy] = useState('newest')
  const [selectedListing, setSelectedListing] = useState(null)
  const [editingListing, setEditingListing] = useState(null)

  const [wishlist, setWishlist] = useState(() => {
    try {
      const saved = localStorage.getItem('cb_wishlist')
      return saved ? JSON.parse(saved) : []
    } catch {
      return []
    }
  })

  const [recentlyViewed, setRecentlyViewed] = useState(() => {
    try {
      const saved = localStorage.getItem('cb_recently_viewed')
      return saved ? JSON.parse(saved) : []
    } catch {
      return []
    }
  })

  useEffect(() => {
    try {
      localStorage.setItem('cb_wishlist', JSON.stringify(wishlist))
    } catch (e) {
      console.warn('localStorage error for wishlist:', e)
    }
  }, [wishlist])

  useEffect(() => {
    try {
      localStorage.setItem('cb_recently_viewed', JSON.stringify(recentlyViewed))
    } catch (e) {
      console.warn('localStorage error for recentlyViewed:', e)
    }
  }, [recentlyViewed])

  const toggleWishlist = (listingId) => {
    if (wishlist.includes(listingId)) {
      setWishlist(wishlist.filter((id) => id !== listingId))
      showToast('Item removed from Wishlist', 'info')
    } else {
      setWishlist([...wishlist, listingId])
      showToast('Item saved to Wishlist! ❤️', 'success')
    }
  }

  const handleOpenDetail = (listing) => {
    if (!listing) return
    setSelectedListing(listing)
    setRecentlyViewed((prev) => {
      const miniItem = { ...listing }
      const filtered = prev.filter((item) => item.id !== listing.id)
      return [miniItem, ...filtered].slice(0, 5)
    })
  }

  const handleOfferSubmission = async (listingId, offerData) => {
    if (propRequestItem) {
      propRequestItem(listingId, offerData)
    } else {
      const result = await dispatch(
        sendBuyerRequest({
          listingId,
          request: {
            id: `req-${Date.now()}`,
            buyerId: currentUser?.id || 'guest',
            message: offerData.message || 'Interested in this item!',
            offerPrice: Number(offerData.offerPrice) || 0,
            status: 'Pending',
            createdAt: new Date().toISOString(),
          },
        }),
      )
      if (!result.success) {
        showToast(result.error, 'error')
        return
      }
      showToast('Buy inquiry sent to seller successfully!', 'success')
    }
  }

  const handleDeleteListing = async (id) => {
    if (propOnDelete) {
      propOnDelete(id)
    } else {
      const result = await dispatch(removeListing(id))
      if (result.success) showToast('Listing deleted.', 'info')
      else showToast(result.error, 'error')
    }
  }

  const handleSaveEdit = async (id, updatedFields) => {
    const result = await dispatch(saveListingEdit(id, updatedFields))
    if (result.success) showToast('Listing updated successfully.', 'success')
    else showToast(result.error, 'error')
  }

  const categories = [...new Set(listings.map((item) => item.category).filter(Boolean))]

  const filteredListings = listings.filter((item) => {
    const matchesSearch =
      item.title?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      item.description?.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesCategory = selectedCategory === 'All' || item.category === selectedCategory
    const matchesPrice = Number(item.price || 0) <= maxPrice
    return matchesSearch && matchesCategory && matchesPrice
  })

  const sortedListings = [...filteredListings].sort((a, b) => {
    if (sortBy === 'price-low') return Number(a.price) - Number(b.price)
    if (sortBy === 'price-high') return Number(b.price) - Number(a.price)
    return new Date(b.createdAt || 0) - new Date(a.createdAt || 0)
  })

  return (
    <>
      <section className="marketplace-banner">
        <div>
          <span className="banner-chip">Campus-Only Verified</span>
          <h2>Find your next useful thing.</h2>
          <p>Affordable, nearby, and listed by students you share a campus with.</p>
        </div>
        <div className="banner-stat">
          <strong>{listings.length}</strong>
          <span>items available</span>
        </div>
      </section>

      <MarketplaceFilters
        searchTerm={searchTerm}
        setSearchTerm={setSearchTerm}
        selectedCategory={selectedCategory}
        setSelectedCategory={setSelectedCategory}
        maxPrice={maxPrice}
        setMaxPrice={setMaxPrice}
        categories={categories}
      />

      <div className="section-heading compact-heading marketplace-header-actions">
        <div>
          <p className="eyebrow">Latest listings</p>
          <h2>Browse the marketplace</h2>
        </div>

        <div className="sort-container">
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="sort-dropdown"
          >
            <option value="newest">Sort by: Newest</option>
            <option value="price-low">Price: Low to High</option>
            <option value="price-high">Price: High to Low</option>
          </select>
          <span className="result-count">{sortedListings.length} items</span>
        </div>
      </div>

      {sortedListings.length === 0 ? (
        <div className="empty-panel compact-empty marketplace-empty-state">
          <h3>No items match your search</h3>
          <p>Try resetting filters or searching for something else.</p>
        </div>
      ) : (
        <div className="listing-grid">
          {sortedListings.map((listing) => (
            <div key={listing.id} className="listing-card-wrapper">
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation()
                  toggleWishlist(listing.id)
                }}
                className="wishlist-btn"
                title="Save to Wishlist"
              >
                {wishlist.includes(listing.id) ? '❤️' : '🤍'}
              </button>

              <ListingCard
                listing={listing}
                currentUser={currentUser}
                onDelete={handleDeleteListing}
                onEdit={propOnEdit || ((l) => setEditingListing(l))}
                onRequest={() => handleOpenDetail(listing)}
              />
            </div>
          ))}
        </div>
      )}

      {recentlyViewed.length > 0 && (
        <div className="recently-viewed-section">
          <h3>Recently Viewed Items</h3>
          <div className="recently-viewed-list">
            {recentlyViewed.map((item) => (
              <div
                key={`rv-${item.id}`}
                onClick={() => handleOpenDetail(item)}
                className="recently-viewed-card"
              >
                <strong className="recently-viewed-title">{item.title}</strong>
                <span className="recently-viewed-price">₹{item.price}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {selectedListing && (
        <ProductDetailModal
          key={selectedListing.id}
          listing={selectedListing}
          onClose={() => setSelectedListing(null)}
          onSubmitOffer={handleOfferSubmission}
          isOwner={selectedListing.ownerId === currentUser?.id}
        />
      )}

      {editingListing && (
        <EditListingModal
          listing={editingListing}
          onClose={() => setEditingListing(null)}
          onSave={handleSaveEdit}
        />
      )}
    </>
  )
}
