#!/usr/bin/env bash
set -euo pipefail

postgres_version=$(cat /usr/local/share/massbank-postgresql/version)
postgres_bin="/usr/lib/postgresql/$postgres_version/bin"
runtime_dir="$HOME/.cache/massbank-postgres"
data_dir="$runtime_dir/data"

if [[ ${1:-} != --locked ]]; then
    install -d -m 0700 "$runtime_dir"
    exec flock --close "$runtime_dir/init.lock" "$0" --locked
fi

# Reuse a running cluster when the hook runs again in the same container session.
if ! "$postgres_bin/pg_isready" --quiet --host="$runtime_dir"; then
    rm -rf -- "$data_dir" "$runtime_dir/ready"
    "$postgres_bin/initdb" -D "$data_dir" --username="$DB_USER" \
        --auth-local=trust --auth-host=scram-sha-256 --pwfile=<(printf '%s\n' "$DB_PASSWORD")
    "$postgres_bin/pg_ctl" -D "$data_dir" -l "$runtime_dir/postgresql.log" \
        -o "-h $DB_HOST -k $runtime_dir" -w start
fi

if [[ ! -f "$runtime_dir/ready" ]]; then
    "$postgres_bin/createdb" --host="$runtime_dir" --username="$DB_USER" -- "$DB_NAME"
    touch "$runtime_dir/ready"
fi
