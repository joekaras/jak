using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Net;
using System.Text;
using System.Text.RegularExpressions;
using Fis.ReportMigrationOrchestrator.Core.Domain;

namespace Fis.ReportMigrationOrchestrator.Reporting.Generators
{
    /// <summary>Helpers and styles shared by the readiness report summary and its detail sections.</summary>
    internal static class DiscoveryReportShared
    {
        /// <summary>Folder all discovery reports for one holding company and wave are written to.</summary>
        public static string ReportDirectory(string outputRoot, MigrationDiscovery discovery)
        {
            return Path.Combine(outputRoot, "discovery", Sanitize(discovery.HoldingCompany), Sanitize(discovery.WaveId), "reports");
        }

        /// <summary>File name stem shared by every report of one discovery, e.g. 20260925225533667.</summary>
        public static string Stem(MigrationDiscovery discovery)
        {
            return discovery.DiscoveredAt.UtcDateTime.ToString("yyyyMMddHHmmssfff", CultureInfo.InvariantCulture);
        }

        // Left join: a job may reference a document outside the discovery (DISCOVERY_JSON.md §7).
        public static IReadOnlyList<(JobInfo Job, string Relation)> JobsUsing(MigrationDocument document, IEnumerable<JobInfo> jobs)
        {
            var result = new List<(JobInfo, string)>();
            foreach (var job in jobs)
            {
                if (job.DocumentId != 0 && job.DocumentId == document.DocumentId) result.Add((job, "Runs this report"));
                else if (job.DestinationDocumentIds.Contains(document.DocumentId)) result.Add((job, "Writes output into this report"));
            }
            return result.OrderBy(x => x.Item1.Name, StringComparer.OrdinalIgnoreCase).ToList();
        }

        // A job that uses any report that won't convert is affected, so it is not counted as running on even if its other reports convert.
        public static int JobsKeptRunning(MigrationDiscovery discovery)
        {
            var affected = discovery.Documents.Where(x => !x.ReadyToMigrate).SelectMany(x => JobsUsing(x, discovery.Jobs)).Select(x => x.Job.JobId).ToHashSet();
            return discovery.Documents.Where(x => x.ReadyToMigrate).SelectMany(x => JobsUsing(x, discovery.Jobs)).Select(x => x.Job.JobId).Where(x => !affected.Contains(x)).Distinct().Count();
        }

        public static string ScheduleCell(JobInfo job, IReadOnlyDictionary<string, ScheduleInfo> schedules)
        {
            if (string.IsNullOrEmpty(job.ScheduleId)) return "<span class='muted'>On demand</span>";
            if (!schedules.TryGetValue(job.ScheduleId, out var schedule)) return "<span class='muted'>Unknown schedule</span>";
            return E(schedule.Name) + " " + (schedule.IsEnabled ? "<span class='tag tag-impact'>Enabled</span>" : "<span class='tag'>Disabled</span>");
        }

        public static string Header(string title, MigrationDiscovery discovery)
        {
            return "<header class='page-head'><h1>" + E(title) + "</h1><p class='meta'>Holding company " + E(discovery.HoldingCompany)
                + " <span aria-hidden='true'>&middot;</span> Wave " + E(discovery.WaveId)
                + " <span aria-hidden='true'>&middot;</span> Environment " + E(discovery.Environment)
                + " <span aria-hidden='true'>&middot;</span> Discovered " + E(Date(discovery.DiscoveredAt)) + " UTC</p></header>";
        }

        public static string Footer(MigrationDiscovery discovery)
        {
            return "<footer>Generated " + E(Date(DateTimeOffset.UtcNow)) + " UTC from discovery <span class='num'>" + E(discovery.Id.ToString()) + "</span></footer>";
        }

