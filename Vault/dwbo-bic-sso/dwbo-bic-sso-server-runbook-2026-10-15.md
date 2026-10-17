# Service Delivery Runbook: dwbo-bic-sso (SsoRedirector) on the Windows Tomcat host (2026-10-15)

Audience: the Service Delivery operator with local administrator rights on the SYSTEM Windows host(s) that run the `SsoRedirector` WAR under Tomcat. The SYSTEM tiers are `BOE System` and `CRD System`.

The runbook has five parts:

- Section 1 describes what runs on the host.
- Section 2 covers routine operation.
- Section 3 deploys the `develop` remediation to SYSTEM.
- Section 4 is troubleshooting.
- The final sections cover rollback and what to record on the change.

Testing is ST-01 to ST-05 in [dwbo-bic-sso-system-test-plan.md](dwbo-bic-sso-system-test-plan.md).

Change: `IBSDA-94962` (`f010`, SNOW `AVIT32772748`) and `IBSDA-94956` (`f024`, SNOW `AVIT32772774`), both on `develop`. `IBSDA-96108` (`f091`, SNOW `AVIT32772818`) is a build-only fix already on `master` and needs nothing on the host. SYSTEM only. Nothing here promotes to `master`. `f010` and `f024` stay **unfixed** for reporting until `develop` is promoted to `master` and Mythos rescans it.

Sources for code and config references below (local refs, not re-fetched on 2026-10-15):

