import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import {
  selectAllListings,
  removeListing,
  purchaseListing,
  saveListingEdit,
} from '../store/slices/marketplaceSlice'
import { useAuth } from '../context/AuthContext'
import ListingCard from '../components/ListingCard'
import EditListingModal from '../components/EditListingModal'
import './BuyerMarketplace.css'

export default function MyListings({
  currentUser: propUser,
  listings: propListings,
  removeListing: propRemove,
  markListingSold: propMarkSold,
  onEdit: propOnEdit,
  requestCount: propRequestCount,
}) {
  const dispatch = useDispatch()
  const { currentUser: authUser, users, showToast } = useAuth()
  const currentUser = propUser || authUser

  const allListings = useSelector(selectAllListings)
  const [editingListing, setEditingListing] = useState(null)

  const myListings =
    propListings ||
    allListings
      .filter((l) => l.ownerId === currentUser?.id)
      .map((l) => ({
        ...l,
        owner: users?.find((u) => u.id === l.ownerId) || currentUser,
      }))

  const requestCount =
    propRequestCount ??
    myListings.reduce((sum, item) => sum + (item.buyerRequests?.length || 0), 0)

  const handleDelete = async (id) => {
    if (propRemove) {
      propRemove(id)
    } else {
      const result = await dispatch(removeListing(id))
      if (result.success) showToast('Listing removed successfully.', 'info')
      else showToast(result.error, 'error')
    }
  }

  const handleMarkSold = async (id) => {
    if (propMarkSold) {
      propMarkSold(id)
    } else {
      // Sold to the first student who sent a request (same rule as the old local logic)
      const listing = allListings.find((l) => l.id === id)
      const buyerId = listing?.buyerRequests?.[0]?.buyerId ?? 'unknown'
      const result = await dispatch(purchaseListing({ listingId: id, buyerId }))
      if (result.success) showToast('Item marked as SOLD!', 'success')
      else showToast(result.error, 'error') // e.g. 409: item already sold
    }
  }

  const handleSaveEdit = async (id, updatedFields) => {
    const result = await dispatch(saveListingEdit(id, updatedFields))
    if (result.success) showToast('Listing updated successfully.', 'success')
    else showToast(result.error, 'error')
  }

  if (!myListings.length) {
    return (
      <div className="empty-panel panel">
        <span>＋</span>
        <h2>No listings yet</h2>
        <p>Post your first item and start earning or re-homing campus gear.</p>
        <Link to="/sell" className="btn btn-primary">
          Add an Item
        </Link>
      </div>
    )
  }

  return (
    <section>
      <div className="summary-row">
        <article>
          <span>Total listings</span>
          <strong>{myListings.length}</strong>
        </article>
        <article>
          <span>Buyer interest</span>
          <strong>{requestCount}</strong>
        </article>
      </div>

      <div className="listing-grid">
        {myListings.map((listing) => (
          <ListingCard
            key={listing.id}
            listing={listing}
            currentUser={currentUser}
            onDelete={handleDelete}
            onEdit={propOnEdit || ((l) => setEditingListing(l))}
            onMarkSold={handleMarkSold}
          />
        ))}
      </div>

      {editingListing && (
        <EditListingModal
          listing={editingListing}
          onClose={() => setEditingListing(null)}
          onSave={handleSaveEdit}
        />
      )}
    </section>
  )
}
