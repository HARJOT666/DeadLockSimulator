import { useState } from 'react'
import { Dices, Sparkles } from 'lucide-react'
import type { GeneratorParams, Template, WorkDistribution } from '../../../api/types'
import { api } from '../../../api/endpoints'
import { Button } from '../../../components/ui/Button'
import { inlineControl, NumberInput } from '../../../components/ui/Field'
import { InlineError } from '../../../components/ui/Feedback'

const DEFAULTS: GeneratorParams = {
  seed: 42, cycleCount: 1, cycleLength: 4, blockedDependents: 1, independentProcesses: 1, workDistribution: 'UNIFORM',
  minWork: 20, maxWork: 120, checkpointInterval: 10, resourceDensity: 0.5, maxExtraResources: 2, overlapping: false, maxPreviousVictims: 0,
}

function Field({ label, hint, children }: { label: string; hint?: string; children: React.ReactNode }) {
  return (
    <label className="flex items-center justify-between gap-4 border-b border-rule-soft py-2.5">
      <span><span className="block text-[13.5px] text-ink">{label}</span>{hint && <span className="block text-[12px] text-cocoa">{hint}</span>}</span>
      {children}
    </label>
  )
}

/** The case study's experimental factors, sent to the seeded generator on the server. */
export function GeneratorForm({ onGenerated }: { onGenerated: (t: Template) => void }) {
  const [p, setP] = useState(DEFAULTS)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const set = <K extends keyof GeneratorParams>(k: K) => (v: GeneratorParams[K]) => setP((x) => ({ ...x, [k]: v }))
  const run = async () => {
    setBusy(true); setError(null)
    try { onGenerated(await api.generate(p)) } catch (e) { setError(e) } finally { setBusy(false) }
  }
  return (
    <div>
      <div className="grid gap-x-10 md:grid-cols-2">
        <div>
          <Field label="Seed" hint="same parameters + seed → same scenario">
            <span className="flex items-center gap-1">
              <NumberInput className="w-24" value={p.seed} onChange={set('seed')} aria-label="Seed" />
              <button type="button" onClick={() => set('seed')(Math.floor(Math.random() * 100000))} className="rounded p-1.5 text-cocoa hover:text-terracotta-deep cursor-pointer" aria-label="Random seed"><Dices className="size-4" /></button>
            </span>
          </Field>
          <Field label="Cycles"><NumberInput aria-label="Cycles" className="w-20" min={1} max={5} value={p.cycleCount} onChange={set('cycleCount')} /></Field>
          <Field label="Cycle length" hint="processes per cycle"><NumberInput aria-label="Cycle length" className="w-20" min={2} max={10} value={p.cycleLength} onChange={set('cycleLength')} /></Field>
          <Field label="Overlapping cycles" hint="AND-request across two successors">
            <input type="checkbox" className="size-4 accent-[#A96445]" checked={p.overlapping} onChange={(e) => set('overlapping')(e.target.checked)} />
          </Field>
          <Field label="Blocked dependents"><NumberInput aria-label="Blocked dependents" className="w-20" min={0} max={10} value={p.blockedDependents} onChange={set('blockedDependents')} /></Field>
          <Field label="Independent processes"><NumberInput aria-label="Independent processes" className="w-20" min={0} max={10} value={p.independentProcesses} onChange={set('independentProcesses')} /></Field>
        </div>
        <div>
          <Field label="Work distribution">
            <select aria-label="Work distribution" className={inlineControl} value={p.workDistribution} onChange={(e) => set('workDistribution')(e.target.value as WorkDistribution)}>
              <option value="EQUAL">equal</option><option value="UNIFORM">uniform</option><option value="SKEWED">skewed</option>
            </select>
          </Field>
          <Field label="Work range" hint="done before the deadlock">
            <span className="flex items-center gap-1"><NumberInput aria-label="Minimum work" className="w-20" min={0} value={p.minWork} onChange={set('minWork')} /><span className="text-cocoa">–</span><NumberInput aria-label="Maximum work" className="w-20" min={1} value={p.maxWork} onChange={set('maxWork')} /></span>
          </Field>
          <Field label="Checkpoint interval" hint="0 = initial checkpoint only"><NumberInput aria-label="Checkpoint interval" className="w-20" min={0} value={p.checkpointInterval} onChange={set('checkpointInterval')} /></Field>
          <Field label={`Resource density ${p.resourceDensity.toFixed(2)}`} hint="extra resources held per member">
            <input type="range" aria-label="Resource density" min={0} max={1} step={0.05} value={p.resourceDensity} onChange={(e) => set('resourceDensity')(Number(e.target.value))} className="w-32 accent-[#A96445]" />
          </Field>
          <Field label="Extra resources at density 1"><NumberInput aria-label="Maximum extra resources" className="w-20" min={0} max={5} value={p.maxExtraResources} onChange={set('maxExtraResources')} /></Field>
          <Field label="Past victim count, up to" hint="feeds starvation risk"><NumberInput aria-label="Maximum previous victims" className="w-20" min={0} max={10} value={p.maxPreviousVictims} onChange={set('maxPreviousVictims')} /></Field>
        </div>
      </div>
      <div className="mt-4"><InlineError error={error} /></div>
      <Button className="mt-4" variant="secondary" icon={<Sparkles className="size-4" />} loading={busy} onClick={run}>Generate and load</Button>
    </div>
  )
}
