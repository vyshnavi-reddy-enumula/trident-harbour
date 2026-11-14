-- =====================================================================
-- Cascade-Aware Recovery Prioritization for a Simulated Naval Harbour
-- 01_schema.sql  |  PostgreSQL 14+  |  3NF
--
-- Layers:
--   A. Reference data   (asset_type, hazard_type, source_reliability)
--   B. Harbour model    (asset, dependency)          -- static, loaded into the Java graph
--   C. Experiment setup (scenario, scenario_hazard, crew)
--   D. Run logs         (simulation_run, sensor_reading, event_report,
--                        asset_state_change, repair_task, metric)
--   E. Views            (paired metrics, strategy comparison)
--
-- Time inside a run is SIMULATED hours from the start of the incident
-- (numeric), not wall-clock timestamps.
-- =====================================================================

DROP SCHEMA IF EXISTS harbour CASCADE;
CREATE SCHEMA harbour;
SET search_path TO harbour;

-- ---------------------------------------------------------------------
-- A. Reference data
-- ---------------------------------------------------------------------
CREATE TABLE asset_type (
    type_code    TEXT PRIMARY KEY,
    description  TEXT NOT NULL
);

CREATE TABLE hazard_type (
    hazard_code  TEXT PRIMARY KEY,
    description  TEXT NOT NULL
);

CREATE TABLE source_reliability (
    source_type  TEXT PRIMARY KEY,
    reliability  NUMERIC(3,2) NOT NULL CHECK (reliability BETWEEN 0 AND 1)
);

-- ---------------------------------------------------------------------
-- B. Harbour model
-- asset.health_score / asset.status hold the INITIAL (healthy) state.
-- State during a run is logged in asset_state_change, never overwritten.
-- ---------------------------------------------------------------------
CREATE TABLE asset (
    asset_id      INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          TEXT NOT NULL UNIQUE,
    type_code     TEXT NOT NULL REFERENCES asset_type(type_code),
    criticality   SMALLINT NOT NULL CHECK (criticality BETWEEN 1 AND 5),
    repair_hours  NUMERIC(5,1) NOT NULL CHECK (repair_hours > 0),
    health_score  SMALLINT NOT NULL DEFAULT 100 CHECK (health_score BETWEEN 0 AND 100),
    status        TEXT NOT NULL DEFAULT 'OPERATIONAL'
                  CHECK (status IN ('OPERATIONAL', 'DEGRADED', 'FAILED'))
);

-- Row means: asset_id depends on depends_on_id.
--   REQUIRED : asset cannot operate if depends_on_id is down  -> FAILED
--   BACKUP   : depends_on_id gives partial cover               -> DEGRADED only
-- The graph must stay acyclic; the Java loader validates this on load.
CREATE TABLE dependency (
    asset_id        INT NOT NULL REFERENCES asset(asset_id) ON DELETE CASCADE,
    depends_on_id   INT NOT NULL REFERENCES asset(asset_id) ON DELETE CASCADE,
    dependency_type TEXT NOT NULL CHECK (dependency_type IN ('REQUIRED', 'BACKUP')),
    PRIMARY KEY (asset_id, depends_on_id),
    CHECK (asset_id <> depends_on_id)
);

-- ---------------------------------------------------------------------
-- C. Experiment setup
-- A scenario can contain several hazards (the three combined cases),
-- so hazards live in a junction table, not on scenario.
-- The random seed belongs to a RUN, not a scenario.
-- ---------------------------------------------------------------------
CREATE TABLE scenario (
    scenario_id     INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            TEXT NOT NULL UNIQUE,
    weather_factor  NUMERIC(3,2) NOT NULL CHECK (weather_factor BETWEEN 0 AND 1)
);

CREATE TABLE scenario_hazard (
    scenario_id  INT  NOT NULL REFERENCES scenario(scenario_id) ON DELETE CASCADE,
    hazard_code  TEXT NOT NULL REFERENCES hazard_type(hazard_code),
    severity     SMALLINT NOT NULL CHECK (severity BETWEEN 1 AND 5),
    PRIMARY KEY (scenario_id, hazard_code)
);

-- available_from: simulated hours after incident start when the crew can begin.
CREATE TABLE crew (
    crew_id         INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            TEXT NOT NULL UNIQUE,
    available_from  NUMERIC(5,1) NOT NULL DEFAULT 0 CHECK (available_from >= 0)
);

-- ---------------------------------------------------------------------
-- D. Run logs
-- One row per (scenario, strategy, seed, crew_count). A paired comparison
-- is two rows that differ ONLY in strategy.
-- ---------------------------------------------------------------------
CREATE TABLE simulation_run (
    run_id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    scenario_id  INT  NOT NULL REFERENCES scenario(scenario_id),
    strategy     TEXT NOT NULL CHECK (strategy IN ('BASELINE', 'INTEGRATED')),
    seed         BIGINT NOT NULL,
    crew_count   SMALLINT NOT NULL CHECK (crew_count BETWEEN 1 AND 3),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (scenario_id, strategy, seed, crew_count)
);

CREATE TABLE sensor_reading (
    reading_id  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    run_id      BIGINT NOT NULL REFERENCES simulation_run(run_id) ON DELETE CASCADE,
    asset_id    INT    NOT NULL REFERENCES asset(asset_id),
    sim_hour    NUMERIC(6,2) NOT NULL CHECK (sim_hour >= 0),
    value       NUMERIC(8,3) NOT NULL
);

