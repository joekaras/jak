using System;
using System.Collections.Generic;
using System.Globalization;
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
            // Internal report for service delivery and developers: a dense log with its own look, not the customer one.
            html.AppendLine("<style>" + Css + LocalCss + "</style></head><body class='ops'>");
            html.AppendLine("<header id='summary' class='ops-head'><h1>Migration run report</h1>"
                + "<p class='ops-counts'>" + Severity(AuditOutcome.Failed, failed.Count) + " <span aria-hidden='true'>&middot;</span> " + Severity(AuditOutcome.Skipped, skipped.Count) + " <span aria-hidden='true'>&middot;</span> " + Severity(AuditOutcome.Migrated, migrated) + " <span class='muted'>of " + Plural(objects.Count, "object", "objects") + "</span></p>"
                + "<dl class='kv'><div><dt>Run</dt><dd class='mono'>" + E(runId.ToString()) + "</dd></div>"
                + "<div><dt>Started</dt><dd class='mono'>" + (started is null ? "not recorded" : E(Iso(started.Timestamp))) + "</dd></div>"
                + "<div><dt>Completed</dt><dd class='mono'>" + (completed is null ? "not completed" : E(Iso(completed.Timestamp))) + "</dd></div>"
                + (started is not null && completed is not null ? "<div><dt>Duration</dt><dd class='mono'>" + E(Duration(completed.Timestamp - started.Timestamp)) + "</dd></div>" : string.Empty)
                + "</dl></header><main>");

            // Failures first, then skips: what to raise or trace, with the full error.
            var attention = failed.Concat(skipped).ToList();
            html.AppendLine("<section id='attention' aria-labelledby='attention-title'><h2 id='attention-title'>Needs attention <span class='count'>" + attention.Count + "</span></h2>");
            if (attention.Count == 0)
            {
                html.AppendLine("<p class='muted'>Nothing failed or was skipped in this run.</p>");
            }
            else
            {
                html.AppendLine("<div class='table-wrap'><table class='log'><thead><tr><th>Outcome</th><th>Time (UTC)</th><th>Object</th><th>Reason</th><th>Error</th></tr></thead><tbody>");
                foreach (var item in attention)
                {
                    var reasons = ConversionReasons.FromMessage(item.ErrorMessage);
                    var what = reasons.Count > 0 ? string.Join(", ", reasons.Select(code => E(ConversionReasons.Get(code).Title))) : "<span class='muted'>Not recognised</span>";
                    html.AppendLine("<tr class='" + RowClass(item.Classify()) + "'><td>" + Severity(item.Classify(), null) + "</td><td class='mono'>" + E(Time(item.Timestamp)) + "</td><td>" + E(item.ObjectType) + " <span class='mono'>" + E(item.ObjectId) + "</span></td><td>" + what + "</td><td class='mono err'>" + E(item.ErrorMessage) + "</td></tr>");
                }
                html.AppendLine("</tbody></table></div>");
            }
            html.AppendLine("</section>");

            html.AppendLine("<section id='types' aria-labelledby='types-title'><h2 id='types-title'>By object type</h2><div class='table-wrap'><table class='log types'><thead><tr><th>Type</th><th class='num'>Migrated</th><th class='num'>Skipped</th><th class='num'>Failed</th></tr></thead><tbody>");
            foreach (var type in objects.GroupBy(x => x.ObjectType, StringComparer.OrdinalIgnoreCase).OrderBy(x => x.Key, StringComparer.OrdinalIgnoreCase))
            {
                html.AppendLine("<tr><td>" + E(type.Key) + "</td><td class='num'>" + Count(type, AuditOutcome.Migrated) + "</td><td class='num'>" + Count(type, AuditOutcome.Skipped) + "</td><td class='num'>" + Count(type, AuditOutcome.Failed) + "</td></tr>");
            }
            html.AppendLine("</tbody></table></div></section>");

            html.AppendLine("<section id='events' aria-labelledby='events-title'><h2 id='events-title'>All events <span class='count'>" + events.Count + "</span>" + BackLink + "</h2>");
            html.AppendLine("<div class='table-wrap'><table class='log'><thead><tr><th>Time (UTC)</th><th>Event</th><th>Object</th><th>Outcome</th><th>Details</th></tr></thead><tbody>");
            foreach (var auditEvent in events)
            {
                var outcome = auditEvent.Classify();
                html.AppendLine("<tr class='" + RowClass(outcome) + "'><td class='mono'>" + E(Time(auditEvent.Timestamp)) + "</td><td>" + E(Humanize(auditEvent.EventType)) + "</td><td>" + E(auditEvent.ObjectType) + " <span class='mono'>" + E(auditEvent.ObjectId) + "</span></td><td>" + Severity(outcome, null) + "</td><td class='mono err'>" + E(auditEvent.ErrorMessage) + "</td></tr>");
            }
            html.AppendLine("</tbody></table></div></section>");

            html.AppendLine("</main>");
            html.AppendLine("<footer>Generated " + E(Date(DateTimeOffset.UtcNow)) + " UTC from run <span class='num'>" + E(runId.ToString()) + "</span></footer>");
            html.AppendLine("</body></html>");
            return html.ToString();
        }

        private static string Count(IEnumerable<AuditEvent> events, AuditOutcome outcome)
        {
            var count = events.Count(x => x.Classify() == outcome);
            return count == 0 ? "<span class='muted'>0</span>" : count.ToString();
        }

        // Severity is carried by the word and its weight. Only failures get a status colour, so migrated and skipped
        // don't borrow the readiness report's green and amber, which mean "converts" and "impact" there.
        private static string Severity(AuditOutcome outcome, int? count)
        {
            var (label, css) = outcome switch
            {
                AuditOutcome.Failed => ("Failed", "sev sev-fail"),
                AuditOutcome.Skipped => ("Skipped", "sev sev-skip"),
                AuditOutcome.Migrated => ("Migrated", "sev"),
                _ => ("Run", "sev muted")
            };
            return "<span class='" + css + "'>" + label + (count is null ? string.Empty : " <span class='num'>" + count + "</span>") + "</span>";
        }

        private static string RowClass(AuditOutcome outcome)
        {
            return outcome switch { AuditOutcome.Failed => "row-fail", AuditOutcome.Skipped => "row-skip", _ => string.Empty };
        }

        private static string Iso(DateTimeOffset value) { return value.UtcDateTime.ToString("yyyy-MM-dd'T'HH:mm:ss'Z'", CultureInfo.InvariantCulture); }
        private static string Time(DateTimeOffset value) { return value.UtcDateTime.ToString("HH:mm:ss", CultureInfo.InvariantCulture); }
        private static string Duration(TimeSpan value) { return ((int)value.TotalHours > 0 ? (int)value.TotalHours + "h " : string.Empty) + value.Minutes.ToString("00", CultureInfo.InvariantCulture) + "m " + value.Seconds.ToString("00", CultureInfo.InvariantCulture) + "s"; }

        // "DocumentMigrated" -> "Document migrated".
        private static string Humanize(string eventType)
        {
            var words = Regex.Replace(eventType ?? string.Empty, "(?<=[a-z])(?=[A-Z])", " ");
            return words.Length == 0 ? words : words[0] + words.Substring(1).ToLowerInvariant();
        }

        private const string LocalCss = @"
