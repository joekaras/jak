# CLAUDE.md

Shared instructions for this folder. Both the Claude desktop **Code** tab and **Cowork** use this file. Code loads
it automatically. A Cowork project pointed at this folder needs a project instruction telling it to read
`CLAUDE.md` first. Keep shared context here rather than in either tab's memory, because the two tabs don't share
memory.

## What this is

`RmoReportingPoc` is a proof of concept for the Report Migration Orchestrator (RMO) reporting layer. It's an
ASP.NET Core minimal API on .NET 10 with a static UI in `wwwroot/index.html`. It serves migration discovery data,
generates a customer-facing readiness report per discovery, and generates run reports in HTML, PDF and CSV.
There's no git repo and no test project.

## Build and run

```bash
dotnet build RmoReportingPoc.csproj
```

```bash
dotnet run --project RmoReportingPoc.csproj
```

The URLs come from `Properties/launchSettings.json`: https://localhost:60326 and http://localhost:60327.

## Layout

- `Program.cs`: DI registration and all endpoints (`/api/discoveries...`, `/api/runs...`).
- `Core/Abstractions`, `Core/Domain`: interfaces and models in namespace `Fis.ReportMigrationOrchestrator.Core.*`.
  - `ConversionReason.cs`: why a document won't convert (`ReasonCode` first, `StatusMessage` text as fallback).
  - `AuditEventExtensions.cs`: `AuditEvent.Classify()`, the run outcome rule used by `/api/runs` and the PDF.
- `Core/Stubs`: stand-ins for real services, in namespace `RmoReportingPoc.Core.Stubs`.
  - `InMemoryAuditLogger` seeds two fixed runs: `WaveRunId` and `SmokeRunId`.
  - `StubConfigurationProvider` sets `OutputRoot` to `<content root>/runs`.
- `Reporting/Generators`: one `IReportGenerator` per format. Each declares the formats it handles in
  `CanGenerate`. `HtmlDiscoveryReportGenerator` implements `IDiscoveryReportGenerator`.
  - `HtmlDiscoveryDashboardGenerator`: the customer readiness report (`/api/discoveries/{id}/report/readiness`),
    written as one file, `<stem>-readiness.html`. Summary first, then the sections from
    `DiscoveryReportSections` (won't convert, then will convert). Shared CSS and helpers are in
    `DiscoveryReportShared`.
- `DiscoveryStore.cs`: loads every `Data/*-discovery.json` file once, at startup.
- `Data/`: sample discovery JSON, copied to the output folder.
- `docs/`: reference docs — `architecture/SYSTEM_MAP.md`, `data/DISCOVERY_JSON.md` (format of the input files)
  and `data/discovery.schema.json` (JSON Schema for them).
- `PRODUCT.md`, `DESIGN.md`, `.impeccable/`: product context and the visual system for the customer report, used by
  the Impeccable design skill.
- `runs/`: generated output. Delete it freely; it's recreated on demand.

## Gotchas

- `format=xlsx` writes a CSV named `report.xlsx.csv`. There's no real Excel output yet
  (`Reporting/Generators/ExcelReportGenerator.cs`).
- `Using Remove="Microsoft.Extensions.Configuration"` in the csproj keeps the framework's
  `IConfigurationProvider` from colliding with the RMO interface of the same name.
- JSON responses keep PascalCase property names (`PropertyNamingPolicy = null` in `Program.cs`).
- Discovery report paths are cached in memory per discovery ID, so they're lost on restart.
- The PDF generator needs `GlobalFontSettings.UseWindowsFontsUnderWindows = true`, so PDF output works on Windows
  only.
- The readiness report goes to customers as a file. Keep it one self-contained page: inline CSS, no JavaScript, no
  external fonts or images. Collapsed `<details>` don't print, so every drill-down needs a `.print-only` flat copy.
- Customer wording for each reason lives in the catalogue in `Core/Domain/ConversionReason.cs`; every code has an
  entry, and a new code without one would show as the raw code. The wording is customer-facing and some of it
  commits FIS to act, so review changes with whoever owns the customer relationship.
- A running Visual Studio debug session locks `bin\`. To check a build without stopping it, use
  `dotnet build RmoReportingPoc.csproj -p:OutDir=<scratch folder>\`.

## Rules

- No secrets, connection strings or credentials in this folder.
- Make the smallest change that does the job. Ask before adding NuGet packages or changing the target framework.
- Don't commit or push. The user reviews and submits changes.

## Rules copied from `C:\Work\CLAUDE.md`

These are copied here because Cowork doesn't load the parent file. If you change them there, update them here
too. They were written for the Asset 9946 Mythos security-remediation docs in `C:\Work`:
`KEY_AND_KEYSTORE_STORAGE_STANDARD.md`, `SECRET_ROTATION_AND_HISTORY_PURGE_RUNBOOK.md`, and the draft email
derived from them.

- **No secret values, ciphertext, or key material, ever.** Refer to secrets by name and Vault path
  (`secret/ibsmas/<tier>/<repo>/<name>`, `secret/rs/<tier>/...`), never by value. Example commands must read
  values from Vault or a secure prompt (see the PowerShell pattern in runbook §3.1).
- The two docs cross-reference each other by section number (§4, §9.3, and so on). If you renumber a section,
  update the references in both files and the email.
- References to `docs/plans/` (the five remediation plans) point at the source repo layout and don't resolve
  there. Don't "fix" them unless asked.
- Tier names are fixed: Dev/Unit, System/Test, Readiness, Production, DR. Readiness is prod-like but is not prod.
- Keep the existing style: numbered sections, tables for matrices and gaps, bold for normative requirements, and
  cited references (NIST SP 800-57/800-53, PCI DSS v4.0, OWASP ASVS, CA/B SC-081v3).
