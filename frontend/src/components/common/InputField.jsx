/**
 * Reusable Form Input Field component
 */
export default function InputField({
  label,
  id,
  name,
  type = 'text',
  value,
  onChange,
  onBlur,
  error,
  touched,
  placeholder,
  required = false,
  helperText,
  disabled = false,
  min,
  max,
  step,
  autoComplete,
  className = '',
  ...rest
}) {
  const inputId = id || `field-${name}`
  const showError = Boolean(error && (touched || touched === undefined))

  return (
    <div className={`form-field ${showError ? 'has-error' : ''} ${className}`}>
      {label && (
        <label htmlFor={inputId} className="form-label">
          {label}
          {required && <span className="required-asterisk" aria-hidden="true"> *</span>}
        </label>
      )}

      <div className="input-wrapper">
        <input
          id={inputId}
          name={name}
          type={type}
          value={value ?? ''}
          onChange={onChange}
          onBlur={onBlur}
          placeholder={placeholder}
          required={required}
          disabled={disabled}
          min={min}
          max={max}
          step={step}
          autoComplete={autoComplete}
          aria-invalid={showError}
          aria-describedby={showError ? `${inputId}-error` : helperText ? `${inputId}-helper` : undefined}
          className={`form-input ${showError ? 'input-error' : ''}`}
          {...rest}
        />
      </div>

      {showError ? (
        <span id={`${inputId}-error`} className="field-error-msg" role="alert">
          {error}
        </span>
      ) : helperText ? (
        <span id={`${inputId}-helper`} className="field-helper-msg">
          {helperText}
        </span>
      ) : null}
    </div>
  )
}
