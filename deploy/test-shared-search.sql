-- Run against an imported TEST database with psql -v ON_ERROR_STOP=1.
-- All fixtures, schema changes and refreshes are rolled back.
BEGIN;
SET LOCAL statement_timeout = '180s';
DO $$
BEGIN
    IF current_database() NOT LIKE '%test%' THEN
        RAISE EXCEPTION 'This regression test requires a test database';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public."massbank-records") THEN
        RAISE EXCEPTION 'Import the integration fixtures first';
    END IF;
END $$;

INSERT INTO public."massbank-accession-registry" (accession)
SELECT 'MSBNK-PerfTest-' || kind FROM (VALUES ('A'),('B'),('C'),('D'),('E'),('F')) cases(kind);

INSERT INTO public."massbank-records"
SELECT (jsonb_populate_record(NULL::public."massbank-records",
    to_jsonb(seed) || jsonb_build_object('accession', 'MSBNK-PerfTest-' || kind,
        'ac_mass_spectrometry_ms_type', ms_type))).*
FROM (SELECT * FROM public."massbank-records" LIMIT 1) seed
CROSS JOIN (VALUES ('A','MS2'),('B','MS2'),('C','MS'),('D','MS2'),('E','MS3'),('F','MS4')) cases(kind,ms_type);

UPDATE public."massbank-records"
SET ch_name=jsonb_build_array('A_%', 'quoted "name"', E'back\\slash', E'line\nbreak', 'duplicate', 'duplicate')
WHERE accession='MSBNK-PerfTest-A';
CREATE TEMP TABLE expected_names AS
SELECT accession AS massbank_id,jsonb_array_elements_text(ch_name) AS name FROM public."massbank-records";

INSERT INTO public."Record_msFocusedIon" ("Record_accession", ms_focused_ion_order, ms_focused_ion)
SELECT 'MSBNK-PerfTest-' || kind, ordinal,
       jsonb_build_object('key','PRECURSOR_M/Z','value',value)
FROM (VALUES ('A',0,'100'),('B',0,'100'),('C',0,'100'),('D',0,'N/A'),
             ('E',0,'100'),('F',0,'100'),('F',1,'50')) precursors(kind,ordinal,value);

INSERT INTO public."massbank-peaks" (id,record_id,mz,intensity,rel_intensity)
SELECT -90000000-row_number() OVER (), 'MSBNK-PerfTest-' || kind, mz, intensity, 999
FROM (VALUES ('A',10,100),('A',20,100),('A',0,100),('B',10,100),
             ('C',10,100),('C',20,100),('D',10,100),('D',20,100),
             ('E',10,0),('E',20,100),('F',10,100),('F',20,100)) peaks(kind,mz,intensity);

CREATE TEMP TABLE expected_record_peaks AS
SELECT record_id, jsonb_agg(jsonb_build_object(
    'id', id, 'mz', mz, 'intensity', intensity, 'rel', rel_intensity) ORDER BY mz, id) AS peaks
FROM public."massbank-peaks" WHERE record_id LIKE 'MSBNK-PerfTest-%'
GROUP BY record_id;

CREATE TEMP TABLE expected_losses AS
WITH focused AS (
    SELECT "Record_accession" AS accession,
           CASE WHEN ms_focused_ion->>'value' ~ '^[+-]?([0-9]+([.][0-9]*)?|[.][0-9]+)([eE][+-]?[0-9]+)?$'
                THEN (ms_focused_ion->>'value')::double precision END AS precursor_mass
    FROM public."Record_msFocusedIon" WHERE ms_focused_ion->>'key' = 'PRECURSOR_M/Z'
)
SELECT p.record_id AS spectrum_id,p.id AS peak_id,f.precursor_mass,
       abs(p.mz-f.precursor_mass) AS difference,p.rel_intensity
FROM public."massbank-peaks" p
JOIN public."massbank-records" r ON r.accession=p.record_id
JOIN focused f ON f.accession=r.accession
WHERE r.ac_mass_spectrometry_ms_type IN ('MS2','MS3','MS4')
  AND f.precursor_mass IS NOT NULL AND p.mz>0 AND p.intensity>0
  AND (SELECT count(*) FROM public."massbank-peaks" q WHERE q.record_id=r.accession)>1;

CREATE TEMP TABLE expected_statistics AS
SELECT 'spectra'::text AS kind,''::text AS value,count(DISTINCT splash) AS count FROM massbank_api.spectrum
UNION ALL SELECT 'compound','',count(DISTINCT inchi) FROM massbank_api.compound WHERE inchi<>'N/A'
UNION ALL SELECT 'class',class,count(class) FROM massbank_api.compound_class GROUP BY class
UNION ALL SELECT 'chemont',identifier,count(identifier) FROM massbank_api.compound_link
WHERE database='ChemOnt' GROUP BY identifier;

\ir ../src/main/resources/shared-api.sql
REFRESH MATERIALIZED VIEW massbank_api.neutral_loss_index;
REFRESH MATERIALIZED VIEW massbank_api.name_search;
REFRESH MATERIALIZED VIEW massbank_api.metadata_statistics;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM expected_record_peaks expected
        LEFT JOIN massbank_api.record_data actual ON actual.accession=expected.record_id
        WHERE actual.document->'peaks' IS DISTINCT FROM expected.peaks
    ) THEN
        RAISE EXCEPTION 'Full records must preserve all stored peak rows, including zero intensity and zero m/z';
    END IF;
    IF EXISTS (
        (SELECT * FROM expected_names EXCEPT ALL SELECT * FROM massbank_api.compound_name)
        UNION ALL
        (SELECT * FROM massbank_api.compound_name EXCEPT ALL SELECT * FROM expected_names)
    ) THEN
        RAISE EXCEPTION 'Name values or multiplicity changed';
    END IF;
    IF EXISTS (
        (SELECT * FROM expected_losses EXCEPT ALL SELECT * FROM massbank_api.peak_differences)
        UNION ALL
        (SELECT * FROM massbank_api.peak_differences EXCEPT ALL SELECT * FROM expected_losses)
    ) THEN
        RAISE EXCEPTION 'Neutral-loss results changed';
    END IF;
    IF (SELECT count(*) FROM massbank_api.peak_differences WHERE spectrum_id LIKE 'MSBNK-PerfTest-%') <> 7 THEN
        RAISE EXCEPTION 'MS type, multiple precursors, single/invalid peak or invalid precursor handling changed';
    END IF;
    IF EXISTS (
        (SELECT * FROM expected_statistics EXCEPT ALL SELECT * FROM massbank_api.metadata_statistics)
        UNION ALL
        (SELECT * FROM massbank_api.metadata_statistics EXCEPT ALL SELECT * FROM expected_statistics)
    ) THEN
        RAISE EXCEPTION 'Metadata aggregates changed';
    END IF;
END $$;

UPDATE public."Record_msFocusedIon"
SET ms_focused_ion=jsonb_set(ms_focused_ion,'{value}','"55"')
WHERE "Record_accession"='MSBNK-PerfTest-F' AND ms_focused_ion_order=1;
REFRESH MATERIALIZED VIEW massbank_api.neutral_loss_index;
DO $$
BEGIN
    IF (SELECT count(*) FROM massbank_api.peak_differences
        WHERE spectrum_id='MSBNK-PerfTest-F' AND precursor_mass=55 AND difference IN (35,45)) <> 2 THEN
        RAISE EXCEPTION 'Import-completion refresh failed to update the search projection';
    END IF;
END $$;
ROLLBACK;
