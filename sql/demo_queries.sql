SET search_path TO harbour, public;

--A. Schema
-- 1. Tables and views
SELECT table_name, table_type
FROM information_schema.tables
WHERE table_schema = 'harbour'
ORDER BY table_type, table_name;

-- 2. Every column
SELECT table_name, column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = 'harbour'
ORDER BY table_name, ordinal_position;

-- 3. Primary keys, foreign keys, unique, CHECK rules
SELECT conrelid::regclass AS table_name,
       CASE contype WHEN 'p' THEN 'PRIMARY KEY'
                    WHEN 'f' THEN 'FOREIGN KEY'
                    WHEN 'u' THEN 'UNIQUE'
                    WHEN 'c' THEN 'CHECK' END AS constraint_type,
       conname,
       pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE connamespace = 'harbour'::regnamespace
ORDER BY conrelid::regclass::text, contype;

-- 4. Indexes
SELECT tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'harbour'
ORDER BY tablename, indexname;






-- 5. Row count of every table
SELECT table_name,
       (xpath('/row/c/text()',
              query_to_xml(format('SELECT count(*) AS c FROM %I.%I', table_schema, table_name),
                           false, true, '')))[1]::text::int AS row_count
FROM information_schema.tables
WHERE table_schema = 'harbour' AND table_type = 'BASE TABLE'
ORDER BY table_name;

--B. Lookups, assets, dependencies 

-- 6a. Lookup tables (run each separately)
SELECT * FROM asset_type ORDER BY type_code;

-- 6b
SELECT * FROM hazard_type ORDER BY hazard_code;

-- 6c
SELECT * FROM source_reliability ORDER BY reliability DESC;

-- 7. Assets with type description
SELECT a.asset_id, a.name, a.type_code, t.description AS type_description,
       a.criticality, a.repair_hours, a.health_score, a.status
FROM asset a
JOIN asset_type t ON t.type_code = a.type_code
ORDER BY a.asset_id;

-- 8. Dependencies: "<asset> needs <depends_on>"
SELECT a.name AS asset, d.dependency_type, b.name AS depends_on
FROM dependency d
JOIN asset a ON a.asset_id = d.asset_id
JOIN asset b ON b.asset_id = d.depends_on_id
ORDER BY a.asset_id, b.asset_id;









-- 9. Root-cause candidates (most direct dependents)
SELECT b.name AS asset,
       COUNT(*) AS direct_dependents,
       SUM(a.criticality) AS criticality_hanging_on_it
FROM dependency d
JOIN asset a ON a.asset_id = d.asset_id
JOIN asset b ON b.asset_id = d.depends_on_id
WHERE UPPER(d.dependency_type) = 'REQUIRED'
GROUP BY b.name
ORDER BY direct_dependents DESC, criticality_hanging_on_it DESC;

-- 10. Assets that depend on nothing
SELECT a.name, a.criticality
FROM asset a
WHERE NOT EXISTS (SELECT 1 FROM dependency d WHERE d.asset_id = a.asset_id);


-- 11. Dependency types
SELECT dependency_type, COUNT(*) AS links
FROM dependency
GROUP BY dependency_type;

--C. Cascade in SQL (recursive CTE) 

-- 12. Everything that goes down when Substation A fails
WITH RECURSIVE cascade AS (
    SELECT a.asset_id, a.name, a.criticality, 0 AS depth
    FROM asset a
    WHERE a.name = 'Substation A'
  UNION
    SELECT x.asset_id, x.name, x.criticality, c.depth + 1
    FROM cascade c
    JOIN dependency d ON d.depends_on_id = c.asset_id
                     AND UPPER(d.dependency_type) = 'REQUIRED'
    JOIN asset x ON x.asset_id = d.asset_id
)
SELECT name, criticality, MIN(depth) AS depth
FROM cascade
WHERE depth > 0
GROUP BY name, criticality
ORDER BY MIN(depth), name;




