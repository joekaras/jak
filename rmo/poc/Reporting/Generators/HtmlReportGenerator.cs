using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using Fis.ReportMigrationOrchestrator.Core.Abstractions;
using Fis.ReportMigrationOrchestrator.Core.Domain;
using static Fis.ReportMigrationOrchestrator.Reporting.Generators.DiscoveryReportShared;

namespace Fis.ReportMigrationOrchestrator.Reporting.Generators
{
    /// <summary>
    /// Generates the HTML run report in the readiness report's style: outcome summary, then what failed, what was
    /// skipped and every event. One self-contained file, so <see cref="PdfReportGenerator"/> prints it as the PDF.
    /// </summary>
    public sealed class HtmlReportGenerator : IReportGenerator
    {
        private readonly IAuditLogger _auditLogger;

        /// <summary>
        /// Initializes a new instance of the <see cref="HtmlReportGenerator"/> class.
        /// </summary>
        /// <param name="auditLogger">The audit logger used to read run events.</param>
        public HtmlReportGenerator(IAuditLogger auditLogger)
        {
            _auditLogger = auditLogger;
        }

        /// <summary>
        /// Determines whether the generator supports the requested format.
        /// </summary>
        /// <param name="format">The requested report format.</param>
        /// <returns><see langword="true"/> when the format is supported; otherwise, <see langword="false"/>.</returns>
        public bool CanGenerate(string format)
        {
            return string.Equals(format, "html", StringComparison.OrdinalIgnoreCase);
        }

        /// <summary>
        /// Generates an HTML summary of the audit data.
        /// </summary>
        /// <param name="runId">The migration run identifier.</param>
        /// <param name="format">The requested report format.</param>
        /// <param name="outputPath">The folder where the report should be written.</param>
        /// <param name="cancellationToken">The token used to cancel the operation.</param>
        /// <returns>The path to the generated report file.</returns>
        public async Task<string> GenerateAsync(Guid runId, string format, string outputPath, CancellationToken cancellationToken)
        {
            var auditEvents = await _auditLogger.QueryAsync(runId, cancellationToken).ConfigureAwait(false);
            Directory.CreateDirectory(outputPath);
            var filePath = Path.Combine(outputPath, "report.html");
            File.WriteAllText(filePath, Build(runId, auditEvents.OrderBy(x => x.Timestamp).ToList()), Encoding.UTF8);
            return filePath;
        }

