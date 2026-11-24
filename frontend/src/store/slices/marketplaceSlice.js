import { createSlice } from '@reduxjs/toolkit'
import { loadState } from '../../lib/storage'
import { STORAGE_KEY } from '../../data/seedState'

const initialAppState = loadState()

const initialState = {
  listings: initialAppState.listings || [],
  purchaseHistory: initialAppState.purchaseHistory || [],
  isLoading: false,
  error: null,
}

function syncToLocalStorage(listings, purchaseHistory) {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const existing = raw ? JSON.parse(raw) : {}
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        ...existing,
        listings,
        purchaseHistory,
      }),
    )
  } catch (e) {
    console.warn('Sync to localStorage failed:', e)
  }
}

export const marketplaceSlice = createSlice({
  name: 'marketplace',
  initialState,
  reducers: {
    addListing: (state, action) => {
      state.listings.unshift(action.payload)
      syncToLocalStorage(state.listings, state.purchaseHistory)
    },
    updateListing: (state, action) => {
      const updated = action.payload
      const index = state.listings.findIndex((item) => item.id === updated.id)
      if (index !== -1) {
        state.listings[index] = { ...state.listings[index], ...updated }
        syncToLocalStorage(state.listings, state.purchaseHistory)
      }
    },
    deleteListing: (state, action) => {
      const idToDelete = action.payload
      state.listings = state.listings.filter((item) => item.id !== idToDelete)
      syncToLocalStorage(state.listings, state.purchaseHistory)
    },
    markListingSold: (state, action) => {
      const { listingId, buyerId } = action.payload
      const listing = state.listings.find((item) => item.id === listingId)
      if (listing) {
        listing.status = 'Sold'
        const purchaseRecord = {
          id: `p-${Date.now()}`,
          listingId: listing.id,
          title: listing.title,
          price: listing.price,
          sellerId: listing.ownerId,
          buyerId: buyerId || (listing.buyerRequests[0]?.buyerId ?? 'unknown'),
          purchasedAt: new Date().toISOString(),
          imageUrl: listing.imageUrl,
        }
        state.purchaseHistory.unshift(purchaseRecord)
        syncToLocalStorage(state.listings, state.purchaseHistory)
      }
    },
    addBuyerRequest: (state, action) => {
      const { listingId, request } = action.payload
      const listing = state.listings.find((item) => item.id === listingId)
      if (listing) {
        if (!Array.isArray(listing.buyerRequests)) {
          listing.buyerRequests = []
        }
        listing.buyerRequests.unshift(request)
        if (listing.status === 'Available') {
          listing.status = 'Pending'
        }
        syncToLocalStorage(state.listings, state.purchaseHistory)
      }
    },
    setLoading: (state, action) => {
      state.isLoading = action.payload
    },
    setError: (state, action) => {
      state.error = action.payload
    },
  },
})

export const {
  addListing,
  updateListing,
  deleteListing,
  markListingSold,
  addBuyerRequest,
  setLoading,
  setError,
} = marketplaceSlice.actions

// Selectors
export const selectAllListings = (state) => state.marketplace.listings
export const selectAvailableListings = (state) =>
  state.marketplace.listings.filter((item) => item.status === 'Available')
export const selectListingById = (listingId) => (state) =>
  state.marketplace.listings.find((item) => item.id === listingId)
export const selectMyListings = (userId) => (state) =>
  userId ? state.marketplace.listings.filter((item) => item.ownerId === userId) : []
export const selectPurchaseHistory = (state) => state.marketplace.purchaseHistory
export const selectIsLoading = (state) => state.marketplace.isLoading

export default marketplaceSlice.reducer
