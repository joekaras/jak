# Discovery JSON input files

**Status:** reference — written 2026-10-04 from the two sample files in `Data/` and the C# model in `Core/Domain/`;
verify against the producer before relying on it for anything the samples don't show.

Scope: `Data/*-discovery.json`, the only input the POC reads. Each file is one **migration discovery**: a
snapshot of what the Report Migration Orchestrator (RMO) found for one holding company and wave in a
BusinessObjects environment — the Webi/Excel documents, their data providers (Oracle universe → Snowflake universe
mapping), CHAR-trailing-space findings, and the Report Scheduler jobs and schedules that run those documents.

## 1. File conventions

| Item | Value |
|---|---|
| Location | `Data/` under the content root. The csproj copies `Data\*.json` to the output folder (`PreserveNewest`). |
| Name | `<yyyyMMddHHmmssfff>-discovery.json`, UTC timestamp of `DiscoveredAt` (e.g. `20260925225533667` ↔ `2026-09-25T22:55:33.667Z`). The HTML discovery report reuses the same stem. |
| Encoding | UTF-8, pretty-printed, PascalCase property names. Non-ASCII and quotes inside `FullDefinitionJson` are `\uXXXX`-escaped. |
| Loader | `DiscoveryStore.cs`: globs `*-discovery.json`, sorts by path, deserialises with `System.Text.Json` (`PropertyNameCaseInsensitive = true`), once at startup. Unknown properties are ignored; missing properties take the C# defaults (empty string / empty list / 0 / false). |
| Served by | `GET /api/discoveries` (header + counts), `GET /api/discoveries/{id}` (whole object, unchanged), `POST/GET /api/discoveries/{id}/report[/view]` (HTML report via `HtmlDiscoveryReportGenerator`), `GET /api/discoveries/{id}/report/readiness` (customer readiness report via `HtmlDiscoveryDashboardGenerator`: summary, reports that will not convert, reports that will convert, in one file written as `<stem>-readiness.html`). |
| Schema | `docs/data/discovery.schema.json` (JSON Schema 2020-12). Both samples validate. The POC does not validate at load time. |
| Producer | Upstream RMO discovery run (UNVERIFIED: not in this repo). Files are copied in by hand. |

Sample inventory:

| File | `Id` | `HoldingCompany` | `WaveId` | `Environment` | Documents | Jobs | Schedules | Notes |
|---|---|---|---|---|---|---|---|---|
| `20260925225533667-discovery.json` | `fb1c818b-…` | `548` | `1` | `system` | 88 | 62 | 32 | Realistic bank (H548 OPAC Test Bank): 43 docs not ready, UNV universes, Excel docs, personal folders, jobs with extracts. No CHAR findings. |
| `20261001202032371-discovery.json` | `8a9972b9-…` | `999` | `1` | `system` | 26 | 0 | 4 | Synthetic CHAR/VARCHAR test set (H999): every doc ready, 12 docs with CHAR findings, no jobs. |

## 2. Top level — `MigrationDiscovery`

| Property | Type | Null? | Meaning / observed values |
|---|---|---|---|
| `Id` | GUID string | no | Discovery run id. Used as the route key in `/api/discoveries/{id}`. |
| `DiscoveredAt` | ISO-8601 with offset | no | When the run completed, UTC (`+00:00`). Drives the file name and report path. |
| `HoldingCompany` | string | no | Bank / holding-company number: `"548"`, `"999"`. Matches the `H<nnn>` prefix of every `FolderPath` and the `OrgId` inside job and schedule definitions. |
| `WaveId` | string | no | Migration wave: `"1"` in both samples. |
| `Environment` | string | no | Source BO environment tier: `"system"` in both samples. (Tier names elsewhere in this folder: Dev/Unit, System/Test, Readiness, Production, DR.) |
| `Documents` | `MigrationDocument[]` | no | See §3. |
| `Jobs` | `JobInfo[]` | no (may be `[]`) | Report Scheduler jobs, see §5. |
| `Schedules` | `ScheduleInfo[]` | no (may be `[]`) | Report Scheduler schedules, see §6. |

## 3. `Documents[]` — `MigrationDocument`

