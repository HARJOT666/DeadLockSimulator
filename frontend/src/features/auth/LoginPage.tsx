import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ArrowRight, Lock, Mail } from 'lucide-react'
import plugboard from '../../assets/images/plugboard.webp'
import plugboardSmall from '../../assets/images/plugboard-800.webp'
import { useAuth } from './AuthContext'
import { Wordmark } from '../../components/brand/Wordmark'
import { Button } from '../../components/ui/Button'
import { Input } from '../../components/ui/Field'
import { InlineError } from '../../components/ui/Feedback'

const HIGHLIGHTS = [
  ['Detection', 'A Wait-For Graph searched with three-colour DFS returns the actual cycles.'],
  ['Automatic recovery', 'Termination, checkpoint rollback and total abort, each verified by re-detection.'],
  ['Comparative analysis', 'Identical deep-copied snapshots, seven metrics and a ranking that states its assumptions.'],
]

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const state = useLocation().state as { from?: string; expired?: boolean } | null
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({})
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const v: typeof errors = {}
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) v.email = 'Enter the email address you registered with.'
    if (!password) v.password = 'Enter your password.'
    setErrors(v)
    if (Object.keys(v).length) return
    setBusy(true)
    setError(null)
    try {
      await login(email.trim(), password)
      navigate(state?.from && !['/login', '/register'].includes(state.from) ? state.from : '/dashboard', { replace: true })
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="min-h-screen bg-ivory lg:grid lg:grid-cols-[minmax(0,0.95fr)_minmax(480px,1.05fr)]">
      {/* Photograph set into the page like a plate, with its caption — the page around it stays light. */}
      <section className="px-5 pt-5 lg:sticky lg:top-0 lg:flex lg:h-screen lg:flex-col lg:py-6 lg:pl-6 lg:pr-0">
        <figure className="flex h-full flex-col">
          <div className="relative h-[220px] overflow-hidden rounded-[4px] shadow-[var(--shadow-lift)] sm:h-[300px] lg:h-auto lg:flex-1">
            <img src={plugboard} srcSet={`${plugboardSmall} 800w, ${plugboard} 1600w`} sizes="(min-width: 1024px) 46vw, 100vw"
              alt="Hundreds of patch cords crossing an analog computer plugboard — every cord a dependency between two terminals."
              className="absolute inset-0 size-full object-cover object-[50%_40%]" />
          </div>
          <figcaption className="mt-3 grid grid-cols-[auto_minmax(0,1fr)] gap-x-3 text-[12px] leading-snug text-cocoa">
            <span className="font-mono text-terracotta-deep">Fig. 1</span>
            <span>Every cord is a dependency between two terminals. EAI 580 analog computer plugboard, Computer History Museum. CC0.</span>
          </figcaption>
        </figure>
      </section>

      <section className="flex flex-col px-6 py-10 sm:px-12 lg:px-16 lg:py-12 xl:px-24">
        <Link to="/" aria-label="About the simulator"><Wordmark /></Link>
        <div className="my-auto w-full max-w-[440px] pt-14 lg:pt-10">
          <p className="kicker">Sign in</p>
          <h1 className="display mt-3 text-[36px] sm:text-[44px]">
            Self-Healing <span className="display-italic text-terracotta-deep">Deadlock</span> Simulator
          </h1>
          <p className="mt-4 text-[15.5px] leading-relaxed text-cocoa">
            Reproduce a circular wait, find it, heal it three different ways from the same frozen state, and measure what each recovery costs.
          </p>
          {state?.expired && (
            <p className="mt-6 border-l-2 border-highlight pl-3 text-[13.5px] text-gold-deep" role="status">
              Your session expired. Sign in again to continue where you left off.
            </p>
          )}
          <form onSubmit={submit} noValidate className="mt-9 space-y-5">
            <Input label="Email" type="email" autoComplete="email" autoFocus icon={<Mail className="size-4" strokeWidth={1.6} />}
              value={email} onChange={(e) => setEmail(e.target.value)} error={errors.email} placeholder="you@university.edu" />
            <Input label="Password" revealable autoComplete="current-password" icon={<Lock className="size-4" strokeWidth={1.6} />}
              value={password} onChange={(e) => setPassword(e.target.value)} error={errors.password} />
            <InlineError error={error} />
            <Button type="submit" size="lg" className="w-full" loading={busy}>
              {busy ? 'Signing in' : <>Sign in <ArrowRight className="size-4" /></>}
            </Button>
          </form>
          <p className="mt-6 text-[14px] text-cocoa">
            No account yet? <Link to="/register" className="link font-medium">Create one</Link> — it takes a minute.
          </p>
          <ol className="mt-12 grid gap-5 border-t border-rule pt-6 sm:grid-cols-3 sm:gap-6">
            {HIGHLIGHTS.map(([t, b], i) => (
              <li key={t}>
                <p className="font-mono text-[11px] tracking-[0.14em] text-terracotta-deep">0{i + 1}</p>
                <p className="mt-1 text-[14px] font-medium text-ink">{t}</p>
                <p className="mt-1 text-[12.5px] leading-snug text-cocoa">{b}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>
    </div>
  )
}
