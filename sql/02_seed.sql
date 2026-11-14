-- =====================================================================
-- 02_seed.sql  |  TRIDENT harbour_db (PostgreSQL, schema: harbour)
-- Seeds lookup tables, the Appendix A harbour (15 assets), its
-- dependencies (18 rows) and source reliabilities.
--
-- Values match the Java code:
--   asset types      -> AssetType enum names
--   repair_hours     -> AppendixAHarbour.DEFAULT_REPAIR_HOURS
--   source_reliability -> SourceType.java
--
-- Run ONLY on empty tables. One transaction: any error rolls back
-- everything (run ROLLBACK; in pgAdmin to clear an aborted state).
-- =====================================================================

BEGIN;

INSERT INTO harbour.asset_type (type_code, description) VALUES
 ('POWER_SOURCE',   'Power input and backup generation'),
 ('SUBSTATION',     'Power distribution'),
 ('PUMP',           'Drainage and dewatering pumps'),
 ('DRYDOCK',        'Ship maintenance'),
 ('COMMS_TOWER',    'Communications'),
 ('CONTROL_CENTRE', 'Harbour command'),
 ('CRANE',          'Loading and unloading'),
 ('BERTH',          'Vessel berthing'),
 ('FUEL_JETTY',     'Refuelling'),
 ('ACCESS_GATE',    'Personnel and vehicle access');

INSERT INTO harbour.hazard_type (hazard_code, description) VALUES
 ('FLOOD',             'Flooding'),
 ('POWER_OUTAGE',      'Power outage'),
 ('EQUIPMENT_FAILURE', 'Equipment failure');

INSERT INTO harbour.source_reliability (source_type, reliability) VALUES
 ('SENSOR',       0.9),
 ('CREW',         0.7),
 ('WEATHER_FEED', 0.5);

-- crew_id and asset_id are GENERATED ALWAYS identity columns
INSERT INTO harbour.crew (crew_id, name, available_from)
OVERRIDING SYSTEM VALUE VALUES
 (1, 'Crew 1', 0),
 (2, 'Crew 2', 0),
 (3, 'Crew 3', 0);

INSERT INTO harbour.asset
 (asset_id, name, type_code, criticality, repair_hours, health_score, status)
OVERRIDING SYSTEM VALUE VALUES
 (1 , 'Main Grid Feed',          'POWER_SOURCE',   5, 8.0, 100, 'OPERATIONAL'),
 (2 , 'Backup Generator',        'POWER_SOURCE',   3, 3.0, 100, 'OPERATIONAL'),
 (3 , 'Substation A',            'SUBSTATION',     5, 4.0, 100, 'OPERATIONAL'),
 (4 , 'Drainage Pump Station 1', 'PUMP',           4, 4.0, 100, 'OPERATIONAL'),
 (5 , 'Drainage Pump Station 2', 'PUMP',           3, 3.0, 100, 'OPERATIONAL'),
 (6 , 'Drydock Dewatering Pump', 'PUMP',           4, 5.0, 100, 'OPERATIONAL'),
 (7 , 'Drydock',                 'DRYDOCK',        4, 6.0, 100, 'OPERATIONAL'),
 (8 , 'Comms Tower',             'COMMS_TOWER',    5, 5.0, 100, 'OPERATIONAL'),
 (9 , 'Control Centre',          'CONTROL_CENTRE', 5, 6.0, 100, 'OPERATIONAL'),
 (10, 'Crane 1',                 'CRANE',          3, 6.0, 100, 'OPERATIONAL'),
 (11, 'Crane 2',                 'CRANE',          3, 6.0, 100, 'OPERATIONAL'),
 (12, 'Berth 1',                 'BERTH',          4, 3.0, 100, 'OPERATIONAL'),
 (13, 'Berth 2',                 'BERTH',          4, 3.0, 100, 'OPERATIONAL'),
 (14, 'Fuel Jetty',              'FUEL_JETTY',     4, 5.0, 100, 'OPERATIONAL'),
 (15, 'Access Road and Gate',    'ACCESS_GATE',    3, 2.0, 100, 'OPERATIONAL');

-- asset_id DEPENDS ON depends_on_id
INSERT INTO harbour.dependency (asset_id, depends_on_id, dependency_type) VALUES
 (3 , 1 , 'REQUIRED'), (3 , 2 , 'BACKUP'),
 (4 , 3 , 'REQUIRED'), (5 , 3 , 'REQUIRED'), (6 , 3 , 'REQUIRED'),
 (7 , 6 , 'REQUIRED'), (8 , 3 , 'REQUIRED'),
 (9 , 3 , 'REQUIRED'), (9 , 8 , 'REQUIRED'),
 (10, 3 , 'REQUIRED'), (11, 3 , 'REQUIRED'),
 (12, 10, 'REQUIRED'), (12, 4 , 'REQUIRED'),
 (13, 11, 'REQUIRED'), (13, 5 , 'REQUIRED'),
 (14, 3 , 'REQUIRED'), (14, 13, 'REQUIRED'),
 (15, 4 , 'REQUIRED');

-- Re-sync identity sequences (no-op if a column is not an identity)
SELECT setval(pg_get_serial_sequence('harbour.crew','crew_id'),
              (SELECT MAX(crew_id) FROM harbour.crew))
WHERE pg_get_serial_sequence('harbour.crew','crew_id') IS NOT NULL;
SELECT setval(pg_get_serial_sequence('harbour.asset','asset_id'),
              (SELECT MAX(asset_id) FROM harbour.asset))
WHERE pg_get_serial_sequence('harbour.asset','asset_id') IS NOT NULL;

COMMIT;

-- Verify (expect 15 | 18 | 46 | 69.0 | 3 | 10)
SELECT (SELECT COUNT(*) FROM harbour.asset)      AS assets,
       (SELECT COUNT(*) FROM harbour.dependency) AS deps,
       (SELECT SUM(criticality) FROM harbour.asset
         WHERE asset_id IN (4,5,6,7,8,9,10,11,12,13,14,15)) AS sub_a_cascade,
       (SELECT SUM(repair_hours) FROM harbour.asset) AS total_repair_hours,
       (SELECT COUNT(*) FROM harbour.source_reliability) AS sources,
       (SELECT COUNT(*) FROM harbour.asset_type) AS asset_types;
