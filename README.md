# Self-Healing Deadlock Simulator

**Detection, Automatic Recovery and Comparative Analysis**

A full-stack Operating Systems research tool that builds reproducible process/resource scenarios,
detects deadlock with a Wait-For Graph (WFG) and three-colour depth-first search, then runs three
recovery strategies on **independent deep copies of the same deadlocked snapshot**, verifies every
result by re-running detection, and compares them on seven metrics.

> Research hypothesis: no recovery strategy is universally optimal — the best one depends on cycle
> length, work distribution, checkpoint interval and resource density. The contribution is the
> controlled, reproducible, state-isolated comparison, not the individual algorithms.

| Layer | Stack |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring Web / Data JPA / Security / Validation, jjwt 0.12, Flyway, PostgreSQL 16, JUnit 5 |
| Frontend | React 19, Vite 7, TypeScript, Tailwind CSS 4, React Router 7, React Flow 12, Recharts 3, Framer Motion, Lucide, Axios |
| Infra | Docker Compose (PostgreSQL) |

---

## 1. Quick start

Prerequisites: **JDK 21** (`javac` required), Maven 3.9+, Node 20+ / npm, Docker.

```bash
# 1. PostgreSQL (creates deadlock_simulator and deadlock_simulator_test, user1/user1)
docker compose up -d

# 2. Backend  →  http://localhost:8080   (Swagger UI: /swagger-ui.html)
cd backend
export JWT_SECRET=$(openssl rand -base64 32)      # optional; otherwise an ephemeral key is used
mvn spring-boot:run

# 3. Frontend →  http://localhost:5173
cd ../frontend
npm install
npm run dev
```

Then: **register → New scenario → pick "Cascading deadlock" → Save & run simulation → Compare
recovery strategies.**

A command-line walkthrough of the same flow against the REST API:

```bash
./scripts/demo.sh cascading-deadlock     # any template key works, e.g. classic-three-cycle
```

### Commands

| Task | Command |
|---|---|
| All backend tests (unit + PostgreSQL integration) | `cd backend && mvn test` |
| Build backend jar | `cd backend && mvn package` → `java -jar target/deadlock-simulator-1.0.0.jar` |
| Frontend type-check | `cd frontend && npm run typecheck` |
| Frontend production build | `cd frontend && npm run build` (output in `frontend/dist`) |
| Stop / wipe database | `docker compose down` / `docker compose down -v` |
| Regenerate login background | `python3 scripts/generate_login_background.py` |

### Configuration

Backend (`backend/src/main/resources/application.yml`, see `backend/.env.example`):

| Variable | Default (local dev only) | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/deadlock_simulator` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `user1` / `user1` | DB credentials |
| `JWT_SECRET` | *(empty → ephemeral random key + warning)* | Base64 HMAC-SHA256 key ≥ 32 bytes |
| `JWT_EXPIRATION_MINUTES` | `120` | Token lifetime |
| `JWT_COOKIE_SECURE` | `false` | Set `true` behind HTTPS |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,…` | Allowed browser origins |
| `TEST_DB_URL` | `…/deadlock_simulator_test` | Integration-test database |

Frontend (`frontend/.env.example`): `VITE_API_BASE_URL` (default `/api`), `VITE_BACKEND_URL`
(dev-proxy target, default `http://localhost:8080`).

No secret is hard-coded. `user1/user1` are local development credentials only.

---

## 2. Architecture

```
frontend (Vite :5173) ──/api proxy──► backend (Spring Boot :8080) ──JPA/Flyway──► PostgreSQL 16
```

Backend packages (`com.deadlocksimulator`):

