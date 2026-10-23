import type { ReactNode } from 'react'
import { Reveal } from '../motion/Reveal'

/**
 * Editorial section: a numbered label column that stays pinned (sticky) on wide screens while
 * the content beside it scrolls. Separated from the previous section by a single hairline.
 */
export function Section({ number, title, note, children, id, className = '', stacked = false }: {
  number?: string; title: ReactNode; note?: ReactNode; children: ReactNode; id?: string; className?: string
  /** Heading above the content instead of beside it — for wide tables and charts. */
  stacked?: boolean
}) {
  if (stacked) {
    return (
      <Reveal as="section" className={`border-t border-rule pt-8 pb-14 ${className}`}>
        <div id={id} className="mb-8 grid gap-2 lg:grid-cols-[200px_minmax(0,1fr)] lg:gap-12">
          <div>
            {number && <p className="font-mono text-[11px] tracking-[0.14em] text-terracotta-deep">{number}</p>}
            <h2 className="display mt-1 text-[22px] text-ink">{title}</h2>
          </div>
          {note && <p className="max-w-[640px] self-end text-[13px] leading-relaxed text-cocoa">{note}</p>}
        </div>
        {children}
      </Reveal>
    )
  }
  return (
    <Reveal as="section" className={`border-t border-rule pt-8 pb-14 ${className}`}>
      <div id={id} className="grid gap-6 lg:grid-cols-[200px_minmax(0,1fr)] lg:gap-12">
        <div className="lg:sticky lg:top-10 lg:self-start">
          {number && <p className="font-mono text-[11px] tracking-[0.14em] text-terracotta-deep">{number}</p>}
          <h2 className="display mt-1 text-[22px] text-ink">{title}</h2>
          {note && <p className="mt-2 text-[13px] leading-relaxed text-cocoa">{note}</p>}
        </div>
        <div className="min-w-0">{children}</div>
      </div>
    </Reveal>
  )
}
