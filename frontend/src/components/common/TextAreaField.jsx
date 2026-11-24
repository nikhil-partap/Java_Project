/**
 * Reusable Textarea Field component
 */
export default function TextAreaField({
  label,
  id,
  name,
  value,
  onChange,
  onBlur,
  error,
  touched,
  placeholder,
  rows = 4,
  maxLength,
  showCount = false,
  required = false,
  disabled = false,
  helperText,
  className = '',
  ...rest
}) {
  const textareaId = id || `field-${name}`
  const showError = Boolean(error && (touched || touched === undefined))
  const currentLength = (value ?? '').length

  return (
    <div className={`form-field ${showError ? 'has-error' : ''} ${className}`}>
      <div className="field-header">
        {label && (
          <label htmlFor={textareaId} className="form-label">
            {label}
            {required && <span className="required-asterisk" aria-hidden="true"> *</span>}
          </label>
        )}
        {showCount && maxLength && (
          <span className="char-counter">
            {currentLength} / {maxLength}
          </span>
        )}
      </div>

      <div className="textarea-wrapper">
        <textarea
          id={textareaId}
          name={name}
          value={value ?? ''}
          onChange={onChange}
          onBlur={onBlur}
          placeholder={placeholder}
          rows={rows}
          maxLength={maxLength}
          required={required}
          disabled={disabled}
          aria-invalid={showError}
          aria-describedby={showError ? `${textareaId}-error` : helperText ? `${textareaId}-helper` : undefined}
          className={`form-textarea ${showError ? 'input-error' : ''}`}
          {...rest}
        />
      </div>

      {showError ? (
        <span id={`${textareaId}-error`} className="field-error-msg" role="alert">
          {error}
        </span>
      ) : helperText ? (
        <span id={`${textareaId}-helper`} className="field-helper-msg">
          {helperText}
        </span>
      ) : null}
    </div>
  )
}