| Package | Responsibility |
|---|---|
| `controllers` | HTTP + validation only; return DTOs |
| `services` | Application logic, ownership checks, transactions, JSON persistence |
| `repositories` | Spring Data JPA |
| `entities` | Persisted records (`User`, `ScenarioEntity`, `SimulationRunEntity`, `RecoveryResultEntity`) |
| `models` | Simulation domain (`Process`, `Resource`, `SystemState`, `Checkpoint`, `WaitForGraph`, metrics, reports) |
| `simulation` | `SimulationEngine` (deterministic replay), `ScenarioGenerator` (seeded), templates, `RecoveryEngine` (pure compare pipeline) |
| `algorithms` | `WaitForGraphBuilder`, `DfsCycleDetector`, `StateCloner`, `VictimSelector`, `RecoveryVerifier`, `StateConsistencyChecker`, `MetricsCalculator`, `StrategyRanker` |
| `strategies` | `RecoveryStrategy` interface, shared detect→act→re-detect loop, `TerminationStrategy`, `RollbackStrategy`, `TotalAbortStrategy` |
| `security`, `config` | JWT filter/service, user details, Spring Security, CORS, OpenAPI |
| `dto`, `exceptions`, `enums` | API contract, uniform `ApiError`, enums |

The algorithm layer has no Spring/DB dependencies beyond `@Component`, so it is unit-tested directly.

### Database (Flyway `V1__initial_schema.sql`)

`users` · `scenarios` (config as JSONB) · `simulation_runs` (copy of the config used, deadlocked
snapshot, detection, SHA-256 fingerprint, comparison summary — all JSONB) · `recovery_results`
(one row per run × strategy: final state, round trace, verification, metrics as JSONB **plus** typed
metric columns for history/aggregation). Runs survive scenario deletion (`ON DELETE SET NULL`) because
they store the exact configuration they used.

