import type { DetectionResult, ScenarioConfig } from '../../../api/types'

/**
 * Static sketch of the configuration: processes above, resources below. Solid ink lines are
 * initial allocations; thin sage lines are resources each process requests somewhere in the trace.
 * It describes the input — the final state is only known after the server replays it.
 */
export function ScenarioSketch({ config, detection }: { config: ScenarioConfig; detection: DetectionResult | null }) {
  const P = config.processes.map((p) => p.id)
  const R = config.resources
  if (P.length === 0 || R.length === 0) return <p className="text-[13px] text-cocoa">Add processes and resources to see the sketch.</p>
  if (P.length > 12 || R.length > 18) {
    return <p className="text-[13px] text-cocoa">{P.length} processes and {R.length} resources — too many to sketch legibly. Use the preview below.</p>
  }
  const W = 320
  const px = (i: number) => 24 + (i * (W - 48)) / Math.max(1, P.length - 1)
  const rx = (i: number) => 24 + (i * (W - 48)) / Math.max(1, R.length - 1)
  const center = (n: number, f: (i: number) => number, i: number) => (n === 1 ? W / 2 : f(i))
  const requests = new Set<string>()
  config.requestSequence.forEach((o) => { if (o.type === 'REQUEST') o.resources.forEach((r) => requests.add(`${o.processId}|${r}`)) })
  const dead = new Set(detection?.deadlockedProcesses ?? [])
  const blocked = new Set(detection?.blockedProcesses ?? [])
  return (
    <figure>
      <svg viewBox={`0 0 ${W} 150`} className="w-full" role="img"
        aria-label={`Sketch of ${P.length} processes and ${R.length} resources with their allocations and requests.`}>
        {[...requests].map((k) => {
          const [p, r] = k.split('|')
          const i = P.indexOf(p), j = R.indexOf(r)
          if (i < 0 || j < 0) return null
          return <line key={k} x1={center(P.length, px, i)} y1={34} x2={center(R.length, rx, j)} y2={116} stroke="#74816A" strokeWidth="1" opacity="0.7" />
        })}
        {config.initialAllocations.map((a) => {
          const i = P.indexOf(a.processId), j = R.indexOf(a.resourceId)
          if (i < 0 || j < 0) return null
          return <line key={a.resourceId} x1={center(R.length, rx, j)} y1={116} x2={center(P.length, px, i)} y2={34} stroke="#493528" strokeWidth="1.8" />
        })}
        {P.map((p, i) => (
          <g key={p}>
            <circle cx={center(P.length, px, i)} cy={24} r={10} fill="#FFFCF6"
              stroke={dead.has(p) ? '#A96445' : blocked.has(p) ? '#C9A866' : '#493528'} strokeWidth={dead.has(p) || blocked.has(p) ? 3 : 1.4} />
            <text x={center(P.length, px, i)} y={8} textAnchor="middle" fontSize="8.5" fontFamily="JetBrains Mono Variable" fill="#786858">{p}</text>
          </g>
        ))}
        {R.map((r, j) => (
          <g key={r}>
            <rect x={center(R.length, rx, j) - 7} y={116} width={14} height={14} fill="#FFFCF6" stroke="#786858" strokeWidth="1.2" />
            <text x={center(R.length, rx, j)} y={144} textAnchor="middle" fontSize="8.5" fontFamily="JetBrains Mono Variable" fill="#786858">{r}</text>
          </g>
        ))}
      </svg>
      <figcaption className="mt-2 text-[11.5px] leading-snug text-cocoa">
        <span className="text-ink">○</span> process · <span className="text-ink">□</span> resource · thick line: allocated at start ·
        thin line: requested in the trace{detection ? ' · ring: deadlocked (terracotta) or blocked (gold) in the preview' : ''}
      </figcaption>
    </figure>
  )
}
