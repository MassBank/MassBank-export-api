#!/usr/bin/env python3
"""Check both deployed APIs against the same imported record set."""

import argparse
import json
import math
import urllib.parse
import urllib.request


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--api", default="http://127.0.0.1:18081/MassBank-api")
    parser.add_argument("--export", default="http://127.0.0.1:18083")
    parser.add_argument("--expected-records", type=int, default=677)
    parser.add_argument("--accession", default="MSBNK-IPB_Halle-PB011601")
    args = parser.parse_args()

    def get(url):
        with urllib.request.urlopen(url, timeout=60) as response:
            return response.read().decode()

    def api(path):
        return json.loads(get(args.api + path))

    count = api("/records/count")
    status = json.loads(get(args.export + "/database/status"))
    assert status["ready"], status
    assert count == status["active_record_count"] == args.expected_records, (count, status)
    get(args.export + "/database/ready")

    accession = args.accession
    record = api("/records/" + accession)
    simple = api("/records/" + accession + "/simple")
    raw = get(args.export + "/rawtext/" + accession)
    assert "ACCESSION: " + accession in raw
    assert "CH$FORMULA: " + record["compound"]["formula"] in raw
    assert "CH$SMILES: " + record["compound"]["smiles"] in raw
    exact_mass = next(line.split(": ", 1)[1] for line in raw.splitlines() if line.startswith("CH$EXACT_MASS:"))
    assert math.isclose(float(exact_mass), record["compound"]["mass"], rel_tol=1e-12)
    assert record["peak"]["numPeak"] == simple["peak"]["numPeak"]
    assert record["peak"]["peak"]["values"] == simple["peak"]["peak"]["values"]

    assert api("/filter/browse")
    assert api("/metadata")
    filters = [
        {"formula": record["compound"]["formula"]},
        {"substructure": record["compound"]["smiles"]},
        {"compound_name": record["compound"]["names"][0]},
        {"peaks": record["peak"]["peak"]["values"][0]["mz"], "intensity": 1},
    ]
    precursor = next(float(item["value"]) for item in record["mass_spectrometry"]["focused_ion"]
                     if item["subtag"] == "PRECURSOR_M/Z")
    filters.append({"neutral_loss": abs(precursor - record["peak"]["peak"]["values"][0]["mz"]), "intensity": 1})
    for query in filters:
        results = api("/records/search?" + urllib.parse.urlencode(query))
        assert any(hit["accession"] == accession for hit in results["data"]), query
        print(f"{next(iter(query))}: {len(results['data'])} matches")
    records = api("/records?" + urllib.parse.urlencode({"formula": record["compound"]["formula"]}))
    assert next(item for item in records if item["accession"] == accession) == record
    print(f"Both APIs: {count} active records; record, export, browse and search checks passed.")


if __name__ == "__main__":
    main()
