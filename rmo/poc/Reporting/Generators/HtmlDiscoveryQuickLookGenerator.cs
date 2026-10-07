using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Fis.ReportMigrationOrchestrator.Core.Abstractions;
using Fis.ReportMigrationOrchestrator.Core.Domain;
using static Fis.ReportMigrationOrchestrator.Reporting.Generators.DiscoveryReportShared;

namespace Fis.ReportMigrationOrchestrator.Reporting.Generators
{
    /// <summary>
    /// Generates the quick-look version of the readiness report as one file: three scan lists first (documents that won't
    /// convert and why, documents whose expressions get whitespace changes, scheduler jobs that use the documents that
    /// won't convert), then the same detail sections as the readiness report, which the lists link into.
    /// </summary>
    public sealed class HtmlDiscoveryQuickLookGenerator : IDiscoveryReportGenerator
    {
        private readonly IConfigurationProvider _configurationProvider;

        public HtmlDiscoveryQuickLookGenerator(IConfigurationProvider configurationProvider)
        {
            _configurationProvider = configurationProvider;
        }

        public Task<string> GenerateAsync(MigrationDiscovery discovery, CancellationToken cancellationToken)
        {
            cancellationToken.ThrowIfCancellationRequested();
            var directory = ReportDirectory(_configurationProvider.GetConfiguration().OutputRoot ?? ".\\runs", discovery);
            Directory.CreateDirectory(directory);
            var path = Path.Combine(directory, Stem(discovery) + "-quicklook.html");

            var notReady = discovery.Documents
                .Where(x => !x.ReadyToMigrate)
                .OrderBy(x => x.FolderPath, StringComparer.OrdinalIgnoreCase)
                .ThenBy(x => x.Name, StringComparer.OrdinalIgnoreCase)
                .Select(x => (Document: x, Reasons: x.Reasons()))
                .ToList();
            var whitespace = discovery.Documents
                .Where(x => x.ReadyToMigrate && x.CharSpaceFindings.Count > 0)
                .OrderBy(x => x.FolderPath, StringComparer.OrdinalIgnoreCase)
                .ThenBy(x => x.Name, StringComparer.OrdinalIgnoreCase)
                .ToList();
            var anchors = DiscoveryReportSections.DocumentAnchors(discovery);
            // Each listed document's row in table 1 or 2, where its detail's "Back to quick look" link returns to.
            var rows = notReady.Select(x => x.Document).Concat(whitespace).ToDictionary(x => x, x => "q-" + anchors[x], (IEqualityComparer<MigrationDocument>)ReferenceEqualityComparer.Instance);
            var schedules = discovery.Schedules.GroupBy(x => x.ScheduleId).ToDictionary(x => x.Key, x => x.First());
            // One row per job and document: a job that uses two documents that won't convert is listed twice, once for each.
            var jobs = notReady
                .SelectMany(x => JobsUsing(x.Document, discovery.Jobs).Select(j => (j.Job, j.Relation, x.Document, x.Reasons)))
                .OrderBy(x => Rank(x.Job, schedules))
                .ThenBy(x => x.Job.Name, StringComparer.OrdinalIgnoreCase)
                .ThenBy(x => x.Document.Name, StringComparer.OrdinalIgnoreCase)
                .ToList();

            var html = new StringBuilder();
            html.AppendLine("<!DOCTYPE html><html lang='en'><head><meta charset='utf-8'/><meta name='viewport' content='width=device-width, initial-scale=1'/>");
            html.AppendLine("<title>Report conversion quick look - " + E(discovery.HoldingCompany) + " wave " + E(discovery.WaveId) + "</title>");
            html.AppendLine("<style>" + Css + LocalCss + "</style></head><body>");
            html.AppendLine(Header("Report conversion quick look", discovery));
            // id 'summary' is where the detail sections' "Back to summary" links land.
            html.AppendLine("<main><div id='summary' class='quick'>");

            // 1. Documents that won't convert, and why.
            html.AppendLine("<section class='card' aria-labelledby='q-fail'><h2 id='q-fail'><i class='key key-fail' aria-hidden='true'></i>Documents that will not convert <span class='count'>" + notReady.Count + "</span>" + (notReady.Count > 0 ? DetailsLink("#" + DiscoveryReportSections.NotConvertingId) : string.Empty) + "</h2>");
            if (notReady.Count == 0)
            {
                html.AppendLine("<p class='muted'>Every document in this discovery will convert.</p>");
            }
            else
            {
                html.AppendLine("<div class='table-wrap'><table><thead><tr><th>Document</th><th>Folder</th><th>Reason</th></tr></thead><tbody>");
                foreach (var (document, reasons) in notReady)
                {
                    html.AppendLine("<tr id='" + rows[document] + "'><td class='doc-cell'>" + DocumentLink(document, anchors) + TypeTag(document) + "</td><td class='folder'>" + E(document.FolderPath) + "</td><td>" + string.Join("<br/>", reasons.Select(code => "<a href='#" + ReasonAnchor(code) + "'>" + E(ConversionReasons.Get(code).Title) + "</a>")) + "</td></tr>");
                }
                html.AppendLine("</tbody></table></div>");
            }
            html.AppendLine("</section>");

            // 2. Documents that convert with whitespace changes to their expressions.
            html.AppendLine("<section class='card' aria-labelledby='q-space'><h2 id='q-space'><i class='key key-change' aria-hidden='true'></i>Documents with whitespace changes <span class='count'>" + whitespace.Count + "</span>" + (whitespace.Count > 0 ? DetailsLink("#" + DiscoveryReportSections.ConvertingId) : string.Empty) + "</h2>");
            if (whitespace.Count == 0)
            {
                html.AppendLine("<p class='muted'>No converting document needs its expressions changed.</p>");
            }
            else
            {
                html.AppendLine("<p class='explain muted'>These documents convert, but trailing spaces inside text values in their expressions are removed so comparisons still match in Snowflake.</p>");
                html.AppendLine("<div class='table-wrap'><table><thead><tr><th>Document</th><th>Folder</th><th>What changes</th></tr></thead><tbody>");
                foreach (var document in whitespace)
                {
                    html.AppendLine("<tr id='" + rows[document] + "'><td class='doc-cell'>" + DocumentLink(document, anchors) + TypeTag(document) + "</td><td class='folder'>" + E(document.FolderPath) + "</td><td>" + WhitespaceChangeSummary(document) + "</td></tr>");
                }
                html.AppendLine("</tbody></table></div>");
            }
            html.AppendLine("</section>");

            // 3. Scheduler jobs that use the documents that won't convert.
            html.AppendLine("<section class='card' aria-labelledby='q-jobs'><h2 id='q-jobs'><i class='key key-fail' aria-hidden='true'></i>Report Scheduler jobs affected <span class='count'>" + jobs.Select(x => x.Job.JobId).Distinct().Count() + "</span></h2>");
            if (jobs.Count == 0)
            {
                html.AppendLine("<p class='muted'>" + (discovery.Jobs.Count == 0 ? "No Report Scheduler jobs were found in this discovery." : "No Report Scheduler job uses a document that won't convert.") + "</p>");
            }
            else
            {
                html.AppendLine("<div class='table-wrap'><table><thead><tr><th>Job</th><th>Schedule</th><th>Document</th><th>Reason</th></tr></thead><tbody>");
                foreach (var (job, relation, document, reasons) in jobs)
                {
                    html.AppendLine("<tr><td>" + E(job.Name) + "</td><td>" + ScheduleCell(job, schedules) + "</td><td>" + DocumentLink(document, anchors) + "<div class='muted rel'>" + E(relation) + "</div></td><td>" + string.Join("<br/>", reasons.Select(code => "<a href='#" + ReasonAnchor(code) + "'>" + E(ConversionReasons.Get(code).Title) + "</a>")) + "</td></tr>");
                }
                html.AppendLine("</tbody></table></div><p class='note muted'>Enabled schedules first, then disabled ones, then jobs run on demand.</p>");
            }
            html.AppendLine("</section></div>");

            // Detail sections, shared with the readiness report. Each starts a new printed page.
            html.Append(DiscoveryReportSections.NotConverting(discovery, schedules, (rows, "Back to quick look"), fullList: false));
            html.Append(DiscoveryReportSections.Converting(discovery, schedules, (rows, "Back to quick look"), fullList: false));
            html.AppendLine("</main>");

            html.AppendLine(Footer(discovery));
            html.AppendLine("</body></html>");
            File.WriteAllText(path, html.ToString(), Encoding.UTF8);
            return Task.FromResult(path);
        }

