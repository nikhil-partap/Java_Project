import { useState } from 'react'
import { compressImage } from '../lib/imageCompressor'

export default function ImageDropzone({ images, setImages, inputId = 'image-upload-input' }) {
  const [isDragging, setIsDragging] = useState(false)

  const handleFiles = async (files) => {
    for (const file of Array.from(files)) {
      if (!file.type.startsWith('image/')) continue
      const compressed = await compressImage(file)
      if (compressed) {
        setImages((prev) => [...prev, compressed])
      }
    }
  }

  return (
    <div className="full-width form-group">
      <label className="field-label">Item Images</label>
      <div
        className={`image-dropzone ${isDragging ? 'dragging' : ''}`}
        onDragOver={(e) => { e.preventDefault(); setIsDragging(true) }}
        onDragLeave={(e) => { e.preventDefault(); setIsDragging(false) }}
        onDrop={(e) => {
          e.preventDefault()
          setIsDragging(false)
          if (e.dataTransfer.files?.length) handleFiles(e.dataTransfer.files)
        }}
      >
        <input
          type="file"
          id={inputId}
          accept="image/*"
          multiple
          onChange={(e) => e.target.files?.length && handleFiles(e.target.files)}
          style={{ display: 'none' }}
        />
        <label htmlFor={inputId} className="dropzone-content">
          <svg className="upload-icon" viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12" />
          </svg>
          <span className="dropzone-title">Drag & drop images here, or <span className="browse-text">browse</span></span>
        </label>
      </div>

      {images.length > 0 && (
        <div className="image-preview-grid">
          {images.map((img, idx) => (
            <div key={idx} className="preview-thumb">
              <img src={img} alt={`Preview ${idx + 1}`} />
              <button
                type="button"
                className="remove-img-btn"
                onClick={() => setImages((prev) => prev.filter((_, i) => i !== idx))}
                title="Remove image"
              >
                ✕
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
