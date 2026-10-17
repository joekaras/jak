# Vault secret client (Java 8 and .NET 8)

Reads credentials and keys from HashiCorp Vault (Enterprise namespaces, KV v2) without any Vault client
package. Java uses only the JDK; the C# library uses only what ships with .NET 8.

## Secret zero: the two supported modes

| Mode | What the app holds | Who solves secret zero | Restart behavior |
|---|---|---|---|
| `agent` (recommended for Tomcat and Windows) | Nothing. Calls Vault Agent/Proxy on `127.0.0.1:8100`, which injects its own token. | The agent, once per server. | App restarts freely. |
| `approle-wrapped` | role_id (not secret) and, briefly, a wrapping token in a file it deletes after reading. | The deploy pipeline mints a single-use, response-wrapped secret_id per deploy. | Each start needs a newly delivered file. |

Neither mode removes secret zero: the agent still needs its own secret_id. TLS cert auth with
non-exportable machine keys would remove it (see `vault-session-notes.md` §2); it isn't implemented here.

What `approle-wrapped` guarantees:
- The secret_id never appears in config. Both libraries refuse `secretId` settings and `VAULT_SECRET_ID`.
- The wrapped file is deleted on read. If someone unwrapped it first, the unwrap fails and the client
  reports a possible interception.
- Unwrap and login are never retried, and the Java POST runs in fixed-length streaming mode so
  `HttpURLConnection` can't silently resend it.
- With a periodic token (`token_period`, see `deploy/approle-setup.sh`) the app renews forever without a
  second login. Without one, the app stops working at the token's max TTL until it is restarted with a new
  file.

## Settings

| Env var | JSON (C#) / properties (Java, optional `vault.` prefix) | Notes |
|---|---|---|
| `VAULT_ADDR` | `address` | Agent listener in agent mode. AppRole over http is refused unless loopback. |
| `VAULT_NAMESPACE` | `namespace` | Sent as `X-Vault-Namespace`. |
| `VAULT_KV_MOUNT` | `kvMount` | Default `secret`. |
| `VAULT_AUTH_MODE` | `authMode` | `approle-wrapped` (default) or `agent`. |
| `VAULT_AUTH_MOUNT` | `authMount` | Default `approle`. |
| `VAULT_ROLE_ID` | `roleId` | AppRole only. |
| `VAULT_WRAPPED_SECRET_ID_FILE` | `wrappedSecretIdFile` | Raw wrapping token or `vault write -format=json` output. |
| `VAULT_CACERT` | `caCertificateFile` | PEM bundle of the internal CA. HTTPS to Vault is pinned to TLS 1.2 in both languages, with or without this. |
| `VAULT_CACHE_TTL_SECONDS` | `cacheTtlSeconds` | Default 300; 0 disables caching. |
| `VAULT_MAX_RETRIES` | `maxRetries` | Default 3; applies to IO errors, 429, and 5xx only. |
| `VAULT_CONFIG_FILE` | - | Path of the JSON (C#) or properties (Java) file. Env vars override it. |

## API (same shape in both languages)

`getSecret(path)` returns a map, `getSecret(path, key)` returns a string, `getKey(path, key, 32)` returns
bytes (the caller zeroes them), and there's `invalidate(path)` and `close()`/`DisposeAsync()` (which
revokes the token in AppRole mode). Errors:

- `VaultAuthException`: no usable token.
- `VaultPermissionDeniedException`: the token is valid but its policy says no. The client checks with
  `lookup-self` because Vault returns 403 for both cases.
- `VaultNotFoundException`, `VaultSealedException` (503), and `VaultUnavailableException`.

Messages carry paths and status codes, never values or tokens.

## Build and test

```
dotnet build dotnet\Vault.Client.sln
dotnet test dotnet\tests\Vault.Client.Tests
dotnet test dotnet\tests\Vault.Client.Tests --filter "FullyQualifiedName~Cache_serves"   # one test

mvn -f java\pom.xml verify                                                                 # needs JDK 8+ and Maven
mvn -f java\pom.xml -pl vault-client test -Dtest=VaultSecretClientTest#appRoleUnwrapsLogsInAndReadsSecret
```

Integration tests against the shared dev Vault run only when their env vars are set:
- **AppRole:** `VAULT_ADDR`, `VAULT_NAMESPACE`, `VAULT_KV_MOUNT`, `VAULT_ROLE_ID`,
  `VAULT_WRAPPED_SECRET_ID_FILE`, `VAULT_TEST_PATH`. Each run consumes the file.
- **Agent:** `VAULT_AGENT_ADDR`, `VAULT_KV_MOUNT`, `VAULT_TEST_PATH`.

## Layout

- `dotnet/src/Vault.Client`: the library (`Fis.Foundation.Vault`).
- `dotnet/tests/Vault.Client.Tests`: xUnit tests against an in-process fake Vault.
- `dotnet/samples`: `Poc.Web` (ASP.NET Core, `GET /vault/health`), `Poc.WindowsService`, and `Poc.Linux`
  (systemd), with shared check logic in `Poc.Common`.
- `java/vault-client`: the library (`com.fis.foundation.vault`), with a JUnit 5 fake Vault on the JDK HTTP
  server.
- `java/poc-tomcat` (Tomcat 9 WAR), `java/poc-windows` (WinSW), `java/poc-linux` (systemd), and
  `java/poc-common`.
- `deploy/`: Vault policies, AppRole setup, Vault Agent/Proxy configs, and pipeline snippets that deliver
  wrapped secret_ids.

Every POC reads a Snowflake-style credential at `POC_CREDENTIAL_PATH` and a base64 AES-256 key at
`POC_KEY_PATH`/`POC_KEY_NAME`, does an AES-GCM round trip, and reports only key names and a success flag.

The original FIS VaultSharp files in this folder's root are kept as reference and aren't used.
