import type { ScenarioConfig } from '../api/types'

const ID = /^[A-Za-z][A-Za-z0-9_-]{0,15}$/

/** Client-side mirror of the backend's structural checks (the server still dry-runs on save). */
export function validateConfig(c: ScenarioConfig): string[] {
  const p: string[] = []
  if (!c.processes.length) p.push('Add at least one process.')
  if (!c.resources.length) p.push('Add at least one resource.')
  if (!c.requestSequence.length) p.push('The request sequence is empty.')
  if (c.processes.length > 100) p.push('At most 100 processes.')
  if (c.resources.length > 200) p.push('At most 200 resources.')
  if (!Number.isInteger(c.checkpointInterval) || c.checkpointInterval < 0) p.push('Checkpoint interval must be a non-negative integer.')
  const pids = new Set<string>()
  c.processes.forEach((x) => {
    if (!ID.test(x.id)) p.push(`Invalid process id "${x.id}".`)
    if (pids.has(x.id)) p.push(`Duplicate process id ${x.id}.`)
    pids.add(x.id)
    if (!(x.priority >= 1 && x.priority <= 10)) p.push(`${x.id}: priority must be 1–10.`)
    if (!(x.totalWork >= 1)) p.push(`${x.id}: total work must be ≥ 1.`)
    if (!(x.initialWork >= 0) || x.initialWork > x.totalWork) p.push(`${x.id}: initial work must be within [0, total work].`)
    if (!(x.previousVictimCount >= 0)) p.push(`${x.id}: previous victim count must be ≥ 0.`)
  })
  const rids = new Set<string>()
  c.resources.forEach((r) => {
    if (!ID.test(r)) p.push(`Invalid resource id "${r}".`)
    if (rids.has(r)) p.push(`Duplicate resource id ${r}.`)
    if (pids.has(r)) p.push(`${r} is used as both a process and a resource id.`)
    rids.add(r)
  })
  const allocated = new Set<string>()
  c.initialAllocations.forEach((a) => {
    if (!rids.has(a.resourceId) || !pids.has(a.processId)) p.push(`Initial allocation ${a.resourceId} → ${a.processId} references an unknown id.`)
    if (allocated.has(a.resourceId)) p.push(`${a.resourceId} is initially allocated twice (single-instance).`)
    allocated.add(a.resourceId)
  })
  c.requestSequence.forEach((op, i) => {
    const at = `Step ${i + 1}`
    if (!pids.has(op.processId)) p.push(`${at}: unknown process ${op.processId || '(none)'}.`)
    if ((op.type === 'REQUEST' || op.type === 'RELEASE') && !op.resources.length) p.push(`${at}: choose at least one resource.`)
    op.resources.filter((r) => !rids.has(r)).forEach((r) => p.push(`${at}: unknown resource ${r}.`))
    if (op.type === 'WORK' && !(op.amount && op.amount > 0)) p.push(`${at}: work amount must be positive.`)
  })
  return [...new Set(p)]
}

export function emptyConfig(): ScenarioConfig {
  return {
    processes: [
      { id: 'P1', priority: 5, totalWork: 100, initialWork: 0, previousVictimCount: 0 },
      { id: 'P2', priority: 5, totalWork: 100, initialWork: 0, previousVictimCount: 0 },
    ],
    resources: ['R1', 'R2'], initialAllocations: [], requestSequence: [], checkpointInterval: 10, seed: null, parameters: {},
  }
}
