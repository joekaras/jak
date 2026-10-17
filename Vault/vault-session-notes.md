# HashiCorp Vault from Java & C# — Session Notes

Date: 2026-10-14
Context: Retrieving secrets/keys from HashiCorp Vault in Java (Jenkins/Bitbucket) and C# (Azure DevOps) apps running on on-prem Windows and Linux VMs.

---

## 1. The "secret zero" problem

Vault protects all other secrets, but the app needs *something* to prove its identity to Vault. Best practice: avoid storing a credential at all by using an identity the platform already provides.

| Where the app runs | Auth method | What proves identity |
|---|---|---|
| Azure (VM, App Service, AKS) | Azure auth | Managed identity token |
| Kubernetes | Kubernetes auth | Pod service account token |
| CI (Jenkins, Azure DevOps) | JWT/OIDC auth | Short-lived per-job token |
| **On-prem Windows/Linux VMs** | **TLS cert auth** (or AppRole) | Machine cert with non-exportable key |

**Avoid:** credentials in appsettings.json / application.yml / source control; long-lived root or static tokens; hand-set environment variables on servers (leak into crash dumps, process listings, logs).

---

## 2. Recommendation for on-prem VMs

### Primary: TLS certificate auth
Each VM gets a machine certificate from the internal CA. The app presents it to Vault; Vault verifies the issuing CA (and optionally the CN/SAN) and returns a short-lived token.

**Windows VMs**
- Cert in `LocalMachine\My` with a **non-exportable private key**, ideally TPM-backed (Microsoft Platform Crypto Provider).
- If AD CS is available, use **autoenrollment** via Group Policy for automatic issue/renewal.
- Grant private-key read access only to the app's service account.
- C#: load via `X509Store(StoreName.My, StoreLocation.LocalMachine)` → VaultSharp cert auth.
- Java: use the `Windows-MY` keystore (SunMSCAPI) — no .pfx export.

**Linux VMs**
- Key as a file owned by the service user, `chmod 600`, in a directory only that user can read.
- Better: TPM via PKCS#11 (tpm2-pkcs11).
- Automate renewal with certmonger or a scheduled job.

**Vault side**
- One cert role per app, bound with `allowed_common_names` (or DNS SANs), attached to a narrow read-only policy.
- Short token TTLs (e.g., 1h) with renewal.
- Optional `token_bound_cidrs` to restrict tokens to server subnets.

### Fallback: AppRole
Use when per-VM certs are impractical (see section 3).

### Vault Agent (either method)
Run Vault Agent as a Windows service / systemd unit. It handles login and token renewal; Java and C# apps read secrets from it (localhost or a locked-down file). One auth setup per server; often the only practical route for legacy Win32/COM apps.

### Question for infrastructure team
Do we have AD CS with autoenrollment on Windows servers, and what's the cert process for Linux? That usually decides cert auth vs AppRole.

---

## 3. AppRole explained

AppRole is Vault's login method for machines/applications — a username/password split into two pieces with different handling rules.

- **role_id** — identifies *which* app (like a username). Not very sensitive; can live in config.
- **secret_id** — proves the app is *allowed* to log in (the password). Sensitive.

Neither alone gets you in.

**Login flow**
1. App sends role_id + secret_id to `POST /v1/auth/approle/login`.
2. Vault checks the match and restrictions (IP range, expiry, use count).
3. Vault returns a short-lived **token** tied to a **policy** listing readable paths.
4. App uses the token to read secrets, renews it, and logs in again on expiry.

**Why two pieces:** they travel by different paths so no single system/person holds both.
- role_id → baked into config at build/install time.
- secret_id → delivered separately at deploy time by the release pipeline.

**Controls on the secret_id**
- **TTL** — expires after minutes/hours.
- **Use limit** — `secret_id_num_uses=1` makes it worthless after login.
- **CIDR binding** — only works from the server's IP/subnet.
- **Response wrapping** — Vault issues a single-use wrapper token; the app unwraps it (`POST /v1/sys/wrapping/unwrap`). If someone unwrapped it first, the app's unwrap fails → tampering detected.

**Typical on-prem flow**
1. Vault admin creates role `payments-service` with a read-only policy for `secret/data/payments/*`.
2. role_id goes into the app's config on the VM.
3. On deploy, Jenkins/Azure DevOps requests a **wrapped, single-use secret_id** and drops it in a file only the service account can read.
4. App starts, unwraps, logs in, gets a token, deletes the file.
5. App runs on the token and renews it. On restart it needs a fresh secret_id.

**Weakness:** AppRole shrinks and moves "secret zero" rather than eliminating it — the pipeline's ability to generate secret_ids becomes the thing to protect. That's why cert auth is often preferred on-prem.

---

## 4. Review of the team's existing reference implementation

The existing example (bash curl + Java `HttpClient`/Jackson) confirms our setup:
- **Vault Enterprise with namespaces** (`X-Vault-Namespace` header)
- **AppRole** auth
- **KV v2** secrets (`/v1/<mount>/data/<path>`, value at `.data.data.<key>`)
- role_id / secret_id are currently pulled from a **Snowflake query**

