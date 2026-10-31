CREATE SCHEMA IF NOT EXISTS massbank_api;

-- These are live views, not a second copy of the imported records.
CREATE OR REPLACE FUNCTION massbank_api.atom_count(formula text) RETURNS integer
LANGUAGE sql IMMUTABLE STRICT AS $$
    SELECT coalesce(sum(coalesce(nullif(part[1], ''), '1')::integer), 0)::integer
    FROM regexp_matches(formula, '[A-Z][a-z]?([0-9]*)', 'g') AS part
$$;

CREATE OR REPLACE VIEW massbank_api.massbank AS
SELECT accession AS id, accession FROM public."massbank-records";

CREATE OR REPLACE VIEW massbank_api.compound AS
SELECT DISTINCT ch_iupac AS inchi FROM public."massbank-records";

CREATE OR REPLACE VIEW massbank_api.contributor AS
SELECT DISTINCT split_part(accession, '-', 2) AS name FROM public."massbank-records";

CREATE OR REPLACE VIEW massbank_api.acquisition_instrument AS
SELECT DISTINCT ac_instrument_type AS instrument_type FROM public."massbank-records";

CREATE OR REPLACE VIEW massbank_api.acquisition_mass_spectrometry AS
SELECT 'MS_TYPE'::text AS subtag, ac_mass_spectrometry_ms_type AS value FROM public."massbank-records"
UNION
SELECT 'ION_MODE', ac_mass_spectrometry_ion_mode FROM public."massbank-records";

CREATE OR REPLACE VIEW massbank_api.compound_name AS
SELECT accession AS massbank_id, jsonb_array_elements_text(ch_name) AS name
FROM public."massbank-records";

CREATE OR REPLACE VIEW massbank_api.compound_class AS
SELECT accession AS massbank_id, jsonb_array_elements_text(ch_compound_class) AS class
FROM public."massbank-records";

CREATE OR REPLACE VIEW massbank_api.compound_link AS
SELECT "Record_accession" AS massbank_id, ch_link->>'key' AS database,
       ch_link->>'value' AS identifier FROM public."Record_chLink";

CREATE OR REPLACE VIEW massbank_api.spectrum AS
SELECT accession AS id, accession AS massbank_id, pk_splash AS splash
FROM public."massbank-records";

CREATE OR REPLACE VIEW massbank_api.peak AS
SELECT id, record_id AS spectrum_id, mz, intensity, rel_intensity AS relative_intensity
FROM public."massbank-peaks" WHERE mz > 0 AND intensity > 0;

CREATE OR REPLACE VIEW massbank_api.browse_options AS
SELECT r.accession AS massbank_id, r.accession,
       split_part(r.accession, '-', 2) AS contributor,
       r.ac_instrument_type AS instrument_type,
       r.ac_mass_spectrometry_ms_type AS ms_type,
       r.ac_mass_spectrometry_ion_mode AS ion_mode,
       (SELECT string_agg(t, '; ' ORDER BY n)
        FROM jsonb_array_elements_text(r.record_title) WITH ORDINALITY AS titles(t,n)) AS title,
       r.ch_smiles AS smiles, r.ch_iupac AS inchi,
       coalesce((SELECT ch_link->>'value' FROM public."Record_chLink" l
                 WHERE l."Record_accession" = r.accession AND ch_link->>'key' = 'INCHIKEY'
                 ORDER BY ch_link_order LIMIT 1), '') AS inchikey,
       r.pk_splash AS splash, r.ch_formula AS formula, r.ch_exact_mass AS mass,
       massbank_api.atom_count(r.ch_formula) AS atomcount
FROM public."massbank-records" r;

