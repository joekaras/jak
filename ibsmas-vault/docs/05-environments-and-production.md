# Environments and production

How the four IBSMAS tiers map onto Vault, how an app is configured per tier, and what a production Vault needs.
**Status: proposal.** The layout in §2 Option B was tested on a single-node Docker Vault (see
[On-prem Docker Vault](04-on-prem-docker-vault.md)). Everything in §4 is standard HashiCorp practice that hasn't
been tried here, and the decisions it depends on are listed in [Open questions](06-open-questions.md).

## 1. Tiers

| Tier | Purpose | Name in Vault | Data |
|---|---|---|---|
| **UNIT** | Development | `unit` | Synthetic only |
| **OPAC** | System test | `opac` | Synthetic, or masked |
| **OPIN** | Readiness: prod-like, **but not prod** | `opin` | Prod-like; never production secrets |
| **OPER** | Production | `oper` | Production |

**DR** isn't listed. If OPER has a DR site, it needs the same secrets, which means replication or snapshot restore
(§4). That's an open question.

## 2. Layout in Vault

**The rule that makes promotion simple: code is tier-independent, and configuration picks the tier.** An app reads
`ibsmas/orders/snowflake` and encrypts with Transit key `ibsmas-orders` in every tier. Only the namespace or mounts,
the role and the address change from tier to tier. Secrets are **never** copied up from a lower tier. Each tier has
its own values and its own Transit keys, so OPIN ciphertext can't be decrypted in OPER, by design.

### Option A: Vault Enterprise, a namespace per tier (preferred if licensed)

```
namespace ibsmas/unit   ─┐
namespace ibsmas/opac    │  each has the same mounts:  kv/  transit/  auth/approle/
namespace ibsmas/opin    │  and the same paths:        kv/ibsmas/<app>/<secret>, transit key ibsmas-<app>
namespace ibsmas/oper   ─┘
```

- **Configuration:** the client sets only `VAULT_NAMESPACE=ibsmas/<tier>`. Mounts, roles and policies have the same
  names in every tier.
- **Isolation:** namespaces separate policies, tokens and audit views, and each can have its own administrators.

### Option B: Community edition, a mount per tier (tested for UNIT)

| Thing | Name |
|---|---|
| KV v2 mount | `kv-<tier>`, for example `kv-opac` |
| Transit mount | `transit-<tier>` |
| AppRole auth mount | `approle` (shared); roles carry the tier |
| Role and policy per app | `ibsmas-<app>-<tier>`, for example `ibsmas-orders-opac` |
| Deployer policy per tier | `ibsmas-deployer-<tier>` |
| KV path | `ibsmas/<app>/<secret>` (the same in every tier) |
| Transit key | `ibsmas-<app>` (the same name in every tier, a different key per mount) |

- **Isolation:** comes from the policies alone. A policy names exactly one tier's mounts, so a UNIT token can't read
  `kv-oper`.
- **Separate auth mounts** (`approle-<tier>`) are possible if each tier needs its own administrators. The client
  supports them through `VAULT_AUTH_MOUNT`.

**Compared with the earlier key-storage standard:** it used one mount with the tier in the path
(`secret/ibsmas/<tier>/<repo>/<name>`). That works with the client, but it puts the tier into application code or
path configuration. Options A and B keep the tier out of code.

### Clusters

**Recommendation:** run **OPER (and DR) on its own Vault cluster**, and UNIT, OPAC and OPIN on a shared non-prod
cluster.
- **Isolation:** a mistake or compromise in non-prod can't touch production secrets.
- **Production rehearsal:** OPIN gets prod-like *controls* (the §3 matrix) without prod *data*.
- **Placing OPIN** with OPER, or with non-prod, is an open question.

## 3. Controls per tier

| Control | UNIT | OPAC | OPIN | OPER |
|---|---|---|---|---|
| Client auth mode | Proxy, or AppRole | Proxy, or AppRole | Same as OPER | Proxy (recommended), or AppRole with pipeline delivery |
| secret_id delivery | Developer runs `wrap.sh`-style command, or local dev Vault | Pipeline (deployer policy) | Pipeline, same as OPER | Pipeline or provisioning (open question) |
| secret_id use limits | Reusable OK | `num_uses=1`, `ttl=10m` for apps; bounded TTL for proxies | Same as OPER | `num_uses=1`, `ttl=10m` for apps; bounded TTL and CIDR-bound for proxies |
| CIDR binding (`secret_id_bound_cidrs`, `token_bound_cidrs`) | No | Recommended | Yes | Yes |
| Token TTL / max TTL | 1h / 4h | 1h / 4h | 1h / 24h, or `token_period` | 1h / 24h, or `token_period` |
| Who writes secrets | Developers | Team leads | Ops / Security | Ops / Security, two-person rule |
| Transit `rotate` in app policy | Test keys only | No | No | No |
| Transit `auto_rotate_period` | Off | Off | 90d (matches OPER) | 90d (or per data classification) |
| Audit device to SIEM | Optional | Yes | Yes | Yes, two devices |
| Unseal | Shamir OK | Shamir or auto | Auto-unseal | Auto-unseal (HSM/KMS) |

