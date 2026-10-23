import { useEffect, useRef, type ReactNode } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { X } from 'lucide-react'

/** Accessible modal: focus moves inside, Escape and backdrop close, focus is restored. */
export function Modal({ open, onClose, title, children, footer }: {
  open: boolean; onClose: () => void; title: string; children: ReactNode; footer?: ReactNode
}) {
  const panel = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (!open) return
    const prev = document.activeElement as HTMLElement | null
    panel.current?.focus()
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKey)
    return () => { window.removeEventListener('keydown', onKey); prev?.focus() }
  }, [open, onClose])
  return (
    <AnimatePresence>
      {open && (
        <motion.div className="fixed inset-0 z-50 grid place-items-center bg-ink/20 p-4 backdrop-blur-[1px]" onMouseDown={onClose}
          initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} transition={{ duration: 0.15 }}>
          <motion.div ref={panel} tabIndex={-1} role="dialog" aria-modal="true" aria-label={title}
            className="w-full max-w-md rounded-[6px] border border-rule bg-sheet p-6 shadow-[var(--shadow-lift)] outline-none"
            initial={{ y: 10, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: 6, opacity: 0 }}
            transition={{ duration: 0.2, ease: [0.25, 1, 0.5, 1] }} onMouseDown={(e) => e.stopPropagation()}>
            <div className="flex items-start justify-between gap-4">
              <h2 className="display text-[22px]">{title}</h2>
              <button onClick={onClose} className="-mr-2 -mt-1 rounded p-1.5 text-cocoa hover:bg-paper hover:text-ink cursor-pointer" aria-label="Close">
                <X className="size-4" />
              </button>
            </div>
            <div className="mt-3 text-[14px] leading-relaxed text-cocoa">{children}</div>
            {footer && <div className="mt-6 flex justify-end gap-2">{footer}</div>}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
