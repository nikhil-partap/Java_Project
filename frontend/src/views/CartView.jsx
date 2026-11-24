import { useDispatch, useSelector } from 'react-redux'
import { Link } from 'react-router-dom'
import {
  clearCart,
  removeFromCart,
  selectCartItems,
  selectCartTotalPrice,
} from '../store/slices/cartSlice'
import { addBuyerRequest } from '../store/slices/marketplaceSlice'
import { useAuth } from '../context/AuthContext'
import { formatINR } from '../lib/formatters'
import { Button } from '../components/common'
import { useState } from 'react'

export default function CartView() {
  const dispatch = useDispatch()
  const cartItems = useSelector(selectCartItems)
  const totalPrice = useSelector(selectCartTotalPrice)
  const { currentUser, showToast } = useAuth()
  const [isCheckingOut, setIsCheckingOut] = useState(false)
  const [orderPlaced, setOrderPlaced] = useState(false)

  const handleCheckout = () => {
    if (cartItems.length === 0) return
    setIsCheckingOut(true)

    setTimeout(() => {
      // Create a buyer request for each item in the cart
      cartItems.forEach((item) => {
        dispatch(
          addBuyerRequest({
            listingId: item.id,
            request: {
              id: `req-${Date.now()}-${Math.random().toString(36).substring(2, 6)}`,
              buyerId: currentUser?.id || 'guest',
              message: `Interested in buying this item from my cart!`,
              offerPrice: item.price,
              status: 'Pending',
              createdAt: new Date().toISOString(),
            },
          }),
        )
      })

      dispatch(clearCart())
      setIsCheckingOut(false)
      setOrderPlaced(true)
      showToast('Requests submitted successfully for all items in your cart!', 'success')
    }, 600)
  }

  if (orderPlaced) {
    return (
      <section className="view-panel cart-view">
        <div className="panel-header">
          <div>
            <h1 className="view-title">Order Inquiries Sent!</h1>
            <p className="view-subtitle">The sellers will review your requests shortly.</p>
          </div>
        </div>
        <div className="empty-state-card">
          <div className="empty-icon" aria-hidden="true">🎉</div>
          <h3>Requests Dispatched Successfully</h3>
          <p>You can track seller responses in your inquiries or explore more campus deals.</p>
          <div className="empty-actions">
            <Link to="/marketplace" className="btn btn-primary">
              Continue Shopping
            </Link>
            <Link to="/requests" className="btn btn-secondary">
              View Inquiries
            </Link>
          </div>
        </div>
      </section>
    )
  }

  if (cartItems.length === 0) {
    return (
      <section className="view-panel cart-view">
        <div className="panel-header">
          <div>
            <h1 className="view-title">My Cart</h1>
            <p className="view-subtitle">Review saved items and submit multi-item requests</p>
          </div>
        </div>

        <div className="empty-state-card">
          <div className="empty-icon" aria-hidden="true">🛒</div>
          <h3>Your cart is empty</h3>
          <p>Explore the campus marketplace to find books, electronics, bicycles, and study gear.</p>
          <div className="empty-actions">
            <Link to="/marketplace" className="btn btn-primary">
              Explore Marketplace
            </Link>
          </div>
        </div>
      </section>
    )
  }

  return (
    <section className="view-panel cart-view">
      <div className="panel-header">
        <div>
          <h1 className="view-title">Shopping Cart ({cartItems.length})</h1>
          <p className="view-subtitle">Redux Toolkit powered multi-item checkout & state</p>
        </div>
        <Button variant="ghost" onClick={() => dispatch(clearCart())}>
          Clear All
        </Button>
      </div>

      <div className="cart-grid">
        <div className="cart-items-list">
          {cartItems.map((item) => (
            <div key={item.id} className="cart-item-card">
              <img
                src={item.imageUrl}
                alt={item.title}
                className="cart-item-image"
                loading="lazy"
              />
              <div className="cart-item-info">
                <div className="cart-item-category">{item.category}</div>
                <h3 className="cart-item-title">{item.title}</h3>
                <div className="cart-item-condition">
                  Condition: <strong>{item.condition}</strong>
                </div>
                <div className="cart-item-price">{formatINR(item.price)}</div>
              </div>
              <div className="cart-item-actions">
                <Button
                  variant="danger"
                  size="sm"
                  onClick={() => {
                    dispatch(removeFromCart(item.id))
                    showToast(`Removed "${item.title}" from cart`, 'info')
                  }}
                >
                  Remove
                </Button>
              </div>
            </div>
          ))}
        </div>

        <aside className="cart-summary-card">
          <h2 className="summary-title">Summary</h2>
          <div className="summary-row">
            <span>Total Items:</span>
            <strong>{cartItems.length}</strong>
          </div>
          <div className="summary-row">
            <span>Delivery:</span>
            <strong className="free-delivery">Campus Pickup (Free)</strong>
          </div>
          <hr className="summary-divider" />
          <div className="summary-total-row">
            <span>Estimated Total:</span>
            <span className="summary-total-price">{formatINR(totalPrice)}</span>
          </div>

          <Button
            variant="primary"
            size="lg"
            className="btn-block"
            isLoading={isCheckingOut}
            onClick={handleCheckout}
          >
            Submit Purchase Inquiries
          </Button>

          <p className="summary-note">
            🛡️ Direct campus peer-to-peer verification upon physical handover.
          </p>
        </aside>
      </div>
    </section>
  )
}
