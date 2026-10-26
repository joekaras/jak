# Samples

Three samples show the client in the three ways IBSMAS apps run: a Linux service, a Windows service and a Tomcat web
app. They all run the same check, `PocCheck` in `java/poc-common`:

1. Reads a credential (Snowflake-style) from KV and reports its **key names** only.
2. Reads a 32-byte AES key, proves it with an AES-GCM encrypt and decrypt, and wipes it.
3. Reports `{"credentialKeys":[...],"aesGcmRoundTrip":true}`. Values are never printed.

| Setting | Default | Meaning |
|---|---|---|
| `POC_CREDENTIAL_PATH` | `poc/snowflake` | KV path of the credential |
| `POC_KEY_PATH` | `poc/keys` | KV path of the AES key |
| `POC_KEY_NAME` | `aes256` | Key name within that path (base64, 32 bytes) |
| `POC_INTERVAL_SECONDS` | `300` | Linux and Windows services: how often the check repeats |

Plus the client's own `VAULT_*` settings ([Using the client](01-using-the-client.md) §3).

Build all three from `ibsmas-vault/java`:

```sh
mvn install
# poc-linux/target/vault-poc-linux.jar       runnable (shaded) jar
# poc-windows/target/vault-poc-windows.jar   runnable (shaded) jar
# poc-tomcat/target/vault-poc.war            WAR for Tomcat 9
```

To try any sample against a throwaway Vault, start one with `bash it/setup.sh` (see
[Local Vault](03-local-vault.md)). It seeds `poc/snowflake` and `poc/keys` at exactly the default paths above. Tear
it down with `bash it/teardown.sh`.

---

## Linux service (`poc-linux`)

**Entry point:** `ibsmas.vault.poc.linux.Main`, which runs `PocDaemon`:
- runs the check at startup and then every `POC_INTERVAL_SECONDS`;
- logs one line per check;
- when systemd sends SIGTERM, the JVM shutdown hook closes the client, which revokes the token in AppRole mode.

**Try it locally** (Agent or Proxy mode, after `bash it/setup.sh`):

```sh
VAULT_AUTH_MODE=agent VAULT_ADDR=http://127.0.0.1:8100 VAULT_KV_MOUNT=secret \
  java -jar poc-linux/target/vault-poc-linux.jar
# INFO: vault-poc-linux Vault check OK: {"credentialKeys":["account","password","user"],"aesGcmRoundTrip":true}
```

*Tested:* this exact output came from the jar against the local Vault and Agent.

**Install as a service:** use `poc-linux/service/vault-poc-java.service`.

1. Copy the jar to `/opt/vault-poc-java/` and the unit file to `/etc/systemd/system/`. Replace the `REPLACE-...`
   values: address, namespace, KV mount, role_id, CA file.
2. Create the service user: `useradd --system vault-poc`.
3. **AppRole mode, at every deploy:** the pipeline writes a fresh wrapped secret_id to
   `/run/vault-poc-java/wrapped-secret-id` (mode `0400`, owner `vault-poc`), then runs
   `systemctl restart vault-poc-java`.
4. **Proxy mode instead:** set `VAULT_AUTH_MODE=agent` and `VAULT_ADDR=http://127.0.0.1:8100`, and remove the AppRole
   variables.

**Hardening already in the unit:**
- a dedicated user;
- `NoNewPrivileges`, `ProtectSystem=strict`, `ProtectHome`, `PrivateTmp`;
- `LimitCORE=0`, so secrets don't end up in core dumps;
- a private runtime directory with mode `0700`;
- `SuccessExitStatus=143`, so the JVM's normal SIGTERM exit isn't reported as a failure.

*Not tested under systemd.*

---

## Windows service (`poc-windows`)

**Entry point:** `ibsmas.vault.poc.windows.Main`, the same `PocDaemon` loop. WinSW stops the JVM with Ctrl+C
semantics, which runs the shutdown hook.

**Try it locally:** the same as Linux, with `vault-poc-windows.jar`.

**Install as a service** with [WinSW](https://github.com/winsw/winsw) and `poc-windows/service/vault-poc.xml`:

1. Copy the jar and `vault-poc.xml` to `C:\apps\vault-poc\`.
2. Copy `WinSW-x64.exe` there as `vault-poc.exe`.
3. Replace the `REPLACE-...` values in the XML.
4. Run `vault-poc.exe install` (elevated), then `vault-poc.exe start`.
5. Run it as a dedicated low-privilege account.
6. In AppRole mode, restrict the folder holding the wrapped secret_id so only that account can read it.

**Mode:** the XML defaults to **Agent/Proxy mode** (`VAULT_AUTH_MODE=agent`, `VAULT_ADDR=http://127.0.0.1:8100`).
The AppRole alternative is in a comment. On Windows, Proxy mode avoids having a pipeline write secret files on the
server at every restart. For running Vault Proxy itself as a Windows service, see
[Open questions](06-open-questions.md) §4.

*Not tested under WinSW.*

---

## Tomcat 9 web app (`poc-tomcat`)

- **`VaultContextListener`:** creates one client when the web app starts, stores it as a `ServletContext` attribute
  (`ibsmas.vault.SecretClient`), and closes it on undeploy.
- **`VaultHealthServlet`:** serves `GET /vault-poc/vault/health`:
  - on success, the `PocCheck` JSON;
  - on a `VaultException`, `503` with `{"title": "<ExceptionName>", "detail": "<message>"}`;
  - in both cases, with `Cache-Control: no-store`.

**Configuration:**
- **Variables:** set `VAULT_*` and `POC_*` in Tomcat's `bin/setenv.sh` (or `setenv.bat`), or in the service
  definition.
- **Properties file (optional):** name it with the `vault.config` context parameter or `VAULT_CONFIG_FILE`. The
  variables override the file.

```sh
# bin/setenv.sh
export VAULT_AUTH_MODE=agent
export VAULT_ADDR=http://127.0.0.1:8100
export VAULT_KV_MOUNT=kv-opac
```

**Deploy:** drop `vault-poc.war` into `webapps/`, then `curl http://localhost:8080/vault-poc/vault/health`.

**Use Proxy mode for Tomcat.** In AppRole mode, every redeploy and every restart of the web app needs a newly
delivered wrapped secret_id, because the previous one is already spent.

**Tomcat version:** Tomcat 9 provides `javax.servlet`. Tomcat 10 and later need `jakarta.servlet`, which means
changing the imports and the `provided` dependency in `poc-tomcat/pom.xml`.

*Not tested in Tomcat.*

---

## Using a sample as a template for a real app

Copy the lifecycle, not the check:
- **Linux and Windows services:** one client per process, closed in a shutdown hook.
- **Web apps:** one client per web app, closed in `contextDestroyed`.

Replace `PocCheck` with your own reads, and keep the rule that only key names and status are logged.
