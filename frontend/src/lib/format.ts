export const fmtScore = (v: number) => v.toFixed(3)
export const fmtDate = (iso: string) =>
  new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
export const fmtRelative = (iso: string) => {
  const s = (Date.now() - new Date(iso).getTime()) / 1000
  if (s < 60) return 'just now'
  if (s < 3600) return `${Math.floor(s / 60)} min ago`
  if (s < 86400) return `${Math.floor(s / 3600)} h ago`
  return `${Math.floor(s / 86400)} d ago`
}
export const fmtMicros = (us: number) => (us < 1000 ? `${us} µs` : `${(us / 1000).toFixed(1)} ms`)
