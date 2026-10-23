import type { RecoveryResult } from '../../api/types'
import { STRATEGIES } from '../../lib/strategy'
import { fmtMicros, fmtScore } from '../../lib/format'
import { RecoveryStatusText } from '../../components/ui/Status'
import { Disclosure } from '../../components/ui/Disclosure'
import { Reveal } from '../../components/motion/Reveal'

function facts(r: RecoveryResult): [string, string][] {
  const m = r.metrics
  const actions = r.rounds.flatMap((x) => x.actions)
  const victims = [...new Set(actions.map((a) => a.processId))].join(', ') || '—'
  switch (r.strategy) {
    case 'TERMINATION': return [['Victims, in order', victims], ['Terminated', String(m.processesTerminated)], ['Work lost', `${m.workLost} units`], ['Rounds', String(m.recoverySteps)]]
    case 'ROLLBACK': return [['Victims, in order', victims], ['Checkpoints restored', actions.map((a) => `#${a.restoredCheckpoint} @ ${a.workAfter}`).join(', ') || '—'],
      ['Rollbacks', `${m.processesRolledBack} (${m.distinctProcessesRolledBack} proc.)`], ['Work lost', `${m.workLost} units`], ['Resources reclaimed', String(m.resourcesReclaimed)]]
    default: return [['Deadlocked set aborted', victims], ['Work lost', `${m.workLost} units`], ['Resources reclaimed', String(m.resourcesReclaimed)], ['Rounds', String(m.recoverySteps)]]
  }
}

/** One strategy as a numbered chapter: what it did, what it cost, whether re-detection confirms it. */
export function StrategyChapter({ result }: { result: RecoveryResult }) {
  const s = STRATEGIES[result.strategy]
  const v = result.verification
  return (
    <Reveal as="article" className="border-t border-rule py-10">
      <div className="grid gap-8 lg:grid-cols-[200px_minmax(0,1fr)] lg:gap-12">
        <header>
          <p className="display text-[44px] leading-none text-ink">{s.numeral}</p>
          <h3 className="mt-3 flex items-center gap-2 text-[17px] font-medium"><span className="size-2.5 rounded-[1px]" style={{ background: s.color }} aria-hidden />{s.label}</h3>
          <p className="mt-2 text-[13px] text-cocoa">{result.rank ? `Ranked ${result.rank} of 3` : 'Unranked'} · cost <span className="num font-mono text-ink">{fmtScore(result.metrics.aggregateScore)}</span></p>
        </header>
        <div className="min-w-0">
          <p className="max-w-[620px] text-[15px] leading-relaxed text-cocoa">{s.blurb}</p>
          <dl className="mt-6 grid gap-x-8 gap-y-4 sm:grid-cols-2 xl:grid-cols-3">
            {facts(result).map(([k, val]) => (
              <div key={k} className="border-l border-rule pl-4">
                <dt className="text-[12px] text-cocoa">{k}</dt>
                <dd className="num mt-1 font-mono text-[14.5px] text-ink">{val}</dd>
              </div>
            ))}
            <div className="border-l border-rule pl-4">
              <dt className="text-[12px] text-cocoa">Starvation risk</dt>
              <dd className="num mt-1 font-mono text-[14.5px] text-ink">{result.metrics.starvationRisk}</dd>
            </div>
          </dl>
          <div className={`mt-7 border-l-2 py-1 pl-4 ${v.deadlockFree && v.consistent ? 'border-sage' : 'border-danger'}`}>
            <RecoveryStatusText status={result.status} />
            <p className="mt-1 text-[13.5px] text-cocoa">
              {v.deadlockFree ? 'The rebuilt Wait-For Graph contains no cycle' : `${v.remainingCycles.length} cycle(s) remain after recovery`}
              {v.consistent ? '; ownership and wait-queue invariants hold.' : `; ${v.violations.length} invariant violation(s).`}
              <span className="text-cocoa"> Final graph {v.graphVertices} V / {v.graphEdges} E · {result.haltReason.replaceAll('_', ' ').toLowerCase()} · {fmtMicros(result.executionMicros)}</span>
            </p>
            {v.remainingCycles.map((c, i) => <p key={i} className="mt-1 font-mono text-[13px] text-danger">{[...c, c[0]].join(' → ')}</p>)}
            {v.violations.map((x) => <p key={x} className="mt-1 text-[12.5px] text-danger">{x}</p>)}
          </div>
          <Disclosure className="mt-4 border-t border-rule-soft" summary={`Round-by-round evidence · ${result.rounds.length} round${result.rounds.length === 1 ? '' : 's'}`}>
            {result.rounds.length === 0 ? <p className="text-[13px] text-cocoa">No action was required.</p> : (
              <ol className="space-y-6">
                {result.rounds.map((r) => (
                  <li key={r.round} className="grid gap-2 text-[13px] sm:grid-cols-[72px_minmax(0,1fr)]">
                    <span className="font-mono text-[12px] text-terracotta-deep">round {r.round}</span>
                    <div className="min-w-0">
                      <p className="font-mono text-[12px] text-cocoa">before · {r.cyclesBefore.map((c) => c.join('→')).join('  |  ') || 'no cycle'}</p>
                      {r.candidates.length > 0 && (
                        <div className="mt-2 overflow-x-auto">
                          <table className="w-full min-w-[460px] font-mono text-[11.5px]">
                            <thead className="text-cocoa"><tr className="border-b border-rule-soft">
                              {['candidate', 'at risk', 'work', 'priority', 'resources', 'history', 'cost'].map((h, i) => <th key={h} className={`pb-1 font-normal ${i ? 'text-right' : 'text-left'}`}>{h}</th>)}
                            </tr></thead>
                            <tbody>
                              {r.candidates.map((c, i) => (
                                <tr key={c.processId} title={c.option} className={i === 0 ? 'text-ink' : 'text-cocoa'}>
                                  <td className="py-0.5">{c.processId}{i === 0 && <span className="ml-1 text-terracotta-deep">← chosen</span>}</td>
                                  <td className="num text-right">{c.workAtRisk}</td><td className="num text-right">{c.workTerm.toFixed(2)}</td>
                                  <td className="num text-right">{c.priorityTerm.toFixed(2)}</td><td className="num text-right">{c.resourceTerm.toFixed(2)}</td>
                                  <td className="num text-right">{c.historyTerm.toFixed(2)}</td><td className="num text-right">{c.cost.toFixed(3)}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      )}
                      {r.actions.map((a, i) => <p key={i} className="mt-2 leading-relaxed text-ink">{a.explanation} <span className="font-mono text-[11.5px] text-cocoa">[{a.handovers.join(', ')}]</span></p>)}
                      <p className={`mt-1.5 font-mono text-[12px] ${r.cyclesAfter.length ? 'text-gold-deep' : 'text-sage-deep'}`}>
                        after re-detection · {r.cyclesAfter.map((c) => c.join('→')).join('  |  ') || 'no cycle'}
                      </p>
                    </div>
                  </li>
                ))}
              </ol>
            )}
          </Disclosure>
        </div>
      </div>
    </Reveal>
  )
}
