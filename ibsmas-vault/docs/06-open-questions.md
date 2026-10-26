# Open questions: secret_id delivery and Vault Proxy

The client is finished and tested. What's still undecided is **how a credential first reaches each host**, the
"secret zero" problem, and **whether and how to run Vault Proxy**. This page sets out the options, what has been
tested, and a recommendation for each question, so Security, Ops and the BOE teams can decide. The decision list is
in §6.

## 1. The two ways an app can authenticate

| | **AppRole direct** (`approle-wrapped`) | **Through Vault Proxy** (`agent` mode) |
|---|---|---|
| Who holds a credential | The app process (a token, in memory) | The proxy on the host; the app holds nothing |
| What must be delivered | A wrapped secret_id **at every app start** | A secret_id for the proxy **when the host is provisioned** (or at each proxy start, see §3) |
| App restart or Tomcat redeploy | Needs a new delivery (the old secret_id is spent) | Nothing to deliver |
| Token expiry | The app can't log in again; it needs `token_period` or a restart with a new delivery | The proxy logs in again by itself |
| Extra moving part | None | A proxy service per host (or per app) to install, monitor and patch |
| Who on the host can act as the app | Only the app process | **Any local process that can reach the proxy's port** (see §4) |
| Tested | Yes: plain http on loopback, and TLS | Yes: Agent and Proxy, TLS upstream |

**Recommendation:**
- **Proxy** for long-running services and for Tomcat.
- **AppRole direct** for batch jobs and short-lived processes that a pipeline or scheduler starts anyway, and for
  hosts where a local proxy isn't acceptable (see §4).

## 2. Question: how does an app get its secret_id (AppRole direct)?

### Option A: pipeline delivers a response-wrapped secret_id ("trusted orchestrator") — current design

```
Pipeline (deployer identity)                Vault                         App host
  ── write -wrap-ttl=5m role/<app>-<tier>/secret-id ──▶
  ◀── wrapping token (single use, 5 min) ──
  ── copy token to /run/<app>/wrapped-secret-id (0400, app user), restart app ──────────────▶
                                                     ◀── unwrap, then login ── app (deletes file)
```

- **Tested:** the deployer policy ([On-prem](04-on-prem-docker-vault.md) §4) refuses an unwrapped secret_id. The
  client unwraps, logs in, and deletes the file.
- **Why it's strong:**
  - the pipeline never sees the secret_id itself;
  - a wrapped token that is intercepted and used first makes the app's unwrap **fail loudly**, which is detectable;
  - the secret_id is single-use and expires in minutes.
- **Open sub-questions:**
  1. **How the pipeline authenticates to Vault.**
     - Preferred: OIDC/JWT from the CI system (Jenkins, Azure DevOps), so the pipeline has no stored secret.
     - Alternative: its own AppRole, which just moves the secret-zero problem to the CI system.
  2. **Restarts outside a deploy.** Patching a host or a crash restart needs a new delivery. Either all restarts go
     through the pipeline, or this app uses the proxy instead.
  3. **Token lifetime.** Long-running apps need `token_period` on the role, or a restart before `token_max_ttl`.

### Option B: the platform's own identity, so no secret_id at all

Vault authenticates the host by something it already has:
- **TLS certificate auth**, using a machine certificate from the internal PKI;
- **Kerberos**, for Windows domain hosts (a Vault plugin);
- **JWT/OIDC**, for workloads that get identity tokens;
- **Kubernetes**, if apps move there.

This removes secret zero entirely. **Not supported by the client today**: it would need a new `AuthMode`. It can,
however, be used by the proxy (Option C3), with no client change.

## 3. Question: how does the proxy get its secret_id?

The proxy needs a role_id and secret_id (AppRole auto-auth) or another auth method. The role_id isn't secret; it can
sit in a config file.

| Option | How | Pros | Cons |
|---|---|---|---|
| **C1. Reusable secret_id at provisioning** | Ops writes a secret_id file once (bounded `secret_id_ttl`, for example 30–90 days; `secret_id_bound_cidrs` = the host) and a job renews it before expiry | Simple; the proxy survives restarts | A long-lived secret on disk; needs a renewal job; anyone who copies the file can use it from the bound host |
| **C2. Wrapped secret_id at each proxy start** ✔ tested | Like Option A, but for the proxy: `secret_id_response_wrapping_path` in its config. The proxy unwraps the file and deletes it. | No long-lived secret on disk; tamper-evident | Every proxy restart needs a delivery, though proxies restart far less often than apps; app restarts need nothing |
| **C3. Platform identity** | TLS cert, Kerberos or similar auth in the proxy's `auto_auth` | No secret zero at all | Needs Vault auth-method setup and a machine-identity source per platform; not tested here |

*Tested for C2 (Vault Proxy 1.18.3):* a wrapped secret_id in `secret_id_file_path`, with
`secret_id_response_wrapping_path` set to the role's `secret-id` path:
- the proxy logged in and deleted the file;
- KV reads through it returned 200.

The proxy keeps the unwrapped secret_id in memory, so if the role allows reuse it should be able to log in again
after its token expires. *That re-login wasn't tested.*

**Recommendation:**
- **C2 for OPIN and OPER.** It's the same delivery mechanism and deployer policy as Option A, applied to the proxy.
- **C1 for UNIT and OPAC.**
- **Evaluate C3** (TLS certificate auth) if the hosts already have machine certificates.

## 4. Question: how is the proxy set up?

### Where it runs

- **One proxy per app identity, on the app's host.** Each app gets its own role and policy, its own proxy instance
  and its own loopback port, for example 8100, 8101.
- **Not one proxy per host for all apps.** A shared proxy gives every app the union of all their permissions, which
  defeats least privilege.

