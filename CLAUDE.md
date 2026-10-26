# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

The only live code is `Vault/java`: a dependency-free Java 8 client for HashiCorp Vault KV v2, plus proof-of-concept
hosts that show it running under Tomcat 9, as a Windows service (WinSW), and as a Linux systemd unit. It was
written as a port of a C# `Vault.Client`, which was removed from this repo in commit `85b50ac`. Some comments still
reference the C# client (for example `TokenManager.shouldRenew` says "keep the two in step").

## Build and test

Run from `Vault/java`. The POM targets Java 8; the `release-8` profile turns on for JDK 9+ so the build still
compiles against the Java 8 API.

```sh
mvn package                                         # build all modules, run unit tests
mvn -pl vault-client test                           # library tests only
mvn -pl vault-client test -Dtest=VaultConfigTest    # one test class
mvn -pl vault-client test -Dtest=VaultSecretClientTest#methodName
```

- The project has no corporate parent POM and pulls everything from Maven Central.
- Unit tests run against `StubVaultServer`, an in-process HTTP stub, so they need no Vault.
- `IntegrationTest` is tagged `integration` but surefire doesn't exclude it. It skips itself unless env vars are
  set: `VAULT_WRAPPED_SECRET_ID_FILE` (AppRole) or `VAULT_AGENT_ADDR` (Agent), plus `VAULT_TEST_PATH` and the usual
  `VAULT_*` settings. The AppRole test spends the wrapped secret_id, so every run needs a fresh one.

## Modules

- `vault-client`: the library. Its only dependency is JUnit, and only for tests. Keep it that way: the JSON parser
  (`internal/Json`) and the HTTP layer are hand-written because the library can't pull in dependencies.
- `poc-common`: `PocCheck` reads a credential and a 32-byte AES key, proves the key with an AES-GCM round trip,
  wipes it, and reports key names only. `PocDaemon` runs that check on a loop and closes the client on shutdown.
- `poc-windows`, `poc-linux`: thin `Main` wrappers around `PocDaemon`, shaded into runnable jars. Their service
  definitions are in each module's `service/` folder.
- `poc-tomcat`: WAR. `VaultContextListener` builds one client per web app and closes it on undeploy.

## Architecture of `vault-client`

Requests go through four layers:

1. `VaultConfig` reads an optional properties file (keys can carry a `vault.` prefix), then lets `VAULT_*` env vars
   override it, then validates. It rejects any secret_id passed as a setting, including the `VAULT_SECRET_ID` env
   var. Unknown keys throw an error. AppRole mode requires https unless Vault is on loopback.
2. `VaultSecretClient` is the public `SecretClient`. It normalizes paths (no `.`, `..`, or empty segments), keeps a
   TTL cache per path, and reads `<kvMount>/data/<path>`.
3. `TokenManager` owns the token lifecycle: log in on first use, renew at two-thirds of the TTL, and revoke on
   `close()`. In Agent mode the authenticator is `null`, and the local Vault Agent injects the token.
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

## Conventions

- Every Java file starts with the Claude Code attribution comment. Add it to new files.
- **Never log, print, or put secret values into exception messages.** Error messages name paths and keys only.
  The JSON-parse error path in the authenticator avoids echoing the file's contents on purpose. Callers wipe key
  bytes with `Arrays.fill`.
- The service and unit files use `REPLACE-...` placeholders. Don't fill them with real hosts, role IDs, or
  namespaces.
- Stay on the Java 8 API: no `var`, no `java.net.http`, no `List.of`.

## Repo housekeeping

- `.gitignore` is written for .NET and doesn't cover Maven `target/` folders. Build output under
  `Vault/java/*/target/` is committed, so expect it in diffs after a build.
- `.gitattributes` forces LF line endings except for `*.cmd` and `*.bat`.
