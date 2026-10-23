import type { ScoreWeights, VictimWeights } from '../../api/types'
import { METRIC_LABELS } from '../../lib/strategy'
import { NumberInput } from '../../components/ui/Field'

export const SCORE_KEYS = ['workLost', 'processesTerminated', 'processesRolledBack', 'resourcesReclaimed', 'recoverySteps', 'starvationRisk'] as const
const VICTIM_KEYS: { key: keyof VictimWeights; label: string; help: string }[] = [
  { key: 'work', label: 'Work at risk', help: 'more work → costlier victim' },
  { key: 'priority', label: 'Priority', help: 'more important → costlier' },
  { key: 'resources', label: 'Resources held', help: 'holds more → cheaper, frees more' },
  { key: 'history', label: 'Victim history', help: 'chosen before → costlier' },
]

export const scoreSum = (w: ScoreWeights) => SCORE_KEYS.reduce((a, k) => a + (w[k] || 0), 0)
export const victimSum = (w: VictimWeights) => VICTIM_KEYS.reduce((a, k) => a + (w[k.key] || 0), 0)

function Slider({ label, help, value, onChange }: { label: string; help?: string; value: number; onChange: (v: number) => void }) {
  return (
    <label className="grid grid-cols-[150px_minmax(0,1fr)_44px] items-center gap-4 py-2 text-[13px]">
      <span className="text-ink">{label}{help && <span className="block text-[11.5px] text-cocoa">{help}</span>}</span>
      <input type="range" min={0} max={1} step={0.05} value={value} onChange={(e) => onChange(Number(e.target.value))} className="w-full accent-[#A96445]" />
      <span className="num text-right font-mono text-ink">{value.toFixed(2)}</span>
    </label>
  )
}

/** Both weight groups; parents decide when and where the values are applied. */
export function WeightsEditor({ score, victim, maxRounds, onChange, showRounds = true }: {
  score: ScoreWeights; victim: VictimWeights; maxRounds: number | ''; showRounds?: boolean
  onChange: (s: ScoreWeights, v: VictimWeights, maxRounds: number | '') => void
}) {
  return (
    <div className="grid gap-10 lg:grid-cols-2">
      <div>
        <p className="text-[14px] font-medium">Aggregate-score weights</p>
        <p className="mb-3 mt-0.5 text-[12.5px] text-cocoa">Only the ratios matter — re-normalised by Σ = {scoreSum(score).toFixed(2)}.</p>
        {SCORE_KEYS.map((k) => <Slider key={k} label={METRIC_LABELS[k].label} value={score[k]} onChange={(v) => onChange({ ...score, [k]: v }, victim, maxRounds)} />)}
        <label className="grid grid-cols-[150px_minmax(0,1fr)] items-center gap-4 py-2 text-[13px]">
          <span className="text-ink">Starvation threshold<span className="block text-[11.5px] text-cocoa">victim count = maximal risk</span></span>
          <NumberInput className="w-20" min={1} max={100} value={score.starvationThreshold} onChange={(v) => onChange({ ...score, starvationThreshold: v }, victim, maxRounds)} />
        </label>
      </div>
      <div>
        <p className="text-[14px] font-medium">Victim-selection weights</p>
        <p className="mb-3 mt-0.5 text-[12.5px] text-cocoa">Lower cost = preferred victim; inputs min-max normalised over candidates (Σ = {victimSum(victim).toFixed(2)}).</p>
        {VICTIM_KEYS.map(({ key, label, help }) => <Slider key={key} label={label} help={help} value={victim[key]} onChange={(v) => onChange(score, { ...victim, [key]: v }, maxRounds)} />)}
        {showRounds && (
          <label className="grid grid-cols-[150px_minmax(0,1fr)] items-center gap-4 py-2 text-[13px]">
            <span className="text-ink">Safety limit<span className="block text-[11.5px] text-cocoa">rounds; empty = 2|P| + 5</span></span>
            <NumberInput className="w-20" min={1} max={500} placeholder="auto" value={maxRounds === '' ? NaN : maxRounds}
              onChange={(v) => onChange(score, victim, Number.isFinite(v) ? v : '')} />
          </label>
        )}
      </div>
    </div>
  )
}
