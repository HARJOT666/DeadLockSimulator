/**
 * Technical illustrations in a printed-diagram style: espresso line work on paper, terracotta only
 * for the critical path, small mono annotations. They explain concepts; they never stand in for data.
 */
const INK = '#493528'
const COCOA = '#786858'
const TERRA = '#A96445'
const SAGE = '#74816A'
const GOLD = '#D9C49D'
const SHEET = '#FFFCF6'
const label = { fontFamily: 'JetBrains Mono Variable, monospace', fontSize: 9.5, fill: COCOA, letterSpacing: '0.04em' }
const nodeLabel = { fontFamily: 'JetBrains Mono Variable, monospace', fontSize: 12, fontWeight: 600 }

function Arrow({ id, color }: { id: string; color: string }) {
  return (
    <marker id={id} viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
      <path d="M0 1 L9 5 L0 9" fill="none" stroke={color} strokeWidth="1.4" />
    </marker>
  )
}

function Proc({ x, y, id, dark = true, struck = false }: { x: number; y: number; id: string; dark?: boolean; struck?: boolean }) {
  return (
    <g>
      <circle cx={x} cy={y} r="19" fill={dark ? INK : SHEET} stroke={INK} strokeWidth="1.3" strokeDasharray={struck ? '3 3' : undefined} />
      <text x={x} y={y + 4} textAnchor="middle" style={nodeLabel} fill={dark ? SHEET : COCOA}>{id}</text>
    </g>
  )
}

/** Fig. — a three-process circular wait with the resources that explain each edge. */
export function CycleDiagram({ className = '' }: { className?: string }) {
  return (
    <svg viewBox="0 0 320 260" className={className} role="img" aria-label="Three processes each waiting for a resource held by the next, forming a cycle P1 to P2 to P3 and back to P1.">
      <defs><Arrow id="cyc-a" color={TERRA} /></defs>
      <path d="M181 62 C 236 76, 262 120, 254 160" fill="none" stroke={TERRA} strokeWidth="1.6" markerEnd="url(#cyc-a)" />
      <path d="M222 205 C 180 226, 140 226, 98 205" fill="none" stroke={TERRA} strokeWidth="1.6" markerEnd="url(#cyc-a)" />
      <path d="M66 160 C 58 120, 84 76, 139 62" fill="none" stroke={TERRA} strokeWidth="1.6" markerEnd="url(#cyc-a)" />
      <Proc x={160} y={52} id="P1" />
      <Proc x={244} y={190} id="P2" />
      <Proc x={76} y={190} id="P3" />
      <text x={258} y={104} style={label}>waits R2</text>
      <text x={160} y={244} textAnchor="middle" style={label}>waits R3</text>
      <text x={14} y={104} style={label}>waits R1</text>
      <text x={160} y={140} textAnchor="middle" style={{ ...label, fill: TERRA, fontSize: 10 }}>cycle ⇒ deadlock</text>
      <text x={160} y={154} textAnchor="middle" style={label}>(single-instance)</text>
    </svg>
  )
}

/** Fig. — three-colour DFS: white, gray (on the path) and black vertices, and the back edge. */
export function DfsDiagram({ className = '' }: { className?: string }) {
  const node = (x: number, y: number, id: string, fill: string, text: string) => (
    <g>
      <circle cx={x} cy={y} r="17" fill={fill} stroke={INK} strokeWidth="1.3" />
      <text x={x} y={y + 4} textAnchor="middle" style={nodeLabel} fill={text}>{id}</text>
    </g>
  )
  return (
    <svg viewBox="0 0 320 260" className={className} role="img" aria-label="Depth-first search: P1 and P2 are gray on the current path, P4 is black, P5 is still white; the edge from P3 back to P1 is a back edge that closes a cycle.">
      <defs><Arrow id="dfs-a" color={INK} /><Arrow id="dfs-b" color={TERRA} /></defs>
      <g stroke={INK} strokeWidth="1.2" fill="none" markerEnd="url(#dfs-a)">
        <path d="M70 66 L70 112" /><path d="M70 146 L70 192" /><path d="M86 136 L160 168" /><path d="M236 70 L190 160" />
      </g>
      <path d="M54 206 C 10 170, 12 80, 54 58" fill="none" stroke={TERRA} strokeWidth="1.6" strokeDasharray="4 3" markerEnd="url(#dfs-b)" />
      {node(70, 48, 'P1', GOLD, INK)}
      {node(70, 129, 'P2', GOLD, INK)}
      {node(70, 210, 'P3', GOLD, INK)}
      {node(176, 180, 'P4', INK, SHEET)}
      {node(244, 54, 'P5', SHEET, COCOA)}
      <text x={20} y={22} style={{ ...label, fill: TERRA }}>back edge → gray</text>
      <g transform="translate(150 222)">
        <circle cx="0" cy="0" r="5" fill={SHEET} stroke={INK} /><text x="9" y="3" style={label}>white</text>
        <circle cx="58" cy="0" r="5" fill={GOLD} stroke={INK} /><text x="67" y="3" style={label}>gray</text>
        <circle cx="110" cy="0" r="5" fill={INK} /><text x="119" y="3" style={label}>black</text>
      </g>
      <text x={150} y={248} style={label}>O(V + E)</text>
    </svg>
  )
}