CREATE OR REPLACE VIEW massbank_api.peak_differences AS
WITH focused AS (
    SELECT "Record_accession" AS accession,
           CASE WHEN ms_focused_ion->>'value' ~ '^[+-]?([0-9]+([.][0-9]*)?|[.][0-9]+)([eE][+-]?[0-9]+)?$'
                THEN (ms_focused_ion->>'value')::double precision END AS precursor_mass
    FROM public."Record_msFocusedIon" WHERE ms_focused_ion->>'key' = 'PRECURSOR_M/Z'
)
SELECT p.record_id AS spectrum_id, p.id AS peak_id,
       f.precursor_mass,
       abs(p.mz - f.precursor_mass) AS difference,
       p.rel_intensity AS rel_intensity
FROM public."massbank-peaks" p
JOIN public."massbank-records" r ON r.accession = p.record_id
JOIN focused f ON f.accession = r.accession
WHERE r.ac_mass_spectrometry_ms_type IN ('MS2', 'MS3', 'MS4')
  AND f.precursor_mass IS NOT NULL
  AND p.mz > 0 AND p.intensity > 0
  AND (SELECT count(*) FROM public."massbank-peaks" q WHERE q.record_id = r.accession) > 1;

CREATE OR REPLACE VIEW massbank_api.molecules AS
SELECT accession, ch_smiles AS molecule, massbank_api.atom_count(ch_formula) AS atomcount
FROM public."massbank-records" WHERE ch_smiles NOT IN ('', 'N/A');

CREATE OR REPLACE VIEW massbank_api.metadata AS
SELECT git_commit AS commit, imported_at AS timestamp, data_version AS version
FROM public.shared_import_metadata WHERE id = 1 AND ready;

CREATE OR REPLACE VIEW massbank_api.status AS
SELECT 'database_update'::text AS name,
       CASE WHEN ready THEN 'done' ELSE 'importing' END AS value
FROM public.shared_import_metadata WHERE id = 1;

CREATE OR REPLACE VIEW massbank_api.record_data AS
SELECT r.accession, to_jsonb(r) || jsonb_build_object(
    'ch_link', coalesce((SELECT jsonb_agg(ch_link ORDER BY ch_link_order)
        FROM public."Record_chLink" WHERE "Record_accession" = r.accession), '[]'::jsonb),
    'sp_link', coalesce((SELECT jsonb_agg(sp_link ORDER BY sp_link_order)
        FROM public."Record_spLink" WHERE "Record_accession" = r.accession), '[]'::jsonb),
    'ac_mass_spectrometry', coalesce((SELECT jsonb_agg(ac_mass_spectrometry ORDER BY ac_mass_spectrometry_order)
        FROM public."Record_acMassSpectrometry" WHERE "Record_accession" = r.accession), '[]'::jsonb),
    'ac_chromatography', coalesce((SELECT jsonb_agg(ac_chromatography ORDER BY ac_chromatography_order)
        FROM public."Record_acChromatography" WHERE "Record_accession" = r.accession), '[]'::jsonb),
    'ms_focused_ion', coalesce((SELECT jsonb_agg(ms_focused_ion ORDER BY ms_focused_ion_order)
        FROM public."Record_msFocusedIon" WHERE "Record_accession" = r.accession), '[]'::jsonb),
    'ms_data_processing', coalesce((SELECT jsonb_agg(ms_data_processing ORDER BY ms_data_processing_order)
        FROM public."Record_msDataProcessing" WHERE "Record_accession" = r.accession), '[]'::jsonb),
    'peaks', coalesce((SELECT jsonb_agg(jsonb_build_object(
        'id', id, 'mz', mz, 'intensity', intensity, 'rel', rel_intensity) ORDER BY mz, id)
        FROM public."massbank-peaks" WHERE record_id = r.accession AND mz > 0 AND intensity > 0), '[]'::jsonb)
) AS document FROM public."massbank-records" r;

CREATE INDEX IF NOT EXISTS shared_peaks_record_idx ON public."massbank-peaks" (record_id);
CREATE INDEX IF NOT EXISTS shared_peaks_mz_idx ON public."massbank-peaks" (mz);
CREATE INDEX IF NOT EXISTS shared_records_mass_idx ON public."massbank-records" (ch_exact_mass);
CREATE INDEX IF NOT EXISTS shared_records_splash_idx ON public."massbank-records" (pk_splash);