### REST API

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login`, `/api/auth/logout` | Account / session (sets HttpOnly cookie) |
| GET | `/api/auth/me` | Current user |
| GET/POST | `/api/scenarios` | List / create |
| GET/PUT/DELETE | `/api/scenarios/{id}` | Read / update / delete |
| GET | `/api/scenarios/templates` | Built-in examples with detection preview |
| POST | `/api/scenarios/generate` | Seeded generator preview |
| POST | `/api/scenarios/preview` | Simulate + detect an unsaved config |
| POST | `/api/simulation/start` | Run a scenario, detect, persist snapshot |
| GET | `/api/simulation/{id}` · `/state?strategy=` · `/graph?strategy=` · `/results` | Run, snapshot/final state, WFG, results |
| GET | `/api/deadlock/{simulationId}` | Detection result |
| POST | `/api/recovery/{id}/terminate` · `/rollback` · `/abort` | One strategy |
| POST | `/api/recovery/{id}/compare` | All three on isolated copies + ranking |
| GET | `/api/recovery/{id}/comparison` | Stored comparison |
| GET | `/api/metrics/{id}` · `/api/metrics/{id}/csv` | Metrics / CSV |
| GET | `/api/history` (`q`, `deadlocked`, `status`, `recommended`, `from`, `to`) · `/api/history/csv` | History / CSV |
| GET | `/api/dashboard` | Dashboard statistics |

Recovery endpoints accept an optional body `{ scoreWeights, victimWeights, maxRounds }`.
Errors always use `{timestamp,status,error,message,path,details[]}`; validation → 400, unexecutable
scenario → 422, foreign or missing object → 404, duplicate email → 409, auth → 401.

---

## 3. Simulation model

* **Single-instance resources** (one holder, FIFO wait queue). Under this model a directed WFG cycle
  is *sufficient* for deadlock. General multi-instance detection is **not** implemented.
* **AND-requests:** `REQUEST` may name several resources; free ones are granted immediately, busy
  ones queued; the process is `WAITING` while any request is pending.
* **Deterministic trace:** a single globally ordered request sequence (`REQUEST`, `WORK`, `RELEASE`,
  `FINISH`). Operations of a blocked process are *deferred* and run when it is unblocked; a deadlock
  leaves them deferred forever. Same scenario ⇒ same state (verified by SHA-256 in tests).
* **Process states:** `RUNNING`, `WAITING`, `READY` (rolled back, will re-execute from its
  checkpoint), `COMPLETED`, `TERMINATED`.
* **Checkpoints:** #0 at start (initial work, no resources; initial allocations come after it). When a
  `WORK` operation crosses at least one multiple of the checkpoint interval, one checkpoint is
  recorded at the highest multiple reached, with the resources held at that time.
* **Invariants** (checked after every recovery): holder ⇔ held list, queue membership ⇔ pending
  request, `WAITING` ⇔ pending requests, dead processes hold/await nothing, work within bounds.

### Seeded generator (experimental factors)

Cycle count and length, overlapping cycles (AND-request across two successors), work distribution
(`EQUAL`, `UNIFORM`, `SKEWED`), work range, checkpoint interval, resource density (extra resources
per cycle member), blocked dependents, independent processes and prior victim history. The position
of the contended resource in each process's program is seeded, so rollback can be cheap or expensive.

---

## 4. Detection — WFG + three-colour DFS

Edge `Pi → Pj` (label R) ⇔ Pi waits for R and R is held by Pj. DFS colours vertices WHITE → GRAY →
BLACK; an edge to a GRAY vertex is a back edge and the cycle is read off the current DFS path. The
outer loop restarts from every WHITE vertex (disconnected graphs); self-loops are 1-cycles; empty
graphs are handled. **O(V + E)** — the result reports the number of adjacency entries examined.

* `cycles` — one **witness** cycle per back edge. A graph can contain exponentially many elementary
  cycles, so witnesses are not an enumeration of all of them.
* `deadlockedProcesses` — union of cyclic strongly connected components (each contains a back edge,
  so each is seeded by a witness; SCC = forward ∩ backward reachability). This is exact even when
  overlapping cycles yield fewer witnesses than cycles (tested).
* `blockedProcesses` — live processes not on a cycle that transitively wait for a deadlocked one.

---

## 5. Recovery

`RecoveryEngine.compare` fingerprints the snapshot, creates **three deep copies up front**
(`StateCloner` re-creates every mutable object and collection; only immutable records/strings are
shared), checks each copy's fingerprint equals the snapshot's, runs one strategy per copy, and checks
the snapshot's fingerprint is unchanged afterwards. The stored snapshot is also checked against its
persisted fingerprint before every recovery.

Every strategy uses the same loop: **detect → act → rebuild WFG → rerun DFS**, until no cycle remains,
the safety limit (default `2|P| + 5` rounds) is reached, or no admissible action exists. Each round's
cycles before/after, victim candidates with cost breakdown and actions are recorded.

| Strategy | One round |
|---|---|
| **A. Iterative termination** | Pick the minimum-cost victim among the deadlocked set; withdraw its pending requests, mark `TERMINATED`, release all its resources to the next FIFO waiters. |
| **B. Checkpoint rollback** | Candidates: deadlocked processes holding a resource another deadlocked process waits for. To preempt R from V, restore the **latest valid checkpoint**: V did not hold R there, and V still holds everything the checkpoint holds (checkpoint #0 always qualifies). Withdraw V's pending requests (issued after the checkpoint), release every resource acquired after it, reset the work counter, discard newer checkpoints, state `READY`, victim count + 1. Work lost = current − checkpoint work. |
| **C. Total abort** | Terminate the whole deadlocked set (cyclic-SCC members) atomically: all members are withdrawn and marked first, then their resources are released, so nothing is handed to another member. Blocked dependents and unrelated processes are **not** aborted. |

**Verification** is independent of the strategy: `RecoveryVerifier` rebuilds the WFG, reruns DFS and
runs the invariant checker. Status is `RECOVERED` only if no cycle remains, invariants hold and the
loop stopped because the deadlock was resolved; otherwise `PARTIAL` or `FAILED` (never inferred from
"no exception"). `NOT_REQUIRED` if the snapshot was not deadlocked.

### Victim-selection cost (lower = preferred victim)

```
cost(P) = wWork·norm(workAtRisk) + wPriority·norm(priority)
        + wResources·(1 − norm(resourcesHeld)) + wHistory·norm(victimCount)
