import { memo } from 'react'
import { BaseEdge, EdgeLabelRenderer, useInternalNode, type Edge, type EdgeProps } from '@xyflow/react'
import type { GraphEdge } from '../../api/types'
import { NODE_H, NODE_W } from './layout'

export type WaitEdgeType = Edge<{ e: GraphEdge; selected: boolean; dimmed: boolean; offset: number }, 'wait'>

export const EDGE_CYCLE = '#A96445'
export const EDGE_WAIT = '#74816A'

function border(cx: number, cy: number, tx: number, ty: number) {
  const dx = tx - cx, dy = ty - cy
  const s = Math.max(Math.abs(dx) / (NODE_W / 2 + 7), Math.abs(dy) / (NODE_H / 2 + 7)) || 1
  return { x: cx + dx / s, y: cy + dy / s }
}

/** Floating curved edge between node centres; opposite edges bow to opposite sides. */
export const WaitEdge = memo(function WaitEdge({ id, source, target, data, markerEnd }: EdgeProps<WaitEdgeType>) {
  const s = useInternalNode(source)
  const t = useInternalNode(target)
  if (!s || !t || !data) return null
  const sc = { x: s.internals.positionAbsolute.x + NODE_W / 2, y: s.internals.positionAbsolute.y + NODE_H / 2 }
  const tc = { x: t.internals.positionAbsolute.x + NODE_W / 2, y: t.internals.positionAbsolute.y + NODE_H / 2 }
  const dx = tc.x - sc.x, dy = tc.y - sc.y
  const len = Math.hypot(dx, dy) || 1
  const bend = 30 + data.offset * 22
  const mx = (sc.x + tc.x) / 2 - (dy / len) * bend
  const my = (sc.y + tc.y) / 2 + (dx / len) * bend
  const a = border(sc.x, sc.y, mx, my)
  const b = border(tc.x, tc.y, mx, my)
  const path = `M${a.x},${a.y} Q${mx},${my} ${b.x},${b.y}`
  const lx = 0.25 * a.x + 0.5 * mx + 0.25 * b.x
  const ly = 0.25 * a.y + 0.5 * my + 0.25 * b.y
  const cyc = data.e.inCycle
  return (
    <>
      <BaseEdge id={id} path={path} markerEnd={markerEnd} style={{
        stroke: cyc ? EDGE_CYCLE : EDGE_WAIT, strokeWidth: data.selected ? 2.6 : 1.7, opacity: data.dimmed ? 0.2 : 1,
        strokeDasharray: cyc ? '6 4' : undefined, animation: cyc ? 'wait-dash 1.4s linear infinite' : undefined,
        transition: 'opacity .2s, stroke-width .2s',
      }} />
      <EdgeLabelRenderer>
        <div style={{ transform: `translate(-50%,-50%) translate(${lx}px,${ly}px)`, pointerEvents: 'none' }}
          className={`absolute rounded-[3px] border bg-sheet px-1.5 py-px font-mono text-[10px] font-medium
            ${cyc ? 'border-terracotta/50 text-terracotta-deep' : 'border-sage/60 text-sage-deep'} ${data.dimmed ? 'opacity-25' : ''}`}>
          {data.e.resource}
        </div>
      </EdgeLabelRenderer>
    </>
  )
})
