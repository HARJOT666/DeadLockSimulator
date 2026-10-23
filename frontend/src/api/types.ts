// TypeScript mirrors of the backend DTOs (com.deadlocksimulator.dto / models).

export type Role = 'USER' | 'ADMIN'
export type StrategyType = 'TERMINATION' | 'ROLLBACK' | 'TOTAL_ABORT'
export type RecoveryStatus = 'RECOVERED' | 'PARTIAL' | 'FAILED' | 'NOT_REQUIRED'
export type RunStatus = 'NO_DEADLOCK' | 'DEADLOCKED' | 'RECOVERY_EXECUTED'
export type HaltReason = 'DEADLOCK_RESOLVED' | 'SAFETY_LIMIT_REACHED' | 'NO_ADMISSIBLE_ACTION'
export type OperationType = 'REQUEST' | 'WORK' | 'RELEASE' | 'FINISH'
export type ProcessState = 'RUNNING' | 'WAITING' | 'READY' | 'COMPLETED' | 'TERMINATED'
export type WorkDistribution = 'EQUAL' | 'UNIFORM' | 'SKEWED'

export interface User { id: number; name: string; email: string; role: Role; createdAt: string }
export interface AuthResponse { user: User; token: string; expiresInSeconds: number }

export interface ApiError {
  timestamp: string; status: number; error: string; message: string; path: string; details: string[]
}

export interface ProcessSpec { id: string; priority: number; totalWork: number; initialWork: number; previousVictimCount: number }
export interface InitialAllocation { resourceId: string; processId: string }
export interface Operation { type: OperationType; processId: string; resources: string[]; amount: number | null }
export interface ScenarioConfig {
  processes: ProcessSpec[]
  resources: string[]
  initialAllocations: InitialAllocation[]
  requestSequence: Operation[]
  checkpointInterval: number
  seed: number | null
  parameters: Record<string, unknown>
}

export interface Scenario {
  id: number; name: string; description: string | null; processCount: number; resourceCount: number
  operationCount: number; checkpointInterval: number; seed: number | null; runCount: number
  createdAt: string; updatedAt: string; config: ScenarioConfig | null
}

export interface WfgEdge { from: string; to: string; resource: string }
export interface DetectedCycle { processes: string[]; edges: WfgEdge[] }
export interface DetectionResult {
  deadlocked: boolean; cycles: DetectedCycle[]; deadlockedProcesses: string[]; blockedProcesses: string[]
  vertexCount: number; edgeCount: number; edgesExamined: number
}

export interface Template { key: string; name: string; description: string; config: ScenarioConfig; preview: DetectionResult }

export interface GeneratorParams {
  seed: number; cycleCount: number; cycleLength: number; blockedDependents: number; independentProcesses: number
  workDistribution: WorkDistribution; minWork: number; maxWork: number; checkpointInterval: number
  resourceDensity: number; maxExtraResources: number; overlapping: boolean; maxPreviousVictims: number
}

export interface SimulationRun {
  id: number; scenarioId: number | null; scenarioName: string; status: RunStatus; deadlocked: boolean
  cycleCount: number; deadlockedProcessCount: number; snapshotFingerprint: string; detection: DetectionResult
  recommendedStrategy: StrategyType | null; strategiesExecuted: StrategyType[]; scenarioConfig: ScenarioConfig
  createdAt: string
}

export interface GraphNode {
  id: string; state: ProcessState; priority: number; workDone: number; totalWork: number; victimCount: number
  heldResources: string[]; waitingFor: string[]; checkpointCount: number; lastCheckpointWork: number | null
  inCycle: boolean; deadlocked: boolean; blocked: boolean; recoveryAction: 'TERMINATE' | 'ROLLBACK' | 'ABORT' | null
}
export interface GraphEdge { id: string; from: string; to: string; resource: string; inCycle: boolean; explanation: string }
export interface ResourceView { id: string; holder: string | null; waitQueue: string[] }
export interface GraphData {
  view: string; nodes: GraphNode[]; edges: GraphEdge[]; cycles: string[][]; deadlockedProcesses: string[]
  blockedProcesses: string[]; resources: ResourceView[]; deadlocked: boolean
}