```

`norm` is min-max over the current candidates (0 when all equal). Defaults 0.4 / 0.2 / 0.2 / 0.2.
`workAtRisk` is the whole work counter for termination and the work since the restored checkpoint for
rollback. Ties: lower work at risk, lower priority, natural id order.

**Relation to the case study.** The case study's weighted expression gives the victim-history term a
sign that — when the minimum cost is selected — makes a frequently chosen process *cheaper* to choose
again, contradicting its stated anti-starvation intent. This implementation minimises cost and
**adds** the history term, so each previous selection makes a process less attractive. Holding more
resources lowers the cost (more is reclaimed). It is a heuristic, not an optimal policy.

---

## 6. Metrics and ranking

| # | Metric | Unit / counting rule |
|---|---|---|
| 1 | Work lost | work units: full counter of terminated/aborted processes + work since the restored checkpoint per rollback; each action counted once |
| 2 | Processes terminated | termination victims or aborted members |
| 3 | Processes rolled back | rollback **operations** (a process rolled back twice counts twice); distinct processes reported separately |
| 4 | Resources reclaimed | resource release events caused by recovery (single-instance ⇒ one event = one instance) |
| 5 | Recovery steps | detect→act→re-detect rounds (total abort acts on the whole set in one round); per-process actions reported separately |
| 6 | Starvation risk | max victim count among surviving processes, incl. history carried into the scenario |
| 7 | Aggregate cost | `Σ wᵢ · min(rawᵢ / normaliserᵢ, 1) / Σ wᵢ` ∈ [0,1], **lower is better** |

Normalisers depend only on the snapshot (live work, live processes, resources, deadlocked-set size,
starvation threshold), so a score never changes when another strategy is added or removed. Default
weights: work 0.35, terminated 0.25, rollbacks 0.10, steps 0.10, starvation 0.20, resources reclaimed
**0** (it is mostly a consequence of terminations/rollbacks — weighting it by default would count the
same disruption twice). Every term is shown in the UI's score breakdown.

Ranking: only verified strategies are eligible; failed/partial ones rank last regardless of score.
The recommendation names the trade-offs against each alternative and is re-scored under four
alternative weight profiles (work preservation, process survival, fairness, fast recovery). If the
winner changes, the UI says so — the recommendation is always conditional on weights and workload.

Example (`cascading-deadlock`, default weights): rollback 0.257 (5 work lost, 2 rollbacks,
starvation 1) · total abort 0.427 (45 lost, 2 aborted, 1 round) · termination 0.430 (35 lost,
2 terminated). Under the fairness profile termination wins; under fast recovery total abort wins.

---

## 7. Security

* BCrypt password hashes; hashes are never returned. Login uses a dummy hash for unknown emails to
  equalise timing and returns one generic error message.
* HS256 JWT (jjwt 0.12) with issuer and expiry, signing key from `JWT_SECRET`.
* The browser app receives the JWT in an **HttpOnly, SameSite=Strict** cookie (path `/api`); the
  frontend never reads or stores the token. The token is also returned in the login body for API
  clients, which can send `Authorization: Bearer …`.
* Trade-offs, stated plainly: HttpOnly prevents script from *reading* the token but an XSS bug could
  still issue requests while the page is open; CSRF protection relies on SameSite=Strict plus strict
  CORS (no server sessions); logout clears the cookie but stateless JWTs stay valid until expiry. JWT
  alone does not make the application secure.
* Ownership: users only reach their own scenarios/runs (foreign ids → 404). `ADMIN` users can read
  everyone's runs; promote with `UPDATE users SET role='ADMIN' WHERE email='…';`.
* CSV export neutralises spreadsheet formula injection.

---

## 8. Testing

`mvn test` runs **84 tests**:

* **Algorithms (66, no Spring context):** WFG construction; DFS on empty, acyclic, self-loop,
  disconnected, multiple, overlapping and 2000-vertex graphs; SCC-based deadlocked set; simulator
  determinism, deferral, checkpoints, validation; `StateCloner` isolation (mutating one copy leaves
  the snapshot and other copies byte-identical) and JSON round-trip; every victim-selection factor and
  hand-computed costs; each strategy incl. checkpoint restoration, work-loss accounting, ownership
  consistency, cascading/overlapping/multiple cycles, safety-limit partial results; hand-computed
  metrics and scores; ranking and weight sensitivity; a sweep over generated workloads asserting more
  than one strategy wins.
* **Integration (18, Spring Boot + MockMvc against real PostgreSQL `deadlock_simulator_test`):**
  registration + BCrypt, duplicate email, validation, login, cookie, invalid credentials, missing /
  tampered / expired / foreign-key tokens, logout; scenario CRUD + JSONB persistence, 422 for
  unexecutable scenarios, preview; full simulate → detect → compare → persisted metrics workflow;
  single-strategy endpoints; custom and invalid weights; no-deadlock runs; generator determinism;
  dashboard/history/CSV; runs surviving scenario deletion; user isolation on every endpoint; admin
  access.

The integration tests require the Compose database to be running. No H2 is used anywhere.

Frontend: `npm run build` (type-check + production build). After the redesign an end-to-end browser pass
(Playwright driving Chrome) covered every route: the introduction's scroll story, login/registration
validation and errors, protected-route redirects, template → preview → save & run, graph node selection,
comparison (evidence, before/after tabs, weight re-run, CSV download), generator, history search, the delete
dialog, settings, 404, logout, and overflow checks at 1440 px, 820 px and 390 px (none). That script is not
part of the repository's automated suite.

---

## 9. Frontend

**Design language.** A light, beige-first editorial research application: ~80% beige / ivory / warm white,
~15% sand and taupe secondary surfaces, and only small amounts of espresso, terracotta and sage. Page `#F5EFE3`,
sidebar `#FBF7EF`, ivory content surfaces `#FFFCF6` (tables, charts, graph canvas, forms), sand `#EEE5D7`,
borders `#DCCFBD`, text `#493528` / `#786858`, highlight `#D9C49D`. Dark brown is used only for type and small
accents — never for page, sidebar or panel backgrounds. Fraunces (display), DM Sans (interface), JetBrains Mono
(identifiers, measurements). Tokens: `frontend/src/styles/tokens.css`.