        private static string Build(Guid runId, IReadOnlyList<AuditEvent> events)
        {
            var objects = events.Where(x => x.Classify() != AuditOutcome.Lifecycle).ToList();
            var migrated = objects.Count(x => x.Classify() == AuditOutcome.Migrated);
            var skipped = objects.Where(x => x.Classify() == AuditOutcome.Skipped).ToList();
            var failed = objects.Where(x => x.Classify() == AuditOutcome.Failed).ToList();
            var started = events.FirstOrDefault(x => x.Classify() == AuditOutcome.Lifecycle && x.EventType.EndsWith("Started", StringComparison.OrdinalIgnoreCase));
            var completed = events.LastOrDefault(x => x.Classify() == AuditOutcome.Lifecycle && x.EventType.EndsWith("Completed", StringComparison.OrdinalIgnoreCase));

            var html = new StringBuilder();
            html.AppendLine("<!DOCTYPE html><html lang='en'><head><meta charset='utf-8'/><meta name='viewport' content='width=device-width, initial-scale=1'/>");
            html.AppendLine("<title>Migration run report - " + E(runId.ToString()) + "</title>");
            html.AppendLine("<style>" + Css + HtmlDiscoveryDashboardGenerator.LocalCss + LocalCss + "</style></head><body>");
            html.AppendLine("<header class='page-head'><h1>Migration run report</h1><p class='meta'>Run <span class='num'>" + E(runId.ToString()) + "</span>"
                + " <span aria-hidden='true'>&middot;</span> Started " + (started is null ? "not recorded" : E(Date(started.Timestamp)) + " UTC")
                + " <span aria-hidden='true'>&middot;</span> " + (completed is null ? "Not completed" : "Completed " + E(Date(completed.Timestamp)) + " UTC")
                + (started is not null && completed is not null ? " <span aria-hidden='true'>&middot;</span> " + Plural((int)Math.Round((completed.Timestamp - started.Timestamp).TotalMinutes), "minute", "minutes") : string.Empty)
                + "</p></header>");

            html.AppendLine("<div class='shell'><nav class='toc' aria-label='Contents'><p class='toc-title'>Contents</p><ul>"
                + "<li><a href='#summary'>Summary</a></li>"
                + "<li><a href='#failed'>Failed <span class='muted'>" + failed.Count + "</span></a></li>"
                + "<li><a href='#skipped'>Skipped <span class='muted'>" + skipped.Count + "</span></a></li>"
                + "<li><a href='#events'>All events <span class='muted'>" + events.Count + "</span></a></li></ul></nav><main>");

            // Impact strip: what needs attention after this run.
            html.AppendLine(failed.Count > 0
                ? "<section id='summary' class='impact' aria-labelledby='impact-title'>" + HtmlDiscoveryDashboardGenerator.AlertIcon + "<div><h2 id='impact-title'><span class='figure'>" + Plural(failed.Count, "object", "objects") + "</span> failed to migrate</h2><p>" + (skipped.Count > 0 ? Plural(skipped.Count, "object was", "objects were") + " skipped. " : string.Empty) + "Each failure is listed below with the error the run reported.</p></div></section>"
                : "<section id='summary' class='impact impact-clear' aria-labelledby='impact-title'>" + HtmlDiscoveryDashboardGenerator.CheckIcon + "<div><h2 id='impact-title'>No objects failed in this run</h2><p>" + (skipped.Count > 0 ? Plural(skipped.Count, "object was", "objects were") + " skipped; see below." : "Every object in the run migrated.") + "</p></div></section>");

            // Outcome split and per-type counts, side by side.
            html.AppendLine("<div class='layout layout-clear'><div class='side'>");
            html.AppendLine("<section class='card readiness'><h2>Outcome</h2>");
            html.AppendLine("<p class='split-line'><span class='figure big'>" + migrated + "</span> of " + Plural(objects.Count, "object", "objects") + " migrated</p>");
            html.AppendLine("<div class='bar' role='img' aria-label='" + migrated + " migrated, " + skipped.Count + " skipped, " + failed.Count + " failed'>");
            if (migrated > 0) html.Append("<span class='bar-ok' style='flex:" + migrated + "'></span>");
            if (skipped.Count > 0) html.Append("<span class='bar-skip' style='flex:" + skipped.Count + "'></span>");
            if (failed.Count > 0) html.Append("<span class='bar-fail' style='flex:" + failed.Count + "'></span>");
            html.AppendLine("</div><ul class='legend'>"
                + "<li><i class='key key-ok'></i><span class='figure'>" + migrated + "</span> migrated <span class='muted'>" + Percent(migrated, objects.Count) + "</span></li>"
                + "<li><i class='key key-skip'></i><span class='figure'>" + skipped.Count + "</span> skipped <span class='muted'>" + Percent(skipped.Count, objects.Count) + "</span></li>"
                + "<li><i class='key key-fail'></i><span class='figure'>" + failed.Count + "</span> failed <span class='muted'>" + Percent(failed.Count, objects.Count) + "</span></li></ul></section>");

            html.AppendLine("<section class='card'><h2>By object type</h2><div class='table-wrap'><table><thead><tr><th>Type</th><th class='num'>Migrated</th><th class='num'>Skipped</th><th class='num'>Failed</th></tr></thead><tbody>");
            foreach (var type in objects.GroupBy(x => x.ObjectType, StringComparer.OrdinalIgnoreCase).OrderBy(x => x.Key, StringComparer.OrdinalIgnoreCase))
            {
                html.AppendLine("<tr><td>" + E(type.Key) + "</td><td class='num'>" + Count(type, AuditOutcome.Migrated) + "</td><td class='num'>" + Count(type, AuditOutcome.Skipped) + "</td><td class='num'>" + Count(type, AuditOutcome.Failed) + "</td></tr>");
            }
            html.AppendLine("</tbody></table></div></section></div></div>");

            html.Append(Problems("failed", "Failed", "key-fail", failed, "No objects failed in this run."));
            html.Append(Problems("skipped", "Skipped", "key-skip", skipped, "No objects were skipped in this run."));

            html.AppendLine("<section class='run-section' id='events' aria-labelledby='events-title'><h2 class='section-head' id='events-title'>All events <span class='count'>" + events.Count + "</span>" + BackLink + "</h2>");
            html.AppendLine("<div class='card'><div class='table-wrap'><table><thead><tr><th>Time (UTC)</th><th>Event</th><th>Object</th><th>Outcome</th><th>Details</th></tr></thead><tbody>");
            foreach (var auditEvent in events)
            {
                html.AppendLine("<tr><td class='num'>" + E(auditEvent.Timestamp.UtcDateTime.ToString("HH:mm:ss")) + "</td><td>" + E(Humanize(auditEvent.EventType)) + "</td><td>" + E(auditEvent.ObjectType) + " <span class='muted num'>" + E(auditEvent.ObjectId) + "</span></td><td>" + OutcomeTag(auditEvent.Classify()) + "</td><td class='detail'>" + E(auditEvent.ErrorMessage) + "</td></tr>");
            }
            html.AppendLine("</tbody></table></div></div></section>");

            html.AppendLine("</main></div>");
            html.AppendLine("<footer>Generated " + E(Date(DateTimeOffset.UtcNow)) + " UTC from run <span class='num'>" + E(runId.ToString()) + "</span></footer>");
            html.AppendLine("</body></html>");
            return html.ToString();
        }

