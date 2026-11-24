import { Link } from 'react-router-dom'

export default function NotFoundView() {
  return (
    <section className="view-panel not-found-panel">
      <div className="empty-state-card not-found-card">
        <div className="not-found-badge">404</div>
        <h1 className="not-found-title">Page Not Found</h1>
        <p className="not-found-text">
          The campus destination you are looking for does not exist or may have been moved.
        </p>
        <div className="empty-actions">
          <Link to="/marketplace" className="btn btn-primary">
            Back to Marketplace
          </Link>
          <Link to="/" className="btn btn-secondary">
            Home
          </Link>
        </div>
      </div>
    </section>
  )
}
