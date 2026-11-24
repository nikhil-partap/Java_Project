import { useState } from 'react'
import { FALLBACK_IMAGE } from '../data/seedState'
import ImageDropzone from './ImageDropzone'
import '../views/BuyerMarketplace.css'

export default function EditListingModal({ listing, onClose, onSave }) {
  if (!listing) return null
  return <EditListingForm listing={listing} onClose={onClose} onSave={onSave} />
}

function EditListingForm({ listing, onClose, onSave }) {
  const [title, setTitle] = useState(listing.title || '')
  const [category, setCategory] = useState(listing.category || 'Books')
  const [condition, setCondition] = useState(listing.condition || 'Good')
  const [price, setPrice] = useState(listing.price || '')
  const [location, setLocation] = useState(listing.location || '')
  const [description, setDescription] = useState(listing.description || '')
  const [images, setImages] = useState(
    listing.images?.length ? listing.images : listing.imageUrl ? [listing.imageUrl] : [FALLBACK_IMAGE]
  )

  const handleSubmit = (e) => {
    e.preventDefault()
    onSave(listing.id, {
      title,
      category,
      condition,
      price: Number(price),
      location,
      description,
      images: images.length > 0 ? images : [FALLBACK_IMAGE],
      imageUrl: images.length > 0 ? images[0] : FALLBACK_IMAGE,
    })
    onClose()
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card edit-modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-header-left">
            <span className="step-number">✎</span>
            <div>
              <p className="eyebrow" style={{ margin: 0 }}>Update details</p>
              <h2>Edit Listing</h2>
            </div>
          </div>
          <button type="button" className="close-btn" onClick={onClose} title="Close modal">✕</button>
        </div>

        <form className="grid-form two-column modal-form" onSubmit={handleSubmit}>
          <ImageDropzone images={images} setImages={setImages} inputId="edit-image-upload-input" />

          <label className="full-width">
            Item title
            <input type="text" value={title} onChange={(e) => setTitle(e.target.value)} required />
          </label>

          <label>
            Category
            <select value={category} onChange={(e) => setCategory(e.target.value)}>
              {['Books', 'Furniture', 'Electronics', 'Cycles', 'Accessories', 'Other'].map((cat) => (
                <option key={cat} value={cat}>{cat}</option>
              ))}
            </select>
          </label>

          <label>
            Condition
            <select value={condition} onChange={(e) => setCondition(e.target.value)}>
              {['Like New', 'Good', 'Fair'].map((cond) => (
                <option key={cond} value={cond}>{cond}</option>
              ))}
            </select>
          </label>

          <label>
            Price (INR)
            <input type="number" min="1" value={price} onChange={(e) => setPrice(e.target.value)} required />
          </label>

          <label>
            Pickup location
            <input type="text" value={location} onChange={(e) => setLocation(e.target.value)} required />
          </label>

          <label className="full-width">
            Description
            <textarea rows="4" value={description} onChange={(e) => setDescription(e.target.value)} required />
          </label>

          <div className="full-width modal-actions">
            <button type="button" className="secondary-btn" onClick={onClose}>Cancel</button>
            <button type="submit" className="primary-btn">Save Changes</button>
          </div>
        </form>
      </div>
    </div>
  )
}
