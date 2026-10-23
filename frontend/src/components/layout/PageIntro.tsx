import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

export interface Crumb { label: string; to?: string }

/** Restrained page opening: quiet trail, display title, one measured sentence, actions. */
export function PageIntro({ trail, title, lede, actions, aside }: {
  trail?: Crumb[]; title: ReactNode; lede?: ReactNode; actions?: ReactNode; aside?: ReactNode
}) {
  return (
    <header className="mb-12 grid gap-8 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-end">
      <div className="min-w-0 max-w-[720px]" style={{ animation: 'ink-in .4s var(--ease-out-quart) both' }}>
        {trail && (
          <nav aria-label="Breadcrumb" className="mb-4 flex flex-wrap items-center gap-x-2 font-mono text-[11px] uppercase tracking-[0.12em] text-cocoa">
            {trail.map((c, i) => (
              <span key={i} className="flex items-center gap-2">
                {c.to ? <Link to={c.to} className="hover:text-terracotta-deep">{c.label}</Link> : <span className="text-ink">{c.label}</span>}
                {i < trail.length - 1 && <span aria-hidden className="text-cocoa">/</span>}
              </span>
            ))}
          </nav>
        )}
        <h1 className="display text-[34px] text-ink sm:text-[42px]">{title}</h1>
        {lede && <p className="mt-4 text-[16px] leading-relaxed text-cocoa">{lede}</p>}
        {actions && <div className="mt-7 flex flex-wrap items-center gap-3">{actions}</div>}
      </div>
      {aside}
    </header>
  )
}
