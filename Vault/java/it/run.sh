#!/usr/bin/env bash
# Runs all IntegrationTest cases against a throwaway local Vault and Vault Agent, then tears them down.
# Needs curl, Maven, and either Docker or a vault binary (see lib.sh). Extra arguments go to mvn.
set -euo pipefail
source "$(dirname "$0")/lib.sh"

trap 'bash "$IT_DIR/teardown.sh"' EXIT
bash "$IT_DIR/setup.sh"
bash "$IT_DIR/wrap.sh"

cd "$IT_DIR/.."
VAULT_ADDR=http://127.0.0.1:8200 \
VAULT_KV_MOUNT=secret \
VAULT_ROLE_ID="$(cat "$WORK/role-id")" \
VAULT_WRAPPED_SECRET_ID_FILE="$(hostpath "$WORK/wrapped.json")" \
VAULT_TEST_PATH=poc/snowflake \
VAULT_AGENT_ADDR=http://127.0.0.1:8100 \
VAULT_TRANSIT_TEST_KEY=orders \
VAULT_TRANSIT_ROTATE=true \
  mvn -pl ibsmas-vault test -Dtest=IntegrationTest "$@"
