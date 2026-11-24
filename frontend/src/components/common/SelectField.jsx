/**
 * Reusable Select Dropdown Field component
 */
export default function SelectField({
  label,
  id,
  name,
  value,
  onChange,
  onBlur,
  error,
  touched,
  options = [],
  placeholder = 'Select an option...',
  required = false,
  disabled = false,
  helperText,
  className = '',
  ...rest
}) {
  const selectId = id || `field-${name}`
  const showError = Boolean(error && (touched || touched === undefined))

  return (
    <div className={`form-field ${showError ? 'has-error' : ''} ${className}`}>
      {label && (
        <label htmlFor={selectId} className="form-label">
          {label}
          {required && <span className="required-asterisk" aria-hidden="true"> *</span>}
        </label>
      )}

      <div className="select-wrapper">
        <select
          id={selectId}
          name={name}
          value={value ?? ''}
          onChange={onChange}
          onBlur={onBlur}
          disabled={disabled}
          required={required}
          aria-invalid={showError}
          aria-describedby={showError ? `${selectId}-error` : helperText ? `${selectId}-helper` : undefined}
          className={`form-select ${showError ? 'input-error' : ''}`}
          {...rest}
        >
          {placeholder && <option value="">{placeholder}</option>}
          {options.map((opt) => {
            const optVal = typeof opt === 'object' ? opt.value : opt
            const optLabel = typeof opt === 'object' ? opt.label : opt
            return (
              <option key={optVal} value={optVal}>
                {optLabel}
              </option>
            )
          })}
        </select>
      </div>

      {showError ? (
        <span id={`${selectId}-error`} className="field-error-msg" role="alert">
          {error}
        </span>
      ) : helperText ? (
        <span id={`${selectId}-helper`} className="field-helper-msg">
          {helperText}
        </span>
      ) : null}
    </div>
  )
}
