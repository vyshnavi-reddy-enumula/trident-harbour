-- =====================================================================
-- 03_queries.sql  |  Sample queries for the DBMS evidence pack
-- Run after 01_schema.sql and 02_seed.sql.
-- =====================================================================
SET search_path TO harbour;

-- ---------------------------------------------------------------------
-- Q1. Everything downstream of one asset (cascade set) via recursive CTE.
--     Follows REQUIRED edges only: a BACKUP edge degrades, it does not fail.
--     The path array is a cycle guard.
-- ---------------------------------------------------------------------
WITH RECURSIVE downstream AS (
    SELECT d.asset_id, 1 AS depth, ARRAY[d.depends_on_id, d.asset_id] AS path
    FROM dependency d
    JOIN asset root ON root.asset_id = d.depends_on_id
    WHERE root.name = 'Substation A' AND d.dependency_type = 'REQUIRED'
  UNION ALL
    SELECT d.asset_id, ds.depth + 1, ds.path || d.asset_id
    FROM downstream ds
    JOIN dependency d ON d.depends_on_id = ds.asset_id
    WHERE d.dependency_type = 'REQUIRED'
      AND d.asset_id <> ALL (ds.path)
)
SELECT a.name, a.criticality, MIN(ds.depth) AS hops_from_root
FROM downstream ds
JOIN asset a ON a.asset_id = ds.asset_id
GROUP BY a.asset_id, a.name, a.criticality
ORDER BY hops_from_root, a.name;

-- ---------------------------------------------------------------------
-- Q2. Root-cause candidates among failed assets: failed assets none of
--     whose REQUIRED dependencies are also failed. Edit the list to test.
-- ---------------------------------------------------------------------
WITH failed AS (
    SELECT asset_id FROM asset
    WHERE name IN ('Substation A', 'Drainage Pump Station 1', 'Comms Tower',
                   'Control Centre', 'Crane 1', 'Crane 2')
)
SELECT a.name AS root_cause_candidate
FROM failed f
JOIN asset a ON a.asset_id = f.asset_id
WHERE NOT EXISTS (
    SELECT 1
    FROM dependency d
    JOIN failed f2 ON f2.asset_id = d.depends_on_id
    WHERE d.asset_id = f.asset_id AND d.dependency_type = 'REQUIRED'
);

-- ---------------------------------------------------------------------
-- Q3. Static upper bound of "criticality unlocked" per asset:
--     own criticality + criticality of every DISTINCT downstream dependent
--     (REQUIRED edges). Distinct matters: Fuel Jetty is reached twice from
--     Substation A (directly and through Berth 2). The Java planner refines
--     this at run time, because a dependent only comes back if ALL its other
--     required parents are up too.
-- ---------------------------------------------------------------------
WITH RECURSIVE reach AS (
    SELECT a.asset_id AS root_id, d.asset_id AS dep_id, ARRAY[a.asset_id, d.asset_id] AS path
    FROM asset a
    JOIN dependency d ON d.depends_on_id = a.asset_id AND d.dependency_type = 'REQUIRED'
  UNION ALL
    SELECT r.root_id, d.asset_id, r.path || d.asset_id
    FROM reach r
    JOIN dependency d ON d.depends_on_id = r.dep_id AND d.dependency_type = 'REQUIRED'
    WHERE d.asset_id <> ALL (r.path)
),
distinct_reach AS (
    SELECT DISTINCT root_id, dep_id FROM reach
)
SELECT a.name,
       a.criticality AS own_criticality,
       a.criticality + COALESCE(SUM(dep.criticality), 0) AS criticality_unlocked,
       a.repair_hours,
       ROUND((a.criticality + COALESCE(SUM(dep.criticality), 0)) / a.repair_hours, 2)
           AS priority_before_confidence
FROM asset a
LEFT JOIN distinct_reach dr ON dr.root_id = a.asset_id
LEFT JOIN asset dep         ON dep.asset_id = dr.dep_id
GROUP BY a.asset_id, a.name, a.criticality, a.repair_hours
ORDER BY priority_before_confidence DESC;

-- ---------------------------------------------------------------------
-- Q4. Comparison view output (empty until runs are logged).
-- ---------------------------------------------------------------------
SELECT * FROM v_strategy_comparison
ORDER BY scenario, crew_count, metric_name;

-- ---------------------------------------------------------------------
-- Q5. Transaction pattern for logging ONE run (the Java DAO does this via
--     JDBC with autoCommit = false). Either the whole run is stored or none.
--     Shown as a template; the values are placeholders.
-- ---------------------------------------------------------------------
-- BEGIN;
--   INSERT INTO simulation_run (scenario_id, strategy, seed, crew_count)
--   VALUES (1, 'INTEGRATED', 42, 2) RETURNING run_id;      -- use returned id below
--   INSERT INTO asset_state_change (run_id, asset_id, sim_hour, new_status) VALUES (...);
--   INSERT INTO event_report (...) VALUES (...);
--   INSERT INTO repair_task  (...) VALUES (...);
--   INSERT INTO metric (run_id, metric_name, value) VALUES (...), (...), (...), (...);
-- COMMIT;   -- on any exception: ROLLBACK;