### Issues found (don't copy these)
1. **Won't compile as pasted** — `String.format` JSON missing escaped quotes; both `throw new IOException(...)` lines missing `+` before `" - "`.
2. **JSON built with `String.format`** — breaks or becomes injectable if a value contains a quote. Use Jackson `ObjectMapper` to build the body.
3. **Credentials on the curl command line** — visible in `ps` and shell history. Use `--data @-` from stdin.
4. **secret_id stored in Snowflake** — a long-lived secret_id in a database means anyone who can read that table can log in as the app. Confirm with Vault admins that the secret_id has TTL, num_uses, and CIDR binding. **Biggest risk in the flow.**
5. **Logs in on every call, never revokes** — leaves live tokens until TTL. Cache, renew, and revoke on shutdown (`auth/token/revoke-self`).
6. **No timeouts** on `HttpClient`.
7. **Bash fails silently** — on login failure `jq -r` returns `"null"` and the second curl runs with `X-Vault-Token: null`. Needs `set -euo pipefail` and an explicit check.

---

## 5. Final prompt for code generation

```
I need production-quality code to read secrets from HashiCorp Vault in two stacks:
1. Java [17/21], built with [Maven/Gradle], deployed via Jenkins (Bitbucket)
2. C# [.NET 8 / .NET Framework 4.8], built in Azure DevOps
Apps run on on-prem Windows and Linux VMs.

## Our Vault setup (confirmed)
- Vault Enterprise with namespaces: every request sends X-Vault-Namespace.
- Auth: AppRole (POST /v1/auth/approle/login with role_id + secret_id).
- Secrets: KV v2. Read via GET /v1/<mount>/data/<path>; values are under
  .data.data.<key> (e.g. .data.data.token).
- TLS: [custom CA bundle path / Windows cert store / default trust store]

## Reference implementation (has known issues — fix them, don't copy them)
[paste the bash + Java example here]
Known issues to fix:
- Build request JSON with a JSON library, never string formatting.
- Don't put role_id/secret_id on a command line; in bash use --data @- from stdin.
- Cache the Vault token, renew it before TTL expiry, re-login on 403, and
  revoke it (auth/token/revoke-self) on shutdown. Don't log in per call.
- Set connect and request timeouts.
- Bash: set -euo pipefail and fail if jq returns empty or "null".

## Libraries
- Java: Spring Vault or io.github.jopenlibs:vault-java-driver (not the deprecated
  com.bettercloud driver). If plain java.net.http + Jackson is simpler for our
  two-call flow, say so and justify it.
- C#: VaultSharp, or HttpClient + System.Text.Json with justification.

## Credential handling
- role_id from config; secret_id must NOT be hardcoded or in config files.
  Read it from a file readable only by the service account (path via env var),
  ideally delivered as a response-wrapped, single-use token by the deploy
  pipeline. Support unwrapping (POST /v1/sys/wrapping/unwrap).
- Never log role_id, secret_id, Vault tokens, or secret values; redact them
  from exception messages too.
- Note: the secret_id is currently fetched from a Snowflake table. Include a
  short section on the risks of that and what Vault-side restrictions
  (secret_id TTL, num_uses, bound CIDRs) should be in place.

## Requirements
- Reusable thread-safe client class in each language: getSecret(path, key).
- In-memory secret cache with configurable TTL; nothing written to disk.
- Retries with backoff for transient errors; typed exceptions for auth failure,
  permission denied, path not found, and sealed Vault.
- Config via application.yml / appsettings.json with env-var overrides.
- If a secret is an AES-256 key: base64-decode, verify 32 bytes, minimize its
  lifetime in memory (zero byte arrays after use; CryptographicOperations.ZeroMemory
  in C#), and show use with AES-GCM.

## Deliverables
1. Java client + example usage + tests (Testcontainers Vault or mocked HTTP).
2. C# client + example usage + xUnit tests.
3. Corrected bash snippet.
4. Minimal Vault policy (HCL) granting read-only access to just the needed paths.
5. Short security checklist for review.

Ask me clarifying questions before writing code if anything above is ambiguous.
```

### Optional: cert-auth variant of the auth section
```
- Auth method: TLS certificate auth ("cert" auth method). Apps run on on-prem
  Windows and Linux VMs.
  - Windows: client cert in LocalMachine\My with non-exportable private key
    (possibly TPM-backed). C# loads it via X509Store; Java uses the
    SunMSCAPI "Windows-MY" keystore. Do not export the key to a .pfx file.
  - Linux: client cert/key as PEM files readable only by the service account,
    or PKCS#11 if TPM-backed. Java loads via KeyStore; C# via X509Certificate2.
  - Show the Vault-side setup: `vault auth enable cert`, a role per app bound
    with allowed_common_names, short token TTL, and token_bound_cidrs.
  - Fallback: AppRole with response-wrapped secret_id delivered by the
    Jenkins/Azure DevOps deploy pipeline. Show this as an alternative.
  - Also show the Vault Agent option (Windows service / systemd) with
    auto-auth via cert, where the app reads secrets from the agent instead
    of calling Vault directly.
```

---

## Open questions / next steps
- Ask Vault admins: secret_id TTL, num_uses, and CIDR binding on the AppRole whose secret_id lives in Snowflake.
- Ask infrastructure: AD CS autoenrollment on Windows servers? Cert process for Linux?
- Decide cert auth vs AppRole per app; consider Vault Agent for legacy Win32/COM apps.
