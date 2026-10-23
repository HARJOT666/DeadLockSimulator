import type { GraphData } from '../../api/types'

export const NODE_W = 168
export const NODE_H = 86

/**
 * Deterministic layout: every group of overlapping cycles becomes a ring; all remaining processes
 * are placed on rows below. Computed from a reference graph (normally the deadlocked snapshot) so
 * the "before" and "after" graphs keep identical positions.
 */
export function computeLayout(graph: GraphData): Record<string, { x: number; y: number }> {
  // Union overlapping cycles into groups.
  const groups: Set<string>[] = []
  for (const cycle of graph.cycles) {
    const hit = groups.filter((g) => cycle.some((p) => g.has(p)))
    const merged = new Set<string>(cycle)
    hit.forEach((g) => { g.forEach((p) => merged.add(p)); groups.splice(groups.indexOf(g), 1) })
    groups.push(merged)
  }
  // Deadlocked processes that are not on a witness cycle still belong with their SCC.
  const placed = new Set(groups.flatMap((g) => [...g]))
  const extraDeadlocked = graph.deadlockedProcesses.filter((p) => !placed.has(p))
  if (extraDeadlocked.length) {
    const target = groups[0] ?? new Set<string>()
    extraDeadlocked.forEach((p) => target.add(p))
    if (!groups.length) groups.push(target)
  }

  const order = new Map(graph.nodes.map((n, i) => [n.id, i]))
  const pos: Record<string, { x: number; y: number }> = {}
  let cursorX = 0
  let ringBottom = 0
  for (const g of groups) {
    const members = [...g].sort((a, b) => (order.get(a) ?? 0) - (order.get(b) ?? 0))
    const n = members.length
    const r = n <= 1 ? 0 : n === 2 ? NODE_W * 0.9 : Math.max(150, (n * (NODE_W + 50)) / (2 * Math.PI))
    const cx = cursorX + r + NODE_W / 2
    members.forEach((id, i) => {
      // A 2-cycle is laid out side by side; larger rings start at the top.
      const a = (n === 2 ? Math.PI : -Math.PI / 2) + (2 * Math.PI * i) / n
      pos[id] = { x: cx + r * Math.cos(a) - NODE_W / 2, y: (n === 2 ? 0 : r) + r * Math.sin(a) }
    })
    cursorX = cx + r + NODE_W / 2 + 120
    ringBottom = Math.max(ringBottom, (n === 2 ? 0 : 2 * r) + NODE_H)
  }
  const rest = graph.nodes.map((n) => n.id).filter((id) => !pos[id])
  const perRow = Math.max(3, Math.min(6, Math.ceil(Math.sqrt(rest.length * 2))))
  const startY = groups.length ? ringBottom + 90 : 0
  rest.forEach((id, i) => {
    pos[id] = { x: (i % perRow) * (NODE_W + 56), y: startY + Math.floor(i / perRow) * (NODE_H + 70) }
  })
  return pos
}
