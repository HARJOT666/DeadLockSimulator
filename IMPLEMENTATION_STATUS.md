# Implementation status

_Last updated: 2026-10-10_

## Environment used
JDK 21 (Temurin 21.0.12, installed to `~/.jdks`; the system JDK 25 is a JRE without `javac`),
Maven 3.9.12, Node 24 / npm 11, PostgreSQL 16 via Docker Compose (Docker Desktop).

## Acceptance criteria
| # | Criterion | Status | Evidence |
|---|---|---|---|
| 1 | Spring Boot compiles and starts | ✅ | `mvn package`; app starts in ~4 s |
| 2 | PostgreSQL connectivity | ✅ | Hikari → PostgreSQL 16.15 |
| 3 | Flyway migrations | ✅ | V1 applied; `ddl-auto: validate` passes |
| 4 | Registration and login | ✅ | AuthIntegrationTest, browser E2E |
| 5 | JWT rejects unauthenticated / invalid / expired | ✅ | AuthIntegrationTest |
| 6 | Create and save scenario | ✅ | JSONB persisted; dry-run validation (422) |
| 7 | Reproducible deadlock | ✅ | Templates + seeded generator; fingerprint determinism tests |
| 8 | WFG + DFS detect deadlock | ✅ | DfsCycleDetectorTest, WaitForGraphBuilderTest |
| 9 | Same state copied independently ×3 | ✅ | StateClonerTest; `copiesIdentical` / `snapshotUnchanged` in every comparison |
| 10 | Three strategies keep state consistent | ✅ | RecoveryStrategiesTest + invariant checker |
| 11 | Verified by re-detection | ✅ | RecoveryVerifier; PARTIAL/FAILED on remaining cycles |
| 12 | Seven metrics calculated and persisted | ✅ | MetricsCalculatorTest; typed columns + JSONB |
| 13 | Comparison dashboard | ✅ | `/compare/:id` |
| 14 | Graphs/charts from backend data | ✅ | React Flow + Recharts fed by API only |
| 15 | Tests cover algorithms + security | ✅ | 84 tests |
| 16 | Frontend production build | ✅ | `npm run build` |
| 17 | README | ✅ | `README.md` |
| 18 | No fake data / dead buttons | ✅ | Dashboard empty state for new accounts; all actions call the API |

## Test results (latest run)
* `mvn test` — **84 run, 0 failures, 0 errors** (66 algorithm unit tests, 18 integration tests on PostgreSQL).
* `npm run build` — type-check + Vite build succeed.
* Browser E2E (Playwright + Chrome, development-only script): full flow passes; 390 px mobile width
  has 0 px horizontal overflow; console shows only the expected 401s (session probe, deliberate
  wrong password, post-logout redirect).

## Fixed during development
* Checkpoint docs/tests said "one checkpoint per crossed multiple"; the engine records one per WORK
  operation at the highest multiple (intermediate ones are dominated). Docs and tests aligned.
* `CHAR(64)` fingerprint column failed JPA schema validation → `VARCHAR(64)`.
* Recommendation headline printed a raw Java list → readable sentence.
* React Flow attribution had been hidden; restored (themed) per the library's licence request.
* Graph layout: 2-cycles laid out side by side so before/after graphs stay legible.

## Frontend redesign (2026-10-10/11)
Complete replacement of the dark dashboard with a warm editorial design (light beige pages, ivory sheets,
Fraunces / DM Sans / JetBrains Mono), feature-based structure, new public introduction page with parallax and
sticky scroll storytelling, photographic login/registration, recomposed dashboard, builder, run, comparison,
run log and settings pages. Backend untouched; API contract preserved (`docs/FRONTEND_API_CONTRACT.md`).

Fixed while redesigning:
* Graph nodes were controlled without `onNodesChange`, so selection and dragging silently did nothing →
  positions now live in React Flow state; selection comes from select changes (pointer and keyboard).
* An `sr-only` label inside a horizontally scrolling table widened the page by 116 px on phones →
  scroll wrappers are now positioned.
* `NumberInput` ignored width classes (base `w-full` won) → width classes are authoritative.
* Dashboard summary grammar; story stage labels.
* Watermarked rawpixel renditions were discarded in favour of clean Wikimedia Commons originals.

Verification: `npm run build` passes; browser E2E across all routes passes; no page errors or failed
requests (only the expected 401s from signed-out session probes); no horizontal overflow at 1440/820/390 px.

## Light-theme correction (2026-10-11)
* Root cause of the reported dark-brown pages: browser automatic dark mode (reproduced with Chrome
  `WebContentsForceDark`) inverting the beige design. Fixed with `color-scheme: only light` (meta + CSS); verified
  the pages stay beige with forced dark enabled.
* Applied the requested palette exactly (page `#F5EFE3`, alternate `#EFE6D8`, surface `#FFFCF6`, secondary
  `#EAE0D1`, borders `#DCD0BE`, text `#493528`/`#786858`, action `#A96445`→`#A76344` for AA, success `#74816A`).
* Removed every dark surface: espresso graph nodes → cream; espresso intro band → ivory; dark login photo
  overlay → framed photograph on beige; dark selected chip → terracotta wash; ivory sidebar with sand active item.
* Tables, charts and canvases moved onto ivory surfaces; chart colours changed to terracotta / sage-teal / ochre
  (validated for colour-vision deficiency).
* Verified: production build, full browser E2E (all routes; only expected signed-out 401s; no overflow at
  1440/820/390 px) and acceptance screenshots of login, dashboard, builder, graph and comparison under forced dark.

## Dark-brown rendering — actual root cause (2026-10-11)
The user's Chrome profile has the **Dark Reader** extension (4.9.133) and the desktop prefers dark. Dark Reader
recolours pages regardless of `color-scheme`. Reproduced by loading that extension into a test browser:
body `rgb(54,43,19)`, sidebar `rgb(37,28,11)`, graph canvas `rgb(42,28,0)`, text `rgb(209,186,171)`.
Fix: `<meta name="darkreader-lock">` in `frontend/index.html`. With the extension active, all ten pages now
compute body `#F5EFE3`, sidebar `#FBF7EF`, graph canvas `#FFFCF6`, headings `#493528`.
The requested tokens are now literal `:root` variables mapped into Tailwind via `@theme inline`.

## Known limitations / not done
* Single-instance resources only (documented in UI and README).
* Only PostgreSQL is containerised (no Dockerfiles for backend/frontend).
* The browser E2E script is not committed as an automated suite; there are no frontend unit tests.
* Photographs are capped at 1600 px; very large displays may see slight softness in full-bleed crops.
* Stateless JWT: logout cannot revoke a copied token before expiry.

## Run commands
```bash
docker compose up -d
cd backend && mvn spring-boot:run          # :8080
cd frontend && npm install && npm run dev  # :5173
./scripts/demo.sh                          # API walkthrough
cd backend && mvn test                     # needs the Compose DB
```