export interface Checkpoint { sequence: number; workDone: number; heldResources: string[]; takenAtStep: number }
export interface SimProcess {
  id: string; priority: number; totalWork: number; workDone: number; state: ProcessState; heldResources: string[]
  waitingFor: string[]; victimCount: number; checkpoints: Checkpoint[]; deferredOperations: Operation[]
}
export interface SimulationEvent { step: number; type: string; processId: string | null; resourceId: string | null; message: string }
export interface SystemState {
  processes: Record<string, SimProcess>
  resources: Record<string, { id: string; holder: string | null; waitQueue: string[] }>
  checkpointInterval: number; clock: number; events: SimulationEvent[]
  victimHistory: { round: number; action: string; processId: string; cost: number | null }[]
}

export interface ScoreWeights {
  workLost: number; processesTerminated: number; processesRolledBack: number; resourcesReclaimed: number
  recoverySteps: number; starvationRisk: number; starvationThreshold: number
}
export interface VictimWeights { work: number; priority: number; resources: number; history: number }

export interface RecoveryAction {
  round: number; type: 'TERMINATE' | 'ROLLBACK' | 'ABORT'; processId: string; selectionCost: number | null
  workBefore: number; workAfter: number; workLost: number; preemptedResource: string | null
  restoredCheckpoint: number | null; resourcesReleased: string[]; handovers: string[]; explanation: string
}
export interface CandidateScore {
  processId: string; workAtRisk: number; priority: number; resourcesHeld: number; victimCount: number; cost: number
  workTerm: number; priorityTerm: number; resourceTerm: number; historyTerm: number; option: string
}
export interface RoundTrace {
  round: number; cyclesBefore: string[][]; deadlockedBefore: string[]; candidates: CandidateScore[]
  actions: RecoveryAction[]; cyclesAfter: string[][]; deadlockedAfter: string[]
}
export interface VerificationResult {
  deadlockFree: boolean; remainingCycles: string[][]; remainingDeadlocked: string[]; consistent: boolean
  violations: string[]; graphVertices: number; graphEdges: number
}
export interface ScoreComponent { metric: string; rawValue: number; normaliser: number; normalised: number; weight: number; contribution: number }
export interface RecoveryMetrics {
  workLost: number; processesTerminated: number; processesRolledBack: number; distinctProcessesRolledBack: number
  resourcesReclaimed: number; recoverySteps: number; recoveryActions: number; starvationRisk: number
  aggregateScore: number; scoreBreakdown: ScoreComponent[]
}
export interface RecoveryResult {
  id: number; strategy: StrategyType; displayName: string; status: RecoveryStatus; haltReason: HaltReason
  verified: boolean; rank: number | null; rounds: RoundTrace[]; verification: VerificationResult
  metrics: RecoveryMetrics; scoreWeights: ScoreWeights; victimWeights: VictimWeights; executionMicros: number
  executedAt: string
}
export interface RankingEntry { rank: number; strategy: StrategyType; status: RecoveryStatus; eligible: boolean; score: number; explanation: string }
export interface Recommendation { strategy: StrategyType | null; headline: string; tradeoffs: string[]; caveat: string; weightSensitive: boolean }
export interface SensitivityProfile { name: string; weights: ScoreWeights; winner: StrategyType | null; scores: Record<StrategyType, number> }
export interface ComparisonSummary {
  snapshotFingerprint: string; snapshotUnchanged: boolean; copiesIdentical: boolean; persistedSnapshotIntact: boolean
  weights: ScoreWeights; victimWeights: VictimWeights; maxRounds: number; ranking: RankingEntry[]
  recommendation: Recommendation; sensitivity: SensitivityProfile[]
}
export interface Comparison { runId: number; scenarioName: string; summary: ComparisonSummary | null; results: RecoveryResult[] }

export interface StrategySummary {
  strategy: StrategyType; status: RecoveryStatus; rank: number | null; aggregateScore: number; workLost: number
  processesTerminated: number; processesRolledBack: number; resourcesReclaimed: number; recoverySteps: number; starvationRisk: number
}
export interface HistoryItem {
  runId: number; scenarioId: number | null; scenarioName: string; createdAt: string; status: RunStatus; deadlocked: boolean
  cycleCount: number; deadlockedProcessCount: number; recommendedStrategy: StrategyType | null; results: StrategySummary[]
}
export interface StrategyStats {
  strategy: StrategyType; displayName: string; executions: number; averageScore: number; averageWorkLost: number
  timesRankedFirst: number; recovered: number
}
export interface Dashboard {
  totalScenarios: number; totalRuns: number; deadlockedRuns: number; comparedRuns: number; recoverySuccesses: number
  recoveryFailures: number; strategies: StrategyStats[]; recentRuns: HistoryItem[]; latestDeadlockedRunId: number | null
}
