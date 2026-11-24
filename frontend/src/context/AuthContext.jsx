/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useRef, useState } from 'react'
import { blankProfile, STORAGE_KEY } from '../data/seedState'
import { loadState } from '../lib/storage'

export const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [appState, setAppState] = useState(loadState)
  const [toast, setToast] = useState({ message: '', type: 'info' })
  const toastTimer = useRef(null)

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

  function login(email, password) {
    const normalizedEmail = (email ?? '').trim().toLowerCase()
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

  function register(userData) {
    const normalizedEmail = (userData.email ?? '').trim().toLowerCase()
    if (!normalizedEmail || !userData.password) {
      showToast('Email and password are required.', 'error')
      return { success: false, error: 'Email and password are required.' }
    }

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

  function toggleMode() {
    if (!currentUser) return
    const nextMode = currentUser.mode === 'seller' ? 'buyer' : 'seller'
    setAppState((prev) => ({
      ...prev,
      users: prev.users.map((u) => (u.id === currentUser.id ? { ...u, mode: nextMode } : u)),
    }))
    showToast(`Switched to ${nextMode.toUpperCase()} mode.`, 'info')
  }

  function updateProfile(patch) {
    if (!currentUser) return
    setAppState((prev) => ({
      ...prev,
      users: prev.users.map((u) => (u.id === currentUser.id ? { ...u, ...patch } : u)),
    }))
    showToast('Profile updated successfully.', 'success')
  }

  function toggleUserBlock(userId) {
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
