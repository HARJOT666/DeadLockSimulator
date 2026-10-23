import { useState } from 'react'
import { ArrowDown, ArrowUp, Link2, Plus, Trash2 } from 'lucide-react'
import type { Operation, OperationType, ScenarioConfig } from '../../../api/types'
import { inlineControl, NumberInput } from '../../../components/ui/Field'

const OP_TYPES: OperationType[] = ['REQUEST', 'WORK', 'RELEASE', 'FINISH']

export function nextId(prefix: string, taken: string[]) {
  let i = 1
  while (taken.includes(`${prefix}${i}`)) i++
  return `${prefix}${i}`
}

type EditorProps = { config: ScenarioConfig; onChange: (c: ScenarioConfig) => void }

function AddButton({ children, onClick, disabled }: { children: React.ReactNode; onClick: () => void; disabled?: boolean }) {
  return (
    <button type="button" onClick={onClick} disabled={disabled}
      className="inline-flex items-center gap-1.5 text-[13px] font-medium text-terracotta-deep hover:underline disabled:opacity-40 cursor-pointer">
      <Plus className="size-3.5" /> {children}
    </button>
  )
}

function RemoveButton({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <button type="button" onClick={onClick} aria-label={label} className="rounded p-1.5 text-cocoa hover:bg-terracotta-wash hover:text-danger cursor-pointer">
      <Trash2 className="size-3.5" />
    </button>
  )
}

