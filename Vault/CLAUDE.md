# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this directory is

A Vault secret client in two languages, plus reference material. It isn't a git repo. `README.md` covers the
modes, settings, and API.

- `dotnet/`: .NET 8 library `Fis.Foundation.Vault`, xUnit tests, and three POCs (web, Windows service,
  Linux/systemd).
- `java/`: Maven multi-module build, Java 8 target. It contains the `vault-client` library and the
  `poc-tomcat` (Tomcat 9, javax), `poc-windows`, and `poc-linux` POCs.
- `deploy/`: Vault policies, AppRole setup, Vault Agent/Proxy configs, and pipeline snippets.
- Root `*.cs` and `Snowflake/`: the original FIS VaultSharp sample, kept as **reference only**. It isn't
  built, so don't edit it. These files are Windows-1252, which means the Edit/Write tools refuse them.
- `vault-session-notes.md`: background on secret zero, cert auth vs AppRole, and the review of the team's
  earlier reference code.

## Commands

```
dotnet build dotnet\Vault.Client.sln
dotnet test dotnet\tests\Vault.Client.Tests
dotnet test dotnet\tests\Vault.Client.Tests --filter "FullyQualifiedName~<TestName>"
mvn -f java\pom.xml verify
mvn -f java\pom.xml -pl vault-client test -Dtest=<Class>#<method>
```

Java runs on Temurin JDK 8 (machine `JAVA_HOME`) and Maven 3.9.16 (`C:\Users\joeka\tools\apache-maven-3.9.16`,
on the user PATH). Java 8 is what the work apps run on, so tests run on a real Java 8 runtime. Integration tests skip themselves unless the `VAULT_*` variables listed in README.md are set.

## Hard constraints

- **No runtime dependencies in either library.** That means no Vault client packages, no JSON libraries,
  and no NuGet/Maven references outside tests. Java parses JSON with `com.fis.foundation.vault.internal.Json`
  and uses `HttpURLConnection`, because Java 8 has no `java.net.http`. Test frameworks and POC hosting
  packages are the only exceptions. Ask before adding any dependency.
- **Java code must compile on Java 8.** Don't use `var`, `List.of`, records, `java.net.http`, or text blocks.
  The parent POM sets `release 8` when built on JDK 9+.
- **The two languages mirror each other.** The same classes, settings names, env vars, exception types,
  and test cases exist on both sides. A behavior change in one needs the same change in the other.
- **No secret material anywhere.** Exception messages carry only paths and status codes. Tests assert this
  using fixture values (`*-FIXTURE`, `LEAK-CANARY`). Sample configs use `REPLACE-*` placeholders.

## Design points that span files

- The flow is `SecretClient` → `TokenManager` → `AppRoleWrappedAuthenticator` / agent (no authenticator)
  → `VaultHttp`.
- **401/403 recovery** lives in `SecretClient.ReadWithRecovery`. On an auth failure it calls
  `lookup-self`. If the token is valid, the failure is a `PermissionDenied` (policy) and isn't retried.
  Otherwise it resets and retries once, and a second 403 also becomes `PermissionDenied`. In AppRole mode
  the reset fails, because the wrapped secret_id is single-use.
- `VaultHttp.send(..., retryable)` takes `false` for unwrap, login, and revoke. In Java, every POST sends a
  body (`{}` if empty) so `setFixedLengthStreamingMode` stops `HttpURLConnection` from silently resending it.
- `TokenManager.ShouldRenew` sets the renewal policy. The Java version (`shouldRenew`) is a 2/3-of-lease
  rule. Keep both languages in step.
- Agent mode never sends or revokes a token.
