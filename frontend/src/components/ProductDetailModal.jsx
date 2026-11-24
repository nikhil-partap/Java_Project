import { useState } from 'react'
import ImageCarousel from './ImageCarousel'

export default function ProductDetailModal({ listing, onClose, onSubmitOffer, isOwner }) {
  const [offerPrice, setOfferPrice] = useState(listing ? listing.price : 0)
  const [message, setMessage] = useState('')

  if (!listing) return null

  const handleSubmit = (e) => {
    e.preventDefault()
    if (onSubmitOffer) onSubmitOffer(listing.id, { offerPrice, message })
    onClose()
  }

  return (
    <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
      <div style={{ background: '#fff', padding: '24px', borderRadius: '16px', maxWidth: '480px', width: '90%', maxHeight: '90vh', overflowY: 'auto', color: '#333' }}>
        <button onClick={onClose} style={{ float: 'right', border: 'none', background: 'none', cursor: 'pointer', fontSize: '1.2rem' }}>✕</button>
        <h2 style={{ marginTop: 0 }}>{listing.title}</h2>

        <ImageCarousel images={listing.images} imageUrl={listing.imageUrl} alt={listing.title} />

        <p><strong>Price:</strong> ₹{listing.price}</p>
        <p><strong>Category:</strong> {listing.category || 'General'} | <strong>Condition:</strong> {listing.condition || 'Used'}</p>
        <p>{listing.description}</p>

        {!isOwner && (
          <form onSubmit={handleSubmit} style={{ marginTop: '16px', borderTop: '1px solid #eee', paddingTop: '16px' }}>
            <h3 style={{ marginTop: 0 }}>Send Buy Request</h3>
            <div style={{ marginBottom: '10px' }}>
              <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '4px' }}>Offer Amount (₹)</label>
              <input
                type="number"
                value={offerPrice}
                onChange={(e) => setOfferPrice(Number(e.target.value))}
                required
                style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #ccc' }}
              />
            </div>
            <div style={{ marginBottom: '10px' }}>
              <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '4px' }}>Message to Seller</label>
              <textarea
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                placeholder="e.g. Can meet near the library..."
                required
                style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #ccc' }}
              />
            </div>
            <button type="submit" style={{ width: '100%', padding: '10px', backgroundColor: '#0070f3', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer' }}>
              Submit Offer
            </button>
          </form>
        )}
      </div>
    </div>
  )
}
