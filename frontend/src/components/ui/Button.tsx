import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react'
import { Loader2 } from 'lucide-react'

type Variant = 'primary' | 'secondary' | 'quiet' | 'danger'
type Size = 'sm' | 'md' | 'lg'

const variants: Record<Variant, string> = {
  primary: 'bg-action text-sheet hover:bg-action-hover active:translate-y-px shadow-[0_1px_0_rgb(73_53_40/0.12)]',
  secondary: 'border border-rule bg-sheet text-ink hover:border-cocoa/50 hover:bg-sand active:translate-y-px',
  quiet: 'text-cocoa hover:text-ink hover:bg-sand/70',
  danger: 'border border-danger/50 text-danger hover:bg-danger hover:text-sheet',
}
const sizes: Record<Size, string> = {
  sm: 'h-8 px-3 text-[13px] gap-1.5',
  md: 'h-10 px-4 text-[14px] gap-2',
  lg: 'h-12 px-6 text-[15px] gap-2.5',
}

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  size?: Size
  loading?: boolean
  icon?: ReactNode
}

/** Rectangular, restrained buttons. One primary (terracotta) action per view. */
export const Button = forwardRef<HTMLButtonElement, Props>(function Button(
  { variant = 'primary', size = 'md', loading, icon, className = '', children, disabled, ...rest }, ref,
) {
  return (
    <button ref={ref} disabled={disabled || loading} aria-busy={loading || undefined}
      className={`inline-flex items-center justify-center rounded-[5px] font-medium tracking-[0.005em] transition-[background,color,transform]
        duration-150 cursor-pointer select-none whitespace-nowrap disabled:cursor-not-allowed disabled:opacity-45 ${variants[variant]} ${sizes[size]} ${className}`}
      {...rest}>
      {loading ? <Loader2 className="size-4 animate-spin" aria-hidden /> : icon}
      {children}
    </button>
  )
})
