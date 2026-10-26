# Using the client

This page covers everything an application needs: adding the dependency, configuration, the two auth modes, the API,
errors, caching and key handling. Source: `java/ibsmas-vault/src/main/java/ibsmas/vault/`.

## 1. Add the dependency

```xml
<dependency>
  <groupId>ibsmas</groupId>
  <artifactId>ibsmas-vault</artifactId>
  <version>0.1.0</version>
</dependency>
```

- **Requirements:** Java 8 or later.
- **No transitive dependencies:** the HTTP layer uses `HttpURLConnection` and the JSON parser is built in, so the
  library adds nothing else to your classpath.
- **Where to get it:** it isn't published to a shared repository yet. Run `mvn install` from `ibsmas-vault/java` to
  put it in your local `~/.m2`.

## 2. Create one client per process

```java
import ibsmas.vault.VaultConfig;
import ibsmas.vault.VaultSecretClient;

VaultSecretClient vault = new VaultSecretClient(VaultConfig.fromEnvironment());
// ... share it across threads for the life of the process ...
vault.close();   // on shutdown: revokes the token (AppRole mode) and clears caches
```

- **Thread safety:** the client is thread-safe. Create **one per process, or one per web application**, and share
  it.
- **No network call at construction:** the first read logs in.
- **Close it on shutdown.** In AppRole mode, `close()` revokes the token, so it doesn't outlive the process. If it
  isn't closed, the token stays valid until its TTL runs out. The samples close it from a JVM shutdown hook
  (`PocDaemon`) or a `ServletContextListener` (`VaultContextListener`).
- **Interfaces:** `VaultSecretClient` implements both `SecretClient` (KV) and `TransitClient` (Transit). Declare
  fields as the interface you use.

## 3. Configuration

`VaultConfig.fromEnvironment()` loads in this order:

1. The properties file named by `VAULT_CONFIG_FILE`, if it's set and the file exists.
2. `VAULT_*` environment variables, which override the file.
3. Validation. Any problem throws `IllegalStateException` at startup, not at first use.

| Env variable | Property (`vault.` prefix optional) | Default | Meaning |
|---|---|---|---|
| `VAULT_ADDR` | `address` | *(required)* | Vault, or the local proxy, for example `https://vault.example:8200` or `http://127.0.0.1:8100` |
| `VAULT_NAMESPACE` | `namespace` | none | Vault Enterprise namespace, sent as `X-Vault-Namespace` |
| `VAULT_KV_MOUNT` | `kvMount` | `secret` | KV v2 mount |
| `VAULT_TRANSIT_MOUNT` | `transitMount` | `transit` | Transit mount |
| `VAULT_AUTH_MODE` | `authMode` | `approle-wrapped` | `approle-wrapped` or `agent` (see §4) |
| `VAULT_AUTH_MOUNT` | `authMount` | `approle` | AppRole auth mount |
| `VAULT_ROLE_ID` | `roleId` | none | AppRole role_id. Required in AppRole mode. |
| `VAULT_WRAPPED_SECRET_ID_FILE` | `wrappedSecretIdFile` | none | File holding the wrapped secret_id. Required in AppRole mode. |
| `VAULT_CACERT` | `caCertificateFile` | JVM trust store | PEM bundle of the CA(s) that issued Vault's certificate |
| `VAULT_CACHE_TTL_SECONDS` | `cacheTtlSeconds` | `300` | How long reads stay cached. `0` turns caching off. |
| `VAULT_MAX_RETRIES` | `maxRetries` | `3` | Retries for transient failures |
| — | `connectTimeoutSeconds` | `5` | TCP connect timeout |
| — | `requestTimeoutSeconds` | `15` | Read timeout per request |

Example `vault.properties`:

```properties
vault.address=https://vault.example:8200
vault.kvMount=kv-opac
vault.transitMount=transit-opac
vault.roleId=00000000-0000-0000-0000-000000000000
vault.wrappedSecretIdFile=/run/orders/wrapped-secret-id
vault.caCertificateFile=/etc/pki/ca-trust/source/anchors/internal-ca.pem
```

**Rules the validation enforces:**
- **No secret_id as a setting.** A `secretId` or `secret_id` property, or a `VAULT_SECRET_ID` variable, is rejected.
  A secret_id only ever arrives wrapped, in a file.