-- is_real_fault is the simulator's GROUND TRUTH. The strategies never see it;
-- it exists so false-alarm dispatches can be measured.
-- confidence stays NULL until the Event Assessor scores the report.
CREATE TABLE event_report (
    report_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    run_id             BIGINT NOT NULL REFERENCES simulation_run(run_id) ON DELETE CASCADE,
    asset_id           INT    NOT NULL REFERENCES asset(asset_id),
    hazard_code        TEXT   NOT NULL REFERENCES hazard_type(hazard_code),
    source_type        TEXT   NOT NULL REFERENCES source_reliability(source_type),
    reported_severity  SMALLINT NOT NULL CHECK (reported_severity BETWEEN 1 AND 5),
    confidence         NUMERIC(4,3) CHECK (confidence BETWEEN 0 AND 1),
    is_real_fault      BOOLEAN NOT NULL,
    sim_hour           NUMERIC(6,2) NOT NULL CHECK (sim_hour >= 0)
);

-- Every status transition of every asset in a run. Lets you recompute
-- weighted downtime in SQL and cross-check the Java metric code.
CREATE TABLE asset_state_change (
    run_id      BIGINT NOT NULL REFERENCES simulation_run(run_id) ON DELETE CASCADE,
    asset_id    INT    NOT NULL REFERENCES asset(asset_id),
    sim_hour    NUMERIC(6,2) NOT NULL CHECK (sim_hour >= 0),
    new_status  TEXT NOT NULL CHECK (new_status IN ('OPERATIONAL', 'DEGRADED', 'FAILED')),
    PRIMARY KEY (run_id, asset_id, sim_hour)
);

-- outcome:
--   EFFECTIVE   asset became operational
--   WASTED      asset was repaired but an upstream asset was still down
--   FALSE_ALARM crew sent to a report that was not a real fault
CREATE TABLE repair_task (
    task_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    run_id      BIGINT NOT NULL REFERENCES simulation_run(run_id) ON DELETE CASCADE,
    asset_id    INT    NOT NULL REFERENCES asset(asset_id),
    crew_id     INT    NOT NULL REFERENCES crew(crew_id),
    start_hour  NUMERIC(6,2) NOT NULL CHECK (start_hour >= 0),
    end_hour    NUMERIC(6,2) NOT NULL,
    outcome     TEXT NOT NULL CHECK (outcome IN ('EFFECTIVE', 'WASTED', 'FALSE_ALARM')),
    CHECK (end_hour > start_hour)
);

CREATE TABLE metric (
    run_id       BIGINT NOT NULL REFERENCES simulation_run(run_id) ON DELETE CASCADE,
    metric_name  TEXT NOT NULL CHECK (metric_name IN (
                     'WEIGHTED_DOWNTIME',
                     'TIME_TO_RESTORE_CRITICAL',
                     'WASTED_REPAIR_HOURS',
                     'FALSE_ALARM_DISPATCHES')),
    value        NUMERIC(12,3) NOT NULL,
    PRIMARY KEY (run_id, metric_name)
);

-- ---------------------------------------------------------------------
-- Indexes (foreign-key columns that the queries and joins actually use)
-- ---------------------------------------------------------------------
CREATE INDEX idx_dependency_depends_on   ON dependency (depends_on_id);
CREATE INDEX idx_sensor_asset_time       ON sensor_reading (asset_id, sim_hour);
CREATE INDEX idx_sensor_run              ON sensor_reading (run_id);
CREATE INDEX idx_report_asset_time       ON event_report (asset_id, sim_hour);
CREATE INDEX idx_report_run              ON event_report (run_id);
CREATE INDEX idx_state_run_time          ON asset_state_change (run_id, sim_hour);
CREATE INDEX idx_task_run_crew           ON repair_task (run_id, crew_id);
CREATE INDEX idx_run_scenario_strategy   ON simulation_run (scenario_id, strategy);

-- ---------------------------------------------------------------------
-- E. Views
-- ---------------------------------------------------------------------

-- Baseline and integrated result for the SAME scenario, seed and crew count.
CREATE VIEW v_paired_metrics AS
SELECT b.scenario_id,
       b.crew_count,
       b.seed,
       mb.metric_name,
       mb.value AS baseline_value,
       mi.value AS integrated_value
FROM simulation_run b
JOIN simulation_run i
  ON  i.scenario_id = b.scenario_id
  AND i.seed        = b.seed
  AND i.crew_count  = b.crew_count
  AND i.strategy    = 'INTEGRATED'
JOIN metric mb ON mb.run_id = b.run_id
JOIN metric mi ON mi.run_id = i.run_id AND mi.metric_name = mb.metric_name
WHERE b.strategy = 'BASELINE';

-- The comparison table for the report. All four metrics are lower-is-better,
-- so improvement = (baseline - integrated) / baseline. Negative means the
-- integrated strategy did WORSE - report those rows too.
CREATE VIEW v_strategy_comparison AS
SELECT s.name                                             AS scenario,
       p.crew_count,
       p.metric_name,
       COUNT(*)                                           AS n_pairs,
       ROUND(AVG(p.baseline_value), 2)                    AS mean_baseline,
       ROUND(STDDEV_SAMP(p.baseline_value), 2)            AS sd_baseline,
       ROUND(AVG(p.integrated_value), 2)                  AS mean_integrated,
       ROUND(STDDEV_SAMP(p.integrated_value), 2)          AS sd_integrated,
       ROUND(100 * (AVG(p.baseline_value) - AVG(p.integrated_value))
                 / NULLIF(AVG(p.baseline_value), 0), 1)   AS pct_improvement,
       ROUND(AVG(p.baseline_value - p.integrated_value), 2)         AS mean_paired_diff,
       ROUND(STDDEV_SAMP(p.baseline_value - p.integrated_value), 2) AS sd_paired_diff
FROM v_paired_metrics p
JOIN scenario s ON s.scenario_id = p.scenario_id
GROUP BY s.name, p.crew_count, p.metric_name;
