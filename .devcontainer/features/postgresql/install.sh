#!/usr/bin/env bash
# Adapted from https://github.com/itsmechlark/features/tree/main/src/postgresql
set -euo pipefail

export DEBIAN_FRONTEND=noninteractive
postgres_version=${VERSION:-17}
. /etc/os-release

apt-get update
apt-get install -y --no-install-recommends ca-certificates curl
install -d /usr/share/postgresql-common/pgdg
curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc \
    -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc
printf 'deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt %s-pgdg main\n' \
    "$VERSION_CODENAME" > /etc/apt/sources.list.d/pgdg.list

apt-get update
apt-get install -y --no-install-recommends "postgresql-$postgres_version"
rm -rf /var/lib/apt/lists/*

install -d /usr/local/share/massbank-postgresql
printf '%s\n' "$postgres_version" > /usr/local/share/massbank-postgresql/version
install -m 0755 start.sh /usr/local/share/massbank-postgresql/start.sh
