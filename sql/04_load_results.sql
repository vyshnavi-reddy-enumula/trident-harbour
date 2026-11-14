-- =====================================================================
-- 04_load_results.sql  |  TRIDENT harbour_db (PostgreSQL, schema: harbour)
-- Loads experiment results from the staging table into the real tables.
--
-- PRE-STEP (pgAdmin): import results_runs.csv into harbour.stg_results
--   (Import/Export Data -> Import, format csv, header ON, delimiter ,)
--   and confirm: SELECT COUNT(*) FROM harbour.stg_results;  -- 5514
--
-- Run ONLY when scenario, scenario_hazard, simulation_run and metric are
-- empty. One transaction: any error rolls back everything.
--
-- Severity mapping (documented choice, CSV has only MEDIUM and HIGH):
--   MEDIUM = 3, HIGH = 5 on the database's 1..5 scale.
-- Scenario names are '<SCENARIO>_<SEVERITY>', e.g. FLOOD_HIGH.
-- Weather factor 1.2 = value used in all experiments.
-- =====================================================================

BEGIN;

-- 1. Scenarios (expect 11)
INSERT INTO harbour.scenario (name, weather_factor)
SELECT DISTINCT scenario || '_' || severity, 1.2
FROM harbour.stg_results;

-- 2. Hazards per scenario; combined scenarios are split on '+' (expect 17)
INSERT INTO harbour.scenario_hazard (scenario_id, hazard_code, severity)
SELECT sc.scenario_id, h.hazard_code,
       CASE d.severity WHEN 'MEDIUM' THEN 3 WHEN 'HIGH' THEN 5 END
FROM (SELECT DISTINCT scenario, severity FROM harbour.stg_results) d
JOIN harbour.scenario sc ON sc.name = d.scenario || '_' || d.severity
CROSS JOIN LATERAL regexp_split_to_table(d.scenario, '\+') AS h(hazard_code);

-- 3. Simulation runs, one per CSV row (expect 5514)
INSERT INTO harbour.simulation_run (scenario_id, strategy, seed, crew_count)
SELECT sc.scenario_id, s.strategy, s.seed, s.crews
FROM harbour.stg_results s
JOIN harbour.scenario sc ON sc.name = s.scenario || '_' || s.severity;

-- 4. Metrics, three per run (expect 16542)
INSERT INTO harbour.metric (run_id, metric_name, value)
SELECT r.run_id, m.name, m.val
FROM harbour.stg_results s
JOIN harbour.scenario sc ON sc.name = s.scenario || '_' || s.severity
JOIN harbour.simulation_run r
  ON  r.scenario_id = sc.scenario_id
  AND r.strategy    = s.strategy
  AND r.seed        = s.seed
  AND r.crew_count  = s.crews
CROSS JOIN LATERAL (VALUES
   ('WEIGHTED_DOWNTIME',        s.weighted_downtime),
   ('TIME_TO_RESTORE_CRITICAL', s.time_to_critical),
   ('WASTED_REPAIR_HOURS',      s.wasted_hours)) AS m(name, val)
WHERE m.val IS NOT NULL;

COMMIT;

-- Verify (expect 11 | 17 | 5514 | 16542)
SELECT (SELECT COUNT(*) FROM harbour.scenario)        AS scenarios,
       (SELECT COUNT(*) FROM harbour.scenario_hazard) AS hazards,
       (SELECT COUNT(*) FROM harbour.simulation_run)  AS runs,
       (SELECT COUNT(*) FROM harbour.metric)          AS metrics;

SELECT * FROM harbour.v_strategy_comparison LIMIT 10;
