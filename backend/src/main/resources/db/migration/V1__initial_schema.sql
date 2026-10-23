-- Self-Healing Deadlock Simulator — initial schema.
-- Complete experimental states are stored as JSONB so every run is reproducible; frequently
-- queried values (metrics, status, counts) are duplicated into typed columns for filtering.

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_users_email ON users (lower(email));

CREATE TABLE scenarios (
    id                  BIGSERIAL PRIMARY KEY,
    owner_id            BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name                VARCHAR(120) NOT NULL,
    description         TEXT,
    config              JSONB        NOT NULL,   -- processes, resources, allocations, request sequence
    process_count       INT          NOT NULL,
    resource_count      INT          NOT NULL,
    operation_count     INT          NOT NULL,
    checkpoint_interval INT          NOT NULL,
    seed                BIGINT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_scenarios_owner_created ON scenarios (owner_id, created_at DESC);

CREATE TABLE simulation_runs (
    id                        BIGSERIAL PRIMARY KEY,
    owner_id                  BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- Runs survive scenario deletion: the exact configuration used is copied into scenario_config.
    scenario_id               BIGINT       REFERENCES scenarios (id) ON DELETE SET NULL,
    scenario_name             VARCHAR(120) NOT NULL,
    scenario_config           JSONB        NOT NULL,
    initial_state             JSONB        NOT NULL,  -- deadlocked snapshot produced by the simulator
    detection                 JSONB        NOT NULL,  -- WFG cycles, deadlocked and blocked sets
    snapshot_fingerprint      VARCHAR(64)  NOT NULL,  -- SHA-256 of initial_state
    deadlocked                BOOLEAN      NOT NULL,
    cycle_count               INT          NOT NULL,
    deadlocked_process_count  INT          NOT NULL,
    status                    VARCHAR(30)  NOT NULL CHECK (status IN ('NO_DEADLOCK', 'DEADLOCKED', 'RECOVERY_EXECUTED')),
    comparison                JSONB,                  -- ranking, recommendation, sensitivity, isolation checks
    recommended_strategy      VARCHAR(20),
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_runs_owner_created ON simulation_runs (owner_id, created_at DESC);
CREATE INDEX ix_runs_scenario ON simulation_runs (scenario_id);

CREATE TABLE recovery_results (
    id                     BIGSERIAL PRIMARY KEY,
    run_id                 BIGINT           NOT NULL REFERENCES simulation_runs (id) ON DELETE CASCADE,
    strategy               VARCHAR(20)      NOT NULL CHECK (strategy IN ('TERMINATION', 'ROLLBACK', 'TOTAL_ABORT')),
    status                 VARCHAR(20)      NOT NULL CHECK (status IN ('RECOVERED', 'PARTIAL', 'FAILED', 'NOT_REQUIRED')),
    halt_reason            VARCHAR(30)      NOT NULL,
    verified               BOOLEAN          NOT NULL,
    final_state            JSONB            NOT NULL,
    rounds                 JSONB            NOT NULL,  -- per-round cycles before/after, candidates, actions
    verification           JSONB            NOT NULL,
    metrics                JSONB            NOT NULL,  -- seven metrics + score breakdown
    work_lost              INT              NOT NULL,
    processes_terminated   INT              NOT NULL,
    processes_rolled_back  INT              NOT NULL,
    resources_reclaimed    INT              NOT NULL,
    recovery_steps         INT              NOT NULL,
    starvation_risk        INT              NOT NULL,
    aggregate_score        DOUBLE PRECISION NOT NULL,
    rank_position          INT,
    score_weights          JSONB            NOT NULL,
    victim_weights         JSONB            NOT NULL,
    execution_micros       BIGINT           NOT NULL,
    executed_at            TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT ux_result_run_strategy UNIQUE (run_id, strategy)
);
CREATE INDEX ix_results_run ON recovery_results (run_id);
