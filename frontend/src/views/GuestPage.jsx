import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { selectAvailableListings } from '../store/slices/marketplaceSlice'
import { useAuth } from '../context/AuthContext'
import Brand from '../components/Brand'
import ListingCard from '../components/ListingCard'
import { InputField, Button } from '../components/common'
import { validateEmail, validatePassword } from '../lib/validation'

export default function GuestPage() {
  const navigate = useNavigate()
  const { login, register, isAuthenticated, users } = useAuth()
  const listings = useSelector(selectAvailableListings)

  const [authMode, setAuthMode] = useState('login')
  const [formData, setFormData] = useState({ email: '', password: '', name: '' })
  const [errors, setErrors] = useState({})
  const [touched, setTouched] = useState({})
  const [isLoading, setIsLoading] = useState(false)

  const scrollToTopBtnRef = useRef(null)

  // Redirect if already logged in
  useEffect(() => {
    if (isAuthenticated) {
      navigate('/marketplace', { replace: true })
    }
  }, [isAuthenticated, navigate])

  // Scroll to top button handling
  useEffect(() => {
    const btnElement = scrollToTopBtnRef.current
    if (!btnElement) return

    const handleScroll = () => {
      if (window.scrollY > 300) {
        btnElement.style.display = 'block'
        btnElement.classList.add('visible')
      } else {
        btnElement.style.display = 'none'
        btnElement.classList.remove('visible')
      }
    }

    window.addEventListener('scroll', handleScroll)
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: null }))
    }
  }

  const handleBlur = (e) => {
    const { name, value } = e.target
    setTouched((prev) => ({ ...prev, [name]: true }))

    let err = null
    if (name === 'email') err = validateEmail(value)
    if (name === 'password') err = validatePassword(value)
    setErrors((prev) => ({ ...prev, [name]: err }))
  }

  const handleSubmit = (e) => {
    e.preventDefault()

    const emailErr = validateEmail(formData.email)
    const passErr = validatePassword(formData.password)

    const newErrors = { email: emailErr, password: passErr }
    setErrors(newErrors)
    setTouched({ email: true, password: true })

    if (emailErr || passErr) return

    setIsLoading(true)
    setTimeout(async () => {
      if (authMode === 'login') {
        const res = await login(formData.email, formData.password)
        setIsLoading(false)
        if (res.success) {
          navigate('/marketplace')
        }
      } else {
        const res = await register({
          name: formData.name || formData.email.split('@')[0],
          email: formData.email,
          password: formData.password,
        })
        setIsLoading(false)
        if (res.success) {
          navigate('/marketplace')
        }
      }
    }, 400)
  }

  // Demo login helper
  const handleQuickLogin = (email, password) => {
    setIsLoading(true)
    setTimeout(async () => {
      const res = await login(email, password)
      setIsLoading(false)
      if (res.success) navigate('/marketplace')
    }, 300)
  }

  return (
    <div className="guest-page">
      <header className="guest-header">
        <Brand />
        <span className="campus-badge">Chitkara University Campus Edition</span>
      </header>

      <main className="guest-main">
        <section className="guest-hero">
          <div className="hero-copy">
            <p className="eyebrow">Your campus. Your marketplace.</p>
            <h1>Great finds are closer than you think.</h1>
            <p className="lead">
              Buy and sell engineering books, cycles, electronics, and hostel essentials directly with fellow students across campus.
            </p>
            <div className="trust-row">
              <span><strong>{listings.length}</strong> live listings</span>
              <span><strong>{users?.length || 5}</strong> students</span>
              <span><strong>₹</strong> student prices</span>
            </div>

            {/* Demo quick login accounts */}
            <div className="demo-accounts-card">
              <span className="demo-label">Demo Accounts:</span>
              <div className="demo-buttons-group">
                <button
                  type="button"
                  className="demo-btn"
                  onClick={() => handleQuickLogin('ananya@campus.edu', 'password123')}
                  title="Login as Student Buyer (Ananya)"
                >
                  Student Buyer
                </button>
                <button
                  type="button"
                  className="demo-btn"
                  onClick={() => handleQuickLogin('aarav@campus.edu', 'password123')}
                  title="Login as Student Seller (Aarav)"
                >
                  Student Seller
                </button>
                <button
                  type="button"
                  className="demo-btn demo-btn-admin"
                  onClick={() => handleQuickLogin('nikhilpartapsingh@gmail.com', 'password123')}
                  title="Login as Admin (Nikhil)"
                >
                  Admin Account
                </button>
              </div>
            </div>
          </div>

          <section className="auth-card" aria-label="Account access">
            <p className="eyebrow">{authMode === 'login' ? 'Welcome back' : 'Join the marketplace'}</p>
            <h2>{authMode === 'login' ? 'Sign in to continue' : 'Create your account'}</h2>
            <p className="auth-subtitle">
              {authMode === 'login'
                ? 'Use your campus account to buy and sell.'
                : 'Start discovering deals from students nearby.'}
            </p>

            <form className="form-stack" onSubmit={handleSubmit} noValidate>
              {authMode === 'register' && (
                <InputField
                  label="Your Name"
                  name="name"
                  value={formData.name}
                  onChange={handleChange}
                  placeholder="e.g. Nikhil Partap"
                />
              )}

              <InputField
                label="Campus Email"
                name="email"
                type="email"
                value={formData.email}
                onChange={handleChange}
                onBlur={handleBlur}
                error={errors.email}
                touched={touched.email}
                placeholder="name@campus.edu"
                required
                autoComplete="email"
              />

              <InputField
                label="Password"
                name="password"
                type="password"
                value={formData.password}
                onChange={handleChange}
                onBlur={handleBlur}
                error={errors.password}
                touched={touched.password}
                placeholder="••••••••"
                required
                autoComplete={authMode === 'login' ? 'current-password' : 'new-password'}
              />

              <Button
                type="submit"
                variant="primary"
                size="lg"
                isLoading={isLoading}
                className="full-button"
              >
                {authMode === 'login' ? 'Sign In' : 'Create Account'}
              </Button>
            </form>

            <button
              type="button"
              className="auth-switch"
              onClick={() => {
                setAuthMode(authMode === 'login' ? 'register' : 'login')
                setErrors({})
                setTouched({})
              }}
            >
              {authMode === 'login' ? 'New here? Create an account' : 'Already registered? Sign in'}
            </button>
          </section>
        </section>

        <section className="guest-listings">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Fresh on campus</p>
              <h2>See what students are selling</h2>
            </div>
            <span className="muted-note">Sign in to contact a seller or add to cart</span>
          </div>
          <div className="listing-grid">
            {listings.map((listing) => (
              <ListingCard key={listing.id} listing={listing} />
            ))}
          </div>
        </section>
      </main>

      <button
        ref={scrollToTopBtnRef}
        onClick={scrollToTop}
        className="scroll-to-top-btn"
        style={{ display: 'none' }}
        aria-label="Scroll to top"
      >
        ↑ Top
      </button>
    </div>
  )
}
