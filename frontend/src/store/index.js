import { configureStore } from '@reduxjs/toolkit'
import marketplaceReducer from './slices/marketplaceSlice'
import cartReducer from './slices/cartSlice'

export const store = configureStore({
  reducer: {
    marketplace: marketplaceReducer,
    cart: cartReducer,
  },
})

export default store