        // Failed or skipped objects with the reason wording the readiness report uses, when the message is recognised.
        private static string Problems(string id, string title, string key, IReadOnlyList<AuditEvent> items, string emptyText)
        {
            var html = new StringBuilder();
            html.AppendLine("<section class='run-section' id='" + id + "' aria-labelledby='" + id + "-title'><h2 class='section-head' id='" + id + "-title'><i class='key " + key + "' aria-hidden='true'></i>" + E(title) + " <span class='count'>" + items.Count + "</span>" + BackLink + "</h2>");
            if (items.Count == 0)
            {
                return html.AppendLine("<div class='card'><p class='muted'>" + E(emptyText) + "</p></div></section>").ToString();
            }
            html.AppendLine("<div class='card'><div class='table-wrap'><table><thead><tr><th>Object</th><th>Time (UTC)</th><th>What happened</th></tr></thead><tbody>");
            foreach (var item in items)
            {
                var reasons = ConversionReasons.FromMessage(item.ErrorMessage);
                var what = reasons.Count > 0 ? string.Join(", ", reasons.Select(code => E(ConversionReasons.Get(code).Title))) : "<span class='muted'>Not recognised</span>";
                html.AppendLine("<tr><td>" + E(item.ObjectType) + " <span class='muted num'>" + E(item.ObjectId) + "</span></td><td class='num'>" + E(item.Timestamp.UtcDateTime.ToString("HH:mm:ss")) + "</td><td>" + what + (string.IsNullOrWhiteSpace(item.ErrorMessage) ? string.Empty : "<div class='tech'><p class='mono'>" + E(item.ErrorMessage) + "</p></div>") + "</td></tr>");
            }
            return html.AppendLine("</tbody></table></div></div></section>").ToString();
        }

        private static string Count(IEnumerable<AuditEvent> events, AuditOutcome outcome)
        {
            var count = events.Count(x => x.Classify() == outcome);
            return count == 0 ? "<span class='muted'>0</span>" : count.ToString();
        }

        private static string OutcomeTag(AuditOutcome outcome)
        {
            return outcome switch
            {
                AuditOutcome.Migrated => "<span class='tag tag-ok'>Migrated</span>",
                AuditOutcome.Skipped => "<span class='tag tag-impact'>Skipped</span>",
                AuditOutcome.Failed => "<span class='tag tag-fail'>Failed</span>",
                _ => "<span class='tag'>Run</span>"
            };
        }

        // "DocumentMigrated" -> "Document migrated".
        private static string Humanize(string eventType)
        {
            var words = Regex.Replace(eventType ?? string.Empty, "(?<=[a-z])(?=[A-Z])", " ");
            return words.Length == 0 ? words : words[0] + words.Substring(1).ToLowerInvariant();
        }

        private const string LocalCss = @"
.bar-skip { background:var(--impact-fill); } .key-skip { background:var(--impact-fill); }
.run-section { margin-top:28px; }
.run-section .section-head { font-size:18px; }
td.detail { color:var(--muted); font-size:12.5px; word-break:break-word; }
.tech { margin-top:4px; } .tech p { margin:4px 0 0; }
@media print { .bar-skip, .key-skip { -webkit-print-color-adjust:exact; print-color-adjust:exact; } .run-section { break-inside:auto; } }
";
    }
}
