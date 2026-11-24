/**
 * Reusable validation rules and utility functions
 */

export function validateEmail(email) {
  if (!email || !email.trim()) {
    return 'Email address is required.'
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(email.trim())) {
    return 'Please enter a valid email address.'
  }
  return null
}

export function validatePassword(password) {
  if (!password) {
    return 'Password is required.'
  }
  if (password.length < 6) {
    return 'Password must be at least 6 characters long.'
  }
  return null
}

export function validatePrice(price) {
  if (price === '' || price === null || price === undefined) {
    return 'Price is required.'
  }
  const num = Number(price)
  if (Number.isNaN(num) || num < 0) {
    return 'Price must be a valid positive number.'
  }
  if (num > 100000) {
    return 'Price cannot exceed ₹1,00,000.'
  }
  return null
}

export function validateText(text, minLen = 3, maxLen = 100, fieldName = 'Field') {
  if (!text || !text.trim()) {
    return `${fieldName} is required.`
  }
  const len = text.trim().length
  if (len < minLen) {
    return `${fieldName} must be at least ${minLen} characters.`
  }
  if (len > maxLen) {
    return `${fieldName} cannot exceed ${maxLen} characters.`
  }
  return null
}

export function validateRequired(value, fieldName = 'This field') {
  if (!value || (typeof value === 'string' && !value.trim())) {
    return `${fieldName} is required.`
  }
  return null
}

export function validatePhone(phone) {
  if (!phone || !phone.trim()) return null // Optional field
  const phoneRegex = /^[+]?[\d\s-]{10,15}$/
  if (!phoneRegex.test(phone.trim())) {
    return 'Enter a valid phone number (10-15 digits).'
  }
  return null
}
