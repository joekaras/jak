---
name: checkmarx-remediation
description: Triage and remediate Checkmarx One SAST or SCA findings with evidence. Verifies each finding against source, classifies it, applies a minimal fix for confirmed issues, and writes a remediation record. Use when given a Checkmarx finding, a results export, or a request to fix or assess a Checkmarx vulnerability.
argument-hint: [results-file-or-finding] [optional: query name, severity, or path filter]
---

# Checkmarx Finding Remediation

Input: `$ARGUMENTS`. This is either a results export (JSON or SARIF from `cx results show`), or a single finding pasted into the conversation. If neither is present, ask for one and stop.

## Ground rules

- **Evidence.** Every statement about the code carries a `path:line` citation. A verdict without a traced data flow is not a verdict.
- **No guessing.** If the flow cannot be confirmed or refuted from source, the verdict is `NEEDS HUMAN`, with the specific question that would settle it.
- **Fix the code, never the scan.** Do not add exclusion filters, change presets, edit scan configuration, add suppression comments, or delete or rename code to move it out of scan scope.
- **No platform changes.** Do not change finding state, severity, or comments in Checkmarx One. Propose them in the record; a human applies them.
- **No source control actions.** Leave edits as pending changes (TFVC) or unstaged changes (Git). Do not check in, commit, push, or shelve.
- **No scans.** Do not run `cx` scans or download any scanner tooling. State that a rescan is required to confirm the fix.
- **Minimal diff.** Change only what the finding requires. No refactoring, reformatting, or renaming alongside the fix.
- **No new dependencies or version bumps** without explicit approval, except where the finding is itself an SCA dependency finding.
- **Batch size.** Work one query group at a time (all findings sharing the same query name). If the input holds several groups, list them with counts and severities, then start with the highest severity unless a filter was given.

## Phase 1: Intake

1. Extract per finding: finding or similarity ID, query name, CWE, severity, current state, language, and every node of the data-flow path (file, line, element) from source to sink.
2. Group findings that share a sink or a similarity ID. They are usually one fix.
3. Classify the finding type: **SAST** (continue to Phase 2) or **SCA** (go to Phase 5).

## Phase 2: Verify the finding against source

1. Open every node in the path. Confirm the cited element exists at or near the cited line. If the code has drifted, locate the current position and note the drift. If the code no longer exists, the verdict is `STALE`.
2. Trace the flow by hand from source to sink. Record each hop with a citation.
3. Establish whether the source is attacker-controlled: request input, file content, message payload, database value written by another party, registry, environment, or COM caller across a trust boundary.
4. Look for existing mitigation on the path: validation, encoding, parameterization, allow-lists, type conversion that constrains the value. Cite it.
5. Assign one verdict:
   - `CONFIRMED`: tainted data reaches the sink without adequate mitigation.
   - `NOT EXPLOITABLE`: the flow is broken or adequately mitigated. Cite exactly where and why.
   - `MITIGATED BUT UNRECOGNIZED`: a sound custom sanitizer exists that the query does not recognize. Treat as not exploitable; note that a query-level sanitizer override may be warranted.
   - `STALE`: the cited code is gone.
   - `NEEDS HUMAN`: cannot be settled from source.

Do not downgrade a finding because the code is internal, legacy, or "unlikely to be reached". Reachability claims need a citation like any other.

## Phase 3: Remediate (CONFIRMED only)

1. Choose the fix location. Prefer the point that covers the most grouped findings: usually the sink, or a shared helper the sink calls.
2. Use the platform's standard mechanism. Do not write a custom sanitizer, escaping routine, or cryptographic primitive.
3. Respect the project's toolchain as found in its build files (language version, framework version, platform toolset). Do not use APIs newer than the project targets.
4. Preserve behavior for valid input. If the fix can reject input that was previously accepted, say so explicitly in the record.
5. Do not alter monetary calculation or rounding logic as a side effect. If a fix touches such code, stop and flag it.

Reference fixes by category:

| Category | Standard fix |
|---|---|
| SQL injection | Parameterized commands / prepared statements. Allow-list for identifiers that cannot be parameterized. |
| XXE | Java: `disallow-doctype-decl` on the parser factory, external entities off. .NET: `DtdProcessing.Prohibit`, `XmlResolver = null`. C++ (MSXML): `ProhibitDTD` true, `ResolveExternals` false. |
| Path traversal | Canonicalize, then verify the result is inside the intended base directory. |
| Command injection | Avoid the shell. Pass an argument array to the process API. Allow-list the executable. |
| XSS | Context-appropriate output encoding at the point of output. |
| Unsafe deserialization | Replace type-unsafe formatters (`BinaryFormatter`, native Java serialization, `pickle`) or restrict to an allow-list of types. |
| Weak or misused crypto | AES-256 with an authenticated mode, platform providers only (Windows CNG / BCrypt, JCA, .NET CNG classes). Random IV per message. No hard-coded keys. |
| Hard-coded secrets | Move to the project's existing secret or configuration mechanism. Report that the exposed secret needs rotation. |
| Buffer and integer issues (C/C++) | Bounded APIs with explicit sizes, size checks before arithmetic, RAII ownership. |
| Weak TLS / certificate validation | Remove validation bypasses. Enforce the platform default or stricter. |

## Phase 4: Verify the fix

1. Re-trace the path from Phase 2 against the changed code and show where it is now broken.
2. Search for sibling occurrences of the same pattern in the same component. List them. Fix only those in the current findings; report the rest.
3. Identify the build and test commands from the project's build or CI files. Run them only if the user has asked for that; otherwise list them.
4. Where a test project already exists for the component, add a test that exercises the malicious input and a test for valid input.
5. State plainly what was and was not verified. A fix is not confirmed until a Checkmarx rescan shows the finding resolved.

## Phase 5: SCA findings

1. Identify the package, the installed version, the vulnerability ID, and whether the dependency is direct or transitive (cite the manifest: `packages.config`, `.csproj`, `pom.xml`, `requirements.txt`).
2. Determine whether the vulnerable code is used: search for imports and calls into the affected API. Cite results. Absence of use lowers urgency but is not a fix.
3. Identify the minimum fixed version and check it against the project's constraints (target framework, JDK level, Python version).
4. If the upgrade is a patch or minor version within those constraints, make the manifest change. If it is a major version, changes the target framework, or is transitive with no clean override, do not change anything: document the options and stop.

## Phase 6: Deliverable

Write `docs/security/checkmarx/<query-name>-<YYYYMMDD>.md` under the affected component root (create the folder if needed) and summarize it in the reply. Sections:

1. **Summary table:** `Finding ID | Query | CWE | Severity | File:Line (sink) | Verdict | Action`
2. **Per finding or group:**
   - Traced data flow, hop by hop, with citations.
   - Verdict and the reasoning for it.
   - For fixes: files changed, what changed, and any behavior change for valid input.
   - For non-exploitable verdicts: a justification paragraph written for a security reviewer, suitable for pasting into the Checkmarx comment field.
3. **Proposed Checkmarx state** per finding: `Confirmed`, `Urgent`, `Proposed Not Exploitable`, or leave `To Verify`. Proposal only.
4. **Verification status:** what was run, what was not, and the rescan still required.
5. **Sibling occurrences** found but not fixed.
6. **Open questions and follow-ups:** everything marked `NEEDS HUMAN`, secrets needing rotation, upgrades needing approval.
