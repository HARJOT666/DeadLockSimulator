import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Pencil, Play, Plus, Trash2 } from 'lucide-react'
import { api } from '../../api/endpoints'
import type { Scenario } from '../../api/types'
import { useAsync } from '../../hooks/useAsync'
import { fmtRelative } from '../../lib/format'
import { PageIntro } from '../../components/layout/PageIntro'
import { Button } from '../../components/ui/Button'
import { Modal } from '../../components/ui/Modal'
import { EmptyState, ErrorNotice, InlineError, Skeleton } from '../../components/ui/Feedback'
import { Reveal } from '../../components/motion/Reveal'
import { CycleDiagram } from '../../components/illustrations/Diagrams'

export default function ScenariosPage() {
  const navigate = useNavigate()
  const { data, error, loading, reload } = useAsync(() => api.scenarios(), [])
  const [running, setRunning] = useState<number | null>(null)
  const [actionError, setActionError] = useState<unknown>(null)
  const [toDelete, setToDelete] = useState<Scenario | null>(null)
  const [deleting, setDeleting] = useState(false)

  const run = async (s: Scenario) => {
    setRunning(s.id); setActionError(null)
    try { navigate(`/simulations/${(await api.startSimulation(s.id)).id}`) } catch (e) { setActionError(e); setRunning(null) }
  }
  const confirmDelete = async () => {
    if (!toDelete) return
    setDeleting(true)
    try { await api.deleteScenario(toDelete.id); setToDelete(null); reload() } catch (e) { setActionError(e); setToDelete(null) } finally { setDeleting(false) }
  }

  return (
    <>
      <PageIntro trail={[{ label: 'Workspace' }, { label: 'Scenarios' }]} title="Scenarios"
        lede="Saved, reproducible experiment configurations. Running one replays its request sequence and records the resulting state as a new run."
        actions={<Button icon={<Plus className="size-4" />} onClick={() => navigate('/scenarios/new')}>New scenario</Button>} />
      {actionError ? <div className="mb-6"><InlineError error={actionError} /></div> : null}

      {loading ? <div className="space-y-6">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-20" />)}</div>
        : error ? <ErrorNotice error={error} onRetry={reload} />
          : !data?.length ? (
            <EmptyState title="No scenarios saved yet."
              body="Compose one from a built-in example, the seeded generator, or your own request sequence. Each saved scenario can be run as often as you like."
              action={<Button icon={<Plus className="size-4" />} onClick={() => navigate('/scenarios/new')}>Compose a scenario</Button>}
              figure={<CycleDiagram className="mx-auto w-full max-w-[280px]" />} />
          ) : (
            <ol className="border-t border-rule">
              {data.map((s, i) => (
                <Reveal as="li" key={s.id} delay={Math.min(i, 6) * 0.04} className="border-b border-rule">
                  <article className="grid gap-4 py-7 md:grid-cols-[48px_minmax(0,1fr)_auto] md:gap-8">
                    <span className="num font-mono text-[12px] text-cocoa">{String(i + 1).padStart(2, '0')}</span>
                    <div className="min-w-0">
                      <h2 className="display text-[24px] leading-tight">{s.name}</h2>
                      {s.description && <p className="mt-2 max-w-[640px] text-[14.5px] leading-relaxed text-cocoa">{s.description}</p>}
                      <p className="mt-3 font-mono text-[12px] text-cocoa">
                        {s.processCount} processes · {s.resourceCount} resources · {s.operationCount} operations · checkpoint every {s.checkpointInterval || '—'}
                        {s.seed != null && <> · seed {s.seed}</>}
                      </p>
                      <p className="mt-1 text-[12.5px] text-cocoa">{s.runCount} run{s.runCount === 1 ? '' : 's'} · edited {fmtRelative(s.updatedAt)}</p>
                    </div>
                    <div className="flex items-start gap-1 md:flex-col md:items-end">
                      <Button size="sm" icon={<Play className="size-3.5" />} loading={running === s.id} onClick={() => run(s)}>Run simulation</Button>
                      <div className="flex">
                        <Link to={`/scenarios/${s.id}/edit`} className="inline-flex h-8 items-center gap-1.5 px-3 text-[13px] text-cocoa hover:text-ink"><Pencil className="size-3.5" /> Edit</Link>
                        <button onClick={() => setToDelete(s)} className="inline-flex h-8 items-center gap-1.5 px-3 text-[13px] text-cocoa hover:text-danger cursor-pointer" aria-label={`Delete ${s.name}`}>
                          <Trash2 className="size-3.5" /> Delete
                        </button>
                      </div>
                    </div>
                  </article>
                </Reveal>
              ))}
            </ol>
          )}

      <Modal open={!!toDelete} onClose={() => setToDelete(null)} title="Delete this scenario?"
        footer={<><Button variant="quiet" onClick={() => setToDelete(null)}>Keep it</Button><Button variant="danger" loading={deleting} onClick={confirmDelete}>Delete scenario</Button></>}>
        <p><span className="text-ink">{toDelete?.name}</span> will be removed. Its existing runs stay in the run log — each run stores the exact configuration it used, so its results remain reproducible.</p>
      </Modal>
    </>
  )
}