        /// <summary>
        /// Encodes an expression and makes the trailing spaces inside a literal visible (<c>"DP   "</c>), since those
        /// spaces are exactly what the CHAR rewrite removes and are invisible in normal rendering.
        /// </summary>
        public static string ShowTrailingSpaces(string? expression)
        {
            if (string.IsNullOrEmpty(expression)) return string.Empty;
            var html = new StringBuilder();
            var last = 0;
            var literals = Literals.Matches(expression);
            foreach (Match literal in literals)
            {
                // Only the run of spaces just inside the closing quote is marked; spaces elsewhere are ordinary text.
                var quote = literal.Value[0];
                var inner = literal.Value.Substring(1, literal.Length - 2);
                var trimmed = inner.TrimEnd(' ');
                html.Append(E(expression.Substring(last, literal.Index - last)));
                html.Append(E(quote + trimmed));
                html.Append(SpaceMark(inner.Length - trimmed.Length));
                html.Append(E(quote.ToString()));
                last = literal.Index + literal.Length;
            }
            var rest = expression.Substring(last);
            if (literals.Count == 0)
            {
                // A bare value such as "DP   " has no quotes; its trailing run is the padding.
                var trimmedRest = rest.TrimEnd(' ');
                html.Append(E(trimmedRest));
                html.Append(SpaceMark(rest.Length - trimmedRest.Length));
            }
            else
            {
                html.Append(E(rest));
            }
            return html.ToString();
        }

        private static string SpaceMark(int count)
        {
            return count == 0 ? string.Empty : "<mark class='sp' title='" + count + " trailing space" + (count == 1 ? string.Empty : "s") + "'>" + new string('·', count) + "</mark>";
        }

        private static readonly Regex Literals = new("\"[^\"]*\"|'[^']*'", RegexOptions.Compiled);

        public static string Percent(int part, int total)
        {
            return (total == 0 ? 0 : part * 100.0 / total).ToString("0", CultureInfo.InvariantCulture) + "%";
        }

        public static string Plural(int count, string singular, string plural)
        {
            return count.ToString(CultureInfo.InvariantCulture) + " " + (count == 1 ? singular : plural);
        }

        // Targets the summary's impact strip (id 'summary'); PDF viewers often have no obvious back button.
        public const string BackLink = "<a class='back' href='#summary'>Back to summary</a>";

        public static string ReasonAnchor(string code) { return "reason-" + code.ToLowerInvariant().Replace('_', '-'); }
        public static string Date(DateTimeOffset value) { return value.UtcDateTime.ToString("d MMM yyyy HH:mm", CultureInfo.InvariantCulture); }
        public static string E(string? value) { return WebUtility.HtmlEncode(value ?? string.Empty); }
        public static string Sanitize(string value) { return string.IsNullOrWhiteSpace(value) ? "default" : value.Replace(" ", "_"); }

