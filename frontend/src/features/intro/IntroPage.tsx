import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { AnimatePresence, motion, useInView } from 'framer-motion'
import { ArrowRight, ArrowUpRight } from 'lucide-react'
import hero from '../../assets/images/patent-dyewood.webp'
import heroSmall from '../../assets/images/patent-dyewood-800.webp'
import lever from '../../assets/images/switch-lever.webp'
import leverSmall from '../../assets/images/switch-lever-800.webp'
import room from '../../assets/images/reading-room.webp'
import roomSmall from '../../assets/images/reading-room-800.webp'
import { useAuth } from '../auth/AuthContext'
import { Wordmark } from '../../components/brand/Wordmark'
import { Parallax } from '../../components/motion/Parallax'
import { Reveal } from '../../components/motion/Reveal'
import { Figure } from '../../components/ui/Figure'
import { CheckpointDiagram, CycleDiagram, DfsDiagram, VerifyDiagram } from '../../components/illustrations/Diagrams'

const STEPS = [
  {
    key: 'detect', short: 'Detect', n: '01', title: 'Detect',
    body: 'The simulator replays a deterministic request sequence until processes block. From the final state it builds a Wait-For Graph: an edge Pi → Pj means Pi waits for a resource Pj holds. With single-instance resources, any directed cycle is a deadlock.',
    Figure: CycleDiagram,
  },
  {
    key: 'analyze', short: 'Analyse', n: '02', title: 'Analyse',
    body: 'A three-colour depth-first search walks the graph in O(V + E). An edge to a gray vertex — one still on the current path — is a back edge and closes a cycle. Every cycle is returned with the resources behind it, together with the complete deadlocked set and the processes merely blocked behind it.',
    Figure: DfsDiagram,
  },
  {
    key: 'recover', short: 'Recover', n: '03', title: 'Recover — three ways',
    body: 'The deadlocked state is frozen and deep-copied three times. One copy is healed by terminating cost-selected victims, one by preempting resources through checkpoint rollback, one by aborting the whole deadlocked set. Each works alone; none can disturb the others.',
    Figure: CheckpointDiagram,
  },
  {
    key: 'verify', short: 'Verify', n: '04', title: 'Verify, then compare',
    body: 'Success is never assumed. Each healed copy is rebuilt into a fresh graph and searched again, and its ownership invariants are checked. Only then are seven metrics compared — and the ranking is shown to depend on the weights you choose.',
    Figure: VerifyDiagram,
  },
]

function StoryStep({ step, onActive }: { step: (typeof STEPS)[number]; onActive: () => void }) {
  const ref = useRef<HTMLDivElement>(null)
  const inView = useInView(ref, { amount: 0.6 })
  useEffect(() => { if (inView) onActive() }, [inView, onActive])
  const Fig = step.Figure
  return (
    <div ref={ref} className="flex min-h-[62vh] flex-col justify-center py-10 lg:min-h-[78vh]">
      <p className="font-mono text-[12px] tracking-[0.14em] text-terracotta-deep">{step.n}</p>
      <h3 className="display mt-2 text-[32px] sm:text-[38px]">{step.title}</h3>
      <p className="mt-5 max-w-[520px] text-[16.5px] leading-[1.7] text-cocoa-strong">{step.body}</p>
      {/* On small screens the illustration sits with its step instead of in a pinned column. */}
      <Fig className="mt-8 w-full max-w-[340px] lg:hidden" />
    </div>
  )
}

