import { useState, type ReactNode } from 'react'

/**
 * Locally bundled photograph with responsive sources, lazy loading, a paper-toned fallback if the
 * file cannot load, and an optional editorial caption.
 */
export function Figure({ src, srcSmall, alt, caption, className = '', imgClassName = '', eager = false, sizes = '(min-width: 1024px) 50vw, 100vw' }: {
  src: string; srcSmall?: string; alt: string; caption?: ReactNode; className?: string; imgClassName?: string; eager?: boolean; sizes?: string
}) {
  const [failed, setFailed] = useState(false)
  return (
    <figure className={className}>
      {failed ? (
        <div role="img" aria-label={alt} className={`grid place-items-center bg-paper text-[12px] text-cocoa ${imgClassName}`}>
          <span className="px-4 text-center">{alt}</span>
        </div>
      ) : (
        <img src={src} srcSet={srcSmall ? `${srcSmall} 800w, ${src} 1600w` : undefined} sizes={srcSmall ? sizes : undefined}
          alt={alt} loading={eager ? 'eager' : 'lazy'} decoding="async" onError={() => setFailed(true)}
          className={`block ${imgClassName}`} />
      )}
      {caption && <figcaption className="mt-2.5 text-[12px] leading-snug text-cocoa">{caption}</figcaption>}
    </figure>
  )
}
