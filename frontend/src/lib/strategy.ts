import type { RecoveryStatus, StrategyType } from '../api/types'

/**
 * Chart identity per strategy: terracotta (termination), sage-teal (rollback), ochre (total abort).
 * Validated for colour-vision deficiency on the #FFFCF6 chart canvas; a plain sage was indistinguishable
 * from terracotta for deuteranopes, so the sage leans teal. Ochre is light against ivory, so every chart
 * labels its values directly and the same numbers appear in tables.
 */
export const STRATEGIES: Record<StrategyType, { label: string; short: string; numeral: string; color: string; blurb: string }> = {
  TERMINATION: {
    label: 'Iterative termination', short: 'Termination', numeral: 'I', color: '#B0522E',
    blurb: 'Kill one cost-selected victim per round, release its resources, re-detect.',
  },
  ROLLBACK: {
    label: 'Checkpoint rollback', short: 'Rollback', numeral: 'II', color: '#1F8A70',
    blurb: 'Preempt a resource by restoring the victim to its latest valid checkpoint.',
  },
  TOTAL_ABORT: {
    label: 'Total abort', short: 'Total abort', numeral: 'III', color: '#C99528',
    blurb: 'Abort every process in the deadlocked set at once.',
  },
}
export const STRATEGY_ORDER: StrategyType[] = ['TERMINATION', 'ROLLBACK', 'TOTAL_ABORT']

export const STATUS_META: Record<RecoveryStatus, { label: string; tone: 'ok' | 'warn' | 'bad' | 'neutral' }> = {
  RECOVERED: { label: 'Verified', tone: 'ok' },
  PARTIAL: { label: 'Partial — cycles remain', tone: 'warn' },
  FAILED: { label: 'Failed', tone: 'bad' },
  NOT_REQUIRED: { label: 'Not required', tone: 'neutral' },
}

export const METRIC_LABELS: Record<string, { label: string; unit: string; help: string }> = {
  workLost: { label: 'Work lost', unit: 'work units', help: 'Full work counter of killed processes plus work since the restored checkpoint for rollbacks.' },
  processesTerminated: { label: 'Processes terminated', unit: 'processes', help: 'Termination victims or aborted deadlocked-set members.' },
  processesRolledBack: { label: 'Rollbacks', unit: 'operations', help: 'Rollback operations performed (a process rolled back twice counts twice).' },
  resourcesReclaimed: { label: 'Resources reclaimed', unit: 'release events', help: 'Single-instance resource releases caused by recovery.' },
  recoverySteps: { label: 'Recovery rounds', unit: 'rounds', help: 'Detect → act → re-detect iterations.' },
  starvationRisk: { label: 'Starvation risk', unit: 'max victim count', help: 'Highest victim count among surviving processes (incl. prior history).' },
  aggregateScore: { label: 'Aggregate cost', unit: '0–1, lower is better', help: 'Weighted, normalised composite of the metrics above.' },
}
