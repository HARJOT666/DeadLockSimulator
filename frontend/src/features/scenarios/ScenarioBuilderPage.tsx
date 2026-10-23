import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { CircleDashed, Eye, FileJson, Play, Save } from 'lucide-react'
import { api } from '../../api/endpoints'
import type { DetectionResult, ScenarioConfig, Template } from '../../api/types'
import { useAsync } from '../../hooks/useAsync'
import { emptyConfig, validateConfig } from '../../lib/scenarioValidation'
import { PageIntro } from '../../components/layout/PageIntro'
import { Button } from '../../components/ui/Button'
import { Input, NumberInput } from '../../components/ui/Field'
import { ErrorNotice, InlineError, Loading, Skeleton, Spinner } from '../../components/ui/Feedback'
import { StatusText } from '../../components/ui/Status'
import { Disclosure } from '../../components/ui/Disclosure'
import { ProcessesEditor, ResourcesEditor, SequenceEditor } from './builder/editors'
import { GeneratorForm } from './builder/GeneratorForm'
import { ScenarioSketch } from './builder/ScenarioSketch'

type Source = 'examples' | 'generator' | 'blank'

function Step({ n, title, note, children }: { n: string; title: string; note?: string; children: ReactNode }) {
  return (
    <section className="border-t border-rule pb-12 pt-6">
      <div className="mb-6 flex items-baseline gap-4">
        <span className="font-mono text-[11px] tracking-[0.14em] text-terracotta-deep">{n}</span>
        <div>
          <h2 className="display text-[22px]">{title}</h2>
          {note && <p className="mt-1 max-w-[560px] text-[13.5px] text-cocoa">{note}</p>}
        </div>
      </div>
      {children}
    </section>
  )
}

function PreviewResult({ d, stale }: { d: DetectionResult; stale: boolean }) {
  return (
    <div className={stale ? 'opacity-50' : ''}>
      {d.deadlocked
        ? <p className="flex items-center gap-1.5 text-[14px] font-medium text-terracotta-deep"><CircleDashed className="size-4" /> Deadlock — {d.cycles.length} cycle{d.cycles.length > 1 ? 's' : ''}</p>
        : <StatusText tone="ok" label="No deadlock: the trace ends without a cycle" />}
      {d.cycles.slice(0, 4).map((c, i) => (
        <p key={i} className="mt-2 font-mono text-[12.5px] text-ink">{[...c.processes, c.processes[0]].join(' → ')}</p>
      ))}
      <dl className="mt-4 grid grid-cols-2 gap-x-4 gap-y-2 text-[12.5px]">
        <dt className="text-cocoa">deadlocked</dt><dd className="font-mono">{d.deadlockedProcesses.join(' ') || '—'}</dd>
        <dt className="text-cocoa">blocked behind</dt><dd className="font-mono">{d.blockedProcesses.join(' ') || '—'}</dd>
        <dt className="text-cocoa">graph |V| / |E|</dt><dd className="num font-mono">{d.vertexCount} / {d.edgeCount}</dd>
      </dl>
      {stale && <p className="mt-3 text-[12px] text-gold-deep">Edited since this preview — run it again.</p>}
    </div>
  )
}