        public const string Css = @"
:root { --navy:#2d3561; --navy-ink:#dfe3f5; --ok:#0a7d32; --ok-tint:#e6f3ea; --impact:#8a5300; --impact-fill:#b26a00; --impact-tint:#fff3e0; --fail:#b00020; --fail-tint:#fbe9ec; --ink:#1a1a2e; --muted:#5f5f70; --line:#e2e3ea; --bg:#f4f5f7; --card:#fff; }
* { box-sizing:border-box; }
html { -webkit-text-size-adjust:100%; }
body { font-family:'Segoe UI',system-ui,-apple-system,Arial,sans-serif; margin:0 auto; max-width:1120px; padding:24px; color:var(--ink); background:var(--bg); line-height:1.45; font-size:14px; }
::selection { background:var(--navy); color:#fff; }
a { color:var(--navy); text-underline-offset:3px; text-decoration-thickness:1px; }
a:hover { text-decoration-thickness:2px; }
:focus-visible { outline:2px solid var(--navy); outline-offset:2px; border-radius:3px; }
.num, td.num, .figure { font-variant-numeric:tabular-nums; }
.mono { font-family:Consolas,'Cascadia Mono',monospace; font-size:12.5px; }
.muted { color:var(--muted); }
.page-head { background:var(--navy); color:#fff; padding:22px 24px 20px; border-radius:10px; margin-bottom:14px; }
.page-head h1 { font-size:24px; line-height:1.2; margin:0 0 6px; font-weight:600; letter-spacing:-0.01em; }
.page-head .meta { margin:0; color:var(--navy-ink); font-size:13px; }
.card { background:var(--card); border-radius:10px; box-shadow:0 1px 2px rgba(26,26,46,.06), 0 2px 8px rgba(26,26,46,.05); padding:18px 20px; margin-bottom:14px; }
h2, .card-title { font-size:16px; line-height:1.3; margin:0 0 12px; color:var(--navy); font-weight:600; }
h4 { font-size:13px; margin:18px 0 6px; color:var(--ink); font-weight:600; }
.report-section { margin-top:34px; }
.section-head { font-size:20px; color:var(--ink); margin:0 0 14px; padding-bottom:10px; border-bottom:1px solid var(--line); display:flex; align-items:center; gap:10px; }
.key { display:inline-block; width:10px; height:10px; border-radius:2px; flex:none; } .key-ok { background:var(--ok); } .key-fail { background:var(--fail); }
.back { margin-left:auto; font-size:12.5px; font-weight:400; color:var(--navy); white-space:nowrap; }
.ok-ink { color:var(--ok); } .fail-ink { color:var(--fail); }
.summary .lede { font-size:18px; margin:0 0 14px; } .summary .lede strong { font-weight:600; }
dl.stats { display:grid; grid-template-columns:repeat(3,1fr); gap:12px 24px; margin:0; padding-top:14px; border-top:1px solid var(--line); }
dl.stats > div { display:flex; gap:10px; align-items:baseline; }
dl.stats dt { font-size:22px; font-weight:600; color:var(--navy); }
dl.stats dd { margin:0; display:flex; flex-direction:column; font-size:13px; }
.note { font-size:12.5px; margin:10px 0 0; }
.explain { margin:4px 0 10px; max-width:75ch; }
.callout { background:var(--impact-tint); color:#5c3a00; padding:9px 12px; border-radius:6px; margin:0 0 10px; max-width:75ch; }
.finding { margin:8px 0 12px; }
.finding-type { font-size:12.5px; font-weight:600; color:var(--muted); margin-bottom:4px; }
table.rewrites td.where { font-size:12px; color:var(--muted); width:30%; word-break:break-word; }
table.rewrites td.expr { white-space:pre-wrap; word-break:break-word; }
.tech { margin-top:12px; font-size:12px; color:var(--muted); }
.tech ul { background:var(--bg); padding:8px 8px 8px 26px; border-radius:6px; margin:6px 0 0; }
.tech li { white-space:pre-wrap; word-break:break-word; } .tech li + li { margin-top:4px; }
.status-msg { margin:4px 0 0; padding-left:18px; font-size:11.5px; color:var(--muted); } .status-msg li { word-break:break-word; }
.empty { text-align:center; padding:32px 20px; } .empty .card-title { color:var(--ok); }
.print-only, .print-inline { display:none; }
.raw { margin:8px 0 4px; font-size:12px; }
.raw > summary { color:var(--navy); font-weight:600; }
.raw-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:10px; margin-top:8px; }
.raw h5 { margin:0 0 4px; font-size:12px; color:var(--muted); font-weight:600; }
.raw pre { margin:0; max-height:360px; overflow:auto; white-space:pre; background:var(--bg); padding:8px 10px; border-radius:6px; font-size:11.5px; line-height:1.4; }
.raw .first-change { display:inline-block; min-width:100%; background:var(--impact-tint); scroll-initial-target:nearest; scroll-margin-top:4.2em; }
th.num.wrap { white-space:normal; min-width:72px; }
.count { display:inline-block; min-width:24px; padding:1px 8px; border-radius:999px; background:var(--bg); color:var(--muted); font-size:12px; text-align:center; font-weight:600; font-variant-numeric:tabular-nums; }
.table-wrap { overflow-x:auto; }
table { width:100%; border-collapse:collapse; }
th, td { text-align:left; padding:8px 10px; border-bottom:1px solid var(--line); vertical-align:top; }
th { font-size:12px; color:var(--muted); font-weight:600; background:#fafafc; }
tbody tr:last-child td { border-bottom:none; }
td.num, th.num { text-align:right; white-space:nowrap; }
.folder { font-size:12px; color:var(--muted); word-break:break-word; }
.tag { display:inline-block; margin-left:6px; padding:1px 8px; border-radius:999px; font-size:11.5px; font-weight:600; background:var(--bg); color:var(--muted); vertical-align:1px; white-space:nowrap; }
.tag-impact { background:var(--impact-tint); color:var(--impact); }
.tag-ok { background:var(--ok-tint); color:var(--ok); }
.tag-fail { background:var(--fail-tint); color:var(--fail); }
details > summary { cursor:pointer; list-style:none; display:flex; align-items:center; gap:8px; }
details > summary::-webkit-details-marker { display:none; }
details > summary::before { content:''; flex:none; width:14px; height:14px; background:url(""data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 14 14'%3E%3Cpath d='M5 3l4 4-4 4' fill='none' stroke='%235f5f70' stroke-width='1.7' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E"") no-repeat center; transition:transform .18s cubic-bezier(.2,.8,.2,1); }
details[open] > summary::before { transform:rotate(90deg); }
details > summary:hover .summary-title { text-decoration:underline; text-underline-offset:3px; }
.group > summary { font-size:15px; font-weight:600; color:var(--navy); }
.group > summary .count { margin-left:auto; }
.group[open] > summary { margin-bottom:6px; }
.doc { border-top:1px solid var(--line); padding:9px 0; }
.doc > summary { flex-wrap:wrap; }
.doc-body { padding:6px 0 6px 22px; }
dl.facts { display:grid; grid-template-columns:max-content 1fr; gap:4px 16px; margin:4px 0 8px; }
dl.facts dt { color:var(--muted); } dl.facts dd { margin:0; }
mark.sp { background:var(--impact-tint); color:var(--impact); border-radius:2px; padding:0 1px; }
.doc-back-line { margin:0 0 4px; text-align:right; font-size:12.5px; }
tbody tr:target td { background:var(--impact-tint); }
footer { text-align:center; color:var(--muted); font-size:12px; margin:18px 0 4px; }
@media (max-width:720px) {
  body { padding:12px; font-size:14px; } .page-head { padding:18px 16px; } .page-head h1 { font-size:21px; } .card { padding:14px; } .doc-body { padding-left:0; } th, td { padding:7px 8px; }
  dl.facts { grid-template-columns:1fr; gap:0 0; } dl.facts dd { margin-bottom:6px; }
  dl.stats { grid-template-columns:1fr; } table.rewrites td.where { width:auto; } .section-head { font-size:18px; }
  .raw-grid { grid-template-columns:minmax(0,1fr); }
  .appendix thead { display:none; }
  .appendix tr { display:block; padding:10px 0; border-bottom:1px solid var(--line); }
  .appendix tbody tr:last-child { border-bottom:none; }
  .appendix td { display:block; border:none; padding:2px 0; text-align:left; }
  .appendix td.report { overflow-wrap:anywhere; font-weight:600; }
  .appendix td[data-label]::before { content:attr(data-label) ': '; color:var(--muted); font-size:12px; font-weight:400; }
}
@media print {
  body { background:#fff; padding:0; max-width:none; }
  .card, .page-head { box-shadow:none; break-inside:avoid; }
  .page-head, .tag, mark.sp, .bar span, .impact, .key, .callout { -webkit-print-color-adjust:exact; print-color-adjust:exact; }
  .report-section { break-before:page; margin-top:0; } .report-section-empty { break-before:auto; margin-top:24px; }
  .drill, .screen-only { display:none; } .print-only { display:block; } .print-inline { display:inline; }
  .appendix { break-inside:auto; } .appendix tr { break-inside:avoid; } .appendix .folder { min-width:30%; } .appendix td.report { overflow-wrap:anywhere; }
  .print-group { break-inside:avoid; margin-bottom:8px; } .print-group-title { margin:12px 0 4px; font-size:14px; display:flex; align-items:baseline; gap:8px; }
  .print-doc { break-inside:avoid-page; border-top:1px solid var(--line); padding-top:6px; } .print-doc-title { font-size:14px; margin:10px 0 2px; }
  .print-doc-title { display:flex; align-items:baseline; gap:8px; } .doc-back-line { display:none; }
  a { color:inherit; text-decoration:none; }
}
";
    }
}
