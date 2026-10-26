# On-prem Docker Vault

How to run a **persistent, TLS-only** Vault on an on-prem Docker host and configure an IBSMAS tier on it. This suits a
shared development or test Vault (UNIT, OPAC). For OPIN and OPER, read
[Environments and production](05-environments-and-production.md) first: a single container isn't a production
deployment.

*Tested:* the compose file, server config, init and unseal, tier setup, deployer policy, and the client (AppRole over
TLS, and through Vault Proxy with a TLS upstream) all ran end to end against Vault 1.18.3 on Docker Desktop. Each step
below that was run is marked ✔.

## 1. Files on the Docker host

```
/opt/vault/
  docker-compose.yml
  config/vault.hcl
  tls/ca.pem  tls/vault.crt  tls/vault.key
```

**TLS:**
- **Issuer:** have your internal CA issue `vault.crt` with a SAN for every name clients use (for example
  `vault-nonprod.example.internal`). `ca.pem` is the CA chain clients will trust through `VAULT_CACERT`.
- **The key** must be readable by the container's `vault` user. Use mode `0640` with a group the container can read,
  or `0644` on a host where only admins can log in.

**`config/vault.hcl`** ✔

```hcl
ui            = true
disable_mlock = true   # recommended with integrated (raft) storage

storage "raft" {
  path    = "/vault/file"
  node_id = "vault-1"
}

listener "tcp" {
  address         = "0.0.0.0:8200"
  tls_cert_file   = "/vault/tls/vault.crt"
  tls_key_file    = "/vault/tls/vault.key"
  tls_min_version = "tls12"
}

api_addr     = "https://vault-nonprod.example.internal:8200"
cluster_addr = "https://vault:8201"
```

**`docker-compose.yml`** ✔

```yaml
services:
  vault:
    image: hashicorp/vault:1.18.3        # pin the version; upgrade deliberately
    container_name: vault
    hostname: vault
    command: server
    cap_add: [IPC_LOCK]
    ports:
      - "8200:8200"                       # the test used 127.0.0.1:8443:8200
    environment:
      VAULT_ADDR: https://127.0.0.1:8200
      VAULT_CACERT: /vault/tls/ca.pem
    volumes:
      - ./config:/vault/config:ro
      - ./tls:/vault/tls:ro
      - vault-file:/vault/file            # raft data; the image creates this owned by the vault user
      - vault-logs:/vault/logs            # audit log
    restart: unless-stopped
volumes:
  vault-file:
  vault-logs:
```

**Two gotchas found while testing:**
- **Don't put raft data at `/vault/data`.** The container runs as the `vault` user. A volume mounted at a path the
  image didn't create is owned by root, and Vault fails with `failed to open bolt file: permission denied`. Use the
  image's own `/vault/file` and `/vault/logs`.
- **The `chown` warnings are harmless.** The entrypoint tries to `chown` `/vault/config`, and with a read-only mount it
  prints `Read-only file system`. Ignore those lines.

## 2. Start, initialise, unseal ✔

```sh
docker compose up -d
docker exec vault vault status                 # Initialized: false, Sealed: true
docker exec vault vault operator init -key-shares=5 -key-threshold=3
```

`operator init` prints **5 unseal key shares and the initial root token, once.**
- **Give each share to a different custodian**, never all to one person and never in a ticket or chat.
- **Who holds the shares** (Security, Ops) is an [open question](06-open-questions.md).
- **The test** used `-key-shares=1 -key-threshold=1` only because it was throwaway.

```sh
docker exec -it vault vault operator unseal     # run 3 times, each custodian enters a share
docker exec vault vault status                  # Sealed: false
```

**Every restart of the container seals Vault again**, and it needs 3 shares to unseal. To avoid that, configure
auto-unseal (HSM, cloud KMS, or a Transit-based unseal from another Vault). See
[Environments](05-environments-and-production.md) §4.

## 3. Baseline hardening ✔

```sh
export VAULT_TOKEN=<initial root token>           # in your shell only; never in a file or history
docker exec -e VAULT_TOKEN vault vault audit enable file file_path=/vault/logs/audit.log
```

- **Audit device:** turn it on before anything else. It logs every request with secret values HMAC'd. *Tested:* the
  audit log contained no raw test values.
- **Root token:** create admin access through OIDC, LDAP or userpass with an admin policy, then
  `vault token revoke <root>`. Make a new root token later only with `operator generate-root`, which needs the unseal
  quorum.

## 4. Configure a tier ✔

This uses the **Community-edition layout**: one mount per tier, with tier-independent paths underneath. With Vault
Enterprise, use a namespace per tier instead. Both are explained in
[Environments](05-environments-and-production.md) §2. The example tier is UNIT, and the example app is `poc`.