body.ops { background:#fff; max-width:1280px; font-size:13px; }
.ops-head { border-bottom:2px solid var(--ink); padding-bottom:10px; margin-bottom:18px; }
.ops-head h1 { font-size:18px; margin:0 0 4px; font-weight:600; }
.ops-counts { margin:0 0 8px; font-size:14px; }
.kv { display:flex; flex-wrap:wrap; gap:4px 22px; margin:0; font-size:12.5px; }
.kv div { display:flex; gap:6px; } .kv dt { color:var(--muted); } .kv dd { margin:0; word-break:break-all; }
.ops section { margin-bottom:22px; }
.ops h2 { font-size:14px; color:var(--ink); display:flex; align-items:center; gap:8px; margin:0 0 6px; }
.sev { font-weight:600; } .sev-fail { color:var(--fail); } .sev-skip { color:var(--ink); }
table.log th, table.log td { padding:4px 8px; font-size:12.5px; }
table.log .mono { font-size:12px; white-space:nowrap; }
table.log td.err { white-space:normal; word-break:break-word; color:var(--muted); }
table.log tr.row-fail td { background:var(--fail-tint); } table.log tr.row-fail td.err { color:var(--ink); }
table.log tr.row-skip td { background:var(--bg); }
table.types { width:auto; min-width:360px; }
@page { size:landscape; margin:10mm; }
@media print { body.ops { font-size:12px; } table.log thead { display:table-header-group; } table.log tr { break-inside:avoid; } table.log tr.row-fail td, table.log tr.row-skip td { -webkit-print-color-adjust:exact; print-color-adjust:exact; } }
";
    }
}
