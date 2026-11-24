import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import {
  selectAllListings,
  deleteListing,
  markListingSold,
  updateListing,
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

  const handleDelete = (id) => {
    if (propRemove) {
      propRemove(id)
    } else {
      dispatch(deleteListing(id))
      showToast('Listing removed successfully.', 'info')
    }
  }

  const handleMarkSold = (id) => {
    if (propMarkSold) {
      propMarkSold(id)
    } else {
      dispatch(markListingSold({ listingId: id }))
      showToast('Item marked as SOLD!', 'success')
    }
  }

  const handleSaveEdit = (id, updatedFields) => {
    dispatch(updateListing({ id, ...updatedFields }))
    showToast('Listing updated successfully.', 'success')
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
