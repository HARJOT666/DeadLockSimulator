import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import { Check, Download, RotateCcw, SlidersHorizontal, X } from 'lucide-react'
import { api } from '../../api/endpoints'
import type { Comparison, RecoveryResult, ScoreWeights, StrategyType, VictimWeights } from '../../api/types'
import { useAsync } from '../../hooks/useAsync'
import { METRIC_LABELS, STRATEGIES, STRATEGY_ORDER } from '../../lib/strategy'
import { fmtScore } from '../../lib/format'
import { DEFAULT_SCORE_WEIGHTS, DEFAULT_VICTIM_WEIGHTS, loadPreferences } from '../../lib/preferences'
import { PageIntro } from '../../components/layout/PageIntro'
import { Section } from '../../components/layout/Section'
import { Button } from '../../components/ui/Button'
import { EmptyState, ErrorNotice, InlineError, Loading, Skeleton } from '../../components/ui/Feedback'
import { StrategyMark } from '../../components/ui/Status'
import { Surface } from '../../components/ui/Surface'
import { Reveal } from '../../components/motion/Reveal'
import { MetricProfileChart, StrategyValueBars } from '../../components/charts/StrategyCharts'
import { WaitForGraph } from '../../components/graphs/WaitForGraph'
import { StrategyChapter } from './StrategyChapter'
import { ScoreBreakdown } from './ScoreBreakdown'
import { scoreSum, victimSum, WeightsEditor } from './WeightsEditor'

const METRIC_ROWS = ['workLost', 'processesTerminated', 'processesRolledBack', 'resourcesReclaimed', 'recoverySteps', 'starvationRisk', 'aggregateScore'] as const

function IsolationStrip({ c }: { c: NonNullable<Comparison['summary']> }) {
  const item = (ok: boolean, text: string) => (
    <li className={`flex items-center gap-1.5 ${ok ? 'text-sage-deep' : 'text-danger'}`}>{ok ? <Check className="size-3.5" /> : <X className="size-3.5" />}{text}</li>
  )
  return (
    <div className="mb-14 flex flex-col gap-3 border-y border-rule py-3 font-mono text-[11.5px] md:flex-row md:items-center md:justify-between">
      <span className="text-cocoa">state isolation · snapshot {c.snapshotFingerprint.slice(0, 12)}…</span>
      <ul className="flex flex-wrap gap-x-6 gap-y-1" aria-label="Isolation checks">
        {item(c.persistedSnapshotIntact, 'stored snapshot matches fingerprint')}
        {item(c.copiesIdentical, '3 copies identical before recovery')}
        {item(c.snapshotUnchanged, 'snapshot untouched after recovery')}
      </ul>
    </div>
  )
}

function Findings({ c, results }: { c: NonNullable<Comparison['summary']>; results: RecoveryResult[] }) {
  const rec = c.recommendation
  const failed = results.filter((r) => r.status === 'FAILED' || r.status === 'PARTIAL')
  const best = rec.strategy ? results.find((r) => r.strategy === rec.strategy) : undefined
  return (
    <Reveal className="mb-16 grid gap-12 lg:grid-cols-[minmax(0,1.35fr)_minmax(0,1fr)]">
      <div className="border-l-2 border-gold pl-6">
        <p className="kicker">Finding · under the current weights</p>
        {best ? (
          <>
            <p className="display mt-4 text-[40px] leading-tight sm:text-[52px]">{STRATEGIES[best.strategy].label}</p>
            <p className="mt-2 text-[15px] text-cocoa">ranks first with an aggregate cost of <span className="num font-mono text-ink">{fmtScore(best.metrics.aggregateScore)}</span>.</p>
          </>
        ) : <p className="display mt-4 text-[32px]">{rec.headline}</p>}
        {rec.tradeoffs.length > 0 && (
          <ul className="mt-7 space-y-3 text-[14.5px] leading-relaxed text-ink">
            {rec.tradeoffs.map((t) => <li key={t} className="grid grid-cols-[14px_minmax(0,1fr)] gap-2"><span className="text-gold-deep">—</span>{t}</li>)}
          </ul>
        )}
        <p className="mt-7 max-w-[620px] text-[13.5px] leading-relaxed text-cocoa">
          {rec.weightSensitive && <><span className="font-medium text-gold-deep">Weight-sensitive. </span>Another weighting below picks a different strategy. </>}
          {rec.caveat}
        </p>
        {failed.length > 0 && (
          <p className="mt-4 text-[14px] text-danger">
            {failed.map((f) => STRATEGIES[f.strategy].label).join(' and ')} did not reach a verified deadlock-free state and {failed.length === 1 ? 'is' : 'are'} ranked last regardless of cost.
          </p>
        )}
      </div>
      <ol className="space-y-6 self-end" aria-label="Ranking">
        {c.ranking.map((r) => (
          <li key={r.strategy} className="grid grid-cols-[34px_minmax(0,1fr)_auto] items-baseline gap-x-3 border-b border-rule pb-4">
            <span className="display text-[26px] text-cocoa">{r.rank}</span>
            <span className="text-[15px] font-medium"><StrategyMark strategy={r.strategy} full /></span>
            <span className="num font-mono text-[15px]">{fmtScore(r.score)}</span>
            <p className="col-start-2 col-end-4 mt-1 text-[12.5px] leading-snug text-cocoa">{r.explanation}</p>
          </li>
        ))}
      </ol>
    </Reveal>
  )
}

