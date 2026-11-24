import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { InputField, TextAreaField, Button } from '../components/common'
import { validateText, validateEmail, validatePhone } from '../lib/validation'

export default function ProfileView() {
  const { currentUser, updateProfile, showToast } = useAuth()

  const [formData, setFormData] = useState({
    name: currentUser?.name || '',
    email: currentUser?.email || '',
    phone: currentUser?.phone || '',
    password: currentUser?.password || '',
    department: currentUser?.department || '',
    year: currentUser?.year || '',
    bio: currentUser?.bio || '',
  })
  const [errors, setErrors] = useState({})
  const [touched, setTouched] = useState({})
  const [isSaving, setIsSaving] = useState(false)

  const initials = (currentUser?.name || 'U')
    .split(' ')
    .map((part) => part.charAt(0))
    .join('')
    .slice(0, 2)
    .toUpperCase()

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

    let error = null
    if (name === 'name') error = validateText(value, 2, 50, 'Full name')
    if (name === 'email') error = validateEmail(value)
    if (name === 'phone') error = validatePhone(value)
    setErrors((prev) => ({ ...prev, [name]: error }))
  }

  const handleSubmit = (e) => {
    e.preventDefault()

    const nameErr = validateText(formData.name, 2, 50, 'Full name')
    const emailErr = validateEmail(formData.email)
    const phoneErr = validatePhone(formData.phone)

    const newErrors = {
      name: nameErr,
      email: emailErr,
      phone: phoneErr,
    }

    setErrors(newErrors)
    setTouched({ name: true, email: true, phone: true })

    if (Object.values(newErrors).some(Boolean)) {
      showToast('Please fix the errors in your profile details.', 'error')
      return
    }

    setIsSaving(true)
    setTimeout(async () => {
      await updateProfile({
        name: formData.name.trim(),
        email: formData.email.trim().toLowerCase(),
        phone: formData.phone.trim(),
        password: formData.password,
        department: formData.department.trim(),
        year: formData.year.trim(),
        bio: formData.bio.trim(),
      })
      setIsSaving(false)
    }, 350)
  }

  return (
    <section className="profile-layout">
      <aside className="profile-card panel">
        <div className="profile-avatar">{initials}</div>
        <h2>{currentUser?.name}</h2>
        <p>{currentUser?.email}</p>
        <span className="profile-role">{currentUser?.mode} account</span>
        <div className="profile-facts">
          <div>
            <span>Department</span>
            <strong>{currentUser?.department || 'Not specified'}</strong>
          </div>
          <div>
            <span>Year</span>
            <strong>{currentUser?.year || 'Not specified'}</strong>
          </div>
          <div>
            <span>Role</span>
            <strong className="badge-highlight">{currentUser?.role}</strong>
          </div>
        </div>
      </aside>

      <div className="panel form-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Personal details</p>
            <h2>Edit your profile</h2>
          </div>
        </div>

        <form className="grid-form two-column" onSubmit={handleSubmit} noValidate>
          <InputField
            label="Full Name"
            name="name"
            value={formData.name}
            onChange={handleChange}
            onBlur={handleBlur}
            error={errors.name}
            touched={touched.name}
            required
          />

          <InputField
            label="Campus Email"
            name="email"
            type="email"
            value={formData.email}
            onChange={handleChange}
            onBlur={handleBlur}
            error={errors.email}
            touched={touched.email}
            required
          />

          <InputField
            label="Phone Number"
            name="phone"
            value={formData.phone}
            onChange={handleChange}
            onBlur={handleBlur}
            error={errors.phone}
            touched={touched.phone}
            placeholder="+91 98765 43210"
          />

          <InputField
            label="Password"
            name="password"
            type="password"
            value={formData.password}
            onChange={handleChange}
            required
          />

          <InputField
            label="Department"
            name="department"
            value={formData.department}
            onChange={handleChange}
            placeholder="e.g. Computer Science & Engineering"
          />

          <InputField
            label="Year of Study"
            name="year"
            value={formData.year}
            onChange={handleChange}
            placeholder="e.g. 3rd Year"
          />

          <TextAreaField
            label="Bio / Hostel Info"
            name="bio"
            value={formData.bio}
            onChange={handleChange}
            placeholder="Hostel block, room area, or best times to meet on campus."
            rows={3}
            className="full-width"
          />

          <div className="full-width form-actions-row">
            <Button type="submit" variant="primary" isLoading={isSaving}>
              Save Profile Changes
            </Button>
          </div>
        </form>
      </div>
    </section>
  )
}
