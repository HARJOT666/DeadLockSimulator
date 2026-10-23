import { useState } from 'react'
import { Check } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'
import { DEFAULT_SCORE_WEIGHTS, DEFAULT_VICTIM_WEIGHTS, loadPreferences, savePreferences } from '../../lib/preferences'
import { fmtDate } from '../../lib/format'
import { PageIntro } from '../../components/layout/PageIntro'
import { Section } from '../../components/layout/Section'
import { Button } from '../../components/ui/Button'
import { scoreSum, victimSum, WeightsEditor } from '../comparison/WeightsEditor'

export default function SettingsPage() {
  const { user } = useAuth()
  const [prefs, setPrefs] = useState(loadPreferences)
  const [saved, setSaved] = useState(false)
  if (!user) return null
  const valid = scoreSum(prefs.scoreWeights) > 0 && victimSum(prefs.victimWeights) > 0
  return (
    <>
      <PageIntro trail={[{ label: 'Reference' }, { label: 'Settings' }]} title="Settings"
        lede="Your account, the default weights used when you start a comparison, and the assumptions behind every result." />

      <Section number="01" title="Account">
        <dl className="grid gap-6 sm:grid-cols-2">
          {[
            ['Name', user.name],
            ['Email', user.email],
            ['Access', user.role === 'ADMIN' ? 'Administrator — can open every user’s runs' : 'Your own scenarios and runs only'],
            ['Member since', fmtDate(user.createdAt)],
          ].map(([k, v]) => <div key={k} className="border-l border-rule pl-4"><dt className="text-[12.5px] text-cocoa">{k}</dt><dd className="mt-1 text-[15px]">{v}</dd></div>)}
        </dl>
      </Section>

      <Section number="02" title="Default weights" note="Stored in this browser only. Each comparison records the weights it actually used, so changing these never alters past results.">
        <WeightsEditor score={prefs.scoreWeights} victim={prefs.victimWeights} maxRounds="" showRounds={false}
          onChange={(s, v) => { setPrefs({ scoreWeights: s, victimWeights: v }); setSaved(false) }} />
        <div className="mt-6 flex flex-wrap items-center gap-3">
          <Button disabled={!valid} icon={saved ? <Check className="size-4" /> : undefined} onClick={() => { savePreferences(prefs); setSaved(true) }}>{saved ? 'Saved' : 'Save defaults'}</Button>
          <Button variant="quiet" onClick={() => { setPrefs({ scoreWeights: DEFAULT_SCORE_WEIGHTS, victimWeights: DEFAULT_VICTIM_WEIGHTS }); setSaved(false) }}>Restore the documented defaults</Button>
        </div>
      </Section>

      <Section number="03" title="Session security">
        <ul className="max-w-[680px] space-y-3 text-[14.5px] leading-relaxed text-cocoa">
          <li>Your token lives in an <span className="text-ink">HttpOnly, SameSite=Strict</span> cookie, so page scripts cannot read it — which limits, but does not eliminate, what a cross-site-scripting bug could do while the page is open.</li>
          <li>Signing out clears the cookie. Tokens are stateless and expire after their lifetime (two hours by default).</li>
          <li>Passwords are stored only as BCrypt hashes.</li>
        </ul>
      </Section>

      <Section number="04" title="Model assumptions" note="What the results do and do not claim.">
        <ol className="max-w-[720px] space-y-4 text-[14.5px] leading-relaxed text-cocoa">
          {[
            ['Single-instance resources.', 'Each resource has one holder, so a directed Wait-For Graph cycle is sufficient for deadlock. Multi-instance (Banker-style) detection is not implemented.'],
            ['AND-requests.', 'A request may name several resources; free ones are granted at once and the rest are queued FIFO.'],
            ['Witness cycles.', 'DFS reports one cycle per back edge; the deadlocked set is the exact union of cyclic strongly connected components.'],
            ['Rollback.', 'A victim is restored to the latest checkpoint at which it did not yet hold the preempted resource; checkpoint #0 always qualifies.'],
            ['Frozen snapshot.', 'Recovery is evaluated on the deadlocked state; deferred operations are not re-executed afterwards.'],
            ['Heuristics.', 'The victim cost and the aggregate score are configurable heuristics. A recommendation holds only for this workload and these weights.'],
          ].map(([t, b], i) => (
            <li key={t} className="grid grid-cols-[28px_minmax(0,1fr)]"><span className="font-mono text-[12px] text-terracotta-deep">{String(i + 1).padStart(2, '0')}</span><span><span className="font-medium text-ink">{t}</span> {b}</span></li>
          ))}
        </ol>
      </Section>
    </>
  )
}
