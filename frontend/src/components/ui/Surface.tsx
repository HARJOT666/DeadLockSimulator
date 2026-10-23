import type { HTMLAttributes } from 'react'

/** Warm ivory surface with a fine beige border and a barely-there shadow — for tables, charts and canvases. */
export function Surface({ className = '', children, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={`rounded-[6px] border border-rule bg-sheet p-5 shadow-[var(--shadow-paper)] sm:p-6 ${className}`} {...rest}>
      {children}
    </div>
  )
}
