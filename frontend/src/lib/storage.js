import { FALLBACK_IMAGE, seedState, STORAGE_KEY } from '../data/seedState'

export function loadState() {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return seedState

  try {
    const parsed = JSON.parse(raw)
    const persistedUsers = Array.isArray(parsed.users) ? parsed.users : seedState.users
    const persistedListings = Array.isArray(parsed.listings) ? parsed.listings : seedState.listings
    const needsSeedMigration = Number(parsed.dataVersion ?? 0) < seedState.dataVersion
    const users = needsSeedMigration
      ? [
        ...persistedUsers,
        ...seedState.users.filter((seedUser) => !persistedUsers.some((user) => user.id === seedUser.id)),
      ]
      : persistedUsers
    const listings = needsSeedMigration
      ? [
        ...persistedListings,
        ...seedState.listings.filter((seedListing) => !persistedListings.some((listing) => listing.id === seedListing.id)),
      ]
      : persistedListings
    const currentUserId = users.some((user) => user.id === parsed.currentUserId && !user.isBlocked)
      ? parsed.currentUserId
      : null

    return {
      ...seedState,
      ...parsed,
      dataVersion: seedState.dataVersion,
      currentUserId,
      users: users.map((user) => {
        const seedUser = seedState.users.find((candidate) => candidate.id === user.id)
        return {
          ...user,
          role: needsSeedMigration && seedUser ? seedUser.role : user.role ?? seedUser?.role ?? 'user',
          isBlocked: Boolean(user.isBlocked),
        }
      }),
      listings: listings.map((listing) => ({
        ...listing,
        buyerRequests: Array.isArray(listing.buyerRequests) ? listing.buyerRequests : [],
        createdAt: listing.createdAt ?? new Date().toISOString(),
        imageUrl: listing.imageUrl ?? FALLBACK_IMAGE,
        images: Array.isArray(listing.images) && listing.images.length > 0 ? listing.images : [listing.imageUrl ?? FALLBACK_IMAGE],
        status: listing.status ?? 'Available',
      })),
      purchaseHistory: Array.isArray(parsed.purchaseHistory) ? parsed.purchaseHistory : seedState.purchaseHistory,
    }
  } catch {
    return seedState
  }
}
