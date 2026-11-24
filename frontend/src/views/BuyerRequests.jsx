import { useSelector } from 'react-redux'
import { selectAllListings } from '../store/slices/marketplaceSlice'
import { useAuth } from '../context/AuthContext'
import { formatInr } from '../lib/formatters'

export default function BuyerRequests({ listings: propListings, requestCount: propCount, users: propUsers }) {
  const { currentUser, users: authUsers } = useAuth()
  const allListings = useSelector(selectAllListings)

  const users = propUsers || authUsers || []
  const listings =
    propListings ||
    allListings.filter((listing) => listing.ownerId === currentUser?.id)

  const requestCount =
    propCount ??
    listings.reduce((sum, item) => sum + (item.buyerRequests?.length || 0), 0)

  return (
    <section className="panel request-panel">
      <div className="panel-heading request-heading">
        <div>
          <p className="eyebrow">Your inbox</p>
          <h2>Requests received</h2>
        </div>
        <span className="result-count">{requestCount} total</span>
      </div>

      <div className="request-stack">
        {!listings.length ? (
          <div className="empty-panel compact-empty">
            <h3>No listings to show</h3>
            <p>Buyer requests will appear after you publish an item.</p>
          </div>
        ) : (
          listings.map((listing) => (
            <article className="listing-request" key={listing.id}>
              <div className="request-item-heading">
                <img src={listing.imageUrl} alt={listing.title} />
                <div>
                  <p className="eyebrow">{listing.category}</p>
                  <h3>{listing.title}</h3>
                  <span>{formatInr(listing.price)} · {listing.location}</span>
                </div>
                <span className="request-count">{listing.buyerRequests?.length || 0}</span>
              </div>

              {!listing.buyerRequests || !listing.buyerRequests.length ? (
                <p className="empty-request">No buyer requests yet.</p>
              ) : (
                <div className="request-list">
                  {listing.buyerRequests.map((request) => {
                    const buyer = users.find((user) => user.id === request.buyerId)
                    return (
                      <div className="request-item" key={request.id}>
                        <div className="mini-avatar">{(buyer?.name ?? 'B').charAt(0)}</div>
                        <div className="request-copy">
                          <strong>{buyer?.name ?? 'Buyer'}</strong>
                          <p>{request.message}</p>
                          {request.offerPrice && (
                            <span className="offer-tag">Offer: {formatInr(request.offerPrice)}</span>
                          )}
                        </div>
                        <span className="pending-pill">{request.status}</span>
                      </div>
                    )
                  })}
                </div>
              )}
            </article>
          ))
        )}
      </div>
    </section>
  )
}