        // Returns HTML for the "What changes" cell: encode any text that comes from data with E().
        private static string WhitespaceChangeSummary(MigrationDocument document)
        {
            // One line per expression type, largest first, e.g. "Variable formula (2)".
            return string.Join("<br/>", document.CharSpaceFindings
                .GroupBy(f => f.ExpressionType, StringComparer.OrdinalIgnoreCase)
                .Select(g => (Type: g.Key, Count: g.Sum(f => f.CharSpaceFindingExpressions.Count)))
                .OrderByDescending(x => x.Count)
                .ThenBy(x => x.Type, StringComparer.OrdinalIgnoreCase)
                .Select(x => "<span class='change'>" + E(ExpressionTypes.Title(x.Type)) + " <span class='muted'>(" + x.Count + ")</span></span>"));
        }

        // Every document is a Webi document unless tagged otherwise.
        private static string TypeTag(MigrationDocument document)
        {
            return string.Equals(document.DocumentType, "Webi", StringComparison.OrdinalIgnoreCase) ? string.Empty : "<span class='tag'>" + E(document.DocumentType) + "</span>";
        }

        // One link per medium, like the Jobs counts in the full report lists: the screen id is inside the drill-down,
        // the print id is on the flat print copy.
        private static string DocumentLink(MigrationDocument document, IReadOnlyDictionary<MigrationDocument, string> anchors)
        {
            var anchor = anchors[document];
            return "<a class='screen-only' href='#" + anchor + "'>" + E(document.Name) + "</a><a class='print-inline' href='#" + anchor + "-print'>" + E(document.Name) + "</a>";
        }

        private static string DetailsLink(string href) { return "<a class='back' href='" + href + "'>Details</a>"; }

        // Enabled schedules first, then disabled ones, then on-demand jobs (same order as the readiness report).
        private static int Rank(JobInfo job, IReadOnlyDictionary<string, ScheduleInfo> schedules)
        {
            if (string.IsNullOrEmpty(job.ScheduleId)) return 2;
            return schedules.TryGetValue(job.ScheduleId, out var schedule) && schedule.IsEnabled ? 0 : 1;
        }

        private const string LocalCss = @"
.quick h2 { display:flex; align-items:center; gap:10px; }
.quick .key-change { background:var(--impact-fill); }
.quick .doc-cell { font-weight:600; overflow-wrap:anywhere; }
.quick .folder { min-width:28%; }
.quick .rel { font-size:12px; }
.quick .change { white-space:nowrap; }
.quick .explain { margin:0 0 10px; }
@page { size:landscape; margin:12mm; }
@media print { .quick .card { break-inside:auto; } .quick h2, .quick .explain { break-after:avoid; } .quick tr { break-inside:avoid; } .quick .key { -webkit-print-color-adjust:exact; print-color-adjust:exact; } .quick thead { display:table-header-group; } }
";
    }
}
