# SYSTEM Test Plan: dwbo-bic-sso Remediation

## Scope

Validate the remediation for:

| Finding | Jira | SNOW | Test objective |
| --- | --- | --- | --- |
| `f010` | `IBSDA-94962` | `AVIT32772748` | (Revised 2026-10-17) Confirm the BOE trusted-auth secret is read from Vault at run time through the local Vault Proxy (no hardcoded key, no KEK, no secret in config or the environment, per standard §8.4), and that existing SSO trusted-auth login works with the current value in Vault. |
| `f024` | `IBSDA-94956` | `AVIT32772774` | Confirm the MAC4SSO channel now enforces the same entitlement check as LP/CM, and that a token lacking the Mac ApplId is rejected instead of silently logging in. |
| `f091` | `IBSDA-96108` | `AVIT32772818` | Confirm the build fetches dependencies over HTTPS only. |

This plan covers the develop-first remediation for `f010` and `f024`; `f091` was fixed directly on `master` (PR #37) and is tested from a `master` build (ST-05). It does not cover rotating the SSO trust-anchor secret itself (see Deferred Rotation Testing) or the per-channel RSA keypair separation noted as a residual, config-owned risk for `f024`.

## Service Delivery Prerequisites (gate: complete before SYSTEM deployment)

Do not deploy the `develop` artifact to SYSTEM until every item below is signed off. Deploying code before items 1-2 breaks SSO: all channels if item 1 is missing, every MAC4 login if item 2 is missing. The full cross-tier list and open items (O1-O7) are in [dwbo-bic-sso-remediation-plan.md](dwbo-bic-sso-remediation-plan.md#service-delivery-prerequisites-for-sso).

| # | Prerequisite (SYSTEM tier: `BOE System`, `CRD System`) | Owner | Sign-off |
| --- | --- | --- | --- |
| OP-1 | (Revised 2026-10-17) Store the **current** pre-rotation BOE trusted-auth secret in Vault at `secret/ibsmas/<tier>/dwbo-bic-sso/bo4-trusted-auth` (key `value`), install Vault Proxy on the SYSTEM Tomcat host with its secret_id placed by the custodian (runbook 3.3), and deploy the config jar with the `vault.*` keys and `BO4SecretKeyVaultPath`. Set `vault.kvMount` explicitly to the mount the Vault admin confirmed (`kv` or `secret`; standard Q14). The client defaults to `secret`, and the `secret/...` path above assumes that name. Never pass a value via ticket, chat, or email (open items O2, O3). | TBD | |
| OP-2 | Merge and deploy the `IBSMAS-dwbo-bic-sso-config` change adding `Mac4ApplId` to `BOE System` and `CRD System` `bicsso_config.properties` (value from entitlement-service owners, open item O1). Deploy before or with the code. | TBD | |
| OP-3 | Confirm MAC4 SYSTEM test users hold the `Mac4ApplId` entitlement or their bank is on `entitlement.exemption.list`. Confirm at least one MAC4 test user does **not** (needed for ST-02). | TBD | |
| OP-4 | Notify support that disabled BOE accounts are now rejected, not auto-unlocked. | TBD | |
| OP-5 | Service Delivery smoke check after deployment: one SSO login each on LP, CM, and MAC4. A wrong value in Vault shows only as a BOE trusted-auth login failure. If LP/CM login fails, compare the Vault value with the CMC trusted-authentication secret before raising a code defect. | TBD | |

## Preconditions

1. Record the deployed branch, commit SHA, build number, artifact name, and deployment timestamp.
2. Confirm the artifact was built from the remediation changes merged into `develop`.
3. Use synthetic or approved masked SYSTEM test data and test accounts only. Do not use production customer data or real production credentials.
4. Service Delivery Prerequisites OP-1 through OP-5 are signed off. OP-1 puts the **current** (pre-rotation) trusted-auth secret in Vault and starts the Vault Proxy. Do not rotate the actual secret value as part of this test cycle (see Deferred Rotation Testing).
5. `Mac4ApplId` is configured in the SYSTEM environment's `bicsso_config.properties` (OP-2) before testing the MAC4SSO channel. An unconfigured value fails closed and must not be mistaken for a broken fix.
   - As of 2026-10-06, `Mac4ApplId` is absent from all 10 `bicsso_config.properties` files in `IBSMAS-dwbo-bic-sso-config` on `master` (`8332719`), `develop` (`efb9ae9`), and `release/release-2025.11.09` (`b222045`), while all 10 environments configure the MAC4 channel (`MACSSO4LaunchPadPath`, `BO_MAC4_SSO_*`). A config change adding it must deploy before or with the code; deploying the code alone denies every MAC4 login.
   - Obtain the MAC4 ApplId value from the entitlement-service owners, and confirm MAC4 test users hold it or their bank is on `entitlement.exemption.list`.
6. Do not place key material, secrets, tokens, or decrypted SSO trust-anchor values in tickets, screenshots, logs, or test evidence.
7. Confirm test accounts for each channel (`BO_LP_SSO`, `BO_CM_SSO`, `BO_MAC4_SSO`) and at least one test account that lacks the `Mac4ApplId` entitlement.

## Test Cases

### ST-01: Existing SSO Login Still Works (LP/CM channels)

1. Start the application through the normal SYSTEM deployment procedure with the Vault Proxy running and the current trusted-auth secret in Vault (OP-1).
2. Submit an SSO login through the Launchpad (`BO_LP_SSO`) channel with a valid, entitled test account.
3. Confirm login succeeds and redirects to the expected Launchpad URL.
4. Repeat for the Customer Miner (`BO_CM_SSO`) channel.

**Pass criteria:** Both LP and CM channels authenticate successfully with no change in observable behavior from before the fix.

### ST-02: MAC4SSO Channel Now Enforces Entitlement

1. Submit an SSO login through the `BO_MAC4_SSO` channel using a test account that **has** the configured `Mac4ApplId` entitlement.
2. Confirm login succeeds and redirects to the MAC launchpad URL (`getMacsso4LaunchPadPath()`).
3. Submit the same channel using a test account that does **not** have the `Mac4ApplId` entitlement (or a token originally issued for LP/CM, replayed with `channel=BO_MAC4_SSO`).
4. Confirm access is denied — no `TokenLoginBO4` session is established for the unentitled account.

**Pass criteria:** An entitled MAC4SSO account logs in successfully; an unentitled account (or a replayed cross-channel token) is rejected instead of silently authenticating.

### ST-03: Vault Unavailable Fails Safely

Perform this test only in an isolated SYSTEM execution window with Service Delivery approval.

1. Stop the Vault Proxy service on the Tomcat host (Tomcat stays up).
2. Wait at least 5 minutes so the client's cached secret expires, or restart Tomcat.
3. Attempt an SSO login.
4. Confirm the login fails closed (a `VaultUnavailableException`-driven request failure to the error page) rather than falling back to any default, hardcoded, or config-file key.
5. Confirm the log shows only the Vault path and status, and no secret, key material, or token appears in logs or error pages.
6. Start the Vault Proxy and confirm SSO login works again without restarting Tomcat.
7. Optional, with the Vault admin: remove the policy grant temporarily and confirm a `VaultPermissionDeniedException` failure, then restore it.
8. Optional: deploy a config with `vault.kvMount` set to the *other* name, i.e. `secret` if the real mount is `kv`, or the reverse. Confirm the login fails closed with `VaultNotFoundException` or `VaultPermissionDeniedException`, depending on the policy. Then restore the correct value and restart Tomcat, because the config is cached in a static field.

**Pass criteria:** An unreachable Vault or a denied policy fails closed with no silent fallback and no sensitive disclosure; service recovers cleanly once restored.

### ST-04: Unknown Channel Still Rejected

1. Submit a request to `index.jsp` with a `channel` value that is none of `BO_LP_SSO`, `BO_CM_SSO`, or `BO_MAC4_SSO`.
2. Confirm the request throws/rejects with "Unknown Channel name" behavior, unchanged from pre-fix behavior.

**Pass criteria:** Unknown channels are still rejected; the MAC4SSO gating change did not loosen this check.

### ST-05: Build Fetches Dependencies Over HTTPS Only

1. Trigger a clean CI/local build of `dwbo-bic-sso` from `master` with PR #37 merged. The `f091` fix is `master`-only; `develop`'s `pom.xml` has no `<repositories>` block, so a `develop` build does not exercise this fix.
2. Inspect build logs / network trace for the Maven dependency-resolution phase.
3. Confirm all requests to `artifactory.fis.dev` use HTTPS; confirm no plain-HTTP request to that host occurs.

**Pass criteria:** No HTTP (non-TLS) request to `artifactory.fis.dev` occurs during the build. Note if the BOE/SAP SDK provisioning gap otherwise blocks a full build — that is a separate, known, pre-existing condition and does not fail this test case on its own.

## Evidence Required

Attach or record:

- Deployed branch, commit SHA, build number, and artifact identifier.
- Service Delivery Prerequisites OP-1 to OP-5 sign-off (owner, date). Record presence of the Vault secret, the Proxy service, and `Mac4ApplId`, not their values. Record the values of `vault.kvMount` and `vault.namespace`, which are not secret, and the confirmed mount name.
- Deployment and test timestamps.
- Login results (success/failure) for LP, CM, and MAC4SSO channels, including the unentitled-account rejection case.
- Confirmation that `Mac4ApplId` was configured before ST-02, with the configured value's presence (not the value itself) recorded.
- Build log excerpt or network trace confirming HTTPS-only Artifactory access.
- Confirmation that the missing-key test failed closed with no sensitive disclosure, and that service recovered after restoration.
- Tester name and pass/fail decision for each test case.

## Release Decision

Reject promotion to `master` if any of the following occurs:

- The SSO trust-anchor key/IV is not externally provisioned, or a hardcoded fallback is discovered.
- LP or CM channel login regresses.
- The MAC4SSO channel logs in an unentitled account, or fails to log in an entitled one.
- `Mac4ApplId` is not yet present in every target environment's `bicsso_config.properties` in `IBSMAS-dwbo-bic-sso-config`.
- The missing-key test produces a silent fallback, partial success, or sensitive disclosure.
- Any HTTP (non-TLS) request to `artifactory.fis.dev` is observed during the build.
- Unknown channels are no longer rejected.

Rollback means restoring the previously approved deployment artifact and configuration. Do not restore the hardcoded key as a rollback mechanism.

After all tests pass, promote `develop` to `master`, verify the tested commits are included, and request a new Mythos rescan referencing `AVIT32772748`, `AVIT32772774`, and `AVIT32772818`. SYSTEM testing alone does not mark any of the three findings remediated.

## Deferred Rotation Testing

Do not rotate the actual SSO trust-anchor secret value as part of this test cycle. A later test plan must cover:

(Revised 2026-10-17: no AES key, IV, or ciphertext remains, so there is nothing to re-encrypt.)

- **Trusted-auth secret rotation (standard Q16).**
  - Write the new value to Vault and the BOE CMC in one window, per tier.
  - The client caches the value for up to 5 minutes, so expect failed logins for up to 5 minutes after cutover unless Tomcat is restarted. Failures inside that window are expected and are not a defect.
  - Whether the CMC accepts the old secret during the transition is UNVERIFIED. It was asked of the BOE admin in standard Q16; record the answer here before the test.
  - **Pass:** SSO login works on LP, CM, and MAC4 no later than 5 minutes after cutover, without restarting Tomcat (or immediately after a restart, if one is used).
- **Vault Proxy secret_id rotation (standard Q15).**
  - Place a new secret_id, restart the Proxy, and confirm SSO login works.
  - Test expiry without waiting 90 days. The Vault admin either revokes the test secret_id, or mints a short-lived one for the custodian to place (asked in standard Q15). For example, `vault write -f -field=secret_id auth/approle/role/<agent-role>/secret-id ttl=10m`; whether `ttl` is accepted on this endpoint depends on the Vault version (UNVERIFIED).
  - An expired or revoked secret_id does **not** stop a running Proxy: the token it already holds is periodic and keeps renewing. The failure appears only when the Proxy has to log in again, after a restart, a reboot, or a lost token. So after expiry or revocation, restart the Proxy, then wait up to 5 minutes for the client cache to clear.
  - **Pass:** login fails closed with `VaultAuthException` / `VaultPermissionDeniedException`, and no secret appears in the logs.
  - Place a valid secret_id again, restart the Proxy, and confirm login recovers without restarting Tomcat.
- **Old config removed.** Confirm no environment's `bicsso_config.properties` still contains `BO4SecretKey` after the config rollout (remediation plan step 25).
- Per-channel RSA keypair separation for `f024`'s residual risk (all three SSO channels currently share one keypair in production `slapi_config.properties`) — this is a config-only, Service Delivery-owned follow-up outside code-level test scope here.
