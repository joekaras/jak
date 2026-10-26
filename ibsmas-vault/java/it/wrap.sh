#!/usr/bin/env bash
# Writes a fresh response-wrapped secret_id for role "it" to .work/wrapped.json. The AppRole test consumes it.
set -euo pipefail
source "$(dirname "$0")/lib.sh"
v write -wrap-ttl=5m -format=json -f auth/approle/role/it/secret-id > "$WORK/wrapped.json"
