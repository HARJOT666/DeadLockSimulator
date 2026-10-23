import type { TooltipContentProps } from 'recharts'

/** Paper tooltip: text in ink, a small swatch carries series identity. */
export function ChartTooltip({ active, payload, label, valueFormatter }: Partial<TooltipContentProps<number, string>> & {
  valueFormatter?: (v: number, name: string, row: Record<string, number>) => string
}) {
  if (!active || !payload?.length) return null
  return (
    <div className="rounded-[5px] border border-rule bg-sheet px-3 py-2 text-[12px] shadow-[var(--shadow-lift)]">
      {label !== undefined && <p className="mb-1 font-medium text-ink">{String(label)}</p>}
      <ul className="space-y-0.5">
        {payload.map((p) => (
          <li key={String(p.dataKey) + String(p.name)} className="flex items-center gap-2 text-cocoa">
            <span className="size-2 rounded-[1px]" style={{ background: (p.payload as { fill?: string })?.fill ?? p.color }} />
            {p.name}
            <span className="num ml-auto pl-4 font-mono text-ink">
              {valueFormatter ? valueFormatter(Number(p.value), String(p.name), p.payload as Record<string, number>) : String(p.value)}
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}
