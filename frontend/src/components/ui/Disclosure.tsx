import { useId, useState, type ReactNode } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion'
import { ChevronDown } from 'lucide-react'

/** Expandable technical detail with an animated height. */
export function Disclosure({ summary, children, defaultOpen = false, className = '' }: {
  summary: ReactNode; children: ReactNode; defaultOpen?: boolean; className?: string
}) {
  const [open, setOpen] = useState(defaultOpen)
  const id = useId()
  const reduce = useReducedMotion()
  return (
    <div className={className}>
      <button type="button" onClick={() => setOpen((o) => !o)} aria-expanded={open} aria-controls={id}
        className="group flex w-full items-center justify-between gap-4 py-3 text-left text-[14px] font-medium text-ink cursor-pointer">
        <span className="group-hover:text-terracotta-deep">{summary}</span>
        <ChevronDown className={`size-4 shrink-0 text-cocoa transition-transform duration-200 ${open ? 'rotate-180' : ''}`} aria-hidden />
      </button>
      <AnimatePresence initial={false}>
        {open && (
          <motion.div id={id} key="content" className="overflow-hidden"
            initial={reduce ? false : { height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }}
            exit={reduce ? undefined : { height: 0, opacity: 0 }} transition={{ duration: 0.25, ease: [0.25, 1, 0.5, 1] }}>
            <div className="pb-4">{children}</div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