| Property | Type | Null? | Meaning / observed values |
|---|---|---|---|
| `DocumentId` | int | no | BusinessObjects SI_ID of the document. Unique within a file. |
| `DocumentCuid` | string | yes | BO CUID (23 chars, e.g. `AQCjgO9UUrpLsXNzkbWCukI`). `null` only for Excel documents in the samples. |
| `Name` | string | no | Document title. |
| `FolderPath` | string | no | `/`-separated public folder path, no leading slash, starting at the bank root folder (`H548 - OPAC Test Bank/H548_General/...`). Personal-folder documents still carry a path. |
| `Owner` | string | yes | BO owner login. `null` for 112 of 114 sample docs; set on the two Excel docs (`Administrator`, `w548fy6`). |
| `DocumentType` | string | no | `"Webi"` or `"Excel"`. Excel documents have no data providers or CUID; they are the targets of `DocumentRepository` extracts and of Excel data providers. |
| `IsPersonalFolderDocument` | bool | no | `true` when the document lives under a user's Favorites rather than a public folder (51 of 88 in the 548 sample). |
| `ReadyToMigrate` | bool | no | Overall verdict. In the samples it is exactly `StatusMessage == null`: every not-ready doc has a message and every ready doc has none. |
| `StatusMessage` | string | yes | Why the document is not ready. Either the per-data-provider messages joined with a single space (36 of 43 cases), or a document-level error when the data providers could not be read at all (then `DataProviders` is `[]`). Patterns in §8. |
| `ReasonCode` | string | yes | **Proposed, not yet emitted by the producer.** Document-level reason code from §8.1 (in practice `DP_RETRIEVAL_FAILED`). Provider-level reasons go on `DataProviders[].ReasonCode`. |
| `DataProviders` | `DataProviderInfo[]` | no | 0–6 per document. `[]` for Excel docs and for docs whose providers failed to load. |
| `CharSpaceFindings` | `CharSpaceFinding[]` | no | CHAR trailing-space findings, 0–3 per document. See §4. |

### 3.1 `DataProviders[]` — `DataProviderInfo`

One row per Webi data provider (query). `Oracle*` describes the source universe the query uses today;
`Snowflake*` describes the mapped target universe, or is empty when no mapping was resolved.

| Property | Type | Null? | Meaning / observed values |
|---|---|---|---|
| `Id` | string | no | Webi data-provider id: `DP0`, `DP1`, … `DP8`. Not globally unique — scope it by `DocumentId`. |
| `Name` | string | no | Query name as shown in Webi (`Query 1`, `Branch Location Data`, …). |
| `StatusMessage` | string | yes | Provider-level problem; `null` when resolved. Present on exactly the providers whose `Snowflake*` fields are empty. |
| `ReasonCode` | string | yes | **Proposed, not yet emitted by the producer.** Reason code from §8.1; `null` when the provider is mapped. |
| `OracleDataSourceType` | string | no | Universe kind on the Oracle side: `Unx`, `unx`, `unv`, `Excel`, `excel`. **Casing is inconsistent** (lower-case appears on unresolved providers) — compare case-insensitively. `unv` is never migratable. |
| `SnowflakeDataSourceType` | string | yes | `Unx` or `Excel` when mapped, otherwise `null`. |
| `OracleDataSourceId` | int | no | SI_ID of the source universe. `0` when the BO reference could not be found (39 of 157). |
| `OracleDataSourceCuid` | string | yes | CUID of the source universe; `null` when unresolved or for Excel providers. |
| `OracleDataSourceName` | string | no | Universe file name (`Total Customer View Daily Universe.unx`). When the universe is unresolved or is a `unv`, this holds the query name instead (same value as `Name`). |
| `SnowflakeDataSourceId` | int | no | SI_ID of the target universe; `0` when unmapped. |
| `SnowflakeDataSourceCuid` | string | yes | CUID of the target universe; `null` when unmapped. |
| `SnowflakeDataSourceName` | string | yes | Target universe name; `null` when unmapped. Usually the same file name as the Oracle side; 2 of 98 mapped providers map to a differently named target universe. |

## 4. `CharSpaceFindings[]` — `CharSpaceFinding`

Produced by the Oracle→Snowflake CHAR/VARCHAR check: places where a Webi expression compares against a value
padded with trailing spaces (`"DP   "`) that would no longer match once the column is VARCHAR in Snowflake. Each
finding is one container (a variable, a filter, a query spec) with the proposed rewrite.

| Property | Type | Null? | Meaning / observed values |
|---|---|---|---|
| `ExpressionType` | string | no | Kind of container, lower-case free text. Observed: `data provider specification` (8), `variable definition` (5), `data provider queryplan` (1), `element filter` (1), `report filter` (1), `alerter condition operand` (1). |
| `CharSpaceFindingExpressions` | `CharSpaceFindingExpression[]` | no | 1–4 hits inside the container. |
| `OldRaw` | string | yes | The whole container as found: the Webi formula, the `<queryspec:QuerySpec …>` XML, or a JSON blob (`{"alerter":…}`, `{"@custom":…,"statement":…}`). Can be several KB. |
| `UpdateRaw` | string | yes | Same container with every proposed expression applied. |