- **No unknown property names.** A typo fails at startup instead of being silently ignored.
- **https for AppRole.** AppRole mode refuses `http` unless Vault is on loopback, so credentials never cross the
  network in clear text.

## 4. Auth modes

### `approle-wrapped` (the default)

At deploy time, a pipeline writes a **response-wrapped** secret_id to the file `VAULT_WRAPPED_SECRET_ID_FILE`.
On first use, the client then:
1. reads the file and **deletes it**, even if the read fails;
2. calls `sys/wrapping/unwrap` to get the secret_id;
3. logs in once with `role_id` and the secret_id.

A wrapping token can be unwrapped only once. If the unwrap fails, someone else may have used it first, so treat it
as a possible interception (the error message says so).

The token is renewed once two-thirds of its TTL has passed. **The client can't log in again**, because the secret_id
has been spent. When the token reaches the role's `token_max_ttl`, or is revoked, the client throws
`VaultAuthException` and the service needs a restart with a newly delivered wrapped secret_id. For a long-running
service, either:
- configure the role with `token_period` (a periodic token, renewable without limit), or
- use the proxy mode below.

*(The periodic-token behaviour is inferred from the code, not tested.)*

**The file** can hold either the raw wrapping token or the JSON from `vault write -wrap-ttl=... -format=json`.

### `agent` (through a local Vault Proxy or Vault Agent)

The client sends no token. A process on the same host, Vault Proxy (recommended) or Vault Agent, authenticates
by itself and adds its own token to each request (`use_auto_auth_token = "force"`).

- **No credential in the app:** the app holds none and never needs to log in again.
- **Restart-safe:** restarts and Tomcat redeploys need nothing delivered.
- **What `close()` does:** it never revokes the proxy's token.

```sh
VAULT_AUTH_MODE=agent
VAULT_ADDR=http://127.0.0.1:8100    # keep the proxy on loopback
```

The mode is called `agent` for historical reasons, and works the same with **Vault Proxy**. Agent's API-proxy feature
is being deprecated in favour of `vault proxy`: Vault 1.18.3's Agent prints that warning at startup. The client's
integration tests pass through both (see [Local Vault](03-local-vault.md)). Setting up the proxy is covered in
[Open questions](06-open-questions.md) §4.

## 5. Reading secrets (KV v2)

Paths are relative to the KV mount. Use the same paths in every tier and let configuration pick the tier (see
[Environments](05-environments-and-production.md)).

```java
Map<String, String> all = vault.getSecret("ibsmas/orders/snowflake");        // every key at the path
String user = vault.getSecret("ibsmas/orders/snowflake", "user");            // one key
```

- **Return value:** an unmodifiable map. A non-string value is returned as JSON text.
- **Missing path or key:** throws `VaultNotFoundException`.
- **Paths are validated:** a leading or trailing `/` is trimmed, and empty, `.` or `..` segments are rejected.

### Keys

```java
byte[] key = vault.getKey("ibsmas/orders/keys", "aes256", 32);   // base64 in Vault, length-checked
try {
    // use the key
} finally {
    Arrays.fill(key, (byte) 0);                                    // you own the array; wipe it
}
```

`getKey` decodes base64 and checks the length. On a wrong length it wipes the bytes before throwing.

### Versions (KV key rotation)

When a key stored in KV is rotated (a new version written), data encrypted under the old key still has to be
decrypted. Store the version next to the ciphertext, and read that version back later:

```java
int version = vault.getCurrentVersion("ibsmas/orders/keys");             // store this with the ciphertext
byte[] oldKey = vault.getKey("ibsmas/orders/keys", "aes256", 32, version); // later: read that exact version
Map<String, String> v2 = vault.getSecretVersion("ibsmas/orders/keys", 2);
```

A rotation shows up in `getCurrentVersion` when the cache entry expires, or straight away after
`vault.invalidate(path)`.

## 6. Encryption as a service (Transit)

With Transit, **the key never leaves Vault**. Prefer it to KV keys for application data encryption.

