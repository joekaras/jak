---
version: 1
slug: "rators-htmldiscoverydashboardgenerator-cs-eff57abc"
primary_target: "Reporting/Generators/HtmlDiscoveryDashboardGenerator.cs"
related_targets: ["Reporting/Generators/DiscoveryReportSections.cs"]
---

# Discovery dashboard (report migration readiness)

Scope: per-discovery readiness report written by `HtmlDiscoveryDashboardGenerator` as one file, `<ts>-readiness.html`: this summary, then the "will not convert" and "will convert" sections (`DiscoveryReportSections`). Visitor mode: **Operate**.
Audience: bank business stakeholders signing off a wave; secondary, BO admins choosing which detail report to open.
Constraints: one self-contained file, no JS, works offline/emailed/printed; relative links to the two detail reports.
Unresolved: the reason wording in `ConversionReason.cs` is drafted but not yet reviewed by the customer-relationship owner.

## Direction contract

THESIS: Lead with what stops running for the bank, then the conversion split that explains it. Refuses the
category default of four equal KPI tiles over a pie chart.

OWN-WORLD: The incumbent RMO world: navy #2d3561 header band, white cards on #f4f5f7, status colours doing data
jobs (green #0a7d32 ready, amber #b26a00 impact, red #b00020 not converting), Segoe UI, tabular figures.

STORY: The stakeholder sees within one viewport how many scheduled deliveries break, how many reports convert and
why the rest don't, and which detail report to open next.

FIRST VIEWPORT: Navy header (title, holding company · wave · environment · date). Full-width amber impact strip
with the job/schedule count. Left 7/12: affected jobs table (job, report, schedule, state). Right 5/12: readiness
split bar 45/43 with counts, then the reason list with counts. Signature move: the impact strip and the split bar
share one scale of colour, so amber/red/green read as one sentence. Entry cards in the side column jump to the two detail sections below (hidden in print).

FORM: Delivery impact first; position 4 of 7 on the ranked list; seed key e73ae578.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
