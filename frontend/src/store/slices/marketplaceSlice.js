import { createSlice } from '@reduxjs/toolkit'
import { loadState } from '../../lib/storage'
import { FALLBACK_IMAGE, STORAGE_KEY } from '../../data/seedState'
import * as api from '../../lib/api'

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
    // Replace everything with fresh data from the backend (GET /api/state)
    setMarketplaceData: (state, action) => {
      const { listings, purchaseHistory } = action.payload
      state.listings = (listings || []).map((listing) => ({
        ...listing,
        buyerRequests: Array.isArray(listing.buyerRequests) ? listing.buyerRequests : [],
        imageUrl: listing.imageUrl || FALLBACK_IMAGE,
        images: Array.isArray(listing.images) && listing.images.length > 0
          ? listing.images
          : [listing.imageUrl || FALLBACK_IMAGE],
        status: listing.status || 'Available',
      }))
      state.purchaseHistory = purchaseHistory || []
      syncToLocalStorage(state.listings, state.purchaseHistory)
    },
    // Apply a purchase record returned by POST /api/listings/{id}/purchase
    applyPurchase: (state, action) => {
      const purchase = action.payload
      const listing = state.listings.find((item) => item.id === purchase.listingId)
      if (listing) listing.status = 'Sold'
      state.purchaseHistory.unshift(purchase)
      syncToLocalStorage(state.listings, state.purchaseHistory)
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
  setMarketplaceData,
  applyPurchase,
  setLoading,
  setError,
} = marketplaceSlice.actions

// ---- Thunks: call the backend first, fall back to the local reducer when it is offline ----
// Each returns { success: true } or { success: false, error } so views can show a toast.
function onlineOrLocal(callApi, onServerData, onOffline) {
  return async (dispatch) => {
    try {
      const data = await callApi()
      onServerData(dispatch, data)
      return { success: true }
    } catch (err) {
      if (err.offline) {
        onOffline(dispatch)
        return { success: true, offline: true }
      }
      return { success: false, error: err.message }
    }
  }
}

export const saveNewListing = (listing) =>
  onlineOrLocal(
    () => api.createListing(listing),
    (dispatch, saved) => dispatch(addListing(saved)),
    (dispatch) => dispatch(addListing(listing)),
  )

export const saveListingEdit = (id, fields) =>
  onlineOrLocal(
    () => api.updateListing(id, fields),
    (dispatch, saved) => dispatch(updateListing(saved)),
    (dispatch) => dispatch(updateListing({ id, ...fields })),
  )

export const removeListing = (id) =>
  onlineOrLocal(
    () => api.deleteListing(id),
    (dispatch) => dispatch(deleteListing(id)),
    (dispatch) => dispatch(deleteListing(id)),
  )

export const sendBuyerRequest = ({ listingId, request }) =>
  onlineOrLocal(
    () => api.addBuyerRequest(listingId, request),
    (dispatch, savedListing) => dispatch(updateListing(savedListing)),
    (dispatch) => dispatch(addBuyerRequest({ listingId, request })),
  )

// On 409 (already sold) the error message is returned and nothing is marked sold.
export const purchaseListing = ({ listingId, buyerId }) =>
  onlineOrLocal(
    () => api.purchaseListing(listingId, buyerId),
    (dispatch, purchase) => dispatch(applyPurchase(purchase)),
    (dispatch) => dispatch(markListingSold({ listingId, buyerId })),
  )

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
