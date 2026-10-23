import { AlertTriangle, CheckCircle2, CircleDashed, CircleSlash, XCircle } from 'lucide-react'
import type { RecoveryStatus, RunStatus, StrategyType } from '../../api/types'
import { STATUS_META, STRATEGIES } from '../../lib/strategy'

const tones = {
  ok: { cls: 'text-sage-deep', Icon: CheckCircle2 },
  warn: { cls: 'text-gold-deep', Icon: AlertTriangle },
  bad: { cls: 'text-danger', Icon: XCircle },
  neutral: { cls: 'text-cocoa', Icon: CircleSlash },
}

/** Status as an icon + words — never colour alone, never a pill. */
export function StatusText({ tone, label, className = '' }: { tone: keyof typeof tones; label: string; className?: string }) {
  const { cls, Icon } = tones[tone]
  return (
    <span className={`inline-flex items-center gap-1.5 text-[13px] font-medium ${cls} ${className}`}>
      <Icon className="size-3.5 shrink-0" aria-hidden />{label}
    </span>
  )
}

export function RecoveryStatusText({ status }: { status: RecoveryStatus }) {
  const m = STATUS_META[status]
  return <StatusText tone={m.tone} label={m.label} />
}

export function RunStatusText({ status }: { status: RunStatus }) {
  if (status === 'NO_DEADLOCK') return <StatusText tone="neutral" label="No deadlock" />
  if (status === 'DEADLOCKED') {
    return (
      <span className="inline-flex items-center gap-1.5 text-[13px] font-medium text-terracotta-deep">
        <CircleDashed className="size-3.5" aria-hidden />Deadlocked
      </span>
    )
  }
  return <StatusText tone="ok" label="Compared" />
}

/** Strategy identity: a small square of its chart colour beside its name. */
export function StrategyMark({ strategy, full = false, className = '' }: { strategy: StrategyType; full?: boolean; className?: string }) {
  const s = STRATEGIES[strategy]
  return (
    <span className={`inline-flex items-center gap-2 ${className}`}>
      <span className="size-2 shrink-0 rounded-[1px]" style={{ background: s.color }} aria-hidden />
      {full ? s.label : s.short}
    </span>
  )
}
