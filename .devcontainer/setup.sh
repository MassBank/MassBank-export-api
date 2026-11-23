#!/usr/bin/env bash
set -euo pipefail

cd /workspaces/MassBank-export-api

# Compile project and generate OpenAPI interfaces/models for IDE support
mvn -B compile
