#!/usr/bin/env bash
set -euo pipefail

# env
export GOOGLE_APPLICATION_CREDENTIALS=resources/secret.json

# usage
function usage {
    cat <<EOM
Usage: $(basename "$0") [OPTION]...
  -h    display help
  -g    generate the gmail's credential file
  -c    run for cron job
EOM

    exit 2
}

# parse args
while getopts "cgh" optKey; do
    case "$optKey" in
        c)
            lein exec -p src/portcard_api/cmd/gmail_credential/core.clj -c
            ;;
        g)
            echo "step 1) remove previous key"
            rm -f resources/tokens/StoredCredential
            echo "step 2) generate key"
            lein exec -p src/portcard_api/cmd/gmail_credential/core.clj
            ;;

        '-h'|'--help'|*)
            usage
            ;;
    esac
done


