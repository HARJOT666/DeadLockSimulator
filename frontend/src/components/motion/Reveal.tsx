import type { ReactNode } from 'react'
import { motion, useReducedMotion } from 'framer-motion'

/** Section entrance: a short rise + fade, once, when it scrolls into view. */
export function Reveal({ children, delay = 0, className = '', as = 'div' }: {
  children: ReactNode; delay?: number; className?: string; as?: 'div' | 'section' | 'li' | 'article'
}) {
  const reduce = useReducedMotion()
  const M = motion[as]
  if (reduce) return <M className={className}>{children}</M>
  return (
    <M className={className} initial={{ opacity: 0, y: 14 }} whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: '0px 0px -8% 0px' }} transition={{ duration: 0.55, delay, ease: [0.25, 1, 0.5, 1] }}>
      {children}
    </M>
  )
}
