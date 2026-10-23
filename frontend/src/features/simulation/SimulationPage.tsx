import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowRight, GitCompareArrows } from 'lucide-react'
import { api } from '../../api/endpoints'
import { useAsync } from '../../hooks/useAsync'
import { fmtDate } from '../../lib/format'
import { loadPreferences } from '../../lib/preferences'
import { PageIntro } from '../../components/layout/PageIntro'
import { Section } from '../../components/layout/Section'
import { Button } from '../../components/ui/Button'
import { ErrorNotice, InlineError, Loading, Skeleton } from '../../components/ui/Feedback'
import { RunStatusText } from '../../components/ui/Status'
import { Disclosure } from '../../components/ui/Disclosure'
import { Surface } from '../../components/ui/Surface'
import { WaitForGraph } from '../../components/graphs/WaitForGraph'

const EVENT_TONE: Record<string, string> = { BLOCK: 'text-terracotta-deep', GRANT: 'text-sage-deep', CHECKPOINT: 'text-gold-deep', DEFER: 'text-cocoa' }

export default function SimulationPage() {
  const runId = Number(useParams().id)
  const navigate = useNavigate()
  const run = useAsync(() => api.run(runId), [runId])
  const graph = useAsync(() => api.graph(runId), [runId])
  const state = useAsync(() => api.state(runId), [runId])
  const [comparing, setComparing] = useState(false)
  const [compareError, setCompareError] = useState<unknown>(null)

  if (run.loading) return <Loading label="Opening run" />
  if (run.error || !run.data) return <ErrorNotice error={run.error} onRetry={run.reload} />
  const r = run.data
  const d = r.detection

  const compare = async () => {
    setComparing(true); setCompareError(null)
    try {
      const prefs = loadPreferences()
      await api.compare(runId, { scoreWeights: prefs.scoreWeights, victimWeights: prefs.victimWeights })
      navigate(`/compare/${runId}`)
    } catch (e) { setCompareError(e); setComparing(false) }
  }

  const events = state.data?.events ?? []
  const procs = state.data ? Object.values(state.data.processes) : []

  return (
    <>
      <PageIntro trail={[{ label: 'Run log', to: '/history' }, { label: `Run ${r.id}` }]}
        title={r.scenarioName}
        lede={d.deadlocked
          ? <>The trace ended in deadlock: depth-first search found {d.cycles.length === 1 ? 'one cycle' : `${d.cycles.length} cycles`} involving {d.deadlockedProcesses.length} processes{d.blockedProcesses.length ? `, with ${d.blockedProcesses.length} more blocked behind them` : ''}. This frozen state is the shared starting point for every recovery strategy.</>
          : <>The trace ran to its end without a circular wait. There is nothing to recover; the snapshot is kept for the record.</>}
        actions={<>
          {d.deadlocked && (
            <Button size="lg" icon={<GitCompareArrows className="size-4" />} loading={comparing} onClick={compare}>
              {r.status === 'RECOVERY_EXECUTED' ? 'Re-run the comparison' : 'Compare recovery strategies'}
            </Button>)}
          {r.status === 'RECOVERY_EXECUTED' && <Link to={`/compare/${r.id}`} className="link inline-flex items-center gap-1 text-[15px] font-medium">Read the comparison <ArrowRight className="size-4" /></Link>}
        </>} />
      {compareError ? <div className="-mt-6 mb-8"><InlineError error={compareError} /></div> : null}

      <dl className="mb-12 grid grid-cols-2 gap-y-4 border-y border-rule py-5 text-[13px] sm:grid-cols-4">
        <div><dt className="text-cocoa">Status</dt><dd className="mt-1"><RunStatusText status={r.status} /></dd></div>
        <div><dt className="text-cocoa">Recorded</dt><dd className="mt-1 text-ink">{fmtDate(r.createdAt)}</dd></div>
        <div><dt className="text-cocoa">Configuration</dt><dd className="num mt-1 font-mono text-ink">{r.scenarioConfig.processes.length}P · {r.scenarioConfig.resources.length}R · {r.scenarioConfig.requestSequence.length} ops</dd></div>
        <div><dt className="text-cocoa">Snapshot SHA-256</dt><dd className="mt-1 truncate font-mono text-ink" title={r.snapshotFingerprint}>{r.snapshotFingerprint.slice(0, 16)}…</dd></div>
      </dl>

      <Section number="01" title="Wait-For Graph" note="An edge Pi → Pj labelled R: Pi waits for R, which Pj holds. Select a process or an edge for its explanation.">
        {graph.loading ? <Skeleton className="h-[520px]" /> : graph.error || !graph.data ? <ErrorNotice error={graph.error} onRetry={graph.reload} />
          : <WaitForGraph graph={graph.data} height={520} label={`Wait-For Graph of run ${r.id}`} />}
      </Section>

      <Section number="02" title="Detection" note="Three-colour DFS, O(V + E). One witness cycle per back edge; the deadlocked set is exact.">
        {d.cycles.length === 0 ? <p className="text-[15px] text-cocoa">No back edge was found, so the graph is acyclic.</p> : (
          <ol className="space-y-5">
            {d.cycles.map((c, i) => (
              <li key={i} className="grid gap-1 sm:grid-cols-[90px_minmax(0,1fr)]">
                <span className="font-mono text-[12px] text-terracotta-deep">cycle {i + 1}</span>
                <div>
                  <p className="font-mono text-[15px] text-ink">{[...c.processes, c.processes[0]].join(' → ')}</p>
                  <p className="mt-1 text-[13.5px] text-cocoa">{c.edges.map((e) => `${e.from} waits for ${e.resource} (held by ${e.to})`).join('; ')}.</p>
                </div>
              </li>
            ))}
          </ol>
        )}
        <dl className="mt-8 grid gap-6 border-t border-rule-soft pt-6 sm:grid-cols-4">
          {[
            ['Deadlocked set', d.deadlockedProcesses.join(' ') || '—'],
            ['Blocked behind it', d.blockedProcesses.join(' ') || '—'],
            ['Vertices / edges', `${d.vertexCount} / ${d.edgeCount}`],
            ['Adjacency entries examined', String(d.edgesExamined)],
          ].map(([k, v]) => <div key={k}><dt className="text-[12.5px] text-cocoa">{k}</dt><dd className="num mt-1 font-mono text-[14px] text-ink">{v}</dd></div>)}
        </dl>
      </Section>

      <Section number="03" title="Resources" note="Holder and FIFO wait queue of every resource in the snapshot.">
        {!graph.data ? <Skeleton className="h-32" /> : (
          <Surface className="overflow-x-auto">
            <table className="w-full min-w-[420px] text-left text-[13.5px]">
              <thead className="text-cocoa"><tr className="border-b border-rule"><th className="pb-2 font-normal">Resource</th><th className="pb-2 font-normal">Held by</th><th className="pb-2 font-normal">Waiting, in order</th></tr></thead>
              <tbody className="font-mono">
                {graph.data.resources.map((res) => (
                  <tr key={res.id} className="border-b border-rule-soft">
                    <td className="py-2 text-ink">{res.id}</td>
                    <td className="text-ink">{res.holder ?? <span className="text-cocoa">free</span>}</td>
                    <td className={res.waitQueue.length ? 'text-terracotta-deep' : 'text-cocoa'}>{res.waitQueue.join(' ← ') || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Surface>
        )}
      </Section>

      <Section number="04" title="Processes" note="Work counters, checkpoints (number : work @ held resources) and operations deferred while blocked.">
        {state.loading ? <Skeleton className="h-40" /> : state.error ? <ErrorNotice error={state.error} onRetry={state.reload} /> : (
          <Surface className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-left text-[13px]">
              <thead className="text-cocoa">
                <tr className="border-b border-rule">
                  {['Process', 'State', 'Priority', 'Work', 'Checkpoints', 'Holds', 'Waits for', 'Victims', 'Deferred'].map((h) => <th key={h} className="pb-2 pr-3 font-normal">{h}</th>)}
                </tr>
              </thead>
              <tbody className="font-mono">
                {procs.map((p) => (
                  <tr key={p.id} className="border-b border-rule-soft align-top">
                    <td className="py-2 pr-3 font-semibold text-ink">{p.id}{d.deadlockedProcesses.includes(p.id) && <span className="ml-1.5 text-terracotta" aria-label="deadlocked">●</span>}</td>
                    <td className="pr-3 font-sans text-cocoa">{p.state.toLowerCase()}</td>
                    <td className="num pr-3">{p.priority}</td>
                    <td className="num pr-3">{p.workDone}/{p.totalWork}</td>
                    <td className="pr-3 text-cocoa">{p.checkpoints.map((c) => `#${c.sequence}:${c.workDone}@[${c.heldResources.join(',')}]`).join(' ')}</td>
                    <td className="pr-3">{p.heldResources.join(' ') || '—'}</td>
                    <td className={`pr-3 ${p.waitingFor.length ? 'text-terracotta-deep' : 'text-cocoa'}`}>{p.waitingFor.join(' ') || '—'}</td>
                    <td className="num pr-3">{p.victimCount}</td>
                    <td className="num text-cocoa">{p.deferredOperations.length || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Surface>
        )}
      </Section>

      <Section number="05" title="Trace" note={`${events.length} events in logical-clock order.`}>
        {state.loading ? <Skeleton className="h-40" /> : (
          <>
            <ol className="font-mono text-[12.5px]">
              {events.slice(-12).map((e, i) => (
                <li key={i} className="grid grid-cols-[48px_96px_minmax(0,1fr)] gap-3 border-b border-rule-soft py-1.5">
                  <span className="num text-right text-cocoa">t{e.step}</span><span className={EVENT_TONE[e.type] ?? 'text-ink'}>{e.type}</span><span className="text-cocoa">{e.message}</span>
                </li>
              ))}
            </ol>
            {events.length > 12 && (
              <Disclosure summary={`All ${events.length} events`} className="mt-2">
                <ol className="max-h-[420px] overflow-auto font-mono text-[12.5px]">
                  {events.map((e, i) => (
                    <li key={i} className="grid grid-cols-[48px_96px_minmax(0,1fr)] gap-3 border-b border-rule-soft py-1.5">
                      <span className="num text-right text-cocoa">t{e.step}</span><span className={EVENT_TONE[e.type] ?? 'text-ink'}>{e.type}</span><span className="text-cocoa">{e.message}</span>
                    </li>
                  ))}
                </ol>
              </Disclosure>
            )}
          </>
        )}
      </Section>
    </>
  )
}
