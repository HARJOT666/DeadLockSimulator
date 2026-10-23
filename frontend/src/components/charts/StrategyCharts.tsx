import { useReducedMotion } from 'framer-motion'
import { Bar, BarChart, CartesianGrid, Cell, LabelList, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { RecoveryResult, StrategyType } from '../../api/types'
import { METRIC_LABELS, STRATEGIES, STRATEGY_ORDER } from '../../lib/strategy'
import { ChartTooltip } from './ChartTooltip'

const AXIS = { stroke: '#DCD0BE', tick: { fill: '#786858', fontSize: 11.5, fontFamily: 'DM Sans Variable' }, tickLine: false }
const GRID = '#E7DDCE'

/** Axis tick that wraps a multi-word category label onto up to two lines. */
function WrappedTick({ x, y, payload }: { x?: number; y?: number; payload?: { value: string } }) {
  const words = String(payload?.value ?? '').split(' ')
  const lines = words.length > 1 ? [words.slice(0, Math.ceil(words.length / 2)).join(' '), words.slice(Math.ceil(words.length / 2)).join(' ')] : words
  return (
    <text x={x} y={(y ?? 0) + 12} textAnchor="middle" fill="#786858" fontSize={11} fontFamily="DM Sans Variable">
      {lines.map((l, i) => <tspan key={i} x={x} dy={i === 0 ? 0 : 13}>{l}</tspan>)}
    </text>
  )
}

export function StrategyLegend({ strategies, className = '' }: { strategies: StrategyType[]; className?: string }) {
  return (
    <ul className={`flex flex-wrap gap-x-5 gap-y-1 text-[12.5px] text-cocoa ${className}`} aria-label="Legend">
      {strategies.map((s) => (
        <li key={s} className="flex items-center gap-2"><span className="size-2.5 rounded-[1px]" style={{ background: STRATEGIES[s].color }} />{STRATEGIES[s].label}</li>
      ))}
    </ul>
  )
}

/** One measure per strategy: a horizontal bar each, identity colour, value labelled at the end. */
export function StrategyValueBars({ data, format = (v) => String(v), height = 150 }: {
  data: { strategy: StrategyType; value: number }[]; format?: (v: number) => string; height?: number
}) {
  const reduce = useReducedMotion()
  const rows = STRATEGY_ORDER.filter((s) => data.some((d) => d.strategy === s))
    .map((s) => ({ name: STRATEGIES[s].short, value: data.find((d) => d.strategy === s)!.value, fill: STRATEGIES[s].color }))
  return (
    <ResponsiveContainer width="100%" height={height}>
      <BarChart data={rows} layout="vertical" margin={{ top: 2, right: 56, bottom: 2, left: 0 }} barCategoryGap={12}>
        <CartesianGrid stroke={GRID} horizontal={false} />
        <XAxis type="number" {...AXIS} axisLine={false} tickFormatter={(v) => format(Number(v))} />
        <YAxis type="category" dataKey="name" {...AXIS} axisLine={false} width={82} />
        <Tooltip cursor={{ fill: 'rgb(120 104 88 / 0.07)' }} content={<ChartTooltip valueFormatter={(v) => format(v)} />} />
        <Bar dataKey="value" name="Value" radius={[0, 4, 4, 0]} maxBarSize={16} isAnimationActive={!reduce} animationDuration={600}>
          {rows.map((r) => <Cell key={r.name} fill={r.fill} />)}
          <LabelList dataKey="value" position="right" formatter={(v) => format(Number(v))} fill="#493528" fontSize={11.5} fontFamily="JetBrains Mono Variable" />
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  )
}

const COMPARED = ['workLost', 'processesTerminated', 'processesRolledBack', 'resourcesReclaimed', 'recoverySteps', 'starvationRisk'] as const

/**
 * Each metric divided by its scenario-intrinsic normaliser, so different units share one 0–1 axis.
 * Raw values are in the tooltip and in the adjacent table.
 */
export function MetricProfileChart({ results, height = 290 }: { results: RecoveryResult[]; height?: number }) {
  const reduce = useReducedMotion()
  const strategies = STRATEGY_ORDER.filter((s) => results.some((r) => r.strategy === s))
  const rows = COMPARED.map((metric) => {
    const row: Record<string, number | string> = { metric: METRIC_LABELS[metric].label }
    for (const s of strategies) {
      const c = results.find((r) => r.strategy === s)!.metrics.scoreBreakdown.find((x) => x.metric === metric)
      row[s] = c ? c.normalised : 0
      row[`${s}_raw`] = c ? c.rawValue : 0
    }
    return row
  })
  return (
    <div>
      <ResponsiveContainer width="100%" height={height}>
        <BarChart data={rows} margin={{ top: 8, right: 4, bottom: 0, left: -18 }} barGap={2} barCategoryGap="24%">
          <CartesianGrid stroke={GRID} vertical={false} />
          <XAxis dataKey="metric" stroke="#DCD0BE" tickLine={false} interval={0} height={40} tick={<WrappedTick />} />
          <YAxis {...AXIS} axisLine={false} domain={[0, 1]} ticks={[0, 0.5, 1]} />
          <Tooltip cursor={{ fill: 'rgb(120 104 88 / 0.07)' }}
            content={({ active, payload, label }) => (
              <ChartTooltip active={active} label={label} payload={payload as never}
                valueFormatter={(v, name, row) => {
                  const s = strategies.find((x) => STRATEGIES[x].short === name)!
                  return `${v.toFixed(2)}  (raw ${row[`${s}_raw`]})`
                }} />
            )} />
          {strategies.map((s) => (
            <Bar key={s} dataKey={s} name={STRATEGIES[s].short} fill={STRATEGIES[s].color} radius={[4, 4, 0, 0]} maxBarSize={14}
              stroke="#FFFCF6" strokeWidth={2} isAnimationActive={!reduce} animationDuration={600} />
          ))}
        </BarChart>
      </ResponsiveContainer>
      <StrategyLegend strategies={strategies} className="mt-2 pl-6" />
    </div>
  )
}