function MetricsTable({ results }: { results: RecoveryResult[] }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[640px] text-left text-[14px]">
        <thead>
          <tr className="border-b border-ink/70 text-[13px]">
            <th className="sticky left-0 bg-sheet pb-2.5 font-normal text-cocoa">Metric</th>
            {results.map((r) => <th key={r.strategy} className="pb-2.5 text-right font-medium"><StrategyMark strategy={r.strategy} className="justify-end" /></th>)}
            <th className="pb-2.5 pl-8 font-normal text-cocoa">Unit</th>
          </tr>
        </thead>
        <tbody>
          {METRIC_ROWS.map((k) => {
            const vals = results.map((r) => r.metrics[k] as number)
            const best = Math.min(...vals)
            const differ = vals.some((v) => v !== best)
            return (
              <tr key={k} className={`border-b ${k === 'aggregateScore' ? 'border-ink/70 font-medium' : 'border-rule-soft'}`}>
                <td className="sticky left-0 bg-sheet py-3 pr-4 text-ink" title={METRIC_LABELS[k].help}>{METRIC_LABELS[k].label}</td>
                {vals.map((v, i) => (
                  <td key={i} className={`num text-right font-mono ${differ && v === best ? 'text-sage-deep' : 'text-ink'}`}>
                    {k === 'aggregateScore' ? fmtScore(v) : v}{differ && v === best && <span className="sr-only"> (lowest)</span>}
                  </td>
                ))}
                <td className="pl-8 text-[12.5px] text-cocoa">{METRIC_LABELS[k].unit}</td>
              </tr>
            )
          })}
          <tr>
            <td className="sticky left-0 bg-sheet py-3 pr-4 text-ink">Deadlock after recovery</td>
            {results.map((r) => (
              <td key={r.strategy} className={`text-right text-[13px] font-medium ${r.verification.deadlockFree ? 'text-sage-deep' : 'text-danger'}`}>
                {r.verification.deadlockFree ? 'none — verified' : `${r.verification.remainingCycles.length} cycle(s)`}
              </td>
            ))}
            <td className="pl-8 text-[12.5px] text-cocoa">rebuilt WFG + DFS</td>
          </tr>
        </tbody>
      </table>
      <p className="mt-3 text-[12.5px] text-cocoa">Sage marks the lowest value in a row where values differ; for every metric, lower means less disruption.</p>
    </div>
  )
}

function BeforeAfter({ runId }: { runId: number }) {
  const [view, setView] = useState<StrategyType>('TERMINATION')
  const before = useAsync(() => api.graph(runId), [runId])
  const after = useAsync(() => api.graph(runId, view), [runId, view])
  return (
    <div>
      <div role="tablist" aria-label="Strategy shown after recovery" className="mb-5 flex flex-wrap gap-6 border-b border-rule text-[14px]">
        {STRATEGY_ORDER.map((s) => (
          <button key={s} role="tab" aria-selected={view === s} onClick={() => setView(s)}
            className={`-mb-px border-b-2 pb-2.5 cursor-pointer ${view === s ? 'border-terracotta font-medium text-ink' : 'border-transparent text-cocoa hover:text-ink'}`}>
            <StrategyMark strategy={s} full />
          </button>
        ))}
      </div>
      <div className="grid gap-6 xl:grid-cols-2">
        <figure>
          {before.loading ? <Skeleton className="h-[380px]" /> : before.error || !before.data ? <ErrorNotice error={before.error} />
            : <WaitForGraph graph={before.data} height={380} legend={false} label="Snapshot before recovery" />}
          <figcaption className="mt-2 text-[12.5px] text-cocoa">Before — the shared deadlocked snapshot.</figcaption>
        </figure>
        <figure>
          {after.loading ? <Skeleton className="h-[380px]" /> : after.error || !after.data ? <ErrorNotice error={after.error} onRetry={after.reload} />
            : <WaitForGraph graph={after.data} layoutGraph={before.data ?? undefined} height={380} legend={false} label={`State after ${STRATEGIES[view].label}`} />}
          <figcaption className="mt-2 text-[12.5px] text-cocoa">After {STRATEGIES[view].label.toLowerCase()} — rebuilt from its final state.</figcaption>
        </figure>
      </div>
    </div>
  )
}

