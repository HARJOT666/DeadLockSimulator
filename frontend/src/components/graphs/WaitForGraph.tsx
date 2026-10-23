import { useCallback, useEffect, useMemo, useState } from 'react'
import { applyNodeChanges, Controls, MarkerType, ReactFlow, type EdgeChange, type EdgeTypes, type NodeChange, type NodeTypes } from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { AnimatePresence, motion } from 'framer-motion'
import { MoveRight, X } from 'lucide-react'
import type { GraphData } from '../../api/types'
import { computeLayout } from './layout'
import { nodeRole, ProcessNode, type ProcessNodeType } from './ProcessNode'
import { EDGE_CYCLE, EDGE_WAIT, WaitEdge, type WaitEdgeType } from './WaitEdge'

const nodeTypes: NodeTypes = { process: ProcessNode }
const edgeTypes: EdgeTypes = { wait: WaitEdge }

type Selection = { kind: 'node' | 'edge'; id: string } | null

export function GraphLegend({ className = '' }: { className?: string }) {
  const item = (swatch: React.ReactNode, text: string) => <li className="flex items-center gap-2">{swatch}{text}</li>
  return (
    <ul className={`flex flex-wrap gap-x-5 gap-y-1.5 text-[12px] text-cocoa ${className}`} aria-label="Graph legend">
      {item(<svg width="22" height="6" aria-hidden><line x1="0" y1="3" x2="22" y2="3" stroke={EDGE_CYCLE} strokeWidth="1.8" strokeDasharray="5 3" /></svg>, 'wait on a detected cycle')}
      {item(<svg width="22" height="6" aria-hidden><line x1="0" y1="3" x2="22" y2="3" stroke={EDGE_WAIT} strokeWidth="1.8" /></svg>, 'Pi waits for a resource Pj holds')}
      {item(<span className="size-3.5 rounded-[2px] border-2 border-terracotta bg-terracotta-wash" />, 'deadlocked')}
      {item(<span className="size-3.5 rounded-[2px] border-2 border-highlight bg-highlight-wash" />, 'blocked behind it')}
      {item(<span className="size-3.5 rounded-[2px] border-2 border-sage bg-sage-wash" />, 'rolled back')}
      {item(<span className="size-3.5 rounded-[2px] border border-dashed border-taupe bg-ivory" />, 'terminated')}
    </ul>
  )
}

/**
 * Interactive Wait-For Graph rendered strictly from backend data.
 * @param layoutGraph graph whose layout is reused, so before/after views share positions
 */
