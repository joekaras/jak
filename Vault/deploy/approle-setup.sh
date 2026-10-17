#!/usr/bin/env bash
# One-time AppRole setup for an application, run by a Vault admin. Placeholders only; nothing secret here.
#
#   VAULT_ADDR=https://REPLACE-vault-host:8200 VAULT_NAMESPACE=REPLACE-namespace \
#   APP=REPLACE-app ROLE=REPLACE-role SERVER_CIDRS=10.0.0.0/24 [AGENT_ROLE=REPLACE-agent-role] ./approle-setup.sh
set -euo pipefail
: "${VAULT_ADDR:?}" "${VAULT_NAMESPACE:?}" "${APP:?}" "${ROLE:?}" "${SERVER_CIDRS:?}"

vault policy write "${APP}-read" policy-app-read.hcl

# secret_id: single use, short life, only usable from the app servers.
# token: periodic (token_period), so a long-running app can keep renewing it without a second login,
#        which matters because the wrapped secret_id is spent after the first login.
#        It is also bound to the app servers, so a stolen token is useless elsewhere.
vault write "auth/approle/role/${ROLE}" \
  token_policies="${APP}-read" \
  token_period=24h \
  token_bound_cidrs="${SERVER_CIDRS}" \
  secret_id_num_uses=1 \
  secret_id_ttl=15m \
  secret_id_bound_cidrs="${SERVER_CIDRS}"

vault policy write "${APP}-deliverer" policy-deliverer.hcl

# Optional: role for a Vault Proxy that keeps a persistent secret_id (agent-windows.hcl). Set AGENT_ROLE to use it.
# secret_id: reusable so the proxy survives restarts, still bound to the servers, and expires so it gets rotated.
# The custodian mints it with `vault write -f auth/approle/role/${AGENT_ROLE}/secret-id` and places the file by hand.
if [ -n "${AGENT_ROLE:-}" ]; then
  vault write "auth/approle/role/${AGENT_ROLE}" \
    token_policies="${APP}-read" \
    token_period=24h \
    token_bound_cidrs="${SERVER_CIDRS}" \
    secret_id_num_uses=0 \
    secret_id_ttl=2160h \
    secret_id_bound_cidrs="${SERVER_CIDRS}"
  echo "role_id for ${AGENT_ROLE}:"
  vault read -field=role_id "auth/approle/role/${AGENT_ROLE}/role-id"
fi

# role_id is not secret on its own; put it in the app's config or service definition.
echo "role_id for ${ROLE}:"
vault read -field=role_id "auth/approle/role/${ROLE}/role-id"
