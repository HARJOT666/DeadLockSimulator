import { http } from './client'
import type {
  AuthResponse, Comparison, Dashboard, DetectionResult, GeneratorParams, GraphData, HistoryItem, RecoveryResult,
  RunStatus, Scenario, ScenarioConfig, ScoreWeights, SimulationRun, StrategyType, SystemState, Template, User,
  VictimWeights,
} from './types'

export interface RecoveryOptions { scoreWeights?: ScoreWeights; victimWeights?: VictimWeights; maxRounds?: number }
export interface HistoryQuery { q?: string; deadlocked?: boolean; status?: RunStatus; recommended?: StrategyType }

const d = <T,>(p: Promise<{ data: T }>) => p.then((r) => r.data)

export const api = {
  register: (name: string, email: string, password: string) =>
    d<AuthResponse>(http.post('/auth/register', { name, email, password })),
  login: (email: string, password: string) => d<AuthResponse>(http.post('/auth/login', { email, password })),
  logout: () => http.post('/auth/logout'),
  me: () => d<User>(http.get('/auth/me')),

  scenarios: () => d<Scenario[]>(http.get('/scenarios')),
  scenario: (id: number) => d<Scenario>(http.get(`/scenarios/${id}`)),
  createScenario: (body: { name: string; description: string; config: ScenarioConfig }) =>
    d<Scenario>(http.post('/scenarios', body)),
  updateScenario: (id: number, body: { name: string; description: string; config: ScenarioConfig }) =>
    d<Scenario>(http.put(`/scenarios/${id}`, body)),
  deleteScenario: (id: number) => http.delete(`/scenarios/${id}`),
  templates: () => d<Template[]>(http.get('/scenarios/templates')),
  generate: (params: GeneratorParams) => d<Template>(http.post('/scenarios/generate', params)),
  preview: (config: ScenarioConfig) => d<DetectionResult>(http.post('/scenarios/preview', config)),

  startSimulation: (scenarioId: number) => d<SimulationRun>(http.post('/simulation/start', { scenarioId })),
  run: (id: number) => d<SimulationRun>(http.get(`/simulation/${id}`)),
  state: (id: number, strategy?: StrategyType) => d<SystemState>(http.get(`/simulation/${id}/state`, { params: { strategy } })),
  graph: (id: number, strategy?: StrategyType) => d<GraphData>(http.get(`/simulation/${id}/graph`, { params: { strategy } })),
  detection: (id: number) => d<DetectionResult>(http.get(`/deadlock/${id}`)),

  runStrategy: (id: number, strategy: StrategyType, opts: RecoveryOptions = {}) => {
    const path = strategy === 'TERMINATION' ? 'terminate' : strategy === 'ROLLBACK' ? 'rollback' : 'abort'
    return d<RecoveryResult>(http.post(`/recovery/${id}/${path}`, opts))
  },
  compare: (id: number, opts: RecoveryOptions = {}) => d<Comparison>(http.post(`/recovery/${id}/compare`, opts)),
  comparison: (id: number) => d<Comparison>(http.get(`/recovery/${id}/comparison`)),
  metrics: (id: number) => d<RecoveryResult[]>(http.get(`/metrics/${id}`)),

  history: (q: HistoryQuery = {}) => d<HistoryItem[]>(http.get('/history', { params: q })),
  dashboard: () => d<Dashboard>(http.get('/dashboard')),
  /** Downloads a CSV through the authenticated session and saves it as a file. */
  downloadCsv: async (path: string, params: Record<string, unknown>, filename: string) => {
    const res = await http.get(path, { params, responseType: 'blob' })
    const url = URL.createObjectURL(res.data as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
  },
}