export default function ScenarioBuilderPage() {
  const { id } = useParams()
  const editingId = id ? Number(id) : null
  const navigate = useNavigate()
  const [source, setSource] = useState<Source>(editingId ? 'blank' : 'examples')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [config, setConfig] = useState<ScenarioConfig>(emptyConfig())
  const [loadedFrom, setLoadedFrom] = useState<string | null>(null)
  const [preview, setPreview] = useState<DetectionResult | null>(null)
  const [previewKey, setPreviewKey] = useState('')
  const [previewing, setPreviewing] = useState(false)
  const [previewError, setPreviewError] = useState<unknown>(null)
  const [saving, setSaving] = useState<'save' | 'run' | null>(null)
  const [saveError, setSaveError] = useState<unknown>(null)
  const [nameError, setNameError] = useState<string>()

  const templates = useAsync(() => api.templates(), [])
  const existing = useAsync(() => (editingId ? api.scenario(editingId) : Promise.resolve(null)), [editingId])
  useEffect(() => {
    const s = existing.data
    if (s?.config) { setName(s.name); setDescription(s.description ?? ''); setConfig(s.config) }
  }, [existing.data])

  const problems = useMemo(() => validateConfig(config), [config])
  const configKey = useMemo(() => JSON.stringify(config), [config])

  const load = (t: Template) => {
    setConfig(t.config); setName(t.name); setDescription(t.description)
    setPreview(t.preview); setPreviewKey(JSON.stringify(t.config)); setLoadedFrom(t.name)
    document.getElementById('identity')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  const runPreview = async () => {
    setPreviewing(true); setPreviewError(null)
    try { setPreview(await api.preview(config)); setPreviewKey(configKey) } catch (e) { setPreviewError(e) } finally { setPreviewing(false) }
  }

  const save = async (andRun: boolean) => {
    if (!name.trim()) { setNameError('Give the scenario a name.'); document.getElementById('identity')?.scrollIntoView({ behavior: 'smooth' }); return }
    setNameError(undefined)
    if (problems.length) return
    setSaving(andRun ? 'run' : 'save'); setSaveError(null)
    try {
      const body = { name: name.trim(), description, config }
      const saved = editingId ? await api.updateScenario(editingId, body) : await api.createScenario(body)
      if (andRun) navigate(`/simulations/${(await api.startSimulation(saved.id)).id}`)
      else navigate('/scenarios')
    } catch (e) { setSaveError(e); setSaving(null) }
  }

  if (editingId && existing.loading) return <Loading label="Opening scenario" />
  if (editingId && existing.error) return <ErrorNotice error={existing.error} onRetry={existing.reload} />

  return (
    <>
      <PageIntro trail={[{ label: 'Scenarios', to: '/scenarios' }, { label: editingId ? `Edit · ${name || '…'}` : 'New scenario' }]}
        title={editingId ? 'Revise a scenario' : 'Compose a scenario'}
        lede="Define processes, single-instance resources and the exact order of their requests. The server replays the trace deterministically — the same scenario always reproduces the same state." />

      <div className="grid gap-12 xl:grid-cols-[minmax(0,1fr)_340px]">
        <div className="min-w-0">
          {!editingId && (
            <Step n="01" title="Starting point" note="Load a reproducible example or a seeded scenario, then refine it below — or start from two blank processes.">
              <div role="tablist" className="mb-6 flex gap-6 border-b border-rule text-[14px]">
                {([['examples', 'Examples'], ['generator', 'Seeded generator'], ['blank', 'Blank']] as const).map(([k, l]) => (
                  <button key={k} role="tab" aria-selected={source === k} onClick={() => { setSource(k); if (k === 'blank') { setConfig(emptyConfig()); setLoadedFrom(null); setPreview(null) } }}
                    className={`-mb-px border-b-2 pb-2.5 cursor-pointer ${source === k ? 'border-terracotta font-medium text-ink' : 'border-transparent text-cocoa hover:text-ink'}`}>{l}</button>
                ))}
              </div>
              {source === 'examples' && (templates.loading ? <Skeleton className="h-48" /> : templates.error ? <ErrorNotice error={templates.error} onRetry={templates.reload} /> : (
                <ul className="divide-y divide-rule-soft border-y border-rule-soft">
                  {templates.data!.map((t) => (
                    <li key={t.key}>
                      <button onClick={() => load(t)} className="group grid w-full gap-1 py-4 text-left sm:grid-cols-[minmax(0,1fr)_170px] sm:gap-6 cursor-pointer">
                        <span>
                          <span className="block text-[15.5px] font-medium text-ink group-hover:text-terracotta-deep">{t.name}</span>
                          <span className="mt-1 block text-[13.5px] leading-relaxed text-cocoa">{t.description}</span>
                        </span>
                        <span className="text-[12.5px] sm:text-right">
                          {t.preview.deadlocked
                            ? <span className="text-terracotta-deep">{t.preview.cycles.length} cycle{t.preview.cycles.length > 1 ? 's' : ''} · {t.preview.deadlockedProcesses.length} deadlocked</span>
                            : <span className="text-sage-deep">no deadlock</span>}
                          <span className="block font-mono text-[11.5px] text-cocoa">{t.config.processes.length}P · {t.config.resources.length}R · {t.config.requestSequence.length} ops</span>
                        </span>
                      </button>
                    </li>
                  ))}
                </ul>
              ))}
              {source === 'generator' && <GeneratorForm onGenerated={load} />}
              {source === 'blank' && <p className="text-[14px] text-cocoa">Two processes and two resources are ready below. Use the deadlock helper in step 05 to create a circular wait quickly.</p>}
            </Step>
          )}

          <div id="identity" className="scroll-mt-8">
            <Step n={editingId ? '01' : '02'} title="Identity" note={loadedFrom ? `Loaded from “${loadedFrom}”. Rename it if you change the configuration.` : undefined}>
              <div className="grid gap-5 md:grid-cols-[minmax(0,1fr)_minmax(0,1.3fr)_150px]">
                <Input label="Name" value={name} onChange={(e) => setName(e.target.value)} error={nameError} maxLength={120} />
                <Input label="Description" value={description} onChange={(e) => setDescription(e.target.value)} maxLength={2000} />
                <div>
                  <label htmlFor="ckpt" className="mb-1.5 block text-[13px] font-medium">Checkpoint interval</label>
                  <NumberInput id="ckpt" className="!h-11" min={0} value={config.checkpointInterval} onChange={(v) => setConfig({ ...config, checkpointInterval: v })} />
                </div>
              </div>
            </Step>
          </div>
          <Step n={editingId ? '02' : '03'} title="Processes" note="Higher priority and more work make a process costlier to choose as a recovery victim; a past victim count models earlier recoveries.">
            <ProcessesEditor config={config} onChange={setConfig} />
          </Step>
          <Step n={editingId ? '03' : '04'} title="Resources & initial allocations">
            <ResourcesEditor config={config} onChange={setConfig} />
          </Step>
          <Step n={editingId ? '04' : '05'} title="Request sequence" note="One globally ordered trace. A REQUEST naming several resources is an AND-request; operations of a blocked process wait until it is unblocked.">
            <SequenceEditor config={config} onChange={setConfig} />
          </Step>
          <Disclosure summary={<span className="inline-flex items-center gap-2"><FileJson className="size-4" /> Configuration as JSON</span>} className="border-t border-rule">
            <pre className="max-h-80 overflow-auto rounded-[4px] border border-rule bg-sheet p-4 font-mono text-[11.5px] text-cocoa">{JSON.stringify(config, null, 2)}</pre>
          </Disclosure>
        </div>

        <aside className="xl:sticky xl:top-8 xl:self-start" aria-label="Preview and save">
          <div className="rounded-[6px] border border-rule bg-sheet p-5 shadow-[var(--shadow-paper)]">
            <p className="kicker">Workbench</p>
            <div className="mt-4"><ScenarioSketch config={config} detection={preview && previewKey === configKey ? preview : null} /></div>
            <div className="mt-6 border-t border-rule-soft pt-5">
              <p className="mb-3 text-[13px] font-medium">Server preview</p>
              {previewing ? <p className="flex items-center gap-2 text-[13px] text-cocoa"><Spinner /> Replaying the trace…</p>
                : preview ? <PreviewResult d={preview} stale={previewKey !== configKey} />
                  : <p className="text-[13px] text-cocoa">Replay the trace on the server to see whether it deadlocks.</p>}
              {previewError ? <div className="mt-3"><InlineError error={previewError} /></div> : null}
            </div>
            {problems.length > 0 && (
              <div className="mt-5 border-l-2 border-gold pl-3 text-[12.5px] text-gold-deep" role="alert">
                <p className="font-medium">Before saving</p>
                <ul className="mt-1 space-y-0.5">{problems.slice(0, 7).map((p) => <li key={p}>{p}</li>)}</ul>
              </div>
            )}
            {saveError ? <div className="mt-4"><InlineError error={saveError} /></div> : null}
            <div className="mt-6 grid gap-2">
              <Button icon={<Play className="size-4" />} onClick={() => save(true)} loading={saving === 'run'} disabled={problems.length > 0 || !!saving}>Save & run simulation</Button>
              <div className="grid grid-cols-2 gap-2">
                <Button variant="secondary" size="sm" icon={<Eye className="size-3.5" />} onClick={runPreview} loading={previewing} disabled={problems.length > 0}>Preview</Button>
                <Button variant="secondary" size="sm" icon={<Save className="size-3.5" />} onClick={() => save(false)} loading={saving === 'save'} disabled={problems.length > 0 || !!saving}>{editingId ? 'Save' : 'Save only'}</Button>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </>
  )
}
