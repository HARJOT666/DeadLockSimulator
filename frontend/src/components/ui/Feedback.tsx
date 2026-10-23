import type { ReactNode } from 'react'
import { RotateCcw } from 'lucide-react'
import { describeError } from '../../api/client'
import { Button } from './Button'
import { Mark } from '../brand/Wordmark'

export function Spinner({ className = 'size-4' }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" className={`animate-spin ${className}`} aria-hidden>
      <circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" strokeOpacity="0.18" strokeWidth="2" />
      <path d="M21 12a9 9 0 0 0-9-9" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  )
}

export function FullScreenLoader({ label }: { label: string }) {
  return (
    <div className="grid min-h-screen place-items-center bg-ivory" role="status" aria-live="polite">
      <div className="flex flex-col items-center gap-4 text-cocoa">
        <Mark className="size-9 animate-pulse" />
        <p className="text-sm">{label}…</p>
      </div>
    </div>
  )
}

export function Loading({ label = 'Loading', className = '' }: { label?: string; className?: string }) {
  return (
    <div className={`flex items-center gap-3 py-16 text-sm text-cocoa ${className}`} role="status" aria-live="polite">
      <Spinner /> {label}…
    </div>
  )
}

export function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`skeleton rounded-[4px] ${className}`} aria-hidden />
}

/** Full error state with an explicit recovery action. */
export function ErrorNotice({ error, onRetry, className = '' }: { error: unknown; onRetry?: () => void; className?: string }) {
  const { message, details } = describeError(error)
  return (
    <div className={`border-l-2 border-danger bg-sheet px-5 py-4 ${className}`} role="alert">
      <p className="font-medium text-ink">{message}</p>
      {details.length > 0 && <ul className="mt-1.5 space-y-0.5 text-[13px] text-cocoa">{details.slice(0, 6).map((d) => <li key={d}>{d}</li>)}</ul>}
      {onRetry && <Button variant="quiet" size="sm" className="-ml-3 mt-2" icon={<RotateCcw className="size-3.5" />} onClick={onRetry}>Try again</Button>}
    </div>
  )
}

export function InlineError({ error }: { error: unknown }) {
  if (!error) return null
  const { message, details } = describeError(error)
  return (
    <div className="border-l-2 border-danger bg-terracotta-wash/50 px-3 py-2.5 text-[13px] text-danger" role="alert">
      <p className="font-medium">{message}</p>
      {details.length > 0 && <ul className="mt-1 list-disc pl-4">{details.slice(0, 8).map((d) => <li key={d}>{d}</li>)}</ul>}
    </div>
  )
}

/** Intentional empty state: a short editorial note plus the one useful next action. */
export function EmptyState({ title, body, action, figure, className = '' }: {
  title: string; body: ReactNode; action?: ReactNode; figure?: ReactNode; className?: string
}) {
  return (
    <div className={`grid items-center gap-8 py-10 md:grid-cols-[minmax(0,1fr)_minmax(0,0.8fr)] ${className}`}>
      <div className="max-w-md">
        <h3 className="display text-[26px] text-ink">{title}</h3>
        <p className="mt-3 text-[15px] leading-relaxed text-cocoa">{body}</p>
        {action && <div className="mt-6">{action}</div>}
      </div>
      {figure && <div className="hidden md:block">{figure}</div>}
    </div>
  )
}
