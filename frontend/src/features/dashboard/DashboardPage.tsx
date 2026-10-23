import { Link, useNavigate } from 'react-router-dom'
import { ArrowRight, ArrowUpRight, Plus } from 'lucide-react'
import drawing from '../../assets/images/patent-rock.webp'
import drawingSmall from '../../assets/images/patent-rock-800.webp'
import { api } from '../../api/endpoints'
import type { Dashboard } from '../../api/types'
import { useAsync } from '../../hooks/useAsync'
import { useAuth } from '../auth/AuthContext'
import { PageIntro } from '../../components/layout/PageIntro'
import { Section } from '../../components/layout/Section'
import { Button } from '../../components/ui/Button'
import { Surface } from '../../components/ui/Surface'
import { EmptyState, ErrorNotice, Loading, Skeleton } from '../../components/ui/Feedback'
import { Parallax } from '../../components/motion/Parallax'
import { Reveal } from '../../components/motion/Reveal'
import { CycleDiagram } from '../../components/illustrations/Diagrams'
import { LatestDeadlock } from './LatestDeadlock'
import { StrategyFindings } from './StrategyFindings'
import { RecentRuns } from './RecentRuns'

function greeting() {
  const h = new Date().getHours()
  return h < 5 ? 'Good evening' : h < 12 ? 'Good morning' : h < 18 ? 'Good afternoon' : 'Good evening'
}

/** One honest sentence built from the real counts. */
function summary(d: Dashboard) {
  const s = (n: number, w: string) => `${n} ${w}${n === 1 ? '' : 's'}`
  if (d.totalRuns === 0) return `You have ${s(d.totalScenarios, 'saved scenario')} and no simulation runs yet.`
  const healed = d.comparedRuns === 0 ? 'none has been healed yet' : d.comparedRuns === d.deadlockedRuns
    ? (d.deadlockedRuns === 1 ? 'it has been healed and compared' : 'all have been healed and compared')
    : `${d.comparedRuns} ${d.comparedRuns === 1 ? 'has' : 'have'} been healed and compared`
  if (d.deadlockedRuns === 0) return `${s(d.totalScenarios, 'scenario')}, ${s(d.totalRuns, 'run')} — none ended in deadlock.`
  return `${s(d.totalScenarios, 'scenario')}, ${s(d.totalRuns, 'run')} — ${d.deadlockedRuns === d.totalRuns ? (d.totalRuns === 1 ? 'it' : 'all') : d.deadlockedRuns} ended in deadlock, and ${healed}.`
}

function Figures({ d }: { d: Dashboard }) {
  const pct = d.totalRuns ? Math.round((d.deadlockedRuns / d.totalRuns) * 100) : 0
  const small = [
    { v: d.totalScenarios, l: 'saved scenarios' },
    { v: d.deadlockedRuns, l: `runs deadlocked (${pct}%)` },
    { v: d.recoverySuccesses, l: 'recoveries verified' },
    { v: d.recoveryFailures, l: 'failed or partial', warn: d.recoveryFailures > 0 },
  ]
  return (
    <Reveal className="mb-16 grid gap-10 border-y border-rule py-8 md:grid-cols-[auto_minmax(0,1fr)] md:items-end md:gap-16">
      <div>
        <p className="display num text-[72px] leading-none text-ink sm:text-[84px]">{d.totalRuns}</p>
        <p className="mt-2 text-[14px] text-cocoa">simulation runs recorded</p>
      </div>
      <dl className="grid grid-cols-2 gap-y-6 sm:grid-cols-4">
        {small.map((m, i) => (
          <div key={m.l} className={`pr-4 ${i > 0 ? 'sm:border-l sm:border-rule sm:pl-6' : ''}`}>
            <dd className={`num font-mono text-[28px] ${m.warn ? 'text-danger' : 'text-ink'}`}>{m.v}</dd>
            <dt className="mt-1 text-[13px] text-cocoa">{m.l}</dt>
          </div>
        ))}
      </dl>
    </Reveal>
  )
}

export default function DashboardPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const { data, error, loading, reload } = useAsync(() => api.dashboard(), [])
  const first = user?.name.split(' ')[0]

  const intro = (d?: Dashboard) => (
    <PageIntro
      trail={[{ label: 'Research workspace' }, { label: new Date().toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long' }) }]}
      title={<>{greeting()}, {first}.</>}
      lede={d ? summary(d) : 'Loading your workspace…'}
      actions={<>
        <Button size="lg" icon={<Plus className="size-4" />} onClick={() => navigate('/scenarios/new')}>New simulation</Button>
        <Button size="lg" variant="secondary" onClick={() => navigate('/history')}>View history</Button>
      </>}
      aside={
        <div className="hidden w-[230px] lg:block">
          <Parallax distance={14} className="aspect-[4/5] rounded-[3px] shadow-[var(--shadow-lift)]">
            <img src={drawing} srcSet={`${drawingSmall} 800w, ${drawing} 1600w`} sizes="230px"
              alt="1840s patent drawing of a rock-boring machine: a tall timber frame with a suspended drill." className="size-full object-cover" />
          </Parallax>
          <p className="mt-2 text-[11.5px] leading-snug text-cocoa">Israel Overall, machine for boring rock, 1840s. Public domain.</p>
        </div>
      } />
  )

  if (loading) return <>{intro()}<Skeleton className="h-24" /><Loading label="Reading your runs" /></>
  if (error || !data) return <>{intro()}<ErrorNotice error={error} onRetry={reload} /></>

  if (data.totalScenarios === 0 && data.totalRuns === 0) {
    return (
      <>
        {intro(data)}
        <Section number="—" title="Start here">
          <EmptyState title="Your notebook is empty."
            body="Begin with a reproducible example — the classic three-process cycle is a good first run — or generate a seeded scenario with your own cycle length, work distribution, checkpoint interval and resource density."
            action={<Link to="/scenarios/new" className="link inline-flex items-center gap-1.5 text-[15px] font-medium">Open the scenario builder <ArrowRight className="size-4" /></Link>}
            figure={<CycleDiagram className="mx-auto w-full max-w-[300px]" />} />
        </Section>
      </>
    )
  }

  return (
    <>
      {intro(data)}
      <Figures d={data} />
      {data.latestDeadlockedRunId && (
        <Section number="01" title="Latest deadlock" note="The most recent run that ended in a circular wait, drawn from its stored snapshot.">
          <LatestDeadlock runId={data.latestDeadlockedRunId} />
        </Section>
      )}
      {data.strategies.length > 0 && (
        <Section number={data.latestDeadlockedRunId ? '02' : '01'} title="Recovery strategies" note="Averages over every comparison you have run. Rankings depend on the weights used each time.">
          <Surface><StrategyFindings stats={data.strategies} /></Surface>
        </Section>
      )}
      {data.recentRuns.length > 0 && (
        <Section number={String((data.latestDeadlockedRunId ? 1 : 0) + (data.strategies.length ? 1 : 0) + 1).padStart(2, '0')} title="Recent simulations">
          <Surface className="!py-2"><RecentRuns runs={data.recentRuns} /></Surface>
          <Link to="/history" className="link mt-6 inline-flex items-center gap-1 text-[14px] font-medium">The full run log <ArrowUpRight className="size-3.5" /></Link>
        </Section>
      )}
    </>
  )
}
