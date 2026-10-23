import { Link } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import type { HistoryItem } from '../../api/types'
import { RunStatusText, StrategyMark } from '../../components/ui/Status'

/** Editorial list of recent runs separated by hairlines. */
export function RecentRuns({ runs }: { runs: HistoryItem[] }) {
  return (
    <ol className="divide-y divide-rule-soft">
      {runs.map((r) => {
        const to = r.status === 'RECOVERY_EXECUTED' ? `/compare/${r.runId}` : `/simulations/${r.runId}`
        const d = new Date(r.createdAt)
        return (
          <li key={r.runId}>
            <Link to={to} className="group grid grid-cols-[64px_minmax(0,1fr)_auto] items-baseline gap-x-5 gap-y-1 py-4 sm:grid-cols-[84px_minmax(0,1fr)_150px_170px_20px]">
              <span className="font-mono text-[12px] text-cocoa">{d.toLocaleDateString(undefined, { day: '2-digit', month: 'short' })}<br className="sm:hidden" /><span className="hidden sm:inline"> · </span>{d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}</span>
              <span className="min-w-0">
                <span className="block truncate text-[15.5px] font-medium text-ink group-hover:text-terracotta-deep">{r.scenarioName}</span>
                <span className="font-mono text-[11.5px] text-cocoa">run {r.runId} · {r.cycleCount} cycle{r.cycleCount === 1 ? '' : 's'} · {r.deadlockedProcessCount} deadlocked</span>
              </span>
              <span className="col-start-2 sm:col-start-auto"><RunStatusText status={r.status} /></span>
              <span className="col-start-2 text-[13px] text-cocoa sm:col-start-auto">
                {r.recommendedStrategy ? <StrategyMark strategy={r.recommendedStrategy} /> : '—'}
              </span>
              <ArrowRight className="hidden size-4 text-cocoa transition-transform group-hover:translate-x-0.5 group-hover:text-terracotta-deep sm:block" aria-hidden />
            </Link>
          </li>
        )
      })}
    </ol>
  )
}