These numbers are starting points for discussion with Security, not decided policy.

## 4. Configuration per tier

The same app on each tier. Only these values change:

| Setting | UNIT | OPAC | OPIN | OPER |
|---|---|---|---|---|
| `VAULT_ADDR` (AppRole mode) | `https://vault-nonprod…:8200` | same | same, or prod cluster | `https://vault-prod…:8200` |
| `VAULT_ADDR` (Proxy mode) | `http://127.0.0.1:8100` | same | same | same |
| `VAULT_NAMESPACE` (Option A) | `ibsmas/unit` | `ibsmas/opac` | `ibsmas/opin` | `ibsmas/oper` |
| `VAULT_KV_MOUNT` (Option B) | `kv-unit` | `kv-opac` | `kv-opin` | `kv-oper` |
| `VAULT_TRANSIT_MOUNT` (Option B) | `transit-unit` | `transit-opac` | `transit-opin` | `transit-oper` |
| `VAULT_ROLE_ID` (AppRole mode) | role_id of `ibsmas-<app>-unit` | `…-opac` | `…-opin` | `…-oper` |
| `VAULT_CACERT` | internal CA bundle | same | same | same |

In Proxy mode, the namespace, role_id and secret_id belong to the **proxy's** configuration on the host, not the app's.
The app sets only the mounts and `VAULT_ADDR`.

## 5. Production Vault

A single container is fine for UNIT and OPAC. OPIN and OPER need the following. **None of this was tested here.**

- **High availability:** integrated storage (raft) with **3 or 5 nodes** across failure domains, behind a load
  balancer or DNS name that checks `/v1/sys/health`.
- **TLS:**
  - certificates from the internal CA;
  - `tls_min_version = "tls12"`;
  - a SAN for the load-balancer name and each node.
  The client pins TLS 1.2 regardless.
- **Unsealing:** auto-unseal with an HSM or cloud KMS, so a restart doesn't need key custodians at 3 a.m. Keep
  recovery-key shares with named custodians.
- **Audit:**
  - **Two audit devices** (file plus syslog or socket). Vault stops serving requests if every audit device fails,
    so two keep it running when one breaks.
  - Ship the logs to the SIEM.
  - Alert on policy denials and root-token use.
- **Admin access:**
  - humans log in with OIDC or LDAP, with MFA;
  - **no standing root token**: revoke it after setup, and recreate it with `operator generate-root` only under
    the unseal quorum.
- **Configuration as code:** mounts, policies, roles and auth methods in Terraform (Vault provider), changed through
  reviewed pull requests. No hand-made changes in OPER.
- **Backups:**
  - scheduled `vault operator raft snapshot save`, encrypted and stored off the cluster;
  - **test restores** regularly.
- **DR:**
  - Enterprise: DR replication to the DR site.
  - Community: snapshot shipping and a documented restore runbook. Decide the recovery time and recovery point
    objectives with Ops.
- **Monitoring:** telemetry to the monitoring stack, alerting on:
  - seal status;
  - leader changes;
  - raft health;
  - certificate expiry;
  - audit-device failures.
- **Upgrades:**
  - pin the version (the scripts here use 1.18.3);
  - rehearse each upgrade in OPIN;
  - take a snapshot first.
- **Key rotation:**
  - Transit: `auto_rotate_period`, with apps calling `rewrapIfStale` (and `min_decryption_version` raised once old
    data has been rewrapped);
  - KV-stored keys: write a new version, and keep the version number with the ciphertext (see
    [Using the client](01-using-the-client.md) §5);
  - AppRole secret_ids: bounded TTLs.
- **Incident response:**
  - revoke an app's tokens with `vault token revoke -accessor <accessor>`;
  - list a role's secret_ids with `vault list auth/approle/role/<role>/secret-id`, and destroy a leaked one with
    `vault write auth/approle/role/<role>/secret-id-accessor/destroy secret_id_accessor=<accessor>`;
  - lock a role by rewriting its `secret_id_bound_cidrs`;
  - if an unwrap fails, check the audit log for who unwrapped first.

## 6. Promotion checklist for an app

1. **Vault administrators** create the role, policy, KV paths and Transit key in the target tier, and write that
   tier's secret values themselves. Nothing is copied from a lower tier.
2. **The deploy pipeline** gets the target tier's deployer identity (AppRole mode), or the host's proxy is provisioned
   (Proxy mode).
3. **The app's configuration** for the tier sets the address, namespace or mounts, role_id and CA (the §4 table). No
   code changes.
4. **Smoke test:** the app's health check, for example the `PocCheck`-style `/vault/health`, reports success with key
   names only.