Accessibility adjustments to the requested palette (each noted in the token file): the primary-action fill is
`#A76344` (one step from the requested `#A96445`, so ivory button text reaches AA 4.55:1); terracotta text uses
`#8D5037`; success text uses `#5B6752` (the requested `#74816A` stays for marks, borders and edges, where 3:1 is
sufficient); the muted `#968777` is reserved for placeholders and decoration.

**Light only.** The page declares `<meta name="color-scheme" content="only light">`, `color-scheme: only light`
and `<meta name="darkreader-lock">`. Without them, browser auto-dark modes (Chrome "Auto Dark Mode") and the
Dark Reader extension recolour the beige design into dark brown with light text. The theme tokens are the
`:root` variables in `frontend/src/styles/tokens.css` (`--app-background`, `--sidebar-background`,
`--surface-background`, …); Tailwind colours are mapped onto them with `@theme inline`.

**Structure** (`frontend/src`): `app/` (router, providers) · `api/` (typed client — unchanged contract, see
`docs/FRONTEND_API_CONTRACT.md`) · `components/` (layout, ui primitives, graphs, charts, illustrations, motion)
· `features/` (auth, intro, dashboard, scenarios, simulation, comparison, history, settings) · `hooks/` · `lib/`
· `styles/` · `assets/images/`.

