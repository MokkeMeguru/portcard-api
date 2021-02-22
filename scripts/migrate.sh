#!/usr/bin/env bash
set -euo pipefail

export GOOGLE_APPLICATION_CREDENTIALS=resources/secret.json
lein exec -p src/portcard_api/cmd/migrate/core.clj
