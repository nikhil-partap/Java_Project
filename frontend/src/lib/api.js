// Small fetch wrapper for the Java REST backend.
// Requests go to relative '/api/...' URLs; in development Vite proxies them to localhost:8080.
//
// Errors thrown by request():
//   - err.offline === true  -> backend unreachable (network error, timeout, proxy error, not JSON).
//                              Callers fall back to the old localStorage logic.
//   - err.status (e.g. 400, 401, 403, 409) -> the server answered with {"error": "..."}.

const TIMEOUT_MS = 4000

// Tiny listener list so the UI can show "Backend: online/offline".
const statusListeners = new Set()
export function subscribeBackendStatus(listener) {
  statusListeners.add(listener)
  return () => statusListeners.delete(listener)
}
function setBackendStatus(online) {
  statusListeners.forEach((listener) => listener(online))
}

function offlineError(message = 'Backend is offline') {
  const err = new Error(message)
  err.offline = true
  return err
}

async function request(path, { method = 'GET', body } = {}) {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS)

  let response
  try {
    response = await fetch(`/api${path}`, {
      method,
      headers: body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    })
  } catch {
    setBackendStatus(false)
    throw offlineError()
  } finally {
    clearTimeout(timer)
  }

  // null when the body is empty or not JSON
  const data = await response.json().catch(() => null)

  if (response.ok) {
    // A 2xx that isn't JSON (e.g. index.html served without a proxy) means there is no real backend.
    if (data === null) {
      setBackendStatus(false)
      throw offlineError()
    }
    setBackendStatus(true)
    return data
  }

  // 5xx without a JSON error body is what the Vite proxy returns when :8080 is down.
  if (response.status >= 500 && !data?.error) {
    setBackendStatus(false)
    throw offlineError()
  }

  setBackendStatus(true)
  const err = new Error(data?.error || `Request failed (${response.status})`)
  err.status = response.status
  throw err
}

// ---- Endpoints ----
export const getHealth = () => request('/health')
export const getState = () => request('/state')

export const login = (email, password) =>
  request('/auth/login', { method: 'POST', body: { email, password } })
export const register = (userData) => request('/auth/register', { method: 'POST', body: userData })

export const updateUser = (id, patch) => request(`/users/${id}`, { method: 'PUT', body: patch })
export const toggleUserBlock = (id) => request(`/users/${id}/toggle-block`, { method: 'POST' })

export function getListings({ category, search, sort } = {}) {
  const params = new URLSearchParams()
  if (category) params.set('category', category)
  if (search) params.set('search', search)
  if (sort) params.set('sort', sort)
  const query = params.toString()
  return request(`/listings${query ? `?${query}` : ''}`)
}
export const createListing = (listing) => request('/listings', { method: 'POST', body: listing })
export const updateListing = (id, patch) => request(`/listings/${id}`, { method: 'PUT', body: patch })
export const deleteListing = (id) => request(`/listings/${id}`, { method: 'DELETE' })
export const addBuyerRequest = (listingId, buyerRequest) =>
  request(`/listings/${listingId}/requests`, { method: 'POST', body: buyerRequest })
export const purchaseListing = (listingId, buyerId) =>
  request(`/listings/${listingId}/purchase`, { method: 'POST', body: { buyerId } })

export const getPurchases = () => request('/purchases')
export const getStats = () => request('/stats')