export function ProcessesEditor({ config, onChange }: EditorProps) {
  const pids = config.processes.map((p) => p.id)
  const patch = (i: number, k: keyof ScenarioConfig['processes'][number], v: number) =>
    onChange({ ...config, processes: config.processes.map((p, j) => (j === i ? { ...p, [k]: v } : p)) })
  const rename = (i: number, id: string) => {
    const old = config.processes[i].id
    onChange({
      ...config,
      processes: config.processes.map((p, j) => (j === i ? { ...p, id } : p)),
      initialAllocations: config.initialAllocations.map((a) => (a.processId === old ? { ...a, processId: id } : a)),
      requestSequence: config.requestSequence.map((o) => (o.processId === old ? { ...o, processId: id } : o)),
    })
  }
  const remove = (i: number) => {
    const id = config.processes[i].id
    onChange({
      ...config,
      processes: config.processes.filter((_, j) => j !== i),
      initialAllocations: config.initialAllocations.filter((a) => a.processId !== id),
      requestSequence: config.requestSequence.filter((o) => o.processId !== id),
    })
  }
  return (
    <div>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[560px] text-left text-[13px]">
          <thead className="text-cocoa">
            <tr className="border-b border-rule">
              <th className="pb-2 font-normal">Process</th><th className="pb-2 font-normal">Priority <span className="text-cocoa">1–10</span></th>
              <th className="pb-2 font-normal">Total work</th><th className="pb-2 font-normal">Initial work</th><th className="pb-2 font-normal">Past victim count</th><th />
            </tr>
          </thead>
          <tbody>
            {config.processes.map((p, i) => (
              <tr key={i} className="border-b border-rule-soft">
                <td className="py-1.5 pr-3"><input aria-label="Process id" className={`${inlineControl} w-20 font-mono`} value={p.id} onChange={(e) => rename(i, e.target.value.trim())} /></td>
                <td className="py-1.5 pr-3"><NumberInput aria-label={`${p.id} priority`} className="w-20" min={1} max={10} value={p.priority} onChange={(v) => patch(i, 'priority', v)} /></td>
                <td className="py-1.5 pr-3"><NumberInput aria-label={`${p.id} total work`} className="w-24" min={1} value={p.totalWork} onChange={(v) => patch(i, 'totalWork', v)} /></td>
                <td className="py-1.5 pr-3"><NumberInput aria-label={`${p.id} initial work`} className="w-24" min={0} value={p.initialWork} onChange={(v) => patch(i, 'initialWork', v)} /></td>
                <td className="py-1.5 pr-3"><NumberInput aria-label={`${p.id} previous victims`} className="w-20" min={0} value={p.previousVictimCount} onChange={(v) => patch(i, 'previousVictimCount', v)} /></td>
                <td className="py-1.5 text-right"><RemoveButton label={`Remove ${p.id}`} onClick={() => remove(i)} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="mt-3">
        <AddButton onClick={() => onChange({ ...config, processes: [...config.processes, { id: nextId('P', pids), priority: 5, totalWork: 100, initialWork: 0, previousVictimCount: 0 }] })}>Add process</AddButton>
      </div>
    </div>
  )
}

export function ResourcesEditor({ config, onChange }: EditorProps) {
  const pids = config.processes.map((p) => p.id)
  const removeResource = (r: string) => onChange({
    ...config,
    resources: config.resources.filter((x) => x !== r),
    initialAllocations: config.initialAllocations.filter((a) => a.resourceId !== r),
    requestSequence: config.requestSequence.map((o) => ({ ...o, resources: o.resources.filter((x) => x !== r) })),
  })
  const free = config.resources.find((r) => !config.initialAllocations.some((a) => a.resourceId === r))
  return (
    <div className="grid gap-8 md:grid-cols-2">
      <div>
        <p className="mb-3 text-[13px] text-cocoa">Single-instance resources — one holder, a FIFO queue of waiters.</p>
        <ul className="flex flex-wrap gap-2">
          {config.resources.map((r) => (
            <li key={r} className="inline-flex items-center rounded-[4px] border border-rule bg-sheet py-0.5 pl-2.5 pr-0.5 font-mono text-[12.5px]">
              {r}<RemoveButton label={`Remove ${r}`} onClick={() => removeResource(r)} />
            </li>
          ))}
        </ul>
        <div className="mt-3"><AddButton onClick={() => onChange({ ...config, resources: [...config.resources, nextId('R', config.resources)] })}>Add resource</AddButton></div>
      </div>
      <div>
        <p className="mb-3 text-[13px] text-cocoa">Initial allocations — held before the trace begins (after checkpoint #0).</p>
        {config.initialAllocations.length === 0 ? <p className="text-[13px] text-cocoa">None. Resources are acquired by REQUEST operations.</p> : (
          <ul className="space-y-2">
            {config.initialAllocations.map((a, i) => (
              <li key={i} className="flex items-center gap-2 text-[13px]">
                <select aria-label="Resource" className={`${inlineControl} font-mono`} value={a.resourceId}
                  onChange={(e) => onChange({ ...config, initialAllocations: config.initialAllocations.map((x, j) => j === i ? { ...x, resourceId: e.target.value } : x) })}>
                  {config.resources.map((r) => <option key={r}>{r}</option>)}
                </select>
                <span className="text-cocoa">held by</span>
                <select aria-label="Process" className={`${inlineControl} font-mono`} value={a.processId}
                  onChange={(e) => onChange({ ...config, initialAllocations: config.initialAllocations.map((x, j) => j === i ? { ...x, processId: e.target.value } : x) })}>
                  {pids.map((p) => <option key={p}>{p}</option>)}
                </select>
                <RemoveButton label="Remove allocation" onClick={() => onChange({ ...config, initialAllocations: config.initialAllocations.filter((_, j) => j !== i) })} />
              </li>
            ))}
          </ul>
        )}
        <div className="mt-3">
          <AddButton disabled={!free || !pids[0]} onClick={() => free && onChange({ ...config, initialAllocations: [...config.initialAllocations, { resourceId: free, processId: pids[0] }] })}>Add allocation</AddButton>
        </div>
      </div>
    </div>
  )
}

export function SequenceEditor({ config, onChange }: EditorProps) {
  const pids = config.processes.map((p) => p.id)
  const [ring, setRing] = useState<string[]>([])
  const update = (i: number, patch: Partial<Operation>) =>
    onChange({ ...config, requestSequence: config.requestSequence.map((o, j) => (j === i ? { ...o, ...patch } : o)) })
  const move = (i: number, d: -1 | 1) => {
    const seq = [...config.requestSequence]
    const j = i + d
    if (j < 0 || j >= seq.length) return
    ;[seq[i], seq[j]] = [seq[j], seq[i]]
    onChange({ ...config, requestSequence: seq })
  }
  /** Each chosen process takes a fresh resource, works, then requests the next one's — a circular wait. */
  const appendRing = () => {
    const resources = [...config.resources]
    const own = ring.map(() => { const r = nextId('R', resources); resources.push(r); return r })
    const ops: Operation[] = [
      ...ring.map((p, k) => ({ type: 'REQUEST' as const, processId: p, resources: [own[k]], amount: null })),
      ...ring.map((p) => ({ type: 'WORK' as const, processId: p, resources: [], amount: 15 })),
      ...ring.map((p, k) => ({ type: 'REQUEST' as const, processId: p, resources: [own[(k + 1) % ring.length]], amount: null })),
    ]
    onChange({ ...config, resources, requestSequence: [...config.requestSequence, ...ops] })
    setRing([])
  }
  return (
    <div>
      <div className="mb-5 border-l-2 border-gold bg-gold-wash/60 px-4 py-3 text-[13px]">
        <p className="font-medium text-ink">Deadlock helper</p>
        <p className="mt-0.5 text-cocoa">Choose two or more processes in ring order; each will take a new resource, work, then request the next one’s.</p>
        <div className="mt-3 flex flex-wrap items-center gap-2">
          {pids.map((p) => (
            <button key={p} type="button" aria-pressed={ring.includes(p)} onClick={() => setRing((r) => r.includes(p) ? r.filter((x) => x !== p) : [...r, p])}
              className={`rounded-[4px] border px-2 py-0.5 font-mono text-[12px] cursor-pointer ${ring.includes(p) ? 'border-terracotta bg-terracotta-wash text-ink' : 'border-rule bg-sheet text-cocoa hover:border-cocoa'}`}>
              {p}{ring.includes(p) && <sup className="ml-0.5">{ring.indexOf(p) + 1}</sup>}
            </button>
          ))}
          <button type="button" disabled={ring.length < 2} onClick={appendRing}
            className="ml-1 inline-flex items-center gap-1.5 text-[13px] font-medium text-terracotta-deep hover:underline disabled:text-cocoa disabled:no-underline cursor-pointer">
            <Link2 className="size-3.5" /> Append circular wait
          </button>
        </div>
      </div>
      {config.requestSequence.length === 0 ? <p className="text-[13px] text-cocoa">No operations yet.</p> : (
        <ol className="border-t border-rule">
          {config.requestSequence.map((op, i) => (
            <li key={i} className="flex flex-wrap items-center gap-2 border-b border-rule-soft py-2">
              <span className="num w-7 text-right font-mono text-[11.5px] text-cocoa">{i + 1}</span>
              <select aria-label={`Step ${i + 1} type`} className={`${inlineControl} w-[104px]`} value={op.type}
                onChange={(e) => {
                  const type = e.target.value as OperationType
                  update(i, { type, amount: type === 'WORK' ? (op.amount ?? 10) : null, resources: type === 'WORK' || type === 'FINISH' ? [] : op.resources })
                }}>
                {OP_TYPES.map((t) => <option key={t}>{t}</option>)}
              </select>
              <select aria-label={`Step ${i + 1} process`} className={`${inlineControl} w-[84px] font-mono`} value={op.processId} onChange={(e) => update(i, { processId: e.target.value })}>
                {!pids.includes(op.processId) && <option value={op.processId}>{op.processId || '—'}</option>}
                {pids.map((p) => <option key={p}>{p}</option>)}
              </select>
              {(op.type === 'REQUEST' || op.type === 'RELEASE') && (
                <div className="flex flex-wrap gap-1" role="group" aria-label={`Step ${i + 1} resources`}>
                  {config.resources.map((r) => {
                    const on = op.resources.includes(r)
                    return (
                      <button key={r} type="button" aria-pressed={on} onClick={() => update(i, { resources: on ? op.resources.filter((x) => x !== r) : [...op.resources, r] })}
                        className={`rounded-[3px] border px-1.5 py-px font-mono text-[11.5px] cursor-pointer ${on ? 'border-terracotta-deep bg-terracotta-wash text-terracotta-deep' : 'border-rule-soft text-cocoa hover:text-cocoa'}`}>{r}</button>
                    )
                  })}
                </div>
              )}
              {op.type === 'WORK' && <NumberInput aria-label={`Step ${i + 1} amount`} className="w-24" min={1} value={op.amount ?? 0} onChange={(v) => update(i, { amount: v })} />}
              <span className="ml-auto flex">
                <button type="button" onClick={() => move(i, -1)} disabled={i === 0} aria-label="Move up" className="rounded p-1.5 text-cocoa hover:text-ink disabled:opacity-30 cursor-pointer"><ArrowUp className="size-3.5" /></button>
                <button type="button" onClick={() => move(i, 1)} disabled={i === config.requestSequence.length - 1} aria-label="Move down" className="rounded p-1.5 text-cocoa hover:text-ink disabled:opacity-30 cursor-pointer"><ArrowDown className="size-3.5" /></button>
                <RemoveButton label={`Remove step ${i + 1}`} onClick={() => onChange({ ...config, requestSequence: config.requestSequence.filter((_, j) => j !== i) })} />
              </span>
            </li>
          ))}
        </ol>
      )}
      <div className="mt-3">
        <AddButton onClick={() => onChange({ ...config, requestSequence: [...config.requestSequence, { type: 'REQUEST', processId: pids[0] ?? '', resources: config.resources.slice(0, 1), amount: null }] })}>Add operation</AddButton>
      </div>
    </div>
  )
}
