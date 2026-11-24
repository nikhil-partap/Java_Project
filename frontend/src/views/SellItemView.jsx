import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useDispatch } from 'react-redux'
import { addListing } from '../store/slices/marketplaceSlice'
import { useAuth } from '../context/AuthContext'
import { FALLBACK_IMAGE } from '../data/seedState'
import { InputField, SelectField, TextAreaField, Button } from '../components/common'
import { validateText, validatePrice } from '../lib/validation'
import ImageDropzone from '../components/ImageDropzone'
import './BuyerMarketplace.css'

const CATEGORY_OPTIONS = [
  'Books',
  'Furniture',
  'Electronics',
  'Cycles',
  'Accessories',
  'Other',
]

const CONDITION_OPTIONS = ['Like New', 'Good', 'Fair']

export default function SellItemView() {
  const navigate = useNavigate()
  const dispatch = useDispatch()
  const { currentUser, showToast } = useAuth()

  const [images, setImages] = useState([])
  const [formData, setFormData] = useState({
    title: '',
    category: 'Books',
    condition: 'Good',
    price: '',
    location: '',
    description: '',
  })
  const [errors, setErrors] = useState({})
  const [touched, setTouched] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))

    // Clear error dynamically as user types
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: null }))
    }
  }

  const handleBlur = (e) => {
    const { name, value } = e.target
    setTouched((prev) => ({ ...prev, [name]: true }))
    validateField(name, value)
  }

  const validateField = (name, value) => {
    let error = null
    if (name === 'title') {
      error = validateText(value, 3, 80, 'Item title')
    } else if (name === 'price') {
      error = validatePrice(value)
    } else if (name === 'location') {
      error = validateText(value, 3, 60, 'Pickup location')
    } else if (name === 'description') {
      error = validateText(value, 10, 600, 'Description')
    }
    setErrors((prev) => ({ ...prev, [name]: error }))
    return error
  }

  const handleSubmit = (e) => {
    e.preventDefault()

    // Validate all fields before submission
    const titleErr = validateText(formData.title, 3, 80, 'Item title')
    const priceErr = validatePrice(formData.price)
    const locErr = validateText(formData.location, 3, 60, 'Pickup location')
    const descErr = validateText(formData.description, 10, 600, 'Description')

    const newErrors = {
      title: titleErr,
      price: priceErr,
      location: locErr,
      description: descErr,
    }

    setErrors(newErrors)
    setTouched({
      title: true,
      price: true,
      location: true,
      description: true,
    })

    const hasErrors = Object.values(newErrors).some(Boolean)
    if (hasErrors) {
      showToast('Please correct the highlighted errors before publishing.', 'error')
      return
    }

    setIsSubmitting(true)

    setTimeout(() => {
      const primaryImage = images.length > 0 ? images[0] : FALLBACK_IMAGE
      const newListing = {
        id: `l-${Date.now()}`,
        ownerId: currentUser?.id || 'u-guest',
        title: formData.title.trim(),
        category: formData.category,
        condition: formData.condition,
        price: Number(formData.price),
        location: formData.location.trim(),
        description: formData.description.trim(),
        status: 'Available',
        createdAt: new Date().toISOString(),
        imageUrl: primaryImage,
        images: images.length > 0 ? images : [FALLBACK_IMAGE],
        buyerRequests: [],
      }

      dispatch(addListing(newListing))
      setIsSubmitting(false)
      showToast('Listing published successfully to the campus marketplace!', 'success')
      navigate('/my-listings')
    }, 450)
  }

  return (
    <section className="content-grid sell-view-grid">
      <div className="panel form-panel">
        <div className="panel-heading">
          <span className="step-number">01</span>
          <div>
            <p className="eyebrow">Item details</p>
            <h2>What are you selling?</h2>
          </div>
        </div>

        <form className="grid-form two-column" onSubmit={handleSubmit} noValidate>
          <ImageDropzone images={images} setImages={setImages} inputId="sell-image-upload-input" />

          <InputField
            label="Item Title"
            name="title"
            value={formData.title}
            onChange={handleChange}
            onBlur={handleBlur}
            error={errors.title}
            touched={touched.title}
            placeholder="e.g. Engineering Mathematics Textbook Vol 1"
            required
            className="full-width"
            helperText="Include key details like subject, brand, or model."
          />

          <SelectField
            label="Category"
            name="category"
            value={formData.category}
            onChange={handleChange}
            onBlur={handleBlur}
            options={CATEGORY_OPTIONS}
            required
          />

          <SelectField
            label="Condition"
            name="condition"
            value={formData.condition}
            onChange={handleChange}
            onBlur={handleBlur}
            options={CONDITION_OPTIONS}
            required
          />

          <InputField
            label="Price (INR ₹)"
            name="price"
            type="number"
            value={formData.price}
            onChange={handleChange}
            onBlur={handleBlur}
            error={errors.price}
            touched={touched.price}
            placeholder="e.g. 450"
            required
            min="1"
          />

          <InputField
            label="Pickup Location"
            name="location"
            value={formData.location}
            onChange={handleChange}
            onBlur={handleBlur}
            error={errors.location}
            touched={touched.location}
            placeholder="e.g. Hostel C Common Room / Library"
            required
          />

          <TextAreaField
            label="Description"
            name="description"
            value={formData.description}
            onChange={handleChange}
            onBlur={handleBlur}
            error={errors.description}
            touched={touched.description}
            placeholder="Mention item age, condition details, and any accessories included."
            rows={4}
            maxLength={600}
            showCount
            required
            className="full-width"
          />

          <div className="full-width form-actions-row">
            <Button
              type="submit"
              variant="primary"
              size="lg"
              isLoading={isSubmitting}
            >
              Publish Listing
            </Button>
          </div>
        </form>
      </div>

      <aside className="seller-tip panel">
        <span className="tip-icon">✦</span>
        <h3>A good listing sells faster</h3>
        <p>Use high-contrast photos, set a student-friendly price, and specify a safe campus pickup point.</p>
        <div className="tip-preview">
          <span>Security Notice</span>
          <strong>All deals are verified peer-to-peer on campus grounds.</strong>
        </div>
      </aside>
    </section>
  )
}
