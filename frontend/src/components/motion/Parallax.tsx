import { useRef, type ReactNode } from 'react'
import { motion, useReducedMotion, useScroll, useTransform } from 'framer-motion'

/**
 * Gentle vertical parallax for photographs and illustrations. The child moves at most `distance`
 * pixels across the element's pass through the viewport; disabled for reduced motion.
 */
export function Parallax({ children, distance = 40, className = '' }: { children: ReactNode; distance?: number; className?: string }) {
  const ref = useRef<HTMLDivElement>(null)
  const reduce = useReducedMotion()
  const { scrollYProgress } = useScroll({ target: ref, offset: ['start end', 'end start'] })
  const y = useTransform(scrollYProgress, [0, 1], [-distance, distance])
  return (
    <div ref={ref} className={`relative overflow-hidden ${className}`}>
      {/* The moving layer overhangs by `distance` on both edges, so no gap opens at either end. */}
      <motion.div style={{ top: -distance, bottom: -distance, ...(reduce ? {} : { y }) }} className="absolute inset-x-0 will-change-transform">
        {children}
      </motion.div>
    </div>
  )
}
