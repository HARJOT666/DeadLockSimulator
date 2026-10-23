import type { StrategyStats } from '../../api/types'
import { StrategyValueBars } from '../../components/charts/StrategyCharts'
import { StrategyMark } from '../../components/ui/Status'
import { fmtScore } from '../../lib/format'

/** Chart of average aggregate cost and a short, data-derived reading of it. */
export function StrategyFindings({ stats }: { stats: StrategyStats[] }) {
  const ranked = [...stats].sort((a, b) => b.timesRankedFirst - a.timesRankedFirst)
  const winners = stats.filter((s) => s.timesRankedFirst > 0)
  const comparisons = Math.max(...stats.map((s) => s.executions))
  const leastWork = [...stats].sort((a, b) => a.averageWorkLost - b.averageWorkLost)[0]
  return (
    <div className="grid gap-10 xl:grid-cols-[minmax(0,1fr)_300px]">
      <div>
        <p className="mb-3 text-[13px] text-cocoa">Average aggregate recovery cost · lower is better</p>
        <StrategyValueBars data={stats.map((s) => ({ strategy: s.strategy, value: s.averageScore }))} format={fmtScore} />
      </div>
      <div className="space-y-5 text-[14.5px] leading-relaxed text-cocoa">
        <p>
          Across <span className="text-ink">{comparisons}</span> {comparisons === 1 ? 'comparison' : 'comparisons'},{' '}
          {winners.length > 1
            ? <>the first place was shared: {winners.map((w, i) => <span key={w.strategy}>{i > 0 && ', '}<span className="text-ink">{w.displayName.toLowerCase()}</span> ({w.timesRankedFirst})</span>)}.</>
            : ranked[0] && ranked[0].timesRankedFirst > 0
              ? <><span className="text-ink">{ranked[0].displayName.toLowerCase()}</span> ranked first every time so far — with more varied scenarios that may change.</>
              : <>no strategy has ranked first yet.</>}
        </p>
        {leastWork && <p><span className="text-ink">{leastWork.displayName}</span> discarded the least work on average ({leastWork.averageWorkLost.toFixed(1)} units).</p>}
        <table className="w-full text-[13px]">
          <thead><tr className="text-left text-cocoa"><th className="pb-2 font-normal">Strategy</th><th className="pb-2 text-right font-normal">1st</th><th className="pb-2 text-right font-normal">verified</th></tr></thead>
          <tbody className="text-ink">
            {stats.map((s) => (
              <tr key={s.strategy} className="border-t border-rule-soft">
                <td className="py-1.5"><StrategyMark strategy={s.strategy} /></td>
                <td className="num text-right font-mono">{s.timesRankedFirst}</td>
                <td className="num text-right font-mono">{s.recovered}/{s.executions}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