export default function IntroPage() {
  const { user } = useAuth()
  const [active, setActive] = useState(0)
  const ActiveFig = STEPS[active].Figure

  const cta = user ? (
    <Link to="/dashboard" className="inline-flex h-12 items-center gap-2.5 rounded-[5px] bg-action px-6 text-[15px] font-medium text-sheet hover:bg-action-hover">
      Open your workspace <ArrowRight className="size-4" />
    </Link>
  ) : (
    <>
      <Link to="/register" className="inline-flex h-12 items-center gap-2.5 rounded-[5px] bg-action px-6 text-[15px] font-medium text-sheet hover:bg-action-hover">
        Create an account <ArrowRight className="size-4" />
      </Link>
      <Link to="/login" className="link text-[15px] font-medium">Sign in</Link>
    </>
  )

  return (
    <div className="bg-ivory text-ink">
      <header className="mx-auto flex max-w-[1240px] items-center justify-between px-6 py-6 sm:px-10">
        <Wordmark />
        <nav className="flex items-center gap-6 text-[14px]">
          <a href="#how" className="hidden text-cocoa hover:text-ink sm:inline">How it works</a>
          <a href="#strategies" className="hidden text-cocoa hover:text-ink sm:inline">Strategies</a>
          {user ? <Link to="/dashboard" className="link font-medium">Workspace</Link> : <Link to="/login" className="link font-medium">Sign in</Link>}
        </nav>
      </header>

      {/* Hero */}
      <section className="mx-auto grid max-w-[1240px] items-center gap-12 px-6 pb-20 pt-10 sm:px-10 lg:grid-cols-[minmax(0,1.05fr)_minmax(0,1fr)] lg:pt-16">
        <div style={{ animation: 'ink-in .6s var(--ease-out-quart) both' }}>
          <p className="kicker">An operating-systems research instrument</p>
          <h1 className="display mt-5 text-[44px] sm:text-[62px] lg:text-[70px]">
            When every process waits on another, <span className="display-italic text-terracotta-deep">something</span> has to give.
          </h1>
          <p className="mt-7 max-w-[540px] text-[17px] leading-[1.7] text-cocoa">
            The Self-Healing Deadlock Simulator reproduces circular waits, detects them precisely, and heals the same frozen state
            three different ways — so the cost of each recovery can be compared under identical conditions.
          </p>
          <div className="mt-10 flex flex-wrap items-center gap-6">{cta}</div>
        </div>
        <div>
          <Parallax distance={28} className="aspect-[4/3] rounded-[4px] shadow-[var(--shadow-lift)]">
            <img src={hero} srcSet={`${heroSmall} 800w, ${hero} 1600w`} sizes="(min-width: 1024px) 45vw, 100vw" fetchPriority="high"
              alt="Hand-coloured 1840s patent drawing of a machine for rasping dyewood: a long horizontal drum in plan and section."
              className="size-full object-cover" />
          </Parallax>
          <p className="mt-3 text-[12px] text-cocoa"><span className="font-mono text-terracotta-deep">Fig. 1</span> — Jesse Reed, machine for rasping dyewood. U.S. Patent Office, 1840s. Public domain.</p>
        </div>
      </section>

      {/* Sticky storytelling */}
      <section id="how" className="border-t border-rule bg-paper">
        <div className="mx-auto max-w-[1240px] px-6 sm:px-10">
          <Reveal className="max-w-[640px] pt-20">
            <p className="kicker">How it works</p>
            <h2 className="display mt-4 text-[36px] sm:text-[44px]">Four stages, one frozen state.</h2>
          </Reveal>
          <div className="grid gap-10 lg:grid-cols-[minmax(0,1fr)_minmax(0,0.9fr)] lg:gap-20">
            <div className="pb-16">
              {STEPS.map((s, i) => <StoryStep key={s.key} step={s} onActive={() => setActive(i)} />)}
            </div>
            <div className="hidden lg:block">
              <div className="sticky top-[14vh] flex h-[72vh] flex-col justify-center">
                <div className="rounded-[4px] border border-rule bg-sheet p-8 shadow-[var(--shadow-paper)]">
                  <AnimatePresence mode="wait">
                    <motion.div key={active} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }} transition={{ duration: 0.3 }}>
                      <ActiveFig className="mx-auto w-full max-w-[420px]" />
                    </motion.div>
                  </AnimatePresence>
                </div>
                <ol className="mt-5 flex gap-6 font-mono text-[11px] uppercase tracking-[0.12em]" aria-label="Stages">
                  {STEPS.map((s, i) => (
                    <li key={s.key} className={`border-t-2 pt-2 transition-colors ${i === active ? 'border-terracotta text-ink' : 'border-rule text-cocoa'}`}>{s.n} {s.short}</li>
                  ))}
                </ol>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Strategies — espresso band for contrast */}
      <section id="strategies" className="border-y border-rule bg-sheet">
        <div className="mx-auto max-w-[1240px] px-6 py-24 sm:px-10">
          <Reveal className="max-w-[680px]">
            <p className="kicker">The three recoveries</p>
            <h2 className="display mt-4 text-[36px] sm:text-[44px]">None of them is free. None of them always wins.</h2>
          </Reveal>
          <div className="mt-16 grid gap-12 md:grid-cols-3 md:gap-10">
            {[
              ['I', 'Iterative termination', 'Pick the cheapest victim by work at stake, priority, resources held and how often it was chosen before; terminate it; search again.', 'Costs the victim’s whole work. Few processes affected when one kill breaks the cycle.'],
              ['II', 'Checkpoint rollback', 'Preempt a contended resource by restoring its holder to the latest checkpoint taken before it was acquired.', 'Loses only work since the checkpoint — but the same process can be chosen again and again.'],
              ['III', 'Total abort', 'Abort every process on a cycle at once. Processes merely blocked behind the deadlock are left alone.', 'One decisive round. Every deadlocked process loses everything.'],
            ].map(([n, t, how, cost], i) => (
              <Reveal key={n} delay={i * 0.08} className="border-t border-rule pt-6">
                <p className="display text-[30px] text-terracotta-deep">{n}</p>
                <h3 className="mt-3 text-[18px] font-medium">{t}</h3>
                <p className="mt-3 text-[14.5px] leading-relaxed text-ink">{how}</p>
                <p className="mt-4 text-[13.5px] leading-relaxed text-cocoa">{cost}</p>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* Photographic interlude */}
      <section className="mx-auto max-w-[1240px] px-6 py-24 sm:px-10">
        <div className="grid items-end gap-12 lg:grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)]">
          <Reveal>
            <Figure src={lever} srcSmall={leverSmall} alt="A weathered railway switch lever with a red and yellow disc beside the rails it controls."
              imgClassName="aspect-[16/10] w-full object-cover rounded-[4px]"
              caption={<><span className="font-mono text-terracotta-deep">Fig. 2</span> — One lever, two routes: contention at a junction. Railway switch on Grötö. Public domain.</>} />
          </Reveal>
          <Reveal delay={0.08}>
            <p className="kicker">Reproducible by design</p>
            <h2 className="display mt-4 text-[32px] sm:text-[38px]">Every run is a record you can return to.</h2>
            <p className="mt-5 text-[16px] leading-[1.7] text-cocoa">
              Scenarios are deterministic; the exact configuration used is stored with each run, the deadlocked snapshot is fingerprinted,
              and every comparison keeps its weights, per-round evidence and verification result in PostgreSQL.
            </p>
            <Figure className="mt-10" src={room} srcSmall={roomSmall} alt="The domed Main Reading Room of the Library of Congress, desks arranged in concentric rings."
              imgClassName="aspect-[16/9] w-full object-cover rounded-[4px]"
              caption={<><span className="font-mono text-terracotta-deep">Fig. 3</span> — Library of Congress Main Reading Room. Carol M. Highsmith, public domain.</>} />
          </Reveal>
        </div>
      </section>

      <footer className="border-t border-rule">
        <div className="mx-auto flex max-w-[1240px] flex-col gap-6 px-6 py-12 sm:flex-row sm:items-center sm:justify-between sm:px-10">
          <div>
            <p className="display text-[26px]">Begin with a classic three-process cycle.</p>
            <p className="mt-1 text-[14px] text-cocoa">Single-instance resource model · Spring Boot · PostgreSQL · React</p>
          </div>
          <Link to={user ? '/scenarios/new' : '/register'} className="inline-flex items-center gap-2 text-[15px] font-medium text-terracotta-deep hover:underline">
            {user ? 'Build a scenario' : 'Create an account'} <ArrowUpRight className="size-4" />
          </Link>
        </div>
      </footer>
    </div>
  )
}
