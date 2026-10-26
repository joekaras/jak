# Local Vault for development

Use this to run the integration tests, or a sample, against a **real** Vault on your own machine. The unit tests use
an in-process stub and need nothing. Everything here is throwaway:
- the server runs in dev mode, in memory, with all data lost on stop;
- it listens on `127.0.0.1` only;
- the values it uses are random and never printed.

## One command

From `ibsmas-vault/java`:

```sh
bash it/run.sh            # extra arguments go to mvn, for example: bash it/run.sh -q
```

It runs these steps, and tears everything down even if the tests fail:

1. Starts **Vault 1.18.3** in dev mode on `127.0.0.1:8200`, and **Vault Agent** with AppRole auto-auth on
   `127.0.0.1:8100`.
2. Seeds the test data:
   - KV v2 `secret/poc/snowflake` (credential) and `secret/poc/keys` (`aes256`, 32 random bytes);
   - Transit key `orders`;
   - policy `it-read`: read-only on those, plus `rotate` on the Transit key for the rotation test;
   - AppRole role `it`, whose secret_id can be used **once** (like production delivery), and role `it-agent`, whose
     secret_id is reusable so the Agent can log in again.
3. Writes a response-wrapped secret_id for role `it`.
4. Runs all four `IntegrationTest` cases: AppRole read, Agent read, Transit round trip, Transit rotation.

Expected: `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`.

## Docker or no Docker

| Mode | Used when | Needs |
|---|---|---|
| `docker` | Docker answers (`docker info`) | Docker Desktop or Engine; pulls `hashicorp/vault:1.18.3` |
| `native` | Docker doesn't answer | A `vault` binary on PATH, or `VAULT_BIN=/path/to/vault` |

Force one with `IT_MODE=docker` or `IT_MODE=native`.

**Both modes are pinned to Vault 1.18.3** (`VAULT_VERSION` in `it/lib.sh`). In native mode, a binary of any other
version is refused with the command that installs the right one. Override the pin with `IT_VAULT_VERSION`.

**Installing the binary without Docker (Windows):**

```powershell
winget install Hashicorp.Vault --version 1.18.3
winget pin add --id Hashicorp.Vault --version 1.18.3     # stop `winget upgrade --all` moving it
```

Open a new terminal afterwards so `vault` is on PATH. If winget is blocked, download the zip from
`https://releases.hashicorp.com/vault/1.18.3/`, check it against the published `SHA256SUMS`, and point `VAULT_BIN` at
`vault.exe`. On Linux, use HashiCorp's package repository or the same zip.

*Tested:*
- Docker mode on Windows (Docker Desktop);
- native mode on Windows (winget, Vault 1.18.3 and 2.2.0).

Native mode on Linux and macOS isn't tested yet. It stops processes with `kill` instead of `taskkill`.

## Step by step

Use these to keep a Vault running while you work on a sample:

```sh
bash it/setup.sh       # start and seed; prints "mode: ... server: 200 agent lookup-self: 200"
bash it/wrap.sh        # write a fresh wrapped secret_id to it/.work/wrapped.json (each test run spends one)
bash it/teardown.sh    # stop everything and delete it/.work/
```

While it's up, point the client at it.

**Proxy/Agent mode:**

```sh
VAULT_AUTH_MODE=agent  VAULT_ADDR=http://127.0.0.1:8100  VAULT_KV_MOUNT=secret
```

**AppRole mode:**

```sh
VAULT_ADDR=http://127.0.0.1:8200  VAULT_KV_MOUNT=secret
VAULT_ROLE_ID=$(cat it/.work/role-id)
VAULT_WRAPPED_SECRET_ID_FILE=it/.work/wrapped.json
```

AppRole over `http` is allowed only because the server is on loopback. Everywhere else the client requires `https`.

## Generated files

`it/.work/` holds the dev root token, the role IDs, the Agent's secret_id and the wrapped secret_id. It's in
`.gitignore`, and `teardown.sh` deletes it. They're throwaway values, but treat the folder as secret anyway.

## Running the integration tests against another Vault

`IntegrationTest` runs only the cases whose variables are set. The rest are skipped.

| Case | Needs |
|---|---|
| AppRole read | `VAULT_ADDR`, `VAULT_ROLE_ID`, `VAULT_WRAPPED_SECRET_ID_FILE`, `VAULT_TEST_PATH` (+ `VAULT_KV_MOUNT`, `VAULT_CACERT`, `VAULT_NAMESPACE` as needed) |
| Agent/Proxy read | `VAULT_AGENT_ADDR`, `VAULT_TEST_PATH` |
| Transit round trip | `VAULT_AGENT_ADDR`, `VAULT_TRANSIT_TEST_KEY` |
| Transit rotation | The two above, plus `VAULT_TRANSIT_ROTATE=true` and `rotate` permission on the key |

**The rotation test permanently bumps the key version.** Don't enable it against a shared Vault unless the key is a
test key.

```sh
mvn -pl ibsmas-vault test -Dtest=IntegrationTest
```

## Troubleshooting

| Symptom | Cause |
|---|---|
| `Could not find or load main class org.codehaus.plexus.classworlds.launcher.Launcher` | `MSYS_NO_PATHCONV=1` is exported in Git Bash. The scripts set it per command only; don't export it. |
| `A local Vault is already set up; run teardown.sh first.` | A previous run left `it/.work/` behind. Run `bash it/teardown.sh`. |
| `vault is X but the tests are pinned to 1.18.3` | Install 1.18.3 (see above), or set `IT_VAULT_VERSION`. |
| Native mode: `vault.exe` still running after a crash | `taskkill /F /IM vault.exe`, then `bash it/teardown.sh`. |
| Tests report `Skipped` | Their environment variables aren't set (see the table above). |
