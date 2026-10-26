#!/usr/bin/env bash
# Starts a throwaway local Vault (dev mode, in memory) and a Vault Proxy for the integration tests, in Docker or
# as local processes (see lib.sh). Values are random and never printed. Generated state goes to .work/
# (git-ignored). Tear down with teardown.sh.
set -euo pipefail
source "$(dirname "$0")/lib.sh"

if [ -e "$WORK/mode" ]; then
  echo "A local Vault is already set up; run teardown.sh first." >&2
  exit 1
fi
MODE="$(it_mode)"
if [ "$MODE" = native ] && ! command -v "$(vault_bin)" >/dev/null 2>&1; then
  echo "Native mode needs a vault binary: put vault on PATH or set VAULT_BIN (or start Docker)." >&2
  exit 1
fi
if [ "$MODE" = native ]; then
  FOUND="$("$(vault_bin)" version | sed -n 's/^Vault v\([^ ]*\).*/\1/p')"
  if [ "$FOUND" != "$VAULT_VERSION" ]; then
    echo "vault is $FOUND but the tests are pinned to $VAULT_VERSION." >&2
    echo "Install it (winget install Hashicorp.Vault --version $VAULT_VERSION --force) or set IT_VAULT_VERSION." >&2
    exit 1
  fi
fi

mkdir -p "$WORK/proxy"
printf '%s' "$MODE" > "$WORK/mode"
ROOT="$(head -c 24 /dev/urandom | base64 | tr -d '/+=')"
printf '%s' "$ROOT" > "$WORK/root-token"

if [ "$MODE" = docker ]; then
  docker network create "$NET" >/dev/null
  MSYS_NO_PATHCONV=1 docker run -d --name "$SERVER" --network "$NET" --cap-add=IPC_LOCK -p 127.0.0.1:8200:8200 \
    -e VAULT_DEV_ROOT_TOKEN_ID="$ROOT" -e VAULT_DEV_LISTEN_ADDRESS=0.0.0.0:8200 "$IMAGE" >/dev/null
else
  start_bg server "$(vault_bin)" server -dev -dev-root-token-id="$ROOT" -dev-listen-address=127.0.0.1:8200
fi

for i in $(seq 1 30); do v status >/dev/null 2>&1 && break; sleep 1; done
v status >/dev/null

# KV v2 is mounted at secret/ in dev mode. These paths match PocCheck's defaults.
v kv put secret/poc/snowflake user=it-user account=it-account \
  password="$(head -c 18 /dev/urandom | base64)" >/dev/null
v kv put secret/poc/keys aes256="$(head -c 32 /dev/urandom | base64)" >/dev/null

v secrets enable transit >/dev/null
v write -f transit/keys/orders >/dev/null

v policy write it-read - >/dev/null <<'EOF'
path "secret/data/poc/*" { capabilities = ["read"] }
path "transit/encrypt/orders" { capabilities = ["update"] }
path "transit/decrypt/orders" { capabilities = ["update"] }
path "transit/rewrap/orders"  { capabilities = ["update"] }
path "transit/keys/orders"    { capabilities = ["read"] }
# Local only: lets the rotation integration test rotate the key.
path "transit/keys/orders/rotate" { capabilities = ["update"] }
EOF

v auth enable approle >/dev/null
# App role: single-use secret_id, like production delivery.
v write auth/approle/role/it token_policies=it-read token_ttl=10m token_max_ttl=30m secret_id_num_uses=1 >/dev/null
# Proxy role: reusable secret_id so the proxy can re-authenticate.
v write auth/approle/role/it-proxy token_policies=it-read token_ttl=10m token_max_ttl=30m >/dev/null

v read -field=role_id auth/approle/role/it/role-id > "$WORK/role-id"
v read -field=role_id auth/approle/role/it-proxy/role-id > "$WORK/proxy/role-id"
v write -field=secret_id -f auth/approle/role/it-proxy/secret-id > "$WORK/proxy/secret-id"

# The proxy sees its files at /proxy inside the container, or at their real path when run locally.
if [ "$MODE" = docker ]; then
  PROXY_DIR=/proxy VAULT_URL="http://$SERVER:8200" LISTEN=0.0.0.0:8100
else
  PROXY_DIR="$(hostpath "$WORK/proxy")" VAULT_URL=http://127.0.0.1:8200 LISTEN=127.0.0.1:8100
fi
cat > "$WORK/proxy/proxy.hcl" <<EOF
vault { address = "$VAULT_URL" }
auto_auth {
  method "approle" {
    config = {
      role_id_file_path                   = "$PROXY_DIR/role-id"
      secret_id_file_path                 = "$PROXY_DIR/secret-id"
      remove_secret_id_file_after_reading = false
    }
  }
}
api_proxy { use_auto_auth_token = "force" }
listener "tcp" {
  address     = "$LISTEN"
  tls_disable = true
}
EOF

if [ "$MODE" = docker ]; then
  MSYS_NO_PATHCONV=1 docker run -d --name "$PROXY" --network "$NET" -p 127.0.0.1:8100:8100 \
    -v "$(hostpath "$WORK/proxy")":/proxy "$IMAGE" proxy -config=/proxy/proxy.hcl >/dev/null
else
  start_bg proxy "$(vault_bin)" proxy -config="$(hostpath "$WORK/proxy/proxy.hcl")"
fi

for i in $(seq 1 30); do
  curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8100/v1/auth/token/lookup-self | grep -q 200 && break
  sleep 1
done
echo "mode: $MODE server: $(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8200/v1/sys/health) proxy lookup-self: $(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8100/v1/auth/token/lookup-self)"
