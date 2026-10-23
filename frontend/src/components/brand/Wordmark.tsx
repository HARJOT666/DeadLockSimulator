/**
 * Graph-inspired mark: three processes in a wait cycle drawn in ink; one edge is lifted in
 * terracotta — the edge a recovery strategy breaks.
 */
export function Mark({ className = 'size-7' }: { className?: string }) {
  const ink = '#493528'
  return (
    <svg viewBox="0 0 32 32" className={className} aria-hidden>
      <path d="M8 9 L24 9" stroke={ink} strokeWidth="1.6" strokeLinecap="round" />
      <path d="M24.6 11.2 L17.4 23" stroke={ink} strokeWidth="1.6" strokeLinecap="round" />
      <path d="M14.4 23.4 C9 21.5 6.8 16.5 7.4 12" stroke="#A96445" strokeWidth="1.6" strokeLinecap="round" fill="none" strokeDasharray="0.1 3.2" />
      <circle cx="7.6" cy="9" r="3.1" fill={ink} />
      <circle cx="24.4" cy="9" r="3.1" fill="none" stroke={ink} strokeWidth="1.6" />
      <circle cx="16" cy="24.4" r="3.1" fill={ink} />
    </svg>
  )
}

export function Wordmark({ compact = false }: { compact?: boolean }) {
  return (
    <span className="inline-flex items-center gap-2.5">
      <Mark />
      {!compact && (
        <span className="leading-none">
          <span className="display block text-[17px] text-ink">Self-Healing</span>
          <span className="mt-1 block font-mono text-[9.5px] uppercase tracking-[0.18em] text-cocoa">
            Deadlock Simulator
          </span>
        </span>
      )}
    </span>
  )
}
