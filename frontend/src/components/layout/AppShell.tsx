import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import { BookOpen, FilePlus2, FlaskConical, LayoutDashboard, LogOut, Menu, NotebookTabs, Settings2, X } from 'lucide-react'
import { Wordmark } from '../brand/Wordmark'
import { useAuth } from '../../features/auth/AuthContext'

const GROUPS = [
  { label: 'Workspace', items: [
    { to: '/dashboard', label: 'Overview', icon: LayoutDashboard },
    { to: '/scenarios', label: 'Scenarios', icon: FlaskConical, end: true },
    { to: '/scenarios/new', label: 'New scenario', icon: FilePlus2 },
  ] },
  { label: 'Records', items: [
    { to: '/history', label: 'Run log & reports', icon: NotebookTabs },
  ] },
  { label: 'Reference', items: [
    { to: '/', label: 'How it works', icon: BookOpen, end: true },
    { to: '/settings', label: 'Settings', icon: Settings2 },
  ] },
]

function Nav({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <nav aria-label="Main" className="space-y-7">
      {GROUPS.map((g) => (
        <div key={g.label}>
          <p className="kicker mb-2 px-3 !text-[10px] !text-cocoa">{g.label}</p>
          <ul>
            {g.items.map(({ to, label, icon: Icon, end }) => (
              <li key={to}>
                <NavLink to={to} end={end} onClick={onNavigate}
                  className={({ isActive }) => `relative flex items-center gap-3 rounded-[4px] px-3 py-[7px] text-[14px] transition-colors
                    ${isActive ? 'bg-sand font-medium text-ink' : 'text-cocoa hover:bg-sand/50 hover:text-ink'}`}>
                  {({ isActive }) => (
                    <>
                      <span className={`absolute left-0 top-1/2 h-4 w-[2px] -translate-y-1/2 transition-colors ${isActive ? 'bg-terracotta' : 'bg-transparent'}`} aria-hidden />
                      <Icon className={`size-[17px] ${isActive ? 'text-terracotta-deep' : 'text-cocoa'}`} strokeWidth={1.6} aria-hidden />
                      {label}
                    </>
                  )}
                </NavLink>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </nav>
  )
}

function Account({ onNavigate }: { onNavigate?: () => void }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  if (!user) return null
  return (
    <div className="border-t border-rule pt-4">
      <p className="truncate px-3 text-[14px] font-medium text-ink">{user.name}</p>
      <p className="truncate px-3 text-[12.5px] text-cocoa">{user.email}{user.role === 'ADMIN' ? ' · admin' : ''}</p>
      <button onClick={async () => { onNavigate?.(); await logout(); navigate('/login') }}
        className="mt-3 flex items-center gap-2 px-3 py-1 text-[13px] text-cocoa hover:text-terracotta-deep cursor-pointer">
        <LogOut className="size-4" strokeWidth={1.6} /> Sign out
      </button>
    </div>
  )
}

export function AppShell() {
  const [open, setOpen] = useState(false)
  const location = useLocation()
  useEffect(() => { setOpen(false); window.scrollTo(0, 0) }, [location.pathname])

  return (
    <div className="min-h-screen bg-ivory">
      <a href="#main" className="sr-only focus:not-sr-only focus:fixed focus:left-3 focus:top-3 focus:z-50 focus:bg-sheet focus:px-3 focus:py-2">Skip to content</a>

      <aside className="fixed inset-y-0 left-0 z-30 hidden w-[232px] flex-col border-r border-rule bg-sidebar px-3 pb-5 pt-6 lg:flex">
        <Link to="/dashboard" className="mb-10 px-3"><Wordmark /></Link>
        <Nav />
        <div className="mt-auto"><Account /></div>
      </aside>

      <header className="sticky top-0 z-30 flex h-14 items-center justify-between border-b border-rule bg-sidebar/95 px-4 backdrop-blur-sm lg:hidden">
        <Link to="/dashboard"><Wordmark /></Link>
        <button onClick={() => setOpen(true)} className="-mr-2 p-2 text-ink cursor-pointer" aria-label="Open navigation" aria-expanded={open}>
          <Menu className="size-5" strokeWidth={1.6} />
        </button>
      </header>

      <AnimatePresence>
        {open && (
          <motion.div className="fixed inset-0 z-40 lg:hidden" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
            <div className="absolute inset-0 bg-ink/20" onClick={() => setOpen(false)} />
            <motion.aside role="dialog" aria-label="Navigation" initial={{ x: '100%' }} animate={{ x: 0 }} exit={{ x: '100%' }}
              transition={{ type: 'tween', duration: 0.22, ease: [0.25, 1, 0.5, 1] }}
              className="absolute inset-y-0 right-0 flex w-[min(320px,86vw)] flex-col bg-sidebar px-3 pb-6 pt-4 shadow-[var(--shadow-lift)]">
              <div className="mb-8 flex items-center justify-between px-3">
                <Wordmark />
                <button onClick={() => setOpen(false)} className="-mr-2 p-2 cursor-pointer" aria-label="Close navigation"><X className="size-5" strokeWidth={1.6} /></button>
              </div>
              <Nav onNavigate={() => setOpen(false)} />
              <div className="mt-auto"><Account onNavigate={() => setOpen(false)} /></div>
            </motion.aside>
          </motion.div>
        )}
      </AnimatePresence>

      <div className="lg:pl-[232px]">
        <main id="main" className="mx-auto w-full max-w-[1180px] px-5 pb-24 pt-8 sm:px-8 lg:px-12 lg:pt-12">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
