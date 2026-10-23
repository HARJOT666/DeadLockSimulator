# Frontend ↔ backend contract map

Audit taken before the frontend redesign (2026-10-10). The redesign changes presentation only; every
call below must keep working unchanged. Types live in `frontend/src/api/types.ts`; the single client is
`frontend/src/api/endpoints.ts` (axios, `withCredentials`, base `/api`, Vite proxies `/api` → `:8080`).

## Authentication
* The JWT is an **HttpOnly, SameSite=Strict cookie** (`dls_token`, path `/api`) set by register/login and
  cleared by logout. JavaScript never reads or stores it.
* Session discovery: `GET /auth/me` on load (`AuthProvider`). Any other 401 → `onUnauthorized` listeners →
  user cleared, `sessionExpired` flag → `/login` shows "session expired".
* Errors: uniform `ApiError {timestamp,status,error,message,path,details[]}` → `describeError()`.

| Client fn | Method / path | Response | Used by (before redesign) |
|---|---|---|---|
| `register(name,email,password)` | POST `/auth/register` | `AuthResponse` (201) | RegisterPage |
| `login(email,password)` | POST `/auth/login` | `AuthResponse` | LoginPage |
| `logout()` | POST `/auth/logout` | 204 | AppShell account menu |
| `me()` | GET `/auth/me` | `User` | AuthProvider |
| `scenarios()` | GET `/scenarios` | `Scenario[]` (config null) | ScenariosPage |
| `scenario(id)` | GET `/scenarios/{id}` | `Scenario` | ScenarioBuilder (edit) |
| `createScenario(body)` | POST `/scenarios` | `Scenario` (201; 422 if unexecutable) | ScenarioBuilder |
| `updateScenario(id,body)` | PUT `/scenarios/{id}` | `Scenario` | ScenarioBuilder (edit) |
| `deleteScenario(id)` | DELETE `/scenarios/{id}` | 204 | ScenariosPage |
| `templates()` | GET `/scenarios/templates` | `Template[]` (with detection preview) | ScenarioBuilder |
| `generate(params)` | POST `/scenarios/generate` | `Template` | ScenarioBuilder generator |
| `preview(config)` | POST `/scenarios/preview` | `DetectionResult` | ScenarioBuilder |
| `startSimulation(scenarioId)` | POST `/simulation/start` | `SimulationRun` (201) | Scenarios, Builder |
| `run(id)` | GET `/simulation/{id}` | `SimulationRun` | SimulationPage |
| `state(id, strategy?)` | GET `/simulation/{id}/state` | `SystemState` | SimulationPage |
| `graph(id, strategy?)` | GET `/simulation/{id}/graph` | `GraphData` | Dashboard, Simulation, Compare (before/after) |
| `detection(id)` | GET `/deadlock/{id}` | `DetectionResult` | (available, unused) |
| `runStrategy(id, s, opts)` | POST `/recovery/{id}/terminate\|rollback\|abort` | `RecoveryResult` | (available, unused) |
| `compare(id, opts)` | POST `/recovery/{id}/compare` | `Comparison` | Simulation, Compare (weights) |
| `comparison(id)` | GET `/recovery/{id}/comparison` | `Comparison` (summary may be null) | ComparePage |
| `metrics(id)` | GET `/metrics/{id}` | `RecoveryResult[]` | (available, unused) |
| `downloadCsv('/metrics/{id}/csv')` | GET | text/csv blob | ComparePage |
| `history(query)` | GET `/history?q&deadlocked&status&recommended` | `HistoryItem[]` | HistoryPage |
| `downloadCsv('/history/csv', query)` | GET | text/csv blob | HistoryPage |
| `dashboard()` | GET `/dashboard` | `Dashboard` | DashboardPage |

`opts` = `{ scoreWeights?, victimWeights?, maxRounds? }`. Default weights come from per-browser
preferences (`lib/preferences.ts`, localStorage) and are always sent explicitly.

## Routes that must keep working
`/login`, `/register` (guest only) · protected: `/dashboard`, `/scenarios`, `/scenarios/new`,
`/scenarios/:id/edit`, `/simulations/:id`, `/compare/:id`, `/history`, `/settings` · `*` → 404.
`/` previously redirected to `/dashboard`; the redesign adds a public introduction page there.

## Functional behaviours to preserve
Client-side scenario validation (`lib/scenarioValidation.ts`) mirroring server checks; template / generator /
manual editor + deadlock helper; live preview; save, save & run; scenario delete with confirmation;
graph interactions (select node/edge → explanation, zoom/pan/fit, before/after on shared layout);
comparison weights re-run; CSV exports; history search/filters; settings default weights; logout.
