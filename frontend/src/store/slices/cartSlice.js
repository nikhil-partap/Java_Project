import { createSlice } from '@reduxjs/toolkit'

const CART_STORAGE_KEY = 'campus-marketplace-cart'

function loadInitialCart() {
  try {
    const raw = localStorage.getItem(CART_STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

const initialState = {
  items: loadInitialCart(),
}

export const cartSlice = createSlice({
  name: 'cart',
  initialState,
  reducers: {
    addToCart: (state, action) => {
      const listing = action.payload
      const exists = state.items.some((item) => item.id === listing.id)
      if (!exists) {
        state.items.push({
          id: listing.id,
          title: listing.title,
          price: Number(listing.price) || 0,
          category: listing.category,
          condition: listing.condition,
          imageUrl: listing.imageUrl || (listing.images && listing.images[0]),
          ownerId: listing.ownerId,
          addedAt: new Date().toISOString(),
        })
        try {
          localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(state.items))
        } catch (e) {
          console.warn('Failed to save cart to localStorage:', e)
        }
      }
    },
    removeFromCart: (state, action) => {
      const idToRemove = action.payload
      state.items = state.items.filter((item) => item.id !== idToRemove)
      try {
        localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(state.items))
      } catch (e) {
        console.warn('Failed to save cart to localStorage:', e)
      }
    },
    clearCart: (state) => {
      state.items = []
      try {
        localStorage.removeItem(CART_STORAGE_KEY)
      } catch (e) {
        console.warn('Failed to clear cart in localStorage:', e)
      }
    },
  },
})

export const { addToCart, removeFromCart, clearCart } = cartSlice.actions

// Selectors
export const selectCartItems = (state) => state.cart.items
export const selectCartCount = (state) => state.cart.items.length
export const selectCartTotalPrice = (state) =>
  state.cart.items.reduce((sum, item) => sum + (Number(item.price) || 0), 0)
export const selectIsInCart = (listingId) => (state) =>
  state.cart.items.some((item) => item.id === listingId)

export default cartSlice.reducer
