import { forwardRef, useId, useState, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes } from 'react'
import { Eye, EyeOff } from 'lucide-react'

const control = `w-full rounded-[5px] border bg-sheet text-[15px] text-ink placeholder:text-taupe transition-colors
  focus:outline-none focus:border-ink focus:ring-[3px] focus:ring-terracotta/15`

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  hint?: ReactNode
  error?: string
  icon?: ReactNode
  revealable?: boolean
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { label, hint, error, icon, revealable, className = '', type, id, ...rest }, ref,
) {
  const auto = useId()
  const inputId = id ?? auto
  const [shown, setShown] = useState(false)
  return (
    <div className={className}>
      {label && <label htmlFor={inputId} className="mb-1.5 block text-[13px] font-medium text-ink">{label}</label>}
      <div className="relative">
        {icon && <span className="pointer-events-none absolute inset-y-0 left-3 grid place-items-center text-cocoa">{icon}</span>}
        <input ref={ref} id={inputId} type={revealable ? (shown ? 'text' : 'password') : type}
          aria-invalid={!!error} aria-describedby={error ? `${inputId}-err` : hint ? `${inputId}-hint` : undefined}
          className={`${control} h-11 px-3 ${icon ? 'pl-10' : ''} ${revealable ? 'pr-11' : ''} ${error ? 'border-danger/70' : 'border-rule'}`}
          {...rest} />
        {revealable && (
          <button type="button" onClick={() => setShown((s) => !s)} aria-pressed={shown}
            aria-label={shown ? 'Hide password' : 'Show password'}
            className="absolute inset-y-0 right-0 grid w-11 place-items-center text-cocoa hover:text-ink cursor-pointer">
            {shown ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        )}
      </div>
      {error ? <p id={`${inputId}-err`} className="mt-1.5 text-[13px] text-danger">{error}</p>
        : hint ? <p id={`${inputId}-hint`} className="mt-1.5 text-[12.5px] text-cocoa">{hint}</p> : null}
    </div>
  )
})

export function NumberInput({ value, onChange, className = '', ...rest }: {
  value: number; onChange: (v: number) => void; className?: string
} & Omit<InputHTMLAttributes<HTMLInputElement>, 'value' | 'onChange' | 'type'>) {
  return (
    <input type="number" value={Number.isFinite(value) ? value : ''}
      onChange={(e) => onChange(e.target.value === '' ? NaN : Number(e.target.value))}
      className={`${control.replace('w-full ', '')} ${/(^|\s)!?w-/.test(className) ? '' : 'w-full'} h-9 border-rule px-2.5 font-mono text-[13px] num ${className}`} {...rest} />
  )
}

export function Select({ label, className = '', children, id, ...rest }: SelectHTMLAttributes<HTMLSelectElement> & { label?: string }) {
  const auto = useId()
  const selectId = id ?? auto
  return (
    <div className={className}>
      {label && <label htmlFor={selectId} className="mb-1.5 block text-[13px] font-medium text-ink">{label}</label>}
      <select id={selectId} className={`${control} h-10 border-rule pl-3 pr-8 text-[14px]`} {...rest}>{children}</select>
    </div>
  )
}

/** Compact inline select for dense editors. */
export const inlineControl = `h-9 rounded-[5px] border border-rule bg-sheet px-2 text-[13px] text-ink focus:outline-none focus:border-ink`
