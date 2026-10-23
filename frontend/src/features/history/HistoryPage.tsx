import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Check, Download, Minus, Search, TriangleAlert, X } from 'lucide-react'
import { api, type HistoryQuery } from '../../api/endpoints'
import type { HistoryItem, RunStatus, StrategySummary, StrategyType } from '../../api/types'
import { useAsync } from '../../hooks/useAsync'
import { fmtScore } from '../../lib/format'
import { STRATEGIES, STRATEGY_ORDER } from '../../lib/strategy'
import { PageIntro } from '../../components/layout/PageIntro'
import { Button } from '../../components/ui/Button'
import { inlineControl } from '../../components/ui/Field'
import { EmptyState, ErrorNotice, InlineError, Skeleton } from '../../components/ui/Feedback'
import { RunStatusText, StrategyMark } from '../../components/ui/Status'
import { Reveal } from '../../components/motion/Reveal'

function Outcome({ s }: { s?: StrategySummary }) {
  if (!s) return <span className="text-[13px] text-cocoa">—</span>
  const icon = s.status === 'RECOVERED' ? <Check className="size-3.5 text-sage-deep" aria-label="verified" />
    : s.status === 'NOT_REQUIRED' ? <Minus className="size-3.5 text-cocoa" aria-label="not required" />
      : s.status === 'PARTIAL' ? <TriangleAlert className="size-3.5 text-gold-deep" aria-label="partial" />
        : <X className="size-3.5 text-danger" aria-label="failed" />
  return (
    <span className="inline-flex items-center gap-1.5 font-mono text-[13px]"
      title={`${s.status.toLowerCase()} · work lost ${s.workLost} · terminated ${s.processesTerminated} · rollbacks ${s.processesRolledBack} · rounds ${s.recoverySteps}`}>
      {icon}<span className={`num ${s.rank === 1 ? 'font-semibold text-ink' : 'text-cocoa'}`}>{fmtScore(s.aggregateScore)}</span>
    </span>
  )
}

function dayKey(iso: string) {
  return new Date(iso).toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
}