-- 12b. Totals (Java CascadeDemo says: 12 assets, criticality 46)
WITH RECURSIVE cascade AS (
    SELECT a.asset_id, a.criticality, 0 AS depth
    FROM asset a WHERE a.name = 'Substation A'
  UNION
    SELECT x.asset_id, x.criticality, c.depth + 1
    FROM cascade c
    JOIN dependency d ON d.depends_on_id = c.asset_id
                     AND UPPER(d.dependency_type) = 'REQUIRED'
    JOIN asset x ON x.asset_id = d.asset_id
)
SELECT COUNT(DISTINCT asset_id) AS assets_down,
       SUM(criticality) AS total_criticality
FROM (SELECT DISTINCT asset_id, criticality FROM cascade WHERE depth > 0) t;

-- 13. Criticality unlocked for every asset (numerator of the priority formula)
WITH RECURSIVE reach AS (
    SELECT asset_id AS root_id, asset_id AS node_id FROM asset
  UNION
    SELECT r.root_id, d.asset_id
    FROM reach r
    JOIN dependency d ON d.depends_on_id = r.node_id
                     AND UPPER(d.dependency_type) = 'REQUIRED'
)
SELECT root.name,
       COUNT(*) - 1 AS downstream_assets,
       SUM(n.criticality) AS criticality_unlocked
FROM reach r
JOIN asset root ON root.asset_id = r.root_id
JOIN asset n ON n.asset_id = r.node_id
GROUP BY root.name
ORDER BY criticality_unlocked DESC;

--D. Scenarios 

-- 14. Scenarios with hazards and severity
SELECT s.scenario_id, s.name, s.weather_factor,
       h.hazard_code, ht.description AS hazard, h.severity
FROM scenario s
JOIN scenario_hazard h ON h.scenario_id = s.scenario_id
JOIN hazard_type ht ON ht.hazard_code = h.hazard_code
ORDER BY s.scenario_id, h.hazard_code;


--E. Simulation results (these are your real data) 
-- 18. Runs per strategy and crew count
SELECT strategy, crew_count, COUNT(*) AS runs
FROM simulation_run
GROUP BY strategy, crew_count
ORDER BY strategy, crew_count;

-- 19. Metric names stored
SELECT DISTINCT metric_name FROM metric ORDER BY metric_name;

-- 20. THE headline result: the comparison view
SELECT * FROM v_strategy_comparison
ORDER BY scenario, crew_count, metric_name;

-- 20b. Weighted downtime only, rounded (note the UPPERCASE metric name)
SELECT scenario, crew_count, n_pairs,
       ROUND(mean_baseline::numeric, 2)   AS baseline,
       ROUND(mean_integrated::numeric, 2) AS integrated,
       ROUND(pct_improvement::numeric, 1) AS pct_improvement
FROM v_strategy_comparison
WHERE metric_name = 'WEIGHTED_DOWNTIME'
ORDER BY scenario, crew_count;

-- 20c. Where integrated loses or ties (worst first)
SELECT scenario, crew_count, n_pairs,
       ROUND(mean_baseline::numeric,1)   AS baseline,
       ROUND(mean_integrated::numeric,1) AS integrated,
       ROUND(pct_improvement::numeric,1) AS pct
FROM v_strategy_comparison
WHERE metric_name = 'WEIGHTED_DOWNTIME'
ORDER BY pct_improvement
LIMIT 10;











-- 21. Paired view: how often integrated beat baseline on the SAME seed
SELECT metric_name,
       COUNT(*) AS paired_runs,
       SUM(CASE WHEN integrated_value <  baseline_value THEN 1 ELSE 0 END) AS integrated_better,
       SUM(CASE WHEN integrated_value =  baseline_value THEN 1 ELSE 0 END) AS tie,
       SUM(CASE WHEN integrated_value >  baseline_value THEN 1 ELSE 0 END) AS baseline_better
FROM v_paired_metrics
GROUP BY metric_name
ORDER BY metric_name;

--F. Integrity and transactions (live proof)
-- 25. CHECK rule. Run all three lines together; ROLLBACK makes it safe
BEGIN;
UPDATE asset SET criticality = 9 WHERE name = 'Crane 1';
ROLLBACK;

--If it errors, that is your CHECK constraint working. If it succeeds, there is no CHECK, and the ROLLBACK undoes it. In that case say “constraint planned”. 


