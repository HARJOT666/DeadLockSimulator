import type { RecoveryResult } from '../../api/types'
import { METRIC_LABELS, STRATEGIES, STRATEGY_ORDER } from '../../lib/strategy'

/** Every term of the aggregate score: weight × min(raw / normaliser, 1) / Σ weights. */
export function ScoreBreakdown({ results }: { results: RecoveryResult[] }) {
  const ordered = STRATEGY_ORDER.map((s) => results.find((r) => r.strategy === s)).filter(Boolean) as RecoveryResult[]
  const metrics = ordered[0]?.metrics.scoreBreakdown ?? []
  const wsum = metrics.reduce((a, c) => a + c.weight, 0)
  const max = Math.max(0.0001, ...ordered.flatMap((r) => r.metrics.scoreBreakdown.map((c) => c.contribution)))
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[760px] text-left text-[13px]">
        <thead className="text-cocoa">
          <tr className="border-b border-rule">
            <th className="pb-2 font-normal">Metric</th><th className="pb-2 text-right font-normal">Weight</th><th className="pb-2 pr-4 text-right font-normal">÷ by</th>
            {ordered.map((r) => <th key={r.strategy} className="pb-2 pl-4 font-normal"><span className="inline-flex items-center gap-2"><span className="size-2 rounded-[1px]" style={{ background: STRATEGIES[r.strategy].color }} />{STRATEGIES[r.strategy].short}</span></th>)}
          </tr>
        </thead>
        <tbody>
          {metrics.map((ref) => (
            <tr key={ref.metric} className="border-b border-rule-soft">
              <td className="py-2.5 text-ink">{METRIC_LABELS[ref.metric]?.label ?? ref.metric}</td>
              <td className="num text-right font-mono text-cocoa">{ref.weight.toFixed(2)}</td>
              <td className="num pr-4 text-right font-mono text-cocoa">{ref.normaliser}</td>
              {ordered.map((r) => {
                const c = r.metrics.scoreBreakdown.find((x) => x.metric === ref.metric)!
                return (
                  <td key={r.strategy} className="pl-4">
                    <div className="flex items-center gap-3 font-mono text-[12px]">
                      <span className="num w-[86px] text-cocoa">{c.rawValue} → {c.normalised.toFixed(2)}</span>
                      <span className="h-[5px] w-16 bg-paper"><span className="block h-full" style={{ width: `${(c.contribution / max) * 100}%`, background: STRATEGIES[r.strategy].color }} /></span>
                      <span className="num w-12 text-right text-ink">{c.contribution.toFixed(3)}</span>
                    </div>
                  </td>
                )
              })}
            </tr>
          ))}
          <tr>
            <td className="pt-3 font-medium">Aggregate cost</td>
            <td className="num pt-3 text-right font-mono text-cocoa">Σ {wsum.toFixed(2)}</td><td />
            {ordered.map((r) => (
              <td key={r.strategy} className="pl-4 pt-3"><div className="flex justify-end font-mono text-[14px] font-semibold text-ink" style={{ width: 86 + 64 + 48 + 24 }}>{r.metrics.aggregateScore.toFixed(3)}</div></td>
            ))}
          </tr>
        </tbody>
      </table>
      <p className="mt-4 max-w-[640px] text-[12.5px] leading-relaxed text-cocoa">
        Each raw metric is divided by a normaliser taken from the deadlocked snapshot alone (live work, live processes, resources,
        deadlocked-set size, starvation threshold), so one strategy’s score never depends on the others. Lower is better.
      </p>
    </div>
  )
}
