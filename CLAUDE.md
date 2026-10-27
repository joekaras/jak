# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

The only live code is `ibsmas-vault/java`: a dependency-free Java 8 client for HashiCorp Vault (KV v2 reads, including
versions, and Transit encrypt/decrypt/rewrap), published locally as `ibsmas:ibsmas-vault` with packages
`ibsmas.vault.*`. Proof-of-concept hosts show it running under Tomcat 9, as a Windows service (WinSW), and as a
Linux systemd unit. It was
written as a port of a C# `Vault.Client`, which was removed from this repo in commit `85b50ac`. Some comments still
reference the C# client (for example `TokenManager.shouldRenew` says "keep the two in step").

Developer docs live in `ibsmas-vault/README.md` and `ibsmas-vault/docs/`:
1. using the client;
2. the samples;
3. a local Vault;
4. an on-prem Docker Vault;
5. the UNIT/OPAC/OPIN/OPER tiers and production;
6. open questions on secret_id delivery and Vault Proxy.

Each doc marks what was actually tested. Keep those marks accurate when behaviour changes. Vault Agent's API-proxy
feature is deprecated in favour of `vault proxy`; the client's `agent` auth mode works with both.

## Build and test

Run from `ibsmas-vault/java`. The POM targets Java 8; the `release-8` profile turns on for JDK 9+ so the build still
compiles against the Java 8 API.

```sh
mvn package                                         # build all modules, run unit tests
mvn -pl ibsmas-vault test                           # library tests only
mvn -pl ibsmas-vault test -Dtest=VaultConfigTest    # one test class
mvn -pl ibsmas-vault test -Dtest=VaultSecretClientTest#methodName
```