**Pages.** `/` public introduction with a parallax hero and a sticky, scroll-driven story
(detect → analyse → recover → verify) · `/login` split photographic layout · `/register` · `/dashboard`
greeting with a data-derived summary, typographic figures, latest deadlock (live WFG + prose account),
strategy findings, recent runs · `/scenarios` editorial index · `/scenarios/new`, `/scenarios/:id/edit`
numbered workspace with a sticky workbench (configuration sketch, server preview, validation, save) ·
`/simulations/:id` · `/compare/:id` isolation strip, conditional finding, seven-metric table, charts, the
strategies as numbered chapters with round-by-round evidence, score breakdown, weight sensitivity,
before/after graphs, weight adjustment and CSV export · `/history` run log grouped by day with search,
filters and CSV · `/settings`. Every number and graph comes from the API; new accounts see empty states.

**Wait-For Graph.** React Flow on a light ivory canvas: cream process nodes with espresso labels (terracotta
border = on a cycle, gold border = blocked behind one, sage = rolled back, dashed = terminated), terracotta dashed
cycle edges, sage wait edges, resource labels, legend, zoom/pan/fit, drag, and a detail panel for any
selected node or edge (pointer or keyboard). Before/after views share one layout.

**Charts.** Recharts on ivory canvases with hairline grids and espresso labels. Strategy identity:
terracotta `#B0522E` (termination), sage-teal `#1F8A70` (rollback), ochre `#C99528` (total abort). A plain sage
was indistinguishable from terracotta under deuteranopia, so the rollback sage leans teal; the set passes the
colour-vision-deficiency validator. Ochre is light against ivory, so values are labelled directly and also listed
in tables.

**Motion.** Framer Motion section reveals, gentle parallax on photographs, sticky section labels, animated
disclosures and dialogs, short chart entrance; all disabled under `prefers-reduced-motion`
(`MotionConfig reducedMotion="user"` plus a CSS fallback). No scroll hijacking, no continuous background
animation.

**Imagery.** Eight public-domain / CC0 photographs and 1840s U.S. Patent Office drawings from Wikimedia
Commons, stored locally as 1600/800 px WebP with `srcset`, lazy loading, alt text and a paper fallback.
Sources and licences: `frontend/src/assets/images/CREDITS.md`. Four custom SVG technical illustrations
(`components/illustrations/Diagrams.tsx`) explain the concepts; they never stand in for data.

## 10. Limitations

* Single-instance resources only; no multi-instance (Banker-style) detection.
* DFS witnesses are not an enumeration of all elementary cycles (the deadlocked set is exact).
* Recovery is evaluated on the frozen snapshot: deferred operations are not re-executed afterwards,
  and a rolled-back process is left `READY` at its checkpoint.
* Victim cost and aggregate score are configurable heuristics; recommendations are conditional.
* Only PostgreSQL is containerised; backend and frontend run locally.

## 11. Troubleshooting

| Symptom | Fix |
|---|---|
| `release version 21 not supported` | Maven is using a JRE or older JDK. Point `JAVA_HOME` at a full JDK 21. |
| Backend: connection refused on 5432 | `docker compose up -d`; check `docker compose ps` is healthy. |
| Integration tests fail to connect | The `deadlock_simulator_test` DB is created by `docker/postgres-init` **only on first volume init**. On an existing volume: `docker exec -it deadlock-simulator-postgres psql -U user1 -d postgres -c 'CREATE DATABASE deadlock_simulator_test'`. |
| Logged out after backend restart | `JWT_SECRET` is unset, so a new ephemeral key was generated. Set it. |
| Port 8080 already in use | Another backend instance is running: `ss -ltnp \| grep 8080`. |
| Frontend shows "Cannot reach the server" | Backend not running, or `VITE_BACKEND_URL` is wrong. |
| `npm install` warns about esbuild scripts | `npm approve-scripts esbuild` (npm 11 script allow-list). |