### The local-access risk (decide this explicitly)

**Any process on the host that can reach the proxy's listener acts with the app's permissions.** On loopback TCP
that means every local user and process.

| Mitigation | Linux | Windows |
|---|---|---|
| Unix socket listener with owner and mode limited to the app's user | Vault Proxy supports it (not tested) | Not available |
| Dedicated hosts, or few local users | Yes | Yes |
| `require_request_header = true` on the listener (not tested) | Stops browser-based SSRF only, not local processes | Same |
| Use AppRole direct instead (§1) | Yes | Yes |

**The client can't connect over a Unix socket today**, because `HttpURLConnection` is TCP only. Supporting it would
need a client change. On Windows there's no equivalent socket permission, so the risk has to be accepted by Security,
or those apps use AppRole direct.

### Configuration (one app, one tier)

`/etc/vault-proxy/<app>/proxy.hcl` on Linux, or `C:\vault-proxy\<app>\proxy.hcl` on Windows (use forward slashes in
HCL paths):

```hcl
vault {
  address = "https://vault-prod.example.internal:8200"
  ca_cert = "/etc/vault-proxy/internal-ca.pem"
}
auto_auth {
  method "approle" {
    # mount_path = "auth/approle"        # default
    config = {
      role_id_file_path                   = "/etc/vault-proxy/orders/role-id"
      secret_id_file_path                 = "/run/vault-proxy/orders/secret-id"
      secret_id_response_wrapping_path    = "auth/approle/role/ibsmas-orders-oper/secret-id"   # option C2
      remove_secret_id_file_after_reading = true
    }
  }
}
api_proxy { use_auto_auth_token = "force" }
listener "tcp" {
  address     = "127.0.0.1:8100"
  tls_disable = true
}
```

**Namespaces (Enterprise) are untested.** With Option A ([Environments](05-environments-and-production.md) §2), the
proxy must log in and send requests in the tier's namespace. Where that setting goes in the proxy config has to be
confirmed against an Enterprise server. A Community server ignores namespaces, and Vault Proxy 1.18.3 **silently
accepts unknown or misplaced settings**: a test config containing `no_such_setting = "x"` started without complaint.
So a wrong placement won't fail at startup. Confirm it with a policy denial test in UNIT before relying on it.

### Running the proxy as a service

**Linux (systemd).** *Not tested.*

```ini
[Unit]
Description=Vault Proxy for orders
After=network-online.target
Wants=network-online.target

[Service]
User=vault-proxy-orders
ExecStart=/usr/bin/vault proxy -config=/etc/vault-proxy/orders/proxy.hcl
RuntimeDirectory=vault-proxy/orders
RuntimeDirectoryMode=0700
NoNewPrivileges=yes
ProtectSystem=strict
ProtectHome=yes
PrivateTmp=yes
ReadWritePaths=/run/vault-proxy/orders
LimitCORE=0
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**Windows.** Use WinSW, the same tool as the sample service. *Not tested as a service. `vault proxy` itself was tested
on Windows.*

```xml
<service>
  <id>vault-proxy-orders</id>
  <name>Vault Proxy (orders)</name>
  <executable>C:\vault\vault.exe</executable>
  <arguments>proxy -config=C:/vault-proxy/orders/proxy.hcl</arguments>
  <log mode="roll-by-size" />
  <onfailure action="restart" delay="10 sec" />
</service>
```

Run it as a dedicated account. Restrict `C:\vault-proxy\orders\` so only that account and admins can read it. With
option C2, restrict the folder the pipeline writes the wrapped secret_id into the same way.

**Start order:** the app's client doesn't need the proxy at startup, because no network call happens until the first
read. Reads fail with `VaultUnavailableException` until the proxy is up. Make the app's service depend on the proxy's
service, or retry reads at startup.

**Monitoring:** alert when the proxy service is down, and when its log shows `auth.handler` errors (it can't log in).

## 5. Other open questions

| Question | Options | Lean |
|---|---|---|
| Vault edition | Community (mount per tier) or Enterprise (namespace per tier, DR replication) | Depends on licensing; the client supports both |
| Cluster split | One cluster for all tiers; non-prod + prod; one per tier | Non-prod (UNIT, OPAC, OPIN) + prod (OPER, DR) |
| Where OPIN lives | Non-prod cluster, or prod cluster in its own namespace | Non-prod, with prod-like controls |
| DR | Enterprise DR replication; snapshot restore | Set the recovery time and point objectives with Ops first |
| Unseal custodians | Who holds the Shamir or recovery shares | Security plus Ops, never one team alone |
| Who writes OPER secrets | Ops, Security, app owners | Ops/Security, two-person rule |
| CI identity in Vault | OIDC/JWT from the CI system, or an AppRole | OIDC/JWT |
| Transit vs KV-stored keys | New encryption uses Transit; existing KV keys stay until migrated | Transit for new work |
| Rotation cadence | Transit keys, KV keys, proxy secret_ids | 90 days as a starting point |
| Unix-socket support in the client | Needed only if the Linux local-access risk (§4) must be closed | Decide after §4 |

## 6. Decisions needed

1. **App auth mode per app type** (§1). Proposed: Proxy for services and Tomcat; AppRole direct for batch.
2. **Pipeline identity in Vault** (§2, sub-question 1). Proposed: OIDC/JWT from the CI system.
3. **Proxy secret_id delivery per tier** (§3). Proposed: C2 for OPIN and OPER, C1 for UNIT and OPAC; evaluate C3.
4. **Acceptance of the local-access risk** on loopback, or a requirement for Unix sockets (§4).
5. **Edition, cluster split and DR** (§5).