### 4.1 `CharSpaceFindingExpressions[]` — `CharSpaceFindingExpression`

| Property | Type | Null? | Meaning / observed values |
|---|---|---|---|
| `Location` | string | yes | Human-readable position: `Variable L1 definition`, `report 5 condition '[Account Application Code]' value[0]`, `alerter 1 rule 1 condition 'DP0.DO590' operand[0]`, or an indented query-plan node (`  [0] type=SubQuery  object=… operator=InList …`). |
| `Expression` | string | yes | The offending fragment or whole formula (`=If[Account Application Code]="DP   " Then …`, or just `DP   `). |
| `ProposedExpression` | string | yes | Same with trailing spaces removed inside the literal (`…="DP" Then …`). |

## 5. `Jobs[]` — `JobInfo`

Report Scheduler jobs that refresh, export or upload a document. The summary properties are derived from
`FullDefinitionJson`; the definition is the authoritative record and is kept verbatim.

| Property | Type | Null? | Meaning / derivation |
|---|---|---|---|
| `JobId` | GUID string | no | Scheduler job id. |
| `Name` | string | no | = definition `JobName`. |
| `DocumentId` | int | no | = definition `DocumentId`. **Not guaranteed to be in `Documents[]`** (40 of 62 in the 548 sample aren't — not discovered, or outside the discovery scope). `0` for `UploadFile` jobs, which have no BO document. |
| `DocumentName` | string | no | = definition `DocumentName`. For upload jobs it is the upload file path. |
| `ScheduleId` | GUID string | yes | = definition `ScheduleId`; `null` for unscheduled (on-demand) jobs, 23 of 62. When set it always matches a `Schedules[].ScheduleId` in the same file, and that schedule lists the job in `JobIds`. |
| `FullDefinitionJson` | string | no | Serialised job definition, see §5.1. CRLF-indented JSON inside a JSON string. |
| `DependentJobs` | string[] | no | Always `[]` in the samples (UNVERIFIED what populates it). |
| `DependentOracleApplicationCodes` | string[] | no | `"<AppCode>/<SubAppCode>"` for each definition `DependentApplications[]` entry (all have `SourceSystem: "O"`), e.g. `ADS ACH Production Universe/aaa`, `b3/aaa`. |
| `DependentSnowflakeApplicationCodes` | string[] | no | Same shape for Snowflake-side applications; always `[]` in the samples. |
| `DestinationDocumentIds` | int[] | no | `Extracts[].DestinationDocumentId` of every `DocumentRepository` extract (the Excel document the output is written into). |
| `ExtractPaths` | string[] | no | Distinct set of definition `UploadFilePath` (first, if present) plus every `Extracts[].DestinationPath`. Local drive paths (`D:\RSReports\`), UNC paths (`\\VWCAPBOESYSWB01.fisdev.local\…`), or a bare label (`Excel`, `UploadPersonalDataProvider`) for repository extracts. |

### 5.1 Job `FullDefinitionJson`

Parse with `JsonSerializer.Deserialize<JsonElement>(job.FullDefinitionJson)` (or `JsonDocument.Parse`). Optional
properties are simply absent, not `null`.

| Property | Type | Present | Meaning / observed values |
|---|---|---|---|
| `OrgId` | string | always | Holding company; equals the file's `HoldingCompany`. |
| `JobId`, `JobName`, `DocumentId`, `DocumentName`, `ScheduleId` | | always (`ScheduleId` optional) | As in the summary. |
| `JobType` | string | always | `RefreshReport` (no extracts), `CreateOutputFiles` (≥1 extract), `UploadFile` (has `UploadFilePath`, `DocumentId` 0). |
| `Priority` | string | always | `High`, `Medium`, `Low`. |
| `Burst` | bool | always | `false` in all samples. |
| `DocumentType` | string | always | `Webi` or `Excel`. |
| `UserId`, `ModifiedBy` | GUID string | always | Scheduler user ids; `ModifiedBy` may be the all-zero GUID. |
| `ModifiedDate` | ISO-8601, no offset | always | Local time of the scheduler DB (UNVERIFIED zone). |
| `UploadFilePath` | string | `UploadFile` jobs | Path of the file to upload. |
| `Comment` | string | rare | Free text. |
| `PostProcessingEvent` | string | rare | Command run after the job (`.bat` path). One sample value is a command-injection probe (`"& whoami ||"C:\…`) — treat as untrusted text, never execute or shell-quote it. |
| `Extracts[]` | object[] | always (may be empty) | Output definitions, below. |
| `DependentApplications[]` | object[] | always (may be empty) | `{ JobId, AppCode, SubAppCode, OrgId, SourceSystem ("O"), ApplicationDescription, Description }`. `OrgId` can be blank-padded (`"   "`). |

`Extracts[]`:

| Property | Type | Present | Meaning / observed values |
|---|---|---|---|
| `ExtractId` | GUID string | always | |
| `JobId` | GUID string | always | Back-reference to the owning job. |
| `Type` | string | always | `OutputToClient` (file on disk / share) or `DocumentRepository` (write into a BO Excel document; then `DestinationDocumentId` is present). |
| `ExtractFormat` | string | always | `CommaDelimited`, `TabDelimited`, `PipeDelimited`, `Fixed`, `Excel`, `Pdf`, `Xml`, `SimplifiedXml`. |
| `QualifierCharacter` | string | delimited formats | `"`, `'`, or `\u0000` (none). |
| `FixedFormatId` | GUID string | `Fixed` extracts | Fixed-format layout id. |
| `AllReports` | bool | always | Export every report tab vs. only `ExtractReports`. |
| `ExtractReports[]` | object[] | always (0–1) | `{ ExtractId, ReportId (int, Webi report index), ReportName }`. |
| `FileNameFormat` | string | always | `None`, `AppendDate`, `AppendDateTime`. |
| `DestinationPath` | string | always | Folder / share, or a label for repository extracts. |
| `DestinationFileName` | string | optional | Base file name; absent means the document name is used (UNVERIFIED). |
| `DestinationExtension` | string | usually | `csv`, `txt`, `dat`, `asc`, `pdf`, `xlsx`, `xml`. |
| `DestinationDocumentId` | int | `DocumentRepository` | Excel document SI_ID; also surfaced as `Jobs[].DestinationDocumentIds`. |

## 6. `Schedules[]` — `ScheduleInfo`

| Property | Type | Null? | Meaning / derivation |
|---|---|---|---|
| `ScheduleId` | GUID string | no | |
| `Name` | string | no | = definition `ScheduleName`. |
| `IsEnabled` | bool | no | = definition `Enabled`. |
| `FullDefinitionJson` | string | yes (always set in samples) | Serialised schedule definition, see §6.1. |
| `JobIds` | string[] | no | Jobs attached to this schedule; mirrors `Jobs[].ScheduleId` exactly (0–23 per schedule; 31 of 36 sample schedules have none). Definition `JobNames[]` holds the same jobs by name. |

### 6.1 Schedule `FullDefinitionJson`

| Property | Type | Present | Meaning / observed values |
|---|---|---|---|
| `ScheduleId`, `OrgId`, `ScheduleName`, `Enabled`, `JobNames[]` | | always | As above. |
| `Frequency` | string | always | `Daily`, `Weekly`, `Monthly`, `Quarterly`, `Yearly`, `SpecificDays`. |
| `StartDateTime` | ISO-8601, no offset | always | First run (date + time of day). |
| `RunEvery` | int | always | Interval multiplier (`1`, `2`). |
| `NonTaskExecutionDayOverride` | string | always | What to do when the run lands on a non-processing day: `None`, `RunAsScheduled`, `NextDay`, `PreviousDay`. |
| `DayType` | string | most | `Calendar` (only value seen). |
| `DaysOfWeek` | string | Weekly / some Monthly / Yearly | Day name, e.g. `Monday`. Only single values seen (UNVERIFIED whether multi-day is comma-separated). |
| `WeekOfMonth` | string | Monthly / Yearly with `DaysOfWeek` | `First` (only value seen). |
| `SpecificDays` | string | Monthly, Quarterly, Yearly, SpecificDays | Comma-separated days of month (`1,15`, `12,15,17`) or `M/D` dates for `SpecificDays` (`1/1,2/2,10/12,12/10`, `3/11`). |
| `SpecificMonth` | string | Yearly | Month name (`January`). |
| `PeriodIndicator` | string | some | `FromBeginning` (only value seen). |
| `NextRunDate` | ISO-8601, no offset | when computed | Absent for disabled or expired schedules. |
| `EndDate` | string | always | `Never` or a **US-format date `M/D/YYYY`** (`1/15/2026`) — not ISO. Parse with `CultureInfo.InvariantCulture`/`en-US`, not the server culture. |
| `EndDateTime` | ISO-8601, no offset | when `EndDate` ≠ `Never` | Same end date with the cut-off time. |
| `ModifiedBy`, `ModifiedDate` | | always | As in jobs. |
| `Comment` | string | optional | Free text, often a plain-English restatement of the schedule. |

## 7. Relationships and invariants (hold in both samples)

- `Documents[].DocumentId` is unique within a file; `DataProviders[].Id` is unique only within its document.
- `ReadyToMigrate == (StatusMessage == null)`; a document with `DataProviders == []` and a message is a load failure, not a document with no queries.
- A data provider is **mapped** iff `StatusMessage == null` iff `SnowflakeDataSourceName != null` (then `SnowflakeDataSourceId != 0`).
- `Jobs[].ScheduleId` ↔ `Schedules[].JobIds` are mutually consistent; every referenced id exists in the same file.
- `Jobs[].DocumentId` may reference documents that are **not** in `Documents[]`; join as a left join.
- `Extracts[].DestinationDocumentId` and `Jobs[].DestinationDocumentIds` point at Excel documents in `Documents[]` (e.g. `576354`).
- `OrgId` inside every definition equals the file's `HoldingCompany`.
- Summary fields on `JobInfo` / `ScheduleInfo` are derivable from `FullDefinitionJson` as described in §5/§6 (verified for 60/62 jobs; the two exceptions are only de-duplication of repeated `DestinationPath` values).

## 8. Status message catalogue

Observed `StatusMessage` shapes (document-level messages concatenate the provider-level ones with a space):

| Pattern | Level | Meaning |
|---|---|---|
| `Data provider (DPn - <name>) underlying BusinessObjects reference could not be found.` | provider | Universe lookup by id/CUID failed; `OracleDataSourceId` is `0`. |
| `Data provider (DPn - <name>) references a UNV universe which is not supported for migration.` | provider | Legacy `.unv`; `OracleDataSourceType` is `unv`. |
| `No Snowflake folder mapping is configured for Oracle universe '<universe>' in '<folder>'.` | provider | Universe found but no target folder mapping. |
| `Excel data provider (DPn - <name>) does not reference a discovered Excel document with ID (<id>).` | provider | Excel-backed query whose source workbook isn't in this discovery. |
| `Unable to retrieve data providers after N attempts. <exception message>` | document | Discovery could not open the document; `DataProviders` is `[]`. |

### 8.1 `ReasonCode` (proposed contract)

Free-text messages are fragile to group on, so the spec adds a structured `ReasonCode` on documents and data
providers. Until the producer emits it, consumers derive it from the message text
(`Core/Domain/ConversionReason.cs`, `ConversionReasons.Reasons()`).

| `ReasonCode` | Level | Matched message fragment (fallback) | Docs in 548 sample |
|---|---|---|---|
| `BO_REFERENCE_MISSING` | provider | `underlying BusinessObjects reference could not be found` | 24 (39 queries) |
| `DP_RETRIEVAL_FAILED` | document | `Unable to retrieve data providers` | 7 |
| `UNV_UNSUPPORTED` | provider | `references a UNV universe which is not supported` | 5 (13 queries) |
| `SNOWFLAKE_MAPPING_MISSING` | provider | `No Snowflake folder mapping is configured` | 5 (5 queries) |
| `EXCEL_SOURCE_MISSING` | provider | `does not reference a discovered Excel document` | 2 (2 queries) |
| `UNKNOWN` | either | not ready, nothing matched | 0 |

Rules:
- A document's reasons are its own `ReasonCode` plus its providers' codes, de-duplicated.
- An explicit `ReasonCode` wins over text matching.
- A not-ready document with no recognisable reason is `UNKNOWN`. Treat a non-zero `UNKNOWN` count as a new
  message shape to add here.

### 8.2 Customer wording per `ReasonCode`

The readiness report never shows the code itself. It shows the catalogue entry from `Core/Domain/ConversionReason.cs`
(`Catalog`, lines 25-64): a title, an explanation, a "What to do" action and who acts. Titles state what discovery
observed, not a cause it can't confirm.

| `ReasonCode` | Customer title | Action by | Action (summary) |
|---|---|---|---|
| `BO_REFERENCE_MISSING` | Universe not found | Customer | Point the query at an existing universe or retire the report; if the universe exists, FIS checks access and re-runs discovery. |
| `DP_RETRIEVAL_FAILED` | Report could not be read | FIS | FIS investigates and re-runs discovery; if the report won't open for the customer either, retire or rebuild it. |
| `SNOWFLAKE_MAPPING_MISSING` | No Snowflake mapping for universe | FIS | Customer confirms the universe should migrate; FIS adds the folder mapping, or the report is retired. |
| `UNV_UNSUPPORTED` | Uses an unsupported UNV universe | Customer | Rebuild on a UNX universe (or convert the universe) and include in a later wave, or retire the report. |
| `EXCEL_SOURCE_MISSING` | Excel source not found | Customer | Confirm the workbook exists so FIS can include it, or repoint the query, or retire the report. |
| `UNKNOWN` | Reason not yet identified | FIS | FIS reviews the technical detail and confirms the reason; no customer action until then. |

Several actions commit FIS to do something (re-run discovery, add a mapping, review). Review wording changes with
whoever owns the customer relationship. A new code added to §8.1 also needs an entry here and in the catalogue;
without one, the report shows the raw code.

## 9. What the POC reads today

`Program.cs` and `HtmlDiscoveryReportGenerator.cs` use: the five header fields, `Documents[]` (`Name`,
`DocumentType`, `FolderPath`, `Owner`, `DataProviders[].Name/OracleDataSourceName/SnowflakeDataSourceName`,
`CharSpaceFindings[].ExpressionType` + `CharSpaceFindingExpressions[].Location/Expression`), `Schedules[]`
(`Name`, `IsEnabled`, `ScheduleId`) and `Jobs[]` (`Name`, `DocumentName`, `ScheduleId`,
`DependentOracleApplicationCodes`, `DependentSnowflakeApplicationCodes`). `wwwroot/index.html` uses only the
header fields and counts. The readiness report (`HtmlDiscoveryDashboardGenerator.cs` with
`DiscoveryReportSections.cs`) additionally reads `ReadyToMigrate`, `StatusMessage`, `ReasonCode`, `Owner`,
`IsPersonalFolderDocument`, `DocumentId`, `DataProviders[].SnowflakeDataSourceName` (grouping key for converting
reports), `CharSpaceFindings[].ExpressionType` / `CharSpaceFindingExpressions[].Location/Expression/ProposedExpression`,
`Jobs[].DocumentId/DestinationDocumentIds` and `Schedules[].ScheduleId/Name/IsEnabled`. CHAR findings appear only in
the "will convert" section; the samples have no not-ready document with findings. Each finding's
`OldRaw`/`UpdateRaw` is shown on screen in a collapsed "Full definition (before / after)" block, indented when it
is XML or JSON, scrolled to the first change, and left out of the print copy.
Everything else (the CUIDs and source ids,
`ExtractPaths`, and both `FullDefinitionJson` payloads) is deserialised and returned verbatim by
`GET /api/discoveries/{id}` but not rendered in any report.

## 10. Gotchas for consumers

- Nothing is enforced by the loader: a file with a missing array deserialises to an empty list and a missing
  string to `""`, so "no jobs" and "property absent" look the same.
- `OracleDataSourceType` casing varies (`Unx`/`unx`, `Excel`/`excel`). Normalise before grouping.
- `EndDate` in schedule definitions is `M/D/YYYY`; everything else is ISO-8601. `ModifiedDate`, `StartDateTime`,
  `NextRunDate` and `EndDateTime` carry no offset; only the top-level `DiscoveredAt` does.
- `FullDefinitionJson` contains `\r\n`-indented JSON and can hold user-supplied strings (`PostProcessingEvent`,
  `Comment`, paths). HTML-encode on output; never pass to a shell.
- `OldRaw`/`UpdateRaw` can be large XML or JSON blobs; don't put them in table cells without truncation. The
  readiness report shows them in scrolling panes inside a collapsed block and never prints them. Each pane starts at
  the first line that differs (CSS `scroll-initial-target`, Chromium browsers; others start at the top).
- `UpdateRaw` is not always rewritten: for the alerter finding on document `10382234` (999 sample) it is identical to
  `OldRaw` and still contains `"DP   "`, although its expression says `DP` after conversion. The report notes this
  in the block. Raise with the producer.
- Treat job → document as optional: more than half the jobs in the realistic sample point at documents the
  discovery didn't capture.
- The two samples are test banks (`H548 - OPAC Test Bank`, `H999 - No Bank`); names, paths and the
  `fisdev.local` host are non-production but still internal — keep the files out of anything public.
