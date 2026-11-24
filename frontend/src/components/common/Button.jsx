/**
 * Reusable Button component with variant styles and loading state
 */
export default function Button({
  children,
  variant = 'primary', // 'primary' | 'secondary' | 'danger' | 'ghost' | 'outline'
  size = 'md', // 'sm' | 'md' | 'lg'
  type = 'button',
  isLoading = false,
  disabled = false,
  onClick,
  icon,
  className = '',
  ...rest
}) {
  return (
    <button
      type={type}
      disabled={disabled || isLoading}
      onClick={onClick}
      className={`btn btn-${variant} btn-${size} ${isLoading ? 'btn-loading' : ''} ${className}`}
      {...rest}
    >
      {isLoading ? (
        <span className="btn-spinner" aria-hidden="true" />
      ) : (
        icon && <span className="btn-icon" aria-hidden="true">{icon}</span>
      )}
      <span className="btn-text">{children}</span>
    </button>
  )
}