export function WaitForGraph({ graph, layoutGraph, height = 460, legend = true, label }: {
  graph: GraphData; layoutGraph?: GraphData; height?: number; legend?: boolean; label?: string
}) {
  const [selection, setSelection] = useState<Selection>(null)
  const positions = useMemo(() => computeLayout(layoutGraph ?? graph), [layoutGraph, graph])

  const focus = useMemo(() => {
    if (!selection) return null
    if (selection.kind === 'node') {
      const ids = new Set([selection.id])
      graph.edges.forEach((e) => { if (e.from === selection.id || e.to === selection.id) { ids.add(e.from); ids.add(e.to) } })
      return ids
    }
    const e = graph.edges.find((x) => x.id === selection.id)
    return e ? new Set([e.from, e.to]) : null
  }, [selection, graph])

  // Node positions live in React Flow state so dragging works; selection is derived from select changes,
  // which React Flow emits for both pointer clicks and keyboard (Enter / Space on a focused node).
  const [base, setBase] = useState<ProcessNodeType[]>([])
  useEffect(() => {
    setBase(graph.nodes.map((p) => ({
      id: p.id, type: 'process', position: positions[p.id] ?? { x: 0, y: 0 }, ariaLabel: `Process ${p.id}, ${nodeRole(p).label}`,
      data: { p, selected: false, dimmed: false },
    })))
    setSelection(null)
  }, [graph, positions])
  const onNodesChange = useCallback((changes: NodeChange<ProcessNodeType>[]) => {
    changes.forEach((c) => { if (c.type === 'select' && c.selected) setSelection({ kind: 'node', id: c.id }) })
    setBase((ns) => applyNodeChanges(changes.filter((c) => c.type !== 'select'), ns))
  }, [])
  const onEdgesChange = useCallback((changes: EdgeChange[]) => {
    changes.forEach((c) => { if (c.type === 'select' && c.selected) setSelection({ kind: 'edge', id: c.id }) })
  }, [])
  const nodes = useMemo(() => base.map((n) => ({
    ...n, selected: selection?.kind === 'node' && selection.id === n.id,
    data: { ...n.data, selected: selection?.kind === 'node' && selection.id === n.id, dimmed: !!focus && !focus.has(n.id) },
  })), [base, selection, focus])

  const edges: WaitEdgeType[] = useMemo(() => {
    const seen = new Map<string, number>()
    return graph.edges.map((e) => {
      const k = seen.get(`${e.from}>${e.to}`) ?? 0
      seen.set(`${e.from}>${e.to}`, k + 1)
      const color = e.inCycle ? EDGE_CYCLE : EDGE_WAIT
      return {
        id: e.id, source: e.from, target: e.to, type: 'wait', ariaLabel: e.explanation,
        markerEnd: { type: MarkerType.ArrowClosed, color, width: 15, height: 15 },
        selected: selection?.kind === 'edge' && selection.id === e.id,
        data: { e, offset: k, selected: selection?.kind === 'edge' && selection.id === e.id, dimmed: !!focus && !(focus.has(e.from) && focus.has(e.to)) },
      }
    })
  }, [graph, selection, focus])

  const node = selection?.kind === 'node' ? graph.nodes.find((n) => n.id === selection.id) : undefined
  const edge = selection?.kind === 'edge' ? graph.edges.find((e) => e.id === selection.id) : undefined

  return (
    <div>
      <div className="paper-canvas relative overflow-hidden rounded-[6px] border border-rule" style={{ height }}
        role="group" aria-label={label ?? 'Wait-For Graph'}>
        <ReactFlow key={graph.view} nodes={nodes} edges={edges} nodeTypes={nodeTypes} edgeTypes={edgeTypes}
          fitView fitViewOptions={{ padding: 0.22, maxZoom: 1.1 }} minZoom={0.25} maxZoom={2}
          nodesConnectable={false} nodesDraggable elementsSelectable onNodesChange={onNodesChange} onEdgesChange={onEdgesChange}
          onPaneClick={() => setSelection(null)}>
          <Controls showInteractive={false} position="bottom-left" />
        </ReactFlow>
        {graph.nodes.length === 0 && <p className="absolute inset-0 grid place-items-center text-sm text-cocoa">No processes in this state.</p>}
        <AnimatePresence>
          {(node || edge) && (
            <motion.aside initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -6 }} transition={{ duration: 0.18 }}
              className="absolute right-3 top-3 z-10 w-64 max-w-[calc(100%-1.5rem)] rounded-[6px] border border-rule bg-sheet p-4 text-[13px] shadow-[var(--shadow-lift)]"
              aria-live="polite">
              <button onClick={() => setSelection(null)} className="absolute right-2 top-2 rounded p-1 text-cocoa hover:text-ink cursor-pointer" aria-label="Close details">
                <X className="size-4" />
              </button>
              {node && (
                <>
                  <p className="font-mono text-[17px] font-semibold">{node.id}</p>
                  <p className="text-[12px] text-cocoa">{node.state.toLowerCase()} · {nodeRole(node).label}</p>
                  <dl className="mt-3 grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-[12.5px]">
                    <dt className="text-cocoa">work</dt><dd className="num font-mono">{node.workDone} / {node.totalWork}</dd>
                    <dt className="text-cocoa">priority</dt><dd className="font-mono">{node.priority}</dd>
                    <dt className="text-cocoa">victim count</dt><dd className="font-mono">{node.victimCount}</dd>
                    <dt className="text-cocoa">holds</dt><dd className="font-mono">{node.heldResources.join(', ') || '—'}</dd>
                    <dt className="text-cocoa">waits for</dt><dd className="font-mono">{node.waitingFor.join(', ') || '—'}</dd>
                    <dt className="text-cocoa">checkpoints</dt><dd className="font-mono">{node.checkpointCount} (last @ {node.lastCheckpointWork ?? '—'})</dd>
                  </dl>
                </>
              )}
              {edge && (
                <>
                  <p className="flex items-center gap-2 font-mono text-[16px] font-semibold">{edge.from}<MoveRight className="size-4 text-cocoa" />{edge.to}</p>
                  <p className="mt-2 leading-relaxed text-ink">{edge.explanation}</p>
                  <p className={`mt-2 text-[12px] ${edge.inCycle ? 'text-terracotta-deep' : 'text-cocoa'}`}>
                    {edge.inCycle ? 'This edge is part of a detected cycle.' : 'Not part of a detected cycle.'}
                  </p>
                </>
              )}
            </motion.aside>
          )}
        </AnimatePresence>
      </div>
      {legend && <GraphLegend className="mt-3" />}
    </div>
  )
}