export default function ComparisonPage() {
  const runId = Number(useParams().id)
  const cmp = useAsync(() => api.comparison(runId), [runId])
  const run = useAsync(() => api.run(runId), [runId])
  const [busy, setBusy] = useState(false)
  const [actionError, setActionError] = useState<unknown>(null)
  const [weightsOpen, setWeightsOpen] = useState(false)
  const [draft, setDraft] = useState<{ s: ScoreWeights; v: VictimWeights; m: number | '' } | null>(null)

  const rerun = async (s?: ScoreWeights, v?: VictimWeights, m?: number) => {
    setBusy(true); setActionError(null)
    try {
      const prefs = loadPreferences()
      cmp.setData(await api.compare(runId, { scoreWeights: s ?? prefs.scoreWeights, victimWeights: v ?? prefs.victimWeights, maxRounds: m }))
      setWeightsOpen(false)
    } catch (e) { setActionError(e) } finally { setBusy(false) }
  }

  if (cmp.loading) return <Loading label="Opening comparison" />
  if (cmp.error || !cmp.data) return <ErrorNotice error={cmp.error} onRetry={cmp.reload} />
  const { summary, results, scenarioName } = cmp.data
  const trail = [{ label: 'Run log', to: '/history' }, { label: `Run ${runId}`, to: `/simulations/${runId}` }, { label: 'Comparison' }]

  if (!summary || results.length < 3) {
    return (
      <>
        <PageIntro trail={trail} title="No complete comparison yet" lede={`${scenarioName}: run all three strategies on independent copies of this run’s snapshot.`} />
        <EmptyState title="Nothing has been compared." body="The comparison runs termination, rollback and total abort on three deep copies of the same deadlocked state, then verifies each by re-detection."
          action={<Button loading={busy} onClick={() => rerun()}>Compare the three strategies</Button>} />
        {actionError ? <InlineError error={actionError} /> : null}
      </>
    )
  }

  const ordered = STRATEGY_ORDER.map((s) => results.find((r) => r.strategy === s)!).filter(Boolean)
  const byRank = [...results].sort((a, b) => (a.rank ?? 9) - (b.rank ?? 9))
  const det = run.data?.detection
  const w = draft ?? { s: summary.weights, v: summary.victimWeights, m: '' as const }

  return (
    <>
      <PageIntro trail={trail} title={<>How should this deadlock <span className="display-italic">be healed?</span></>}
        lede={<>
          <span className="text-ink">{scenarioName}</span>
          {det && det.cycles[0] ? <> — {det.cycles.length === 1 ? 'a cycle' : `${det.cycles.length} cycles`} through <span className="font-mono text-ink">{det.deadlockedProcesses.join(' ')}</span>{det.blockedProcesses.length ? <>, with <span className="font-mono text-ink">{det.blockedProcesses.join(' ')}</span> blocked behind</> : null}.</> : '.'}{' '}
          Three strategies were applied to three identical copies of the frozen state; each result was then checked by re-running detection.
        </>}
        actions={<>
          <Button variant="secondary" icon={<SlidersHorizontal className="size-4" />} aria-expanded={weightsOpen} onClick={() => setWeightsOpen((o) => !o)}>Adjust weights</Button>
          <Button variant="quiet" icon={<Download className="size-4" />} onClick={() => api.downloadCsv(`/metrics/${runId}/csv`, {}, `run-${runId}-metrics.csv`).catch(setActionError)}>Export CSV</Button>
          <Link to={`/simulations/${runId}`} className="link text-[14px]">View the simulation</Link>
        </>} />
      {actionError ? <div className="-mt-6 mb-8"><InlineError error={actionError} /></div> : null}

      <AnimatePresence initial={false}>
        {weightsOpen && (
          <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} transition={{ duration: 0.25 }} className="overflow-hidden">
            <div className="mb-12 rounded-[6px] border border-rule bg-sheet p-6">
              <WeightsEditor score={w.s} victim={w.v} maxRounds={w.m} onChange={(s, v, m) => setDraft({ s, v, m })} />
              <div className="mt-6 flex flex-wrap items-center gap-3 border-t border-rule-soft pt-5">
                <Button loading={busy} disabled={scoreSum(w.s) <= 0 || victimSum(w.v) <= 0} onClick={() => rerun(w.s, w.v, w.m === '' ? undefined : w.m)}>Re-run with these weights</Button>
                <Button variant="quiet" icon={<RotateCcw className="size-4" />} onClick={() => setDraft({ s: DEFAULT_SCORE_WEIGHTS, v: DEFAULT_VICTIM_WEIGHTS, m: '' })}>Reset to defaults</Button>
                {(scoreSum(w.s) <= 0 || victimSum(w.v) <= 0) && <span className="text-[13px] text-danger">Each group needs at least one positive weight.</span>}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <IsolationStrip c={summary} />
      <Findings c={summary} results={results} />

      <Section number="01" title="The seven metrics" note="Raw values per strategy. Hover a metric name for its counting rule.">
        <Surface><MetricsTable results={ordered} /></Surface>
      </Section>

      <Section stacked number="02" title="At a glance" note="Aggregate cost, and each metric divided by its scenario-intrinsic normaliser.">
        <Surface className="grid gap-12 lg:grid-cols-[minmax(0,0.75fr)_minmax(0,1.25fr)]">
          <div><p className="mb-3 text-[13px] text-cocoa">Aggregate cost · lower is better</p><StrategyValueBars data={ordered.map((r) => ({ strategy: r.strategy, value: r.metrics.aggregateScore }))} format={fmtScore} /></div>
          <div><p className="mb-3 text-[13px] text-cocoa">Metric profile · normalised 0–1</p><MetricProfileChart results={ordered} /></div>
        </Surface>
      </Section>

      <section className="pb-6">
        <p className="kicker border-t border-rule pt-8">03 · The strategies, in rank order</p>
        {byRank.map((r) => <StrategyChapter key={r.strategy} result={r} />)}
      </section>

      <Section stacked number="04" title="How the cost is computed" note={`Weights used: ${METRIC_LABELS.workLost.label.toLowerCase()} ${summary.weights.workLost}, terminated ${summary.weights.processesTerminated}, rollbacks ${summary.weights.processesRolledBack}, rounds ${summary.weights.recoverySteps}, starvation ${summary.weights.starvationRisk}.`}>
        <Surface><ScoreBreakdown results={results} /></Surface>
      </Section>

      <Section number="05" title="Would other weights change the answer?" note="The same verified metrics, re-scored under four named weightings.">
        <Surface className="overflow-x-auto">
          <table className="w-full min-w-[560px] text-left text-[14px]">
            <thead><tr className="border-b border-rule text-[13px] text-cocoa"><th className="pb-2 font-normal">Weighting</th>
              {STRATEGY_ORDER.map((s) => <th key={s} className="pb-2 text-right font-normal">{STRATEGIES[s].short}</th>)}<th className="pb-2 pl-8 font-normal">Lowest cost</th></tr></thead>
            <tbody>
              <tr className="border-b border-rule-soft bg-gold-wash/40">
                <td className="py-2.5 pl-2 font-medium">Current</td>
                {ordered.map((r) => <td key={r.strategy} className="num text-right font-mono">{fmtScore(r.metrics.aggregateScore)}</td>)}
                <td className="pl-8">{summary.recommendation.strategy ? <StrategyMark strategy={summary.recommendation.strategy} /> : '—'}</td>
              </tr>
              {summary.sensitivity.map((p) => (
                <tr key={p.name} className="border-b border-rule-soft">
                  <td className="py-2.5 pl-2">{p.name}</td>
                  {STRATEGY_ORDER.map((s) => <td key={s} className="num text-right font-mono text-cocoa">{fmtScore(p.scores[s])}</td>)}
                  <td className="pl-8">{p.winner ? <StrategyMark strategy={p.winner} /> : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Surface>
      </Section>

      <Section stacked number="06" title="Before and after" note="Both graphs share one layout, so what each strategy removed is easy to see.">
        <BeforeAfter runId={runId} />
      </Section>
    </>
  )
}