- `c:\1mythos\9946\dwbo-bic-sso`, `origin/develop` = `a2cc9b1` (PR #39, which contains PR #38).
- `c:\1mythos\9946\dwbo-bic-sso-config`, `develop` = `efb9ae9`.

> **Deployment is currently blocked.** Prerequisite OP-2 needs `Mac4ApplId` in the `BOE System` and `CRD System` `bicsso_config.properties`. As of 2026-10-06 it is absent from all 10 environments, and its value is NOT FOUND (open item O1). Deploying the code without it denies every MAC4 login. Sections 1, 2, and 4 apply now; Section 3 waits for O1.

## Rules for the whole window

- Never display, paste, or attach the trusted-auth secret, the Vault Proxy secret_id, a Vault token, `BO4SecretKey`, a SLAPI private key, or a logon token. Commands here print names, presence, or hashes only.
- No secret is passed to Tomcat by environment variable or `-D` option any more (revised 2026-10-17). Per the key storage standard §8.4, Harness deploys only: it never writes a secret or the Proxy secret_id. The only secret on the host besides the SLAPI keys is the Vault Proxy secret_id file under `C:\vault\secrets`. Never copy it into a `.bat`, `setenv.bat`, or ticket.
- Treat `Dw_Bic_Redirect_Page.log` as sensitive. The shipped root level is `debug` (`log4j2.xml`), and at that level `index.jsp:80` logs the full federated SLAPI token. Share sanitized excerpts only.
- If the Tomcat instance also serves the BI launch pad (the BusinessObjects-bundled Tomcat), restarting it is a BI outage for that tier. Schedule it that way.
- If any step's **Pass** line is not met, stop and call the change owner (Joe Karas, `e1015974`).

---

## 1. What runs on the host

| Item | Value | Evidence |
|---|---|---|
| Application | `SsoRedirector`, a WAR (`2.0.0-SNAPSHOT`), plain JSP with no framework. The welcome file is `index.jsp`; errors go to `fatalErrorPage.jsp`. Session timeout is 30 minutes. | `pom.xml:12-14`, `WEB-INF/web.xml` |
| Java | Bytecode `1.8` | `pom.xml` `<properties>` |
| Container | External Tomcat; `log4j2.xml` uses `${sys:catalina.base}`. The Tomcat version, service name, port, and context path are NOT FOUND in either repository; discover them in 2.2. | `log4j2.xml:7-8` |
| Environment config | `properties-boe-system.jar` / `properties-crd-system.jar`, built from `dwbo-bic-sso-config`. Each contains `bicsso_config.properties`, `slapi_config.properties`, `Slapi.xml`, and `log4j2.xml`. They are loaded through the webapp classloader, so the jar must be in `WEB-INF\lib` or `%CATALINA_BASE%\lib`. The properties are cached in a static field; a config change needs a restart. | `BOE System/pom.xml`, `EncryptedProperties.java:60-61`, `BicssoConstants.java:5-6` |
| SLAPI key files | `D:\slapi\keys\BLP_SSO-SYS_privateKey.pkcs8.der` and `D:\slapi\keys\DIH_SSO-RDY_pubCert.pem`. All three channels use the same pair. They are loaded once, when the class is first initialised. | `BOE System` and `CRD System` `slapi_config.properties`, `SlapiTokenHandler.java:65-80` |
| Logs | `%CATALINA_BASE%\logs\Dw_Bic_Redirect_Page.log`, rolling at 25 MB and daily, 10 files kept. Also console output (Tomcat stdout). | `log4j2.xml:7-20` |

The SYSTEM config points at the **RDY** public certificate (`DIH_SSO-RDY_pubCert.pem`). That predates this change and the change does not touch it. Record it; do not change it.

### Configuration keys

These key names appear in `BOE System` / `CRD System` `bicsso_config.properties`:

- `SSOMode`: `InDirect` on SYSTEM. `Direct` makes `index.jsp` refuse every request (`index.jsp:22-25`).
- `BO4SecretKeyVaultPath`: Vault KV path of the BOE trusted-authentication secret (key `value`). Replaces `BO4SecretKey`, the old KEK-encrypted ciphertext.
- `vault.address`, `vault.authMode`, `vault.namespace`, `vault.kvMount`: Vault client settings (the Proxy on `127.0.0.1:8100`, agent mode). Not secret. `vault.kvMount` must be set explicitly. The client defaults to `secret`, but the real mount may be `kv` (the current Snowflake client's code default); this is unconfirmed (standard Q14).
- `AdminBO4User`
- `CMS4Name`
- `BIC4ServerName`
- `BIC4LaunchPadPath`
- `MACSSO4LaunchPadPath`
- `MinimumPasswordLength`
- `entitltment.endpoint.url` (the misspelling is the real key name)
- `BoApplId`
- `CustomerMinerApplId`: present in `BOE System`, absent in `CRD System`. This also predates the change.
- `entitlement.exemption.list`

`Mac4ApplId` is new and required by PR #39.

### The SSO flow

The flow is implemented in `index.jsp`:

1. The caller sends `authtkn`, `keytkn`, and `channel` (`BO_LP_SSO`, `BO_CM_SSO`, or `BO_MAC4_SSO`).
2. The SLAPI token is decrypted, and the user ID is taken from it.
3. The entitlement SOAP check runs; the MAC4 branch has it only from PR #39 onward.
4. `LoginHelper.TokenLoginBO4` reads the trusted-auth secret from Vault through the local Proxy (cached for 5 minutes), logs in to the CMS as a trusted principal, and creates a logon token.
5. The browser auto-posts `ivsLogonToken` to the launch pad.

---

## 2. Routine operation

### 2.1 Variables (PowerShell, elevated)

```powershell
$Stamp = Get-Date -Format 'yyyyMMdd'
$RB    = "D:\FIS_change\bicsso_rollback_$Stamp"     # ACL-restricted
$Work  = "D:\FIS_change\bicsso_work_$Stamp"
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Get-PropKeys([string]$Jar, [string]$Entry) {
    $z = [IO.Compression.ZipFile]::OpenRead($Jar)
    try {
        $e = $z.GetEntry($Entry); if (-not $e) { return "MISSING ENTRY $Entry" }
        $r = New-Object IO.StreamReader($e.Open())
        try { while (($l = $r.ReadLine()) -ne $null) { if ($l -match '^\s*([^#!=:\s][^=:]*)\s*[=:]') { $Matches[1].Trim() } } } finally { $r.Dispose() }
    } finally { $z.Dispose() }
}
```

`Get-PropKeys` prints key names only.

### 2.2 Discover the instance (read-only; do this once per host and record it)

```powershell
Get-CimInstance Win32_Service | Where-Object { $_.PathName -match 'tomcat\d*w?\.exe|prunsrv' } |
    Select-Object Name, DisplayName, State, StartName, PathName
$Svc = '<service name from above>'
$Proc = 'HKLM:\SOFTWARE\Apache Software Foundation\Procrun 2.0', 'HKLM:\SOFTWARE\WOW6432Node\Apache Software Foundation\Procrun 2.0' |
    ForEach-Object { "$_\$Svc\Parameters\Java" } | Where-Object { Test-Path $_ } | Select-Object -First 1
(Get-ItemProperty $Proc).Options | Where-Object { $_ -match '^-D(catalina\.(base|home)|java\.io\.tmpdir)=' }
(Get-ItemProperty $Proc).Options | ForEach-Object { ($_ -split '=',2)[0] }      # option NAMES only
$CB = '<catalina.base from above>'
Get-ChildItem "$CB\webapps" -Directory | Where-Object { Select-String -Path "$($_.FullName)\WEB-INF\web.xml" -Pattern 'SsoRedirector' -Quiet -ErrorAction SilentlyContinue } |
    Select-Object FullName
Get-ChildItem "$CB\webapps", "$CB\conf\Catalina" -Recurse -Include 'SsoRedirector*.war', '*.xml' -ErrorAction SilentlyContinue |
    Where-Object { $_.Extension -eq '.war' -or (Select-String -Path $_.FullName -Pattern 'SsoRedirector' -Quiet) } | Select-Object FullName
Get-ChildItem $CB -Recurse -Filter 'properties-*.jar' -ErrorAction SilentlyContinue | Select-Object FullName
Select-String -Path "$CB\conf\server.xml" -Pattern '<Connector |VersionLoggerListener' | Select-Object LineNumber, Line
```

Record the following:

- `$Svc` and the account it runs as.
- `$CB`, the deployed WAR's file name, and the exploded directory (`$App`). The WAR name is the context path.
- The location of the config jar (`$CfgJar`).
- The connector port.
- Whether `VersionLoggerListener` has `logArgs="false"`.

If the service is the BusinessObjects-bundled Tomcat, note it, because of the outage rule above.

### 2.3 Health check

```powershell
Get-Service $Svc | Select-Object Name, Status, StartType
Get-PropKeys $CfgJar 'bicsso_config.properties'
$slapi = [IO.Compression.ZipFile]::OpenRead($CfgJar); $s = New-Object IO.StreamReader($slapi.GetEntry('slapi_config.properties').Open())
$paths = ($s.ReadToEnd() -split "`n") | Where-Object { $_ -match 'FileName=' } | ForEach-Object { ($_ -split '=',2)[1].Trim() -replace '\\\\','\' } | Sort-Object -Unique
$s.Dispose(); $slapi.Dispose()
$paths | ForEach-Object { [pscustomobject]@{ File = $_; Exists = Test-Path $_ } }
Invoke-WebRequest -Uri "https://<host>:<port>/<context>/" -MaximumRedirection 0 -UseBasicParsing -ErrorAction SilentlyContinue |
    Select-Object StatusCode, @{n='Location';e={$_.Headers.Location}}
```

**Pass:**

- The service is `Running`.
- The key list matches Section 1.
- Every SLAPI key file exists.
- The request with no parameters returns `302` to `fatalErrorPage.jsp`. That shows the JSP compiled and `SSOMode` is not `Direct`; the request also logs `SSOTKN : Slapi AuthToken value is missing`, which is expected.

### 2.4 Log watch

These patterns indicate a failing login:

```powershell
Select-String -Path "$CB\logs\Dw_Bic_Redirect_Page*.log" -Pattern 'SSOTKN -Single Sign On failed|Login Exception|Key-encryption secret|Failed to load the Decrypter|Failed to call the service after|account is disabled|No entitlement ApplId|proper entitlments' |
    Group-Object { ($_.Line -split ' - ',2)[1].Substring(0, [Math]::Min(60, ($_.Line -split ' - ',2)[1].Length)) } |
    Select-Object Count, Name
```

This prints counts per message prefix, not raw lines. Raw lines can contain user IDs and tokens.

---

## 3. Deploy the remediation to SYSTEM

**Gate:** test plan items OP-1 to OP-4 are signed off, and open items O1 (`Mac4ApplId` value), O2 (trusted-auth secret in Vault and Proxy secret_id placed by the custodian), O3 (Vault namespace and agent role, standard Q14; `vault-client` published, Q20), and O4 (host list) in [dwbo-bic-sso-remediation-plan.md](dwbo-bic-sso-remediation-plan.md#service-delivery-prerequisites-for-sso) are resolved.

### Artifacts

| Component | Source | Use |
|---|---|---|
| `SsoRedirector` WAR | Jenkins `develop` build containing `a2cc9b1`. The build number is NOT FOUND in this workspace; record it. | Deploy. The changed files are 5 classes and `index.jsp`. `git diff origin/master origin/develop` touches no runtime dependency; the `pom.xml` change is `<repositories>` only. |
| `properties-boe-system.jar` / `properties-crd-system.jar` | A new `dwbo-bic-sso-config` build that adds `Mac4ApplId` (OP-2) and the Vault keys, and removes `BO4SecretKey` | Deploy before or with the WAR. |
| Trusted-auth secret | The **current, pre-rotation** value, entered into Vault by the custodian under O2 | Already in Vault before 3.3. Do **not** rotate it in this window; rotation is deferred (see the test plan's "Deferred Rotation Testing"). |
| Vault Proxy | `vault.exe` and `deploy/vault-agent/agent-windows.hcl` from the Vault client repo | Install as in 3.3. |

### 3.1 Rollback capture

```powershell
New-Item -ItemType Directory -Force $RB, $Work | Out-Null
icacls $RB /inheritance:r /grant:r 'Administrators:(OI)(CI)F' | Out-Null
icacls $Work /inheritance:r /grant:r 'Administrators:(OI)(CI)F' | Out-Null
Copy-Item '<deployed WAR path>' $RB
robocopy $App "$RB\exploded" /E /COPY:DAT /R:1 /W:1 /NFL /NDL
Copy-Item $CfgJar $RB
Copy-Item '<context XML from 2.2, if any>' $RB -ErrorAction SilentlyContinue
reg export "$($Proc -replace '^HKLM:','HKLM')" "$RB\procrun_java.reg" /y | Out-Null
reg export "HKLM\SYSTEM\CurrentControlSet\Services\$Svc" "$RB\service.reg" /y | Out-Null
Get-FileHash "$RB\*.war", "$RB\properties-*.jar" | Select-Object @{n='File';e={Split-Path $_.Path -Leaf}}, @{n='SHA256_8';e={$_.Hash.Substring(0,8)}}
```

Run 2.3 now as the baseline, then complete one LP and one CM login so you have a known-good reference.

**Rollback point A.** Nothing on the host has changed yet.

### 3.2 Check the new config jar before deploying (OP-2)

```powershell
$NewCfg = '<new properties-boe-system.jar or properties-crd-system.jar>'
Compare-Object (Get-PropKeys $CfgJar 'bicsso_config.properties') (Get-PropKeys $NewCfg 'bicsso_config.properties')
```

**Pass:** the only difference is `Mac4ApplId` added (`=>`).

**Stop if** `Mac4ApplId` is missing, or any other key was added or removed.

### 3.3 Install Vault Proxy and check the secret (OP-1)

**Revised 2026-10-17.** The KEK (`BICSSO_BO4_KEK`/`_IV`) is no longer used. `SecretKeyStorage.readConfiguredBO4SecretKey` reads the trusted-auth secret from Vault, at the path in `BO4SecretKeyVaultPath`, key `value`, through a Vault Proxy listening on `127.0.0.1:8100`.

Prerequisites (Vault admin, once per tier; standard Q14): the read policy and the agent AppRole exist (`deploy/approle-setup.sh` with `AGENT_ROLE`), and the secret is stored at `secret/ibsmas/<tier>/dwbo-bic-sso/bo4-trusted-auth`, key `value`, on a **KV version 2** mount. The client reads only `<mount>/data/<path>` (latest version), so a KV v1 mount does not work. The Vault admin can confirm the engine version with `vault secrets list -detailed`, which shows `version:2` under Options for the mount. This follows the standard's §8.4 model: the Proxy uses AppRole, bound to the tier's server CIDRs, with a secret_id of at most 90 days (§4) that the key custodian places by hand.

1. Install Vault Proxy as a Windows service using `deploy/vault-agent/agent-windows.hcl`, with the tier's values filled into the `REPLACE-*` placeholders.
2. Put the agent role_id in `C:\vault\role-id`. The role_id is not secret.
3. Lock `C:\vault\secrets` down with `icacls /inheritance:r`, granting only the Proxy service account and SYSTEM.
4. The key custodian mints the secret_id (`vault write -f auth/approle/role/<agent-role>/secret-id`) and writes it to `C:\vault\secrets\secret-id`. Do not script the value or paste it into a ticket. It expires after 90 days, and the rotation owner is standard Q15; record the rotation date on the change.
5. Start the Proxy, then verify names and status only:

```powershell
Get-Service VaultProxy | Select-Object Status, StartType
(Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:8100/v1/sys/health' -Headers @{ 'X-Vault-Request' = 'true' }).StatusCode
```

**Pass:**

- The Proxy service is Running and set to Automatic.
- The health call returns 200.
- The new `bicsso_config.properties` has `vault.address`, `vault.authMode`, `vault.namespace`, `vault.kvMount` and `BO4SecretKeyVaultPath`, and no `BO4SecretKey`. Check key names only, except `vault.kvMount` and `vault.namespace`. These are not secret, so check that their values match the mount and namespace the Vault admin confirmed for this tier (`kv` or `secret`).

The old WAR ignores the Proxy, so installing it now is safe while the old code is still running.

### 3.4 Deploy

1. Stop Tomcat and confirm it stopped:
   ```powershell
   Stop-Service $Svc; (Get-Service $Svc).Status
   ```
2. Install the new config jar at `$CfgJar` (same path and same file name).
3. Replace the WAR with the new build under the **same file name** as before, so the context path does not change. Then remove the old exploded directory and its JSP work directory so Tomcat redeploys from the WAR:
   ```powershell
   Copy-Item '<new WAR>' '<deployed WAR path>' -Force
   Remove-Item $App -Recurse -Force
   Remove-Item "$CB\work\Catalina\localhost\<context>" -Recurse -Force -ErrorAction SilentlyContinue
   ```
   If 2.2 showed `$CfgJar` inside `$App\WEB-INF\lib`, copy the new config jar back into that location after Tomcat re-expands the WAR, then restart once more.
4. Start Tomcat and wait for the app to deploy:
   ```powershell
   Start-Service $Svc
   ```
   Then wait for the `Deployment of web application archive … has finished` line in `catalina.<date>.log`.
5. Run the leak check:
   ```powershell
   Select-String -Path "$CB\logs\catalina*.log", "$CB\logs\*stdout*.log" -Pattern 'hvs\.|X-Vault-Token' -List | Select-Object Path
   ```
   **Pass:** no output.

### 3.5 Smoke test (OP-5)

1. Run 2.3.
2. Complete one SSO login each on LP, CM (BOE only; `CRD System` has no `CustomerMinerApplId`), and MAC4, using entitled test users.

**Pass:**

- All logins land on their launch pads.
- 2.4 shows no new `Vault`, `BO4SecretKeyVaultPath`, `Login Exception`, or `No entitlement ApplId` lines.

A wrong secret value in Vault does **not** produce a Vault error: the CMS rejects the trusted logon, and the log shows `Token creation failed` or `Failed to set password…`, then `Login Exception`. If LP and CM fail that way, compare the Vault value with the CMC trusted-authentication secret first (O6).

### 3.6 Hand to testers

Hand over ST-01 to ST-04. ST-03 (Vault Proxy stopped) needs its own approved window. ST-05 is a `master` build check and needs nothing on the host. Send the OP-4 notice to support before ST-01: disabled BOE accounts are now rejected and no longer auto-unlocked.

---

## 4. Troubleshooting

| Log line (`Dw_Bic_Redirect_Page.log`) or symptom | Cause | Action |
|---|---|---|
| `Vault path for the BO4 trusted-auth secret is not configured` / `'BO4SecretKeyVaultPath' is not configured` | The deployed config jar lacks `BO4SecretKeyVaultPath` | Deploy the new config jar. |
| `VaultUnavailableException` (no status, or 429/5xx) | Vault Proxy stopped or unreachable on `127.0.0.1:8100`, or Vault itself is down | Check `Get-Service VaultProxy` and the 3.3 health call. Logins recover within 5 minutes of the Proxy coming back, with no Tomcat restart. |
| `VaultPermissionDeniedException` / `VaultAuthException` | The policy does not grant read on the path, or the Proxy's secret_id is bound to another CIDR, or it expired (90 days). An expired secret_id surfaces only after the Proxy restarts or the host reboots, because a running Proxy keeps renewing its token; suspect it first if SSO fails straight after patching. | Vault admin checks the policy and the secret_id's expiry; the custodian places a fresh secret_id and restarts the Proxy. |
| `VaultNotFoundException` | No secret, or no `value` key, at `BO4SecretKeyVaultPath`; the latest version was deleted or destroyed; `vault.kvMount` names the wrong mount (e.g. `secret` when the real mount is `kv`); or the mount is KV **v1**, which has no `data/` path (UNVERIFIED: a v1 mount may return 403 instead, which shows as `VaultPermissionDeniedException`) | Vault admin checks the path, the key name, the latest version's state, and that the mount is KV v2 (`vault secrets list -detailed`). |
| `Token creation failed`, or `Failed to set password for change password on next login`, then `Login Exception`, on every channel | The Vault value differs from the CMS trusted-authentication secret | Compare the two with the custodian and the BOE admin; this is not a code defect. |
| `Login rejected, account is disabled` | The user is disabled in BOE. This is intended behaviour since PR #39; earlier builds auto-unlocked the account. | BOE admin decision. Do not re-enable the account to "fix" SSO. |
| `No entitlement ApplId configured for channel BO_MAC4_SSO. Access denied.` | `Mac4ApplId` is missing from the deployed config (`index.jsp:164`) | Deploy the OP-2 config jar. |
| `User does not have proper entitlments ApplId assigned…` | The user lacks the entitlement, and their bank is not on `entitlement.exemption.list` | This is correct for unentitled users (ST-02). Otherwise check the entitlement with its owners. |
| `Failed to call the service after … attempts` | The entitlement SOAP endpoint (`entitltment.endpoint.url`) is unreachable | Check network, DNS, and TLS to the endpoint. |
| `SSOTKN -Failed to Decrypt token` | Bad or expired SLAPI token, wrong key file, or a channel/key mismatch. Since PR #39 this fails closed rather than continuing with empty credentials. | Check the 2.3 key files; ask the token issuer. |
| `Failed to load the Decrypter SlapiDecryptionMI() AuthException…` (fatal) | SLAPI could not load its key files when the class was first initialised | Check the key-file paths and the service account's read access, then restart. |
| `Unknown Channel name :…` | The `channel` parameter is not one of the three supported values | Fix the caller (ST-04). |
| `Single Sign on mode set as Direct…`, and a blank page | `SSOMode=Direct` in the config | Expected only where SSO is disabled on purpose. |
| User sees "Single Sign On failed, please contact technical support." | `fatalErrorPage.jsp`, the generic page for every failure above | Find the matching log line by timestamp. |

---

## Rollback (from any point after 3.4)

1. Stop the service.
2. Restore the WAR, the exploded directory, the context XML, and the old config jar from `$RB`. Clear `work\Catalina\localhost\<context>`.
3. Start the service, then run 2.3 and an LP login.
4. You may leave the Vault Proxy running, because the old code ignores it. The old WAR reads `BO4SecretKey` from the restored old config jar, so it still needs the pre-change hardcoded key, which it contains.

The restored WAR contains the hardcoded key again. That is the pre-change state, not a new exposure, but do not patch a key into any build as a way to roll back.

Combinations that fail, so never leave the host in one of them:

- The new WAR without a running Vault Proxy, or with the old config jar (no `BO4SecretKeyVaultPath`): every login fails.
- The new WAR with the old config jar: every MAC4 login fails.

## Record on the change

- Host, service name, run-as account, `CATALINA_BASE`, context path, config jar location, and the Tomcat version (from the top of `catalina.<date>.log`).
- The Jenkins build numbers for the WAR and the config jar, and the 8-character SHA-256 prefixes of the old and new files.
- The 3.3 provisioning location (names only) and the empty leak check from 3.4.
- The 3.5 results for each channel.
- The pre-existing observations: the RDY public certificate in the SYSTEM config, no `CustomerMinerApplId` in `CRD System`, and `debug` log level with the token logged at `index.jsp:80`.
- The Proxy secret_id rotation due date (90 days from placement).
- Do not record any secret, secret_id, token, or private-key content.