- The project has no corporate parent POM and pulls everything from Maven Central.
- Unit tests run against `StubVaultServer`, an in-process HTTP stub, so they need no Vault.
- `IntegrationTest` is tagged `integration` but surefire doesn't exclude it. It skips itself unless env vars are
  set: `VAULT_WRAPPED_SECRET_ID_FILE` (AppRole) or `VAULT_AGENT_ADDR` (the local proxy's address; the name is historical), plus `VAULT_TEST_PATH` and the usual
  `VAULT_*` settings. The AppRole test spends the wrapped secret_id, so every run needs a fresh one. The Transit
  tests also need `VAULT_TRANSIT_TEST_KEY`; the rotation test additionally needs `VAULT_TRANSIT_ROTATE=true` and
  `rotate` permission, and permanently bumps the key version, so don't enable it against a shared Vault casually.
- `bash it/run.sh` runs all four integration tests against a throwaway local Vault (dev mode) and Vault Proxy, then
  tears them down. It uses Docker when the engine answers, otherwise a local `vault` binary (`VAULT_BIN` or PATH);
  force one with `IT_MODE=docker|native`. Both modes are pinned to Vault 1.18.3 (`VAULT_VERSION` in `it/lib.sh`); a
  native binary of any other version is refused unless `IT_VAULT_VERSION` says otherwise. Generated tokens and IDs
  live in `it/.work/`, which is git-ignored.
  Under Git Bash, set `MSYS_NO_PATHCONV=1` per command only: exporting it breaks the `mvn` launcher.

## Modules

- `ibsmas-vault`: the library. Its only dependency is JUnit, and only for tests. Keep it that way: the JSON parser
  (`internal/Json`) and the HTTP layer are hand-written because the library can't pull in dependencies.
- `poc-common`: `PocCheck` reads a credential and a 32-byte AES key, proves the key with an AES-GCM round trip,
  wipes it, and reports key names only. `PocDaemon` runs that check on a loop and closes the client on shutdown.
- `poc-windows`, `poc-linux`: thin `Main` wrappers around `PocDaemon`, shaded into runnable jars. Their service
  definitions are in each module's `service/` folder.
- `poc-tomcat`: WAR. `VaultContextListener` builds one client per web app and closes it on undeploy.

## Architecture of `ibsmas-vault`

Requests go through four layers:

1. `VaultConfig` reads an optional properties file (keys can carry a `vault.` prefix), then lets `VAULT_*` env vars
   override it, then validates. It rejects any secret_id passed as a setting, including the `VAULT_SECRET_ID` env
   var. Unknown keys throw an error. AppRole mode requires https unless Vault is on loopback.
2. `VaultSecretClient` implements both public interfaces, `SecretClient` (KV) and `TransitClient`. It normalizes
   paths (no `.`, `..`, or empty segments) and reads `<kvMount>/data/<path>`. It keeps three TTL caches: latest KV
   reads per path, versioned reads per `path@vN`, and Transit `latest_version` per key.
3. `TokenManager` owns the token lifecycle: log in on first use, renew at two-thirds of the TTL, and revoke on
   `close()`. In `agent` mode the authenticator is `null`, and the local Vault Proxy (or Agent) injects the token.
4. `VaultHttp` sends requests with `HttpURLConnection`. It pins TLS 1.2 through the `Tls12Only` wrapper, which Java
   8 needs, can trust a custom CA from a PEM bundle, retries transient statuses with backoff, and maps each status
   code to a typed exception (`VaultAuthException`, `VaultNotFoundException`, `VaultSealedException`, and so on).

These invariants span files and are easy to break:

- **The secret_id is single-use.** `AppRoleWrappedAuthenticator` reads the wrapping-token file, deletes it even if
  the read fails, unwraps it, and logs in once. If the client needs to log in again after that, it fails with
  guidance. It does not retry.
- **Requests that spend something are never retried.** Unwrap and login pass `retryable=false`. Every request with
  a body uses `setFixedLengthStreamingMode`, which stops `HttpURLConnection` from silently re-sending a POST and
  burning the secret_id twice. Unwrap sends an empty `{}` body for this reason.
- **A 403 can mean two things.** Vault returns 403 for both a dead token and a policy denial. After a 401 or 403,
  `readWithRecovery` calls `lookup-self` to tell them apart: a valid token means a policy denial
  (`VaultPermissionDeniedException`), and an invalid one means log in again and retry once.
- **Renewal tolerates outages.** If Vault is down or sealed during renewal, the client keeps using the current
  token until it expires.
- **Each cache has its own invalidation.** `invalidate(path)` clears a KV path and its versions.
  `invalidateKeyVersion(key)` clears a Transit key's cached `latest_version`. They're deliberately separate, because
  KV paths and Transit key names are different namespaces. `rewrapIfStale` relies on the Transit cache, so a rotation
  is seen only after the cache TTL or an explicit `invalidateKeyVersion`.
- **`TransitClient.ciphertextVersion` never echoes its input** in error messages, since a caller may pass plaintext by
  mistake. It accepts only `vault:v<N>:<payload>` with N ≥ 1, made of digits only.

## Conventions

- Every Java file starts with the Claude Code attribution comment. Add it to new files.
- **Never log, print, or put secret values into exception messages.** Error messages name paths and keys only.
  The JSON-parse error path in the authenticator avoids echoing the file's contents on purpose. Callers wipe key
  bytes with `Arrays.fill`.
- The service and unit files use `REPLACE-...` placeholders. Don't fill them with real hosts, role IDs, or
  namespaces.
- Stay on the Java 8 API: no `var`, no `java.net.http`, no `List.of`.

## Repo housekeeping

- `.gitignore` ignores Maven `target/` folders and the shade plugin's `dependency-reduced-pom.xml`, so builds leave
  the working tree clean.
- `.gitattributes` forces LF line endings except for `*.cmd` and `*.bat`.
- **Workflow.** Each change goes on its own branch from `main` and through a PR (merge commits, no squash). The user
  decides when to merge and when merged branches get deleted. After a merge, `mvn install` refreshes the jars in
  `~/.m2`, and `bash it/run.sh` (plus `IT_MODE=native`) rechecks against a real Vault.
- On Windows, a folder can't be renamed or deleted while any shell's working directory is inside it. Move shells
  out first. Pulling a commit that removes tracked build output deletes those files from disk; rebuild afterwards.
