import { memo } from 'react'
import { Handle, Position, type Node, type NodeProps } from '@xyflow/react'
import type { GraphNode } from '../../api/types'
import { NODE_H, NODE_W } from './layout'

export type ProcessNodeType = Node<{ p: GraphNode; selected: boolean; dimmed: boolean }, 'process'>

/** Visual role derived only from backend state flags. */
export function nodeRole(p: GraphNode) {
  if (p.state === 'TERMINATED') return { key: 'terminated', label: p.recoveryAction === 'ABORT' ? 'aborted' : 'terminated' }
  if (p.state === 'COMPLETED') return { key: 'completed', label: 'completed' }
  if (p.state === 'READY') return { key: 'rolledback', label: 'rolled back' }
  if (p.deadlocked) return { key: 'deadlocked', label: 'on cycle' }
  if (p.blocked) return { key: 'blocked', label: 'blocked' }
  if (p.state === 'WAITING') return { key: 'waiting', label: 'waiting' }
  return { key: 'running', label: 'running' }
}

/** Cream nodes on the ivory canvas; state is carried by the border, a small mark and the label. */
const frame: Record<string, string> = {
  deadlocked: 'bg-terracotta-wash/45 border-2 border-terracotta shadow-[var(--shadow-node)]',
  blocked: 'bg-highlight-wash/70 border-2 border-highlight',
  waiting: 'bg-sheet border border-rule',
  running: 'bg-sheet border border-rule',
  rolledback: 'bg-sage-wash border-2 border-sage',
  terminated: 'bg-ivory border border-dashed border-taupe opacity-80',
  completed: 'bg-ivory border border-rule opacity-80',
}

const mark: Record<string, string> = {
  deadlocked: 'bg-terracotta', blocked: 'bg-highlight', running: 'bg-sage', rolledback: 'bg-sage', waiting: 'bg-taupe',
}

export const ProcessNode = memo(function ProcessNode({ data }: NodeProps<ProcessNodeType>) {
  const { p, selected, dimmed } = data
  const role = nodeRole(p)
  const pct = p.totalWork ? Math.min(100, (p.workDone / p.totalWork) * 100) : 0
  return (
    <div style={{ width: NODE_W, height: NODE_H }}
      className={`rounded-[6px] px-3 py-2.5 text-ink shadow-[var(--shadow-node)] transition-[opacity,transform] duration-200
        ${frame[role.key]} ${selected ? 'scale-[1.04]' : ''} ${dimmed ? 'opacity-30' : ''}`}>
      <Handle type="target" position={Position.Top} className="!pointer-events-none !opacity-0" />
      <Handle type="source" position={Position.Bottom} className="!pointer-events-none !opacity-0" />
      <div className="flex items-baseline justify-between gap-2">
        <span className={`font-mono text-[15px] font-semibold text-ink ${role.key === 'terminated' ? 'line-through decoration-terracotta' : ''}`}>{p.id}</span>
        <span className={`flex items-center gap-1.5 font-mono text-[9.5px] uppercase tracking-[0.1em] ${role.key === 'deadlocked' ? 'text-terracotta-deep' : 'text-cocoa'}`}>
          {mark[role.key] && <span className={`size-1.5 rounded-full ${mark[role.key]}`} aria-hidden />}
          {role.label}
        </span>
      </div>
      <div className="mt-2 h-[3px] bg-sand" title={`Work ${p.workDone} of ${p.totalWork}`}>
        <div className="h-full bg-cocoa/55" style={{ width: `${pct}%` }} />
      </div>
      <div className="mt-1.5 flex justify-between font-mono text-[10px] text-cocoa">
        <span>w {p.workDone}/{p.totalWork}</span><span>pr {p.priority}</span><span>v {p.victimCount}</span>
      </div>
      <div className="mt-0.5 truncate font-mono text-[10px] text-cocoa">
        holds {p.heldResources.length ? p.heldResources.join(' ') : '—'}{p.waitingFor.length ? ` · wants ${p.waitingFor.join(' ')}` : ''}
      </div>
    </div>
  )
})