```java
String ct = vault.encrypt("ibsmas-orders", plaintext);          // "vault:v3:..."
byte[] pt = vault.decrypt("ibsmas-orders", ct);                  // you wipe pt
String fresh = vault.rewrap("ibsmas-orders", ct);                // re-encrypt with the newest key version
String maybe = vault.rewrapIfStale("ibsmas-orders", ct);         // rewrap only if made by an older version
int latest = vault.getLatestKeyVersion("ibsmas-orders");         // cached for the cache TTL
vault.invalidateKeyVersion("ibsmas-orders");                     // after a rotation, see it now
int v = TransitClient.ciphertextVersion(ct);                     // 3, no Vault call
```

- **Rolling data forward after a rotation:** call `rewrapIfStale` whenever you read a stored ciphertext, and save
  the result if it changed. Vault does the re-encryption internally, so the plaintext never reaches the app.
- **Picking up a rotation sooner:** `getLatestKeyVersion` is cached, so a rotation is seen after up to the cache TTL.
  Call `invalidateKeyVersion` to see it immediately.
- **Key names** are a single path segment: `a/b` is rejected.
- **Plaintext limitation:** plaintext travels base64-encoded inside a Java `String`, which can't be wiped. This is a
  JDK limitation, the same as for `SecretKeySpec`.

## 7. Errors

All client errors are unchecked and extend `VaultException`. Messages name paths and keys, never values.

| Exception | When | What to do |
|---|---|---|
| `VaultNotFoundException` | 404, missing key, deleted or destroyed version | Configuration or data problem; don't retry |
| `VaultPermissionDeniedException` | The policy doesn't allow the path (confirmed with `lookup-self`) | Fix the policy; don't retry |
| `VaultAuthException` | Login, unwrap or token failure; secret_id already spent | Redeliver the secret_id and restart, or check the proxy |
| `VaultSealedException` | 503: sealed, standby or maintenance | Retry later; alert Ops |
| `VaultUnavailableException` | Network error, or 429/500/502/504 after retries | Retry later; check connectivity |
| `VaultException` | Anything else (bad base64, wrong key length, malformed response) | Treat as a bug or data error |

**How the client handles 401 and 403:** Vault returns 403 both for a dead token and for a policy denial. After a 401
or 403, the client calls `auth/token/lookup-self`:
- **Token still valid:** it's a policy denial, so `VaultPermissionDeniedException` is thrown.
- **Token not valid:** the client logs in again (if it can) and retries once.

**Retries:**
- **Which failures:** network errors and 429/500/502/503/504.
- **How often:** up to `maxRetries` times, with exponential backoff from 250 ms (capped at 10 s) plus jitter.
- **Never retried:** login and unwrap. Each spends a single-use value, so a retry after a lost response would fail
  anyway. POST bodies use fixed-length streaming so the JDK can't re-send them silently.

## 8. Caching

- **KV reads:** cached per path for `cacheTtlSeconds` (default 300).
- **Versioned reads:** cached per `path@vN`.
- **Transit `latest_version`:** cached per key.
- **Clearing a KV path:** `invalidate(path)` clears it and all its versions.
- **Clearing a Transit key:** `invalidateKeyVersion(key)` clears that key's cached version.
- **Turning caching off:** set `cacheTtlSeconds=0`, which sends every read to Vault.

Values cached in memory are as sensitive as the secrets themselves. Keep the TTL short for frequently rotated secrets.

## 9. TLS

- **Version:** TLS 1.2 only, on every connection. The client pins it explicitly, because on Java 8 a context named
  "TLSv1.2" still allows 1.0 and 1.1.
- **Trust:** set `VAULT_CACERT` to a PEM bundle to trust your internal CA. Without it, the JVM's default trust store
  is used.
- **Host name checks:** always on.
- **Redirects:** never followed.
- **Tested:** the TLS path passed against a TLS-only Vault in [On-prem Docker Vault](04-on-prem-docker-vault.md).

## 10. Rules for application code

- **Values stay out of logs.** Never log, print or put secret values or key bytes into exception messages.
- **Wipe key material.** Clear byte arrays from `getKey` and `decrypt` with `Arrays.fill` when done.
- **Tier-independent paths.** Use the same KV paths and Transit key names in every tier, and choose the tier in
  configuration (mount, namespace, role).
- **No secrets in config.** Don't put a secret_id or token in configuration files, environment variables,
  container images or source control.
