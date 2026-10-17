# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this directory is

Not a codebase and not a git repo: no build, lint, or tests. It holds security-remediation documents for the
Asset 9946 Mythos findings (Java BOE repos plus the Report Scheduler .NET solution at `C:\projects\RS`):

- `KEY_AND_KEYSTORE_STORAGE_STANDARD.md`: the target-state standard (status: Proposed). §4 is the per-tier
  environment matrix, §5 ranks where secrets may live, §9 maps it to each repo's finding IDs (`f0xx`), §10 is the
  per-PR compliance checklist, §11 lists open questions Q1–Q20.
- `SECRET_ROTATION_AND_HISTORY_PURGE_RUNBOOK.md`: companion runbook. Rotate first, then purge (Bitbucket via
  git-filter-repo, TFVC via `tf destroy`, JFrog, logs), and record evidence in Jira.
- `Draft Email to review.eml`: raw Gmail export of the draft to Security/Ops/BOE that carries the §11 open
  questions. It's MIME with headers, so edit the doc sources and treat the email as derived.

## Rules for editing these docs

- **No secret values, ciphertext, or key material, ever.** Both docs say so explicitly. Refer to secrets by name
  and Vault path (`secret/ibsmas/<tier>/<repo>/<name>`, `secret/rs/<tier>/...`), never by value. Example commands
  must read values from Vault or a secure prompt (see the PowerShell pattern in runbook §3.1).
- The two docs cross-reference each other by section number (§4, §9.3, and so on). If you renumber a section,
  update the references in both files and the email.
- References to `docs/plans/` (the five remediation plans) point at the source repo layout and don't resolve
  here. Don't "fix" them unless asked.
- Tier names are fixed: Dev/Unit, System/Test, Readiness, Production, DR. Readiness is prod-like but is not prod.
- Keep the existing style: numbered sections, tables for matrices and gaps, bold for normative requirements, and
  cited references (NIST SP 800-57/800-53, PCI DSS v4.0, OWASP ASVS, CA/B SC-081v3).