export default function HistoryPage() {
  const [q, setQ] = useState('')
  const [debounced, setDebounced] = useState('')
  const [deadlock, setDeadlock] = useState<'all' | 'yes' | 'no'>('all')
  const [status, setStatus] = useState<RunStatus | ''>('')
  const [recommended, setRecommended] = useState<StrategyType | ''>('')
  const [exportError, setExportError] = useState<unknown>(null)
  useEffect(() => { const t = setTimeout(() => setDebounced(q.trim()), 250); return () => clearTimeout(t) }, [q])

  const query: HistoryQuery = { q: debounced || undefined, deadlocked: deadlock === 'all' ? undefined : deadlock === 'yes', status: status || undefined, recommended: recommended || undefined }
  const key = JSON.stringify(query)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const { data, error, loading, reload } = useAsync(() => api.history(query), [key])
  const filtered = !!(debounced || deadlock !== 'all' || status || recommended)

  const days = useMemo(() => {
    const m = new Map<string, HistoryItem[]>()
    data?.forEach((h) => { const k = dayKey(h.createdAt); m.set(k, [...(m.get(k) ?? []), h]) })
    return [...m.entries()]
  }, [data])

  return (
    <>
      <PageIntro trail={[{ label: 'Records' }, { label: 'Run log' }]} title="Run log"
        lede="Every simulation you have run, as recorded: what was detected, and how each recovery strategy fared when the run was compared."
        actions={<Button variant="secondary" icon={<Download className="size-4" />}
          onClick={() => { setExportError(null); api.downloadCsv('/history/csv', query as Record<string, unknown>, 'simulation-history.csv').catch(setExportError) }}>Export CSV</Button>} />
      {exportError ? <div className="-mt-6 mb-8"><InlineError error={exportError} /></div> : null}

      <div className="mb-10 flex flex-col gap-4 border-y border-rule py-4 md:flex-row md:items-center">
        <label className="relative flex-1">
          <span className="sr-only">Search by scenario name</span>
          <Search className="pointer-events-none absolute left-0 top-1/2 size-4 -translate-y-1/2 text-cocoa" />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search scenario names"
            className="h-9 w-full border-0 border-b border-transparent bg-transparent pl-7 text-[15px] placeholder:text-taupe focus:border-ink focus:outline-none" />
        </label>
        <div className="flex flex-wrap gap-2">
          <select aria-label="Deadlock" className={inlineControl} value={deadlock} onChange={(e) => setDeadlock(e.target.value as typeof deadlock)}>
            <option value="all">Any outcome</option><option value="yes">Deadlocked</option><option value="no">No deadlock</option>
          </select>
          <select aria-label="Run status" className={inlineControl} value={status} onChange={(e) => setStatus(e.target.value as RunStatus | '')}>
            <option value="">Any status</option><option value="DEADLOCKED">Not yet compared</option><option value="RECOVERY_EXECUTED">Compared</option><option value="NO_DEADLOCK">No deadlock</option>
          </select>
          <select aria-label="Recommended strategy" className={inlineControl} value={recommended} onChange={(e) => setRecommended(e.target.value as StrategyType | '')}>
            <option value="">Any recommendation</option>
            {STRATEGY_ORDER.map((s) => <option key={s} value={s}>{STRATEGIES[s].label}</option>)}
          </select>
        </div>
      </div>

      {loading && !data ? <div className="space-y-4">{[0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-14" />)}</div>
        : error ? <ErrorNotice error={error} onRetry={reload} />
          : !data?.length ? (
            <EmptyState title={filtered ? 'Nothing matches.' : 'The log is empty.'}
              body={filtered ? 'No recorded run fits these filters. Try another search term or clear them.' : 'Run a scenario and it will be recorded here, with its detection result and any recovery comparison.'}
              action={filtered
                ? <Button variant="secondary" onClick={() => { setQ(''); setDeadlock('all'); setStatus(''); setRecommended('') }}>Clear filters</Button>
                : <Link to="/scenarios" className="link font-medium">Go to scenarios</Link>} />
          ) : (
            <div className={loading ? 'opacity-60 transition-opacity' : ''}>
              <div className="mb-2 hidden grid-cols-[70px_minmax(0,1fr)_120px_repeat(3,96px)_130px_92px] gap-4 text-[11.5px] text-cocoa lg:grid">
                <span /><span>Scenario</span><span>Detection</span>
                {STRATEGY_ORDER.map((s) => <span key={s}><StrategyMark strategy={s} /></span>)}
                <span>Lowest cost</span><span />
              </div>
              {days.map(([day, items]) => (
                <Reveal as="section" key={day} className="mb-10">
                  <h2 className="display border-b border-ink/70 pb-2 text-[19px]">{day}</h2>
                  <ol>
                    {items.map((h) => (
                      <li key={h.runId} className="grid gap-x-4 gap-y-2 border-b border-rule-soft py-4 lg:grid-cols-[70px_minmax(0,1fr)_120px_repeat(3,96px)_130px_92px] lg:items-baseline">
                        <span className="font-mono text-[12.5px] text-cocoa">{new Date(h.createdAt).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}</span>
                        <span className="min-w-0">
                          <Link to={`/simulations/${h.runId}`} className="block truncate text-[15px] font-medium hover:text-terracotta-deep">{h.scenarioName}</Link>
                          <span className="font-mono text-[11.5px] text-cocoa">run {h.runId} · {h.cycleCount} cycle{h.cycleCount === 1 ? '' : 's'} · {h.deadlockedProcessCount} deadlocked{h.scenarioId == null ? ' · scenario deleted' : ''}</span>
                        </span>
                        <span><RunStatusText status={h.status} /></span>
                        {STRATEGY_ORDER.map((s) => (
                          <span key={s} className="flex items-center gap-2">
                            <span className="text-[12px] text-cocoa lg:hidden">{STRATEGIES[s].short}</span>
                            <Outcome s={h.results.find((r) => r.strategy === s)} />
                          </span>
                        ))}
                        <span className="text-[13px]">{h.recommendedStrategy ? <StrategyMark strategy={h.recommendedStrategy} /> : <span className="text-cocoa">—</span>}</span>
                        <span className="text-[13px]">{h.results.length > 0 ? <Link to={`/compare/${h.runId}`} className="link font-medium">Comparison</Link> : <Link to={`/simulations/${h.runId}`} className="link">Open run</Link>}</span>
                      </li>
                    ))}
                  </ol>
                </Reveal>
              ))}
              <p className="text-[12.5px] text-cocoa">{data.length} run{data.length === 1 ? '' : 's'}. Strategy cells show aggregate cost (lower is better, first place in bold) with verification: ✓ verified, ! partial, × failed. Hover for the underlying counts.</p>
            </div>
          )}
    </>
  )
}