/** Fig. — a process timeline with checkpoints; rollback returns to the last valid one. */
export function CheckpointDiagram({ className = '' }: { className?: string }) {
  const ticks = [40, 100, 160, 220]
  return (
    <svg viewBox="0 0 320 260" className={className} role="img" aria-label="A process timeline with checkpoints. The resource R1 is acquired after checkpoint 2; to preempt R1 the process is rolled back to checkpoint 2, losing only the work after it.">
      <defs><Arrow id="cp-a" color={TERRA} /><Arrow id="cp-b" color={INK} /></defs>
      <text x={24} y={40} style={label}>work →</text>
      <line x1={24} y1={130} x2={292} y2={130} stroke={INK} strokeWidth="1.3" markerEnd="url(#cp-b)" />
      {ticks.map((x, i) => (
        <g key={x}>
          <line x1={x} y1={120} x2={x} y2={140} stroke={INK} strokeWidth="1.3" />
          <text x={x} y={158} textAnchor="middle" style={label}>#{i}</text>
        </g>
      ))}
      <rect x={176} y={84} width={34} height={20} fill={SHEET} stroke={INK} strokeWidth="1.2" />
      <text x={193} y={98} textAnchor="middle" style={{ ...nodeLabel, fontSize: 10 }} fill={INK}>R1</text>
      <line x1={193} y1={104} x2={193} y2={128} stroke={INK} strokeWidth="1" strokeDasharray="2 2" />
      <text x={214} y={78} style={label}>acquired</text>
      <rect x={160} y={124} width={100} height={12} fill={TERRA} opacity="0.22" />
      <text x={262} y={116} style={{ ...label, fill: TERRA }}>lost</text>
      <path d="M258 172 C 230 214, 176 214, 162 176" fill="none" stroke={TERRA} strokeWidth="1.6" markerEnd="url(#cp-a)" />
      <text x={150} y={232} style={{ ...label, fill: TERRA }}>rollback to #2</text>
      <text x={24} y={250} style={label}>latest checkpoint not holding R1</text>
    </svg>
  )
}

/** Fig. — after recovery, the graph is rebuilt and searched again: no cycle remains. */
export function VerifyDiagram({ className = '' }: { className?: string }) {
  return (
    <svg viewBox="0 0 320 260" className={className} role="img" aria-label="After recovery P3 is terminated and its edges are gone; P1 waits for P2, which can run. Re-running DFS finds no cycle.">
      <defs><Arrow id="vf-a" color={SAGE} /></defs>
      <path d="M181 62 C 236 76, 262 120, 254 160" fill="none" stroke={SAGE} strokeWidth="1.6" markerEnd="url(#vf-a)" />
      <path d="M222 205 C 180 226, 140 226, 98 205" fill="none" stroke={COCOA} strokeWidth="1" strokeDasharray="2 4" opacity="0.6" />
      <path d="M66 160 C 58 120, 84 76, 139 62" fill="none" stroke={COCOA} strokeWidth="1" strokeDasharray="2 4" opacity="0.6" />
      <Proc x={160} y={52} id="P1" />
      <Proc x={244} y={190} id="P2" />
      <Proc x={76} y={190} id="P3" dark={false} struck />
      <line x1={64} y1={178} x2={88} y2={202} stroke={TERRA} strokeWidth="1.4" />
      <g transform="translate(160 132)">
        <circle r="20" fill={SHEET} stroke={SAGE} strokeWidth="1.5" />
        <path d="M-8 0 L-2 6 L9 -6" fill="none" stroke={SAGE} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      </g>
      <text x={160} y={172} textAnchor="middle" style={{ ...label, fill: '#5B6752' }}>re-detect: no cycle</text>
      <text x={30} y={238} style={label}>P3 terminated · R3 handed to P2</text>
    </svg>
  )
}
