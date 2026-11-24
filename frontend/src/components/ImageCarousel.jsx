import { useState } from 'react'
import { FALLBACK_IMAGE } from '../data/seedState'

export default function ImageCarousel({ images = [], imageUrl, alt = '' }) {
  const [index, setIndex] = useState(0)

  const list = images.length ? images : imageUrl ? [imageUrl] : [FALLBACK_IMAGE]
  const hasMultiple = list.length > 1

  return (
    <div style={{ position: 'relative', margin: '12px 0', height: '220px', borderRadius: '12px', overflow: 'hidden', backgroundColor: '#f4f6f2' }}>
      <img src={list[index] || FALLBACK_IMAGE} alt={alt} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
      {hasMultiple && (
        <>
          {index > 0 && (
            <button
              type="button"
              className="carousel-arrow prev"
              onClick={(e) => { e.stopPropagation(); setIndex((i) => i - 1) }}
              title="Previous image"
            >
              ‹
            </button>
          )}
          {index < list.length - 1 && (
            <button
              type="button"
              className="carousel-arrow next"
              onClick={(e) => { e.stopPropagation(); setIndex((i) => i + 1) }}
              title="Next image"
            >
              ›
            </button>
          )}
          <div className="carousel-dots">
            {list.map((_, idx) => (
              <button
                key={idx}
                type="button"
                className={`carousel-dot ${idx === index ? 'active' : ''}`}
                onClick={(e) => { e.stopPropagation(); setIndex(idx) }}
                title={`Go to image ${idx + 1}`}
              />
            ))}
          </div>
        </>
      )}
    </div>
  )
}
