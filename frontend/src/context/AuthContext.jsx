/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useRef, useState } from 'react'
import { useDispatch } from 'react-redux'
import { blankProfile, STORAGE_KEY } from '../data/seedState'
import { loadState } from '../lib/storage'
import * as api from '../lib/api'
import { setMarketplaceData } from '../store/slices/marketplaceSlice'

// Server users never include a password. Keep any password we already know locally
// so the offline login fallback still works for those accounts.
function mergeServerUser(localUser, serverUser) {
  return {
    ...localUser,
    ...serverUser,
    password: serverUser.password ?? localUser?.password,
    isBlocked: Boolean(serverUser.isBlocked),
  }
}

export const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [appState, setAppState] = useState(loadState)
  const [toast, setToast] = useState({ message: '', type: 'info' })
  const toastTimer = useRef(null)
  const [backendOnline, setBackendOnline] = useState(null) // null = still checking
  const dispatch = useDispatch()

  // Track whether the backend answered the last API call (used for the Topbar indicator)
  useEffect(() => api.subscribeBackendStatus(setBackendOnline), [])

  // On app start: if the backend is up, it becomes the source of truth.
  // Otherwise we keep the localStorage state that was loaded above.
  useEffect(() => {
    api
      .getState()
      .then((serverState) => {
        setAppState((prev) => ({
          ...prev,
          users: (serverState.users || []).map((serverUser) =>
            mergeServerUser(prev.users.find((u) => u.id === serverUser.id), serverUser),
          ),
        }))
        dispatch(
          setMarketplaceData({
            listings: serverState.listings,
            purchaseHistory: serverState.purchaseHistory,
          }),
        )
      })
      .catch((err) => {
        console.info('Backend not available, using localStorage data.', err.message)
      })
  }, [dispatch])

  // Put a user returned by the server into our users list (add or replace)
  function upsertUser(serverUser, extra = {}) {
    setAppState((prev) => {
      const exists = prev.users.some((u) => u.id === serverUser.id)
      const users = exists
        ? prev.users.map((u) => (u.id === serverUser.id ? { ...mergeServerUser(u, serverUser), ...extra } : u))
        : [...prev.users, { ...mergeServerUser(null, serverUser), ...extra }]
      return { ...prev, users }
    })
  }

  // Show the server's error message and build the standard failure result
  function serverFailure(err) {
    showToast(err.message, 'error')
    return { success: false, error: err.message }
  }

  // Sync users & currentUserId to localStorage whenever appState changes
  useEffect(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      const existing = raw ? JSON.parse(raw) : {}
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          ...existing,
          users: appState.users,
          currentUserId: appState.currentUserId,
        }),
      )
    } catch (e) {
      console.warn('Auth state localStorage sync failed:', e)
    }
  }, [appState.users, appState.currentUserId])

  // Cleanup toast timer on unmount
  useEffect(() => () => {
    if (toastTimer.current) window.clearTimeout(toastTimer.current)
  }, [])

  function showToast(message, type = 'info') {
    setToast({ message, type })
    if (toastTimer.current) window.clearTimeout(toastTimer.current)
    toastTimer.current = window.setTimeout(() => {
      setToast({ message: '', type: 'info' })
    }, 3000)
  }

  const currentUser =
    appState.users.find((u) => u.id === appState.currentUserId && !u.isBlocked) ?? null
  const isAuthenticated = Boolean(currentUser)
  const isAdmin = currentUser?.role === 'admin'
  const userMode = currentUser?.mode ?? 'buyer'

  async function login(email, password) {
    const normalizedEmail = (email ?? '').trim().toLowerCase()

    try {
      const user = await api.login(normalizedEmail, password)
      // Remember the password locally so offline login keeps working later
      upsertUser(user, { password })
      setAppState((prev) => ({ ...prev, currentUserId: user.id }))
      showToast(`Welcome back, ${user.name}!`, 'success')
      return { success: true, user }
    } catch (err) {
      if (!err.offline) return serverFailure(err)
    }

    // Offline fallback: check the local users list
    const found = appState.users.find(
      (u) => u.email?.toLowerCase() === normalizedEmail && u.password === password,
    )

    if (!found) {
      showToast('Invalid email or password.', 'error')
      return { success: false, error: 'Invalid email or password.' }
    }

    if (found.isBlocked) {
      showToast('This account has been suspended by an administrator.', 'error')
      return { success: false, error: 'Account suspended.' }
    }

    setAppState((prev) => ({ ...prev, currentUserId: found.id }))
    showToast(`Welcome back, ${found.name}!`, 'success')
    return { success: true, user: found }
  }

  async function register(userData) {
    const normalizedEmail = (userData.email ?? '').trim().toLowerCase()
    if (!normalizedEmail || !userData.password) {
      showToast('Email and password are required.', 'error')
      return { success: false, error: 'Email and password are required.' }
    }

    try {
      const user = await api.register({ ...userData, email: normalizedEmail })
      upsertUser(user, { password: userData.password })
      setAppState((prev) => ({ ...prev, currentUserId: user.id }))
      showToast('Account created successfully! Welcome to Campus Marketplace.', 'success')
      return { success: true, user }
    } catch (err) {
      if (!err.offline) return serverFailure(err)
    }

    // Offline fallback: create the account locally
    if (appState.users.some((u) => u.email?.toLowerCase() === normalizedEmail)) {
      showToast('An account with this email already exists.', 'error')
      return { success: false, error: 'Account already exists.' }
    }

    const newUser = {
      ...blankProfile,
      ...userData,
      id: `u-${window.crypto?.randomUUID?.() ?? Date.now()}`,
      name: userData.name?.trim() || normalizedEmail.split('@')[0],
      email: normalizedEmail,
      password: userData.password,
      mode: 'buyer',
      role: 'user',
      isBlocked: false,
    }

    setAppState((prev) => ({
      ...prev,
      users: [...prev.users, newUser],
      currentUserId: newUser.id,
    }))

    showToast('Account created successfully! Welcome to Campus Marketplace.', 'success')
    return { success: true, user: newUser }
  }

  function logout() {
    setAppState((prev) => ({ ...prev, currentUserId: null }))
    showToast('You have been logged out.', 'info')
  }

  async function toggleMode() {
    if (!currentUser) return
    const nextMode = currentUser.mode === 'seller' ? 'buyer' : 'seller'
    try {
      const user = await api.updateUser(currentUser.id, { mode: nextMode })
      upsertUser(user)
      showToast(`Switched to ${(user.mode || nextMode).toUpperCase()} mode.`, 'info')
      return
    } catch (err) {
      if (!err.offline) {
        serverFailure(err)
        return
      }
    }
    setAppState((prev) => ({
      ...prev,
      users: prev.users.map((u) => (u.id === currentUser.id ? { ...u, mode: nextMode } : u)),
    }))
    showToast(`Switched to ${nextMode.toUpperCase()} mode.`, 'info')
  }

  async function updateProfile(patch) {
    if (!currentUser) return { success: false }
    try {
      // The backend accepts these profile fields; email/password edits stay local only
      const { name, phone, department, year, bio } = patch
      const user = await api.updateUser(currentUser.id, { name, phone, department, year, bio })
      const localOnly = {}
      if (patch.email) localOnly.email = patch.email
      if (patch.password) localOnly.password = patch.password
      upsertUser(user, localOnly)
      showToast('Profile updated successfully.', 'success')
      return { success: true }
    } catch (err) {
      if (!err.offline) return serverFailure(err)
    }
    setAppState((prev) => ({
      ...prev,
      users: prev.users.map((u) => (u.id === currentUser.id ? { ...u, ...patch } : u)),
    }))
    showToast('Profile updated successfully.', 'success')
    return { success: true }
  }

  async function toggleUserBlock(userId) {
    try {
      const user = await api.toggleUserBlock(userId)
      upsertUser(user)
      showToast('User status updated.', 'info')
      return
    } catch (err) {
      if (!err.offline) {
        serverFailure(err)
        return
      }
    }
    setAppState((prev) => ({
      ...prev,
      users: prev.users.map((u) => (u.id === userId ? { ...u, isBlocked: !u.isBlocked } : u)),
    }))
    showToast('User status updated.', 'info')
  }

  const value = {
    currentUser,
    users: appState.users,
    isAuthenticated,
    isAdmin,
    userMode,
    toast,
    login,
    register,
    logout,
    toggleMode,
    updateProfile,
    toggleUserBlock,
    showToast,
    backendOnline,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
