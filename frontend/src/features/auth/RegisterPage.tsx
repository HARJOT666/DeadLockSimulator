import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ArrowRight, Check, Minus } from 'lucide-react'
import drawing from '../../assets/images/patent-cards.webp'
import drawingSmall from '../../assets/images/patent-cards-800.webp'
import { useAuth } from './AuthContext'
import { Wordmark } from '../../components/brand/Wordmark'
import { Button } from '../../components/ui/Button'
import { Input } from '../../components/ui/Field'
import { InlineError } from '../../components/ui/Feedback'

type Errors = Partial<Record<'name' | 'email' | 'password' | 'confirm', string>>

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '', confirm: '' })
  const [errors, setErrors] = useState<Errors>({})
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) => setForm((f) => ({ ...f, [k]: e.target.value }))

  const rules = [
    { ok: form.password.length >= 8 && form.password.length <= 72, label: '8 to 72 characters' },
    { ok: /[A-Za-z]/.test(form.password), label: 'at least one letter' },
    { ok: /\d/.test(form.password), label: 'at least one digit' },
  ]

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const v: Errors = {}
    if (!form.name.trim()) v.name = 'Tell us what to call you.'
    if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) v.email = 'Enter a valid email address.'
    if (!rules.every((r) => r.ok)) v.password = 'The password does not meet the rules below yet.'
    if (form.confirm !== form.password) v.confirm = 'The two passwords differ.'
    setErrors(v)
    if (Object.keys(v).length) return
    setBusy(true)
    setError(null)
    try {
      await register(form.name.trim(), form.email.trim(), form.password)
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="min-h-screen bg-ivory lg:grid lg:grid-cols-[minmax(0,1fr)_minmax(0,0.75fr)]">
      <section className="px-6 py-10 sm:px-12 lg:px-20 lg:py-14">
        <Link to="/"><Wordmark /></Link>
        <div className="mt-16 max-w-[520px] lg:mt-24">
          <p className="kicker">New account</p>
          <h1 className="display mt-3 text-[38px] sm:text-[46px]">Open a research notebook.</h1>
          <p className="mt-4 text-[15.5px] leading-relaxed text-cocoa">
            Your scenarios, simulation runs and recovery comparisons are stored privately under this account.
          </p>
          <form onSubmit={submit} noValidate className="mt-12 grid gap-x-5 gap-y-6 sm:grid-cols-2">
            <Input className="sm:col-span-2" label="Name" autoComplete="name" autoFocus value={form.name} onChange={set('name')} error={errors.name} />
            <Input className="sm:col-span-2" label="Email" type="email" autoComplete="email" value={form.email} onChange={set('email')} error={errors.email} />
            <Input label="Password" revealable autoComplete="new-password" value={form.password} onChange={set('password')} error={errors.password} />
            <Input label="Confirm password" revealable autoComplete="new-password" value={form.confirm} onChange={set('confirm')} error={errors.confirm} />
            <ul className="-mt-2 flex flex-wrap gap-x-5 gap-y-1 text-[12.5px] sm:col-span-2" aria-label="Password rules">
              {rules.map((r) => (
                <li key={r.label} className={`flex items-center gap-1.5 ${r.ok ? 'text-sage-deep' : 'text-cocoa'}`}>
                  {r.ok ? <Check className="size-3.5" aria-label="met" /> : <Minus className="size-3.5" aria-label="not met" />}{r.label}
                </li>
              ))}
            </ul>
            <div className="sm:col-span-2"><InlineError error={error} /></div>
            <div className="flex flex-wrap items-center gap-5 sm:col-span-2">
              <Button type="submit" size="lg" loading={busy}>{busy ? 'Creating account' : <>Create account <ArrowRight className="size-4" /></>}</Button>
              <span className="text-[14px] text-cocoa">Already registered? <Link to="/login" className="link font-medium">Sign in</Link></span>
            </div>
          </form>
          <p className="mt-12 max-w-md text-[12.5px] leading-relaxed text-cocoa">
            Passwords are stored only as BCrypt hashes. Your session is held in an HttpOnly cookie that page scripts cannot read.
          </p>
        </div>
      </section>
      <aside className="relative hidden border-l border-rule bg-paper lg:block">
        <div className="sticky top-0 flex h-screen flex-col justify-end p-12">
          <figure>
            <img src={drawing} srcSet={`${drawingSmall} 800w, ${drawing} 1600w`} sizes="40vw"
              alt="Hand-tinted 1840s patent drawing of a grinding machine: two rollers feed a curved guide toward a large wheel."
              className="aspect-[4/5] w-full object-cover object-[30%_50%] shadow-[var(--shadow-lift)]" />
            <figcaption className="mt-5 grid grid-cols-[auto_1fr] gap-x-4 text-[12.5px] leading-snug text-cocoa">
              <span className="font-mono text-terracotta-deep">Fig. 1</span>
              <span>Gideon Drake, machine for grinding and facing cards, U.S. Patent Office, 1840s. A mechanism specified precisely enough to rebuild — as every scenario here is: the same configuration always reproduces the same state. Public domain.</span>
            </figcaption>
          </figure>
        </div>
      </aside>
    </div>
  )
}
