import { Link } from 'react-router-dom'
import { api } from '../../api/endpoints'
import { useAsync } from '../../hooks/useAsync'
import { WaitForGraph } from '../../components/graphs/WaitForGraph'
import { ErrorNotice, Skeleton } from '../../components/ui/Feedback'
import { fmtDate } from '../../lib/format'

/** Live WFG of the latest deadlocked run plus a prose account generated from its detection result. */
export function LatestDeadlock({ runId }: { runId: number }) {
  const run = useAsync(() => api.run(runId), [runId])
  const graph = useAsync(() => api.graph(runId), [runId])
  if (run.loading || graph.loading) return <Skeleton className="h-[420px]" />
  if (run.error || graph.error || !run.data || !graph.data) return <ErrorNotice error={run.error ?? graph.error} onRetry={() => { run.reload(); graph.reload() }} />
  const r = run.data
  const d = r.detection
  const c = d.cycles[0]
  return (
    <div className="grid gap-8 xl:grid-cols-[minmax(0,1fr)_260px]">
      <WaitForGraph graph={graph.data} height={420} label={`Wait-For Graph of run ${r.id}`} />
      <div className="text-[14.5px] leading-[1.7] text-cocoa">
        <p className="font-mono text-[11px] uppercase tracking-[0.12em] text-cocoa">Run {r.id} · {fmtDate(r.createdAt)}</p>
        <p className="display mt-2 text-[22px] leading-snug text-ink">{r.scenarioName}</p>
        {c && (
          <p className="mt-4">
            Depth-first search found {d.cycles.length === 1 ? 'one cycle' : `${d.cycles.length} cycles`}. In the first,{' '}
            {c.edges.slice(0, 3).map((e, i) => (
              <span key={i}>{i > 0 && (i === Math.min(c.edges.length, 3) - 1 ? ' and ' : ', ')}<span className="font-mono text-ink">{e.from}</span> waits for <span className="font-mono text-terracotta-deep">{e.resource}</span>, held by <span className="font-mono text-ink">{e.to}</span></span>
            ))}{c.edges.length > 3 ? ', and so on around the cycle.' : '.'}
          </p>
        )}
        <p className="mt-3">
          <span className="text-ink">{d.deadlockedProcesses.length}</span> processes are deadlocked
          {d.blockedProcesses.length > 0 && <>; <span className="text-ink">{d.blockedProcesses.length}</span> more wait behind them without being part of a cycle</>}.
        </p>
        <div className="mt-6 flex flex-col gap-2 text-[14px]">
          <Link to={`/simulations/${r.id}`} className="link font-medium">Investigate this run</Link>
          {r.status === 'RECOVERY_EXECUTED'
            ? <Link to={`/compare/${r.id}`} className="link">Read its recovery comparison</Link>
            : <span className="text-cocoa">Not yet healed — compare strategies from the run page.</span>}
        </div>
      </div>
    </div>
  )
}
