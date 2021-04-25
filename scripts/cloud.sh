#!/usr/bin/env bash
set -euo pipefail

export GOOGLE_APPLICATION_CREDENTIALS=resources/secret.json
lein with-profile cloud run -m portcard-api.cmd.cloud.core
