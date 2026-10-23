import type { ScoreWeights, VictimWeights } from '../api/types'

/** Per-browser convenience only (default weights). Never used for anything that must persist. */
const KEY = 'dls.preferences.v1'

export interface Preferences { scoreWeights: ScoreWeights; victimWeights: VictimWeights }

export const DEFAULT_SCORE_WEIGHTS: ScoreWeights = {
  workLost: 0.35, processesTerminated: 0.25, processesRolledBack: 0.1, resourcesReclaimed: 0,
  recoverySteps: 0.1, starvationRisk: 0.2, starvationThreshold: 3,
}
export const DEFAULT_VICTIM_WEIGHTS: VictimWeights = { work: 0.4, priority: 0.2, resources: 0.2, history: 0.2 }

export function loadPreferences(): Preferences {
  try {
    const raw = localStorage.getItem(KEY)
    if (raw) {
      const p = JSON.parse(raw) as Partial<Preferences>
      return {
        scoreWeights: { ...DEFAULT_SCORE_WEIGHTS, ...p.scoreWeights },
        victimWeights: { ...DEFAULT_VICTIM_WEIGHTS, ...p.victimWeights },
      }
    }
  } catch { /* storage unavailable */ }
  return { scoreWeights: DEFAULT_SCORE_WEIGHTS, victimWeights: DEFAULT_VICTIM_WEIGHTS }
}

export function savePreferences(p: Preferences) {
  try { localStorage.setItem(KEY, JSON.stringify(p)) } catch { /* ignore */ }
}
