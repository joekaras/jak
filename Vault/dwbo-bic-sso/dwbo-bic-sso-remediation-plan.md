# Plan: Remediation of dwbo-bic-sso on develop

## Status (2026-10-06): PR #37 (`IBSDA-96108`/f091) MERGED to `master` (`230f5d9`, 2026-09-22). PR #38 (`IBSDA-94962`/f010) and PR #39 (`IBSDA-94956`/f024) MERGED to `develop` (tip `a2cc9b1`, 2026-10-06). f010 and f024 remain **unfixed** for reporting until `develop` is promoted to `master` and rescanned. Next: Service Delivery prerequisites, SYSTEM test, promotion PR.

## TL;DR
Remediate the real, previously-merged-then-reverted findings for `dwbo-bic-sso`. This plan was corrected on 2026-09-19 after live git/Jira research showed the original draft (based on the static Mythos `bug_01`/`bug_39`/`bug_71` patch recommendations) did not match what was actually implemented and reverted. Key corrections:

- `develop` and `master` were reverted to **different, non-matching baselines** (`develop` -> `7745c59` 2026-03-24 via PR #36; `master` -> `e78ad47` 2025-10-09 via PR #35) — they do not match each other, contrary to earlier assumption.
- The real fixes used env vars **`BICSSO_BO4_KEK`/`BICSSO_BO4_KEK_IV`**, not `BICSSO_BO4_DECRYPT_KEY`/`_IV`.
- `IBSDA-94956` bundles **three** sub-fixes, not one: (a) `LoginHelper` fail-closed on disabled account (old finding `f082`), (b) MAC4SSO entitlement check via `SingleSignOnConfiguration.getMac4ApplId()` (current `f024`), (c) `SlapiTokenHandler` fail-closed decrypt (old finding `f184`). (a) was previously on `develop` (PR #32) and got reverted there; (b) and (c) were **only ever merged to `master`** (PR #33), never reached `develop`.
- `IBSDA-96108`/`f091` already had a real Jira ticket (`Ready for Test`, not "none yet"), and its one-line `pom.xml` fix (PR #34) applied to `master` only. `develop`'s `pom.xml` never had the vulnerable block at all (removed years earlier under unrelated ticket `IBSDA-82558`) — **no `develop` code change needed for `f091`**. This was reapplied directly to `master` via PR #37 on 2026-09-19.

## Scope & Finding Inventory

| Ticket | Old/current finding IDs | SNOW AVIT | UUID (current scan) | Severity | Target File(s) | Where the real fix lives |
|---|---|---|---|---|---|---|
| `IBSDA-94962` | old `f018` -> current `f010` | `AVIT32772748` | `723c06d0-27fa-41f2-98a8-949c667a73fc` | HIGH (claimed) / MEDIUM (triage-adjusted) | `SecretKeyStorage.java` | Was on `develop` only (PR #32); reverted |
| `IBSDA-94956` (a) | old `f082` (disabled-account fail-closed) | n/a (not separately tracked in current scan) | n/a | — | `LoginHelper.java` | Was on `develop` (PR #32) **and** `master` (PR #33); reverted on both |
| `IBSDA-94956` (b) | old `f184`+`f197` -> current `f024` (also duplicated as `IBSDA-95089`/`IBSDA-95090`) | `AVIT32772774` | `a6640101-62aa-432c-b283-f7c87c04cc43` | MEDIUM | `index.jsp`, `SingleSignOnConfiguration.java` | Was on `master` only (PR #33); **never reached `develop`** |
| `IBSDA-94956` (c) | old `f184` | n/a (not separately tracked in current scan) | n/a | — | `SlapiTokenHandler.java` | Was on `master` only (PR #33); **never reached `develop`** |
| `IBSDA-96108` | `f091` | `AVIT32772818` | `429dfc76-90c7-4f3f-ab51-0da68bf306ed` | LOW | `pom.xml` | **DONE** — reapplied to `master` via PR #37 (2026-09-19), no `develop` change needed |

### Deliberately Excluded Scope
- `dwbo-mac-tools/pom.xml` — separate repository, separate owner (Pete Joda per roster), out of scope.
- Per-channel RSA keypair separation (all three SSO channels share one keypair in production `slapi_config.properties`) — config-only, Service Delivery-owned follow-up, not a code fix.

### Exact Mythos Rationale & Recommendation (from `TRIAGE.json`)

**f010:**
> "SecretKeyStorage.java:109 hardcodes the AES key/IV that decrypts the BO4SecretKey ciphertext committed in production configs (IBSMAS-dwbo-bic-sso-config/master/BOE Production/src/main/resources/bicsso_config.properties, plus CRD Production and six other environments, with prod CMS hostnames). The decrypted value is the TrustedAuthentication shared secret passed to createTrustedPrincipal (SessionManager.java:35) via LoginHelper.java:48, reachable from the live entry point index.jsp:135 — it enables password-less logon as arbitrary users. EncryptedProperties.java:92 confirms no second encryption layer exists (all crypto is commented out), so repo read access alone recovers the production trust-anchor secret offline."

Recommendation: "Vault-held secret, remove hardcoded key/IV, rotate the trusted-auth secret everywhere, purge history."

**IBSDA-94956 (a) and (c):** old IDs `f082`/`f184` have no corresponding entry in the current `TRIAGE.json` — no exact rationale/recommendation quote is available for these two sub-fixes. They were carried forward and re-verified manually against current source, not against a live current-scan finding record.

**f024 (IBSDA-94956 (b)):**
> "index.jsp:149-152 logs the user into BO via TokenLoginBO4 with no UserHasApplId entitlement check, while the LP/CM branches require it (index.jsp:124). The channel that selects both the code branch and the decryption key is the raw request parameter (index.jsp:46, SlapiTokenHandler.java:96), and production slapi_config.properties (BOE Production, lines 5-6/14-15/23-24) assigns all three channels the identical RSA keypair, so a token minted for BO_LP_SSO validates unchanged under channel=BO_MAC4_SSO. Production SSOMode=InDirect (bicsso_config.properties:1) keeps the page live, and TokenLoginBO4 (LoginHelper.java:43-71) performs an admin-trusted SSO with no authorization of its own. Trust-boundary note: requires a valid signed issuer token, so this is entitlement bypass by an authenticated token holder, not unauthenticated access; residual uncertainty is possible in-token channel binding inside the closed-source SLAPI jar."

Recommendation: "Run the entitlement check on the MAC4SSO branch (or distinct keys per channel); verify the in-token channel claim."

**f091:**
> "pom.xml:17-18 fetches all dependencies (with <id>central</id> shadowing Maven Central, including crypto/SSO jars) over plain HTTP into a production SSO redirector WAR built weekly by live CI (Jenkinsfile:7, buildParams.yml:3 IBS_MAS_BIC_SSO_Redirector_prod). Twelve sibling repos use https for the identical Artifactory host, proving HTTPS works and this is misconfiguration drift, not design. No settings.xml, mirror, or Maven version pin exists anywhere in scope to compensate; the pipeline library that might inject one is external (Jenkinsfile:2-5) and the local IBSMAS-Groovy copy is an empty placeholder. Exploitation requires an on-path attacker toward internal artifactory.fis.dev, which supports the claimed LOW severity; confidence is moderate because an unverifiable CI-side mirror could in principle neutralize the URL."

Recommendation: "Change both poms to https; use a Maven >=3.8 build image."

---

## Steps

### Phase 1: Verify Current State — DONE
1. ✅ Confirmed via live git that `develop` (`7aac188`) and `master` (`e22822a`, pre-PR-#37) were reverted to different, non-matching baselines.
2. ✅ Confirmed live Jira: `IBSDA-94962` — need to re-check current status (was `Accepted` in a stale snapshot); `IBSDA-94956` — need to re-check; `IBSDA-96108` was `Ready for Test`.
3. ✅ `IBSDA-96108` already existed; no new ticket needed.

### Phase 2: f091 — DONE (master only)
4. ✅ Cut `feature/IBSDA-96108-master-https-artifactory` from `master`, reapplied the exact PR #34 one-line fix plus remediation comment, pushed, opened PR #37 to `master`.

### Phase 3: Branch Setup for develop Work
*Depends on Phase 1*
5. Confirm the working clone at `c:\1mythos\9946\dwbo-bic-sso` is on `develop`, up to date with `origin/develop` (`7aac188`), clean working tree.
6. Cut `feature/IBSDA-94962-f010-sso-key-removal` from `develop` for the `SecretKeyStorage` fix.
7. Cut `feature/IBSDA-94956-mac4sso-entitlement-and-failclosed` from `develop` for the bundled `IBSDA-94956` work (disabled-account fail-closed + MAC4SSO entitlement + SLAPI fail-closed) — one PR, matching how the real historical work was actually organized (single ticket, one integration branch on the master side).

### Phase 4: IBSDA-94962 / f010 Implementation
*Depends on Phase 3*
8. `SecretKeyStorage.java`: add constants `BO4_KEK_NAME = "BICSSO_BO4_KEK"` and `BO4_KEK_IV_NAME = "BICSSO_BO4_KEK_IV"`. Replace the hardcoded `new AESEncryption("[REDACTED key]", "[REDACTED IV]")` in `DecryptSSO(String)` with a `readRequiredSecret(name)` helper reading `System.getProperty` then falling back to `System.getenv`, throwing `IllegalStateException` (logged, no secret value logged) if neither is set.
9. **Correction after inspecting current `develop` content:** `develop`'s revert baseline (`7745c59`) predates even PR #32's own starting point — it also lacks PR #29's username-masking and the `char[]`-return refactor for `readConfiguredSecretKey`/`readConfiguredBO4SecretKey` (both `String`-based here, unlike PR #32's pre-image). Reintroducing those unrelated, separately-already-reverted improvements is scope creep beyond `f010`. Use the **simpler `master`-side PR #33 structure** instead (matches current `develop`'s `String`-based method signatures exactly): keep `DecryptSSO(String)` returning `String`, source the key/IV via `readRequiredSecret(name)` reading `System.getProperty` then `System.getenv`.
10. Add adjacent remediation comment:
    `// Mythos remediation: Legacy Vulnerability ID 723c06d0-27fa-41f2-98a8-949c667a73fc; SNOW AVIT32772748`
11. Add/restore `SecretKeyStorageTest.java` covering the fail-closed `readRequiredSecret` behavior (matches the real historical test content, not the static-patch-recommended one).
12. Cross-repo note (Service Delivery, not code): production ciphertext for `BO4SecretKey` is committed in `IBSMAS-dwbo-bic-sso-config` across 8 environments, encrypted with the *old* hardcoded key. Once this key is externalized and rotated, those ciphertexts must be regenerated or SSO trusted-auth login breaks at runtime. **Superseded 2026-10-17:** the ciphertext is no longer read; remove it instead (see step 25).

### Phase 5: IBSDA-94956 Implementation (three sub-fixes, one PR)
*Depends on Phase 3, parallel with Phase 4*
13. `LoginHelper.java` (old `f082`): remove the `unlockUser`/retry-login path on a disabled-account exception; replace with a fail-closed rejection (`logger.error("Login rejected, account is disabled:", ...)`, `return null`), matching the real historical fix identically on both branches.
14. `SingleSignOnConfiguration.java` (current `f024`): add `mac4ApplId` field and `getMac4ApplId()` getter reading the `Mac4ApplId` property — **note the real property/method name is `Mac4ApplId`/`getMac4ApplId`, not `MacApplId`/`getMacApplId`** as originally (incorrectly) planned.
15. `index.jsp` (current `f024`): add the entitlement check **inside the existing `else if (channel.equals(BicssoConstants.MAC4SSO))` branch** (not by merging MAC4SSO into the LP/CM shared condition) — build `EntitlementServiceClient`, resolve `transUuid`, check `banksExemptFromEntitlementCheck`, call `client.UserHasApplId(...)` with `configuration.getMac4ApplId()`, and throw if the applId is unconfigured or the check fails. This matches the real historical PR #33 structure exactly.
16. `SlapiTokenHandler.java` (old `f184`): change `getDecryptedCredentials(String slapiXmlToken)` to `getDecryptedCredentials(String slapiXmlToken, String channel)`, and make the catch block `throw new BicSsoException("Failed to decrypt SLAPI token", cred)` instead of swallowing the exception. Update the `index.jsp` call site to pass `channel` and drop the now-redundant `slapihelper.setChannel(channel)` call (per the real diff).
17. Add/restore `SlapiTokenHandlerTest.java` (assert `getDecryptedCredentials` throws `BicSsoException` on a bad token) and the `src/test/resources/bicsso_config.properties` test fixture (`Channel=BO_LP_SSO`), matching the real historical PR #33 test additions.
18. Add adjacent remediation comment near the `index.jsp` and `SingleSignOnConfiguration.java` changes:
    `// Mythos remediation: Legacy Vulnerability ID a6640101-62aa-432c-b283-f7c87c04cc43; SNOW AVIT32772774`
19. Residual risk to record, not fix in this PR: all three channels still share one RSA keypair in production `slapi_config.properties`.
    - **Config dependency (verified 2026-10-06):** `Mac4ApplId` is absent from all 10 `bicsso_config.properties` files in `IBSMAS-dwbo-bic-sso-config` on `master` (`8332719`), `develop` (`efb9ae9`), and `release/release-2025.11.09` (`b222045`). All 10 environments configure the MAC4 channel (`MACSSO4LaunchPadPath`, `BO_MAC4_SSO_*`), so this PR alone denies every MAC4 login (`index.jsp` throws "No entitlement ApplId configured"). The ApplId value must come from the entitlement-service owners.

### Phase 6: Verification & Build
*Depends on Phases 4, 5*
20. Run `mvn clean test` — expect `SecretKeyStorageTest` and `SlapiTokenHandlerTest` to pass. Full build may still be blocked by the known BOE/SAP Client SDK Artifactory provisioning gap; if so, document rather than skip.
21. `git diff develop` per branch to confirm only in-scope files changed and remediation comments are present with exact UUID/SNOW syntax.

### Phase 7: PR to develop & Downstream Lifecycle
*Depends on Phase 6*
22. ✅ Push both feature branches; open one PR per ticket (`IBSDA-94962`, `IBSDA-94956`) targeting `develop`. PR #38 and PR #39 merged 2026-10-06.
23. Add reviewers only on explicit instruction.
24. After merge to `develop`, and before deploying PR #39 to any environment: add `Mac4ApplId` to that environment's `bicsso_config.properties` in `IBSMAS-dwbo-bic-sso-config`, deployed before or with the code. Then deploy to SYSTEM test; validate per `docs/plans/dwbo-bic-sso-system-test-plan.md`.
25. Cross-repo dependency gate before production deployment, revised 2026-10-17 per standard §8.4. In every environment's properties file in `IBSMAS-dwbo-bic-sso-config`:
    - remove `BO4SecretKey`;
    - add `BO4SecretKeyVaultPath` and the `vault.*` keys.

    The per-tier trusted-auth secret must already be in Vault (standard Q14), and the Vault Proxy must be running on each host. No ciphertext is regenerated.
26. Open promotion PR `develop` -> `master`; note `master` already independently has the `f091` fix (PR #37) and will need to reconcile with whatever else `develop` brings — check for conflicts given the two branches' divergent revert baselines.
27. After merge to `master`: submit Mythos rescan referencing `AVIT32772748`, `AVIT32772774`, and `AVIT32772818` (already covered by PR #37, but a fresh rescan should still confirm all three together).

---

## Service Delivery Prerequisites for SSO

None of these items changes `dwbo-bic-sso` source. Items 1 and 4 are runtime provisioning. Item 2 is a properties change in the separate `IBSMAS-dwbo-bic-sso-config` repository and needs its own PR.

**Required before deploying PR #38 / #39 to any environment:**

| # | Prerequisite | Applies to | If missing |
|---|---|---|---|
| 1 | **(Revised 2026-10-17: the KEK approach is replaced; Harness only deploys and cannot inject it.)** Per tier: (a) store the BOE trusted-auth secret in Vault at `secret/ibsmas/<tier>/dwbo-bic-sso/bo4-trusted-auth`, key `value`, on a **KV version 2** mount (the client reads only `<mount>/data/<path>`, latest version; a KV v1 mount fails) (the current value for the SYSTEM test cycle; rotate it in the CMC and Vault together before Readiness/Prod, because the old KEK that protects it is in git history); (b) install Vault Proxy on each Tomcat host (`deploy/vault-agent/agent-windows.hcl` in the Vault client repo) with the agent AppRole and its persistent, ACL'd secret_id file; (c) add `vault.address=http://127.0.0.1:8100`, `vault.authMode=agent`, `vault.namespace`, `vault.kvMount` and `BO4SecretKeyVaultPath` to `bicsso_config.properties` and remove `BO4SecretKey`. Always set `vault.kvMount` explicitly: the client defaults to `secret`, but the real mount may be `kv` (the current Snowflake client's code default), and that is unconfirmed (standard Q14). The mount in `vault.kvMount` and in the read policy must match. | PR #38 + Vault follow-up | `readConfiguredBO4SecretKey` throws (`IllegalStateException` for a missing path, `VaultException` for an unreachable Proxy or denied policy). Trusted-auth SSO login fails closed on every channel. |
| 2 | Add `Mac4ApplId` to `bicsso_config.properties` for all 10 environments in `IBSMAS-dwbo-bic-sso-config`. Deploy it before or with the code. The value comes from the entitlement-service owners. | PR #39 | Every MAC4 login is denied with "No entitlement ApplId configured". |
| 3 | Confirm that MAC4 users hold the `Mac4ApplId` entitlement, or that their bank is on `entitlement.exemption.list`. | PR #39 | Legitimate MAC4 users are denied. |
| 4 | Tell support that disabled BOE accounts are now rejected, not auto-unlocked. | PR #39 | Support tickets for accounts that previously "worked" while disabled. |

**Required before the findings can be closed (not deploy blockers):**

- Rotate the trusted-auth shared secret in the BOE CMC and in Vault together for each tier, with a named owner (standard Q16).
- ~~Generate a new KEK for each tier and re-encrypt `BO4SecretKey`.~~ Superseded 2026-10-17: there is no KEK. Remove `BO4SecretKey` from all 10 `bicsso_config.properties` files in `IBSMAS-dwbo-bic-sso-config`, and add the `vault.*` keys and `BO4SecretKeyVaultPath` (Prereq 1c).
- Use separate RSA keypairs for each channel (LP/CM/MAC4) in `slapi_config.properties`, coordinated with the SLAPI token issuer. This addresses the f024 residual.

~~Follow-up that needs code: moving `AESEncryption` to AES-GCM with a random IV and retiring `BICSSO_BO4_KEK_IV`.~~ No longer needed (2026-10-17). The trusted-auth secret is read from Vault, so `AESEncryption` is off the login path, and the IV is gone.

Related repository (verified 2026-10-06): `dwbo-businessobjects-config` (`f012` keystore password, `f013` access-log tokens; not owned by `e1015974`, not started) configures the BOE/CRD Tomcat that hosts the BI launch pad this app redirects to. No code or config dependency. If its changes deploy to a tier, rerun the O6 SSO smoke test there. Detail: [dwbo-businessobjects-config-remediation-plan.md](dwbo-businessobjects-config-remediation-plan.md#related-repository--dwbo-bic-sso).

### Open items (as of 2026-10-06)

The prerequisites above define *what* is needed. These items must be resolved before Service Delivery can execute them.

| # | Open item | Blocks | Owner |
|---|---|---|---|
| O1 | `Mac4ApplId` value: NOT FOUND in `dwbo-bic-sso`, `IBSMAS-dwbo-bic-sso-config`, or docs. Obtain it from the entitlement-service owners, then open the `IBSMAS-dwbo-bic-sso-config` PR adding it to all 10 `bicsso_config.properties` files. No other config key is missing. `entitltment.endpoint.url`, `entitlement.exemption.list`, `MACSSO4LaunchPadPath`, and `BIC4ServerName` are present in all 10 (verified on config `master` `8332719`). | Prereq 2, 3 | TBD |
| O2 | ~~Secure handoff of the current KEK/IV value.~~ Superseded: no KEK is used. Instead, the key custodian enters the **new** trusted-auth secret into Vault and the CMC, and places the Proxy secret_id file on each host. Neither value goes in a ticket, chat, or email. | Prereq 1 | TBD |
| O3 | Resolved 2026-10-17: Harness only deploys. The secret is read at runtime from Vault through a local Vault Proxy (agent mode, `vault-client` library). Still open: Vault namespace and KV v2 mount per tier (is the mount `kv` or `secret`? Quickest answer: the `kvMountPoint`/`vaultNamespace` values in a deployed Snowflake-client config), and the agent role created per tier (`deploy/approle-setup.sh`, `AGENT_ROLE`) (standard Q14), and `vault-client` 0.1.0 published to JFrog (standard Q20). | Prereq 1 | TBD |
| O4 | Target host list per tier (Tomcat servers running `dwbo-bic-sso`). The config repo has 9 deployable environment folders (BOE ×5 incl. DR, CRD ×4) plus Local. | Prereq 1, 2 | TBD |
| O5 | Named owners for: config-repo PR, ApplId value, the trusted-auth secret in Vault and the Proxy secret_id on each host (key custodian; 90-day secret_id rotation, standard Q15), support notice, and the later BOE CMC secret rotation (standard Q16). | All | TBD |
| O6 | Post-provisioning smoke test per tier: SSO login on LP, CM, and MAC4. This is the only check that the value in Vault matches the CMC trusted-authentication secret. A mismatch produces no Vault error and shows only as a BOE trusted-auth login failure. | Prereq 1 sign-off | TBD |
| O7 | Jira tracking for the Service Delivery work. None found in plan docs (Jira not searched). | Tracking | TBD |

---

## Relevant Files
- `1mythos/9946/dwbo-bic-sso/src/main/java/com/fis/bic/sso/helper/SecretKeyStorage.java` — externalize hardcoded AES key/IV to `BICSSO_BO4_KEK`/`_IV`, char-array clearing
- `1mythos/9946/dwbo-bic-sso/src/main/java/com/fis/security/AESEncryption.java` — add `decryptToCharArray`
- `1mythos/9946/dwbo-bic-sso/src/test/java/com/fis/bic/sso/helper/SecretKeyStorageTest.java` (to restore)
- `1mythos/9946/dwbo-bic-sso/src/main/java/com/fis/bic/sso/helper/LoginHelper.java` — fail-closed on disabled account
- `1mythos/9946/dwbo-bic-sso/src/main/java/com/fis/bic/sso/helper/SingleSignOnConfiguration.java` — add `getMac4ApplId()`
- `1mythos/9946/dwbo-bic-sso/src/main/webapp/index.jsp` — MAC4SSO entitlement check inside its own branch; pass `channel` to `getDecryptedCredentials`
- `1mythos/9946/dwbo-bic-sso/src/main/java/com/fis/bic/sso/helper/SlapiTokenHandler.java` — fail-closed decrypt, channel param
- `1mythos/9946/dwbo-bic-sso/src/test/java/com/fis/bic/sso/helper/SlapiTokenHandlerTest.java` (to restore)
- `1mythos/9946/dwbo-bic-sso/src/test/resources/bicsso_config.properties` (to restore)
- `1mythos/9946/dwbo-bic-sso/pom.xml` — **already fixed on `master`** via PR #37; no `develop` change

---

## Verification
1. `mvn clean test` — `SecretKeyStorageTest` and `SlapiTokenHandlerTest` pass; note if the BOE/SAP SDK gap still blocks the rest of the build.
2. Fail-closed verification: `SecretKeyStorage.readRequiredSecret` throws when unset; `LoginHelper` no longer calls `unlockUser`; `SlapiTokenHandler.getDecryptedCredentials` throws on a bad token instead of returning empty credentials.
3. Entitlement-gate verification: MAC4SSO channel now runs an entitlement check before `TokenLoginBO4`, using `Mac4ApplId`.
4. `git status`/`git diff develop` per branch — clean, in-scope only.
5. Remediation comment syntax check for both UUID/SNOW pairs.

---

## Decisions & Boundaries
- Repository scope: strictly `dwbo-bic-sso`.
- Branch workflow: `develop`-first for `IBSDA-94962`/`IBSDA-94956` (real code changes needed there); `IBSDA-96108`/`f091` was an exception — reapplied directly to `master` because `develop` has no corresponding vulnerable code and there was nothing to merge forward.
- Revert context: both `develop` and `master` reverts were deliberate rollbacks to redo cleanly under the current workflow — not a finding that the prior fixes were wrong. They reverted to **different baseline dates**, which is why `master` needed its own direct fix for `f091` rather than waiting for a `develop` promotion.
- Fix fidelity: this redo replicates the **real, previously-merged** fix content (verified via `git show`/`git diff` against the actual reverted commits), not the Mythos static-patch-recommendation content that was used to draft the first version of this plan — the two differ in file scope, method/property names, and code structure.
- Cross-repo ciphertext dependency: same as before — rotating the SSO trust-anchor key requires re-encrypting `BO4SecretKey` in `IBSMAS-dwbo-bic-sso-config` across 8 environments.
- Build/test blocker: this repo is one of 4 blocked on the BOE/SAP Client SDK Artifactory provisioning gap.
- PR structure: one PR for `IBSDA-94962`, one PR for `IBSDA-94956` (bundling its three sub-fixes, matching how the ticket was actually structured historically) — not three separate PRs by current-finding-ID as originally planned.

---

## Key storage alignment (added 2026-09-28)

Measured against [KEY_AND_KEYSTORE_STORAGE_STANDARD.md](../../docs-copilot/KEY_AND_KEYSTORE_STORAGE_STANDARD.md) §4–§6 and §9.2. Advisory: it does not change the scope of PR #38/#39.

**Re-measured 2026-10-17 against the revised standard.**
- §8.4 is now the model: Harness deploys only, applications read Vault at run time through a local Vault
  Proxy, and the key custodian places the Proxy secret_id.
- §5 rank 1 names the `vault-client` library, and §9.2 cites this app as the reference implementation.
- This app's remaining open items are standard Q14–Q16 and Q20.

| Gap | Standard | Action |
|---|---|---|
| `BICSSO_BO4_KEK` / `_IV` come from an env var or system property, with no source or tier separation | §4, §5 rank 1 (was rank 4), §8.4 | **Done in code 2026-10-17 (target state):** `SecretKeyStorage.readConfiguredBO4SecretKey` reads the trusted-auth secret itself from Vault KV `secret/ibsmas/<tier>/dwbo-bic-sso/bo4-trusted-auth` via the local Vault Proxy. The KEK layer and `BICSSO_BO4_KEK`/`_IV` are removed |
| A static IV is treated as a secret | §6 | **Resolved 2026-10-17:** the IV and KEK are removed, along with the ciphertext they protected. No AES-GCM follow-up is needed for this app (§6 records the removal) |
| All three SSO channels share one prod RSA keypair (f024 residual) | §3.7 | Raise a Jira ticket for **per-channel keypairs** (LP/CM/MAC4), coordinated with the SLAPI token issuer. "Service Delivery follow-up" alone is not tracked work |
| The trusted-auth shared secret was recoverable from the repo | §3.5, runbook §2 | Name the BOE admin as rotation owner. Rotate it in the CMC and Vault together per tier before Readiness/Production and before the history purge of `IBSMAS-dwbo-bic-sso-config` (standard Q16) |
| `bicsso_config.properties` ships committed ciphertext for 8 environments | §3.1, §8.1, §8.4 | Remove `BO4SecretKey` from the config repo. The per-tier value lives only in Vault (`secret/ibsmas/<tier>/dwbo-bic-sso/bo4-trusted-auth`) and is read at run time. Harness never provisions it (§8.4) |
| This plan quoted the old hardcoded key/IV in step 8. It was redacted 2026-09-28, but the value is still in `Mythos_Remediation` git history (commit `6168bc4`, pushed to `origin/main`) | Runbook §1 step 1, §3.1 | Add this repo to the f010 exposure inventory. Rotation under IBSDA-94962 makes the value useless. Purging history (`git filter-repo --replace-text` + force-push of `main`) is optional hygiene and needs owner approval |
| f091 HTTP Artifactory URL | §8.2 | Also pin Maven ≥ 3.8.1 in the Jenkins agent image so HTTP repositories are blocked by default |
