import { useSelector } from 'react-redux'
import { selectPurchaseHistory } from '../store/slices/marketplaceSlice'
import { useAuth } from '../context/AuthContext'
import { formatInr, formatStamp } from '../lib/formatters'

export default function PurchaseHistory({ purchases: propPurchases, users: propUsers }) {
  const { currentUser, users: authUsers } = useAuth()
  const allPurchases = useSelector(selectPurchaseHistory)

  const users = propUsers || authUsers || []
  const purchases =
    propPurchases ||
    allPurchases.filter((p) => p.buyerId === currentUser?.id)

  return (
    <section className="panel purchase-panel">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">Your orders</p>
          <h2>Purchase history</h2>
        </div>
        <span className="result-count">
          {purchases.length} purchase{purchases.length === 1 ? '' : 's'}
        </span>
      </div>

      {!purchases.length ? (
        <div className="empty-panel compact-empty">
          <h3>No purchases yet</h3>
          <p>When a seller marks one of your requests as sold, it will appear here.</p>
        </div>
      ) : (
        <div className="purchase-list">
          {[...purchases].reverse().map((purchase) => {
            const seller = users.find((user) => user.id === purchase.sellerId)
            return (
              <article className="purchase-item" key={purchase.id}>
                <div>
                  <p className="eyebrow">Purchased item</p>
                  <h3>{purchase.title}</h3>
                  <p>Seller: {seller?.name ?? 'Campus seller'}</p>
                </div>
                <div className="purchase-meta">
                  <strong>{formatInr(purchase.price)}</strong>
                  <span>{formatStamp(purchase.purchasedAt)}</span>
                </div>
              </article>
            )
          })}
        </div>
      )}
    </section>
  )
}