```sh
T=unit                                                    # unit | opac | opin | oper
docker exec -e VAULT_TOKEN vault vault secrets enable -path=kv-$T kv-v2
docker exec -e VAULT_TOKEN vault vault secrets enable -path=transit-$T transit
docker exec -e VAULT_TOKEN vault vault auth enable approle   # once per Vault

# Secrets (an operator writes these; values come from a secure prompt, not the command line)
docker exec -i -e VAULT_TOKEN vault vault kv put kv-$T/ibsmas/poc/snowflake \
  user=svc_poc account=xy12345 password=-      # type the password, then Ctrl+D
docker exec -e VAULT_TOKEN vault vault write -f transit-$T/keys/ibsmas-poc
```

With `password=-`, Vault reads that one value from stdin, so it stays out of shell history and process lists. Only one
value per command can come from stdin. For several secret values, use `key=@file` with a file you delete straight
afterwards, or set them in the UI.

**App policy:** least privilege, this app only, this tier only.

```hcl
# ibsmas-poc-unit
path "kv-unit/data/ibsmas/poc/*"      { capabilities = ["read"] }
path "kv-unit/metadata/ibsmas/poc/*"  { capabilities = ["read"] }
path "transit-unit/encrypt/ibsmas-poc" { capabilities = ["update"] }
path "transit-unit/decrypt/ibsmas-poc" { capabilities = ["update"] }
path "transit-unit/rewrap/ibsmas-poc"  { capabilities = ["update"] }
path "transit-unit/keys/ibsmas-poc"    { capabilities = ["read"] }
```

**AppRole role:** a single-use secret_id, valid for 10 minutes, as delivered at deploy.

```sh
docker exec -e VAULT_TOKEN vault vault write auth/approle/role/ibsmas-poc-unit \
  token_policies=ibsmas-poc-unit token_ttl=1h token_max_ttl=4h \
  secret_id_num_uses=1 secret_id_ttl=10m
# Add when the app hosts are known:  secret_id_bound_cidrs=10.1.2.0/24  token_bound_cidrs=10.1.2.0/24
```

**Deployer policy:** the pipeline identity may only mint **wrapped** secret_ids for this one role.

```hcl
# ibsmas-deployer-unit
path "auth/approle/role/ibsmas-poc-unit/secret-id" {
  capabilities     = ["update"]
  min_wrapping_ttl = "1m"
  max_wrapping_ttl = "10m"
}
```

*Tested:* a token with this policy was **refused** a plain secret_id, and given a wrapped one with `-wrap-ttl=5m`.

**What the pipeline does at deploy:**

```sh
vault write -wrap-ttl=5m -format=json -f auth/approle/role/ibsmas-poc-unit/secret-id > wrapped.json
# copy wrapped.json to the app host (mode 0400, the app's user), restart the app; the client deletes it after reading
```

## 5. Point the client at it ✔

**AppRole mode:**

```sh
VAULT_ADDR=https://vault-nonprod.example.internal:8200
VAULT_CACERT=/etc/pki/ca-trust/source/anchors/internal-ca.pem
VAULT_KV_MOUNT=kv-unit
VAULT_TRANSIT_MOUNT=transit-unit
VAULT_ROLE_ID=<role_id of ibsmas-poc-unit>              # not secret on its own; fine in config
VAULT_WRAPPED_SECRET_ID_FILE=/run/poc/wrapped-secret-id
```

**Proxy mode:** run Vault Proxy on the app host. The client then uses `VAULT_AUTH_MODE=agent` and
`VAULT_ADDR=http://127.0.0.1:8100`, and the mount settings stay the same. The proxy's own config, pointed at this
server over TLS:

```hcl
vault {
  address = "https://vault-nonprod.example.internal:8200"
  ca_cert = "/etc/vault-proxy/internal-ca.pem"
}
auto_auth {
  method "approle" {
    config = {
      role_id_file_path                   = "/etc/vault-proxy/role-id"
      secret_id_file_path                 = "/etc/vault-proxy/secret-id"
      remove_secret_id_file_after_reading = false
    }
  }
}
api_proxy { use_auto_auth_token = "force" }
listener "tcp" {
  address     = "127.0.0.1:8100"
  tls_disable = true          # loopback only
}
```

*Tested:*
- **AppRole over TLS:** the read passed with `VAULT_CACERT`, and the client deleted the wrapped file after reading.
- **Through Vault Proxy:** the KV read and the Transit round trip passed. The proxy had a TLS upstream and used a
  separate role with a reusable secret_id.

How the proxy gets its secret_id in each tier is the main [open question](06-open-questions.md).

## 6. Operating it

| Task | Command |
|---|---|
| Health (no token needed) | `docker exec vault vault status` |
| Snapshot (back up raft data) | `docker exec -e VAULT_TOKEN vault vault operator raft snapshot save /vault/file/snap-$(date +%F).snap`, then copy it off the host |
| Restore | `vault operator raft snapshot restore <file>` (needs the unseal keys of the snapshot's cluster) |
| Upgrade | Take a snapshot, change the image tag, `docker compose up -d`, unseal |
| Rotate the encryption key | `vault operator rotate` |
| See who did what | `docker exec vault tail -f /vault/logs/audit.log` (ship it to the SIEM) |

The rows below the health check follow HashiCorp's documented procedures and weren't run in the test.
