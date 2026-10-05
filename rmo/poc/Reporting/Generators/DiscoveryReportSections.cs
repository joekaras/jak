using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Encodings.Web;
using System.Text.Json;
using System.Xml;
using System.Xml.Linq;
using Fis.ReportMigrationOrchestrator.Core.Domain;
using static Fis.ReportMigrationOrchestrator.Reporting.Generators.DiscoveryReportShared;

namespace Fis.ReportMigrationOrchestrator.Reporting.Generators
{
    /// <summary>
    /// The two detail sections of the readiness report: reports that will not convert (grouped by reason) and reports
    /// that will convert (grouped by Snowflake universe). Each has a no-script drill-down and a print-only flat copy.
    /// </summary>
    internal static class DiscoveryReportSections
    {
        public const string NotConvertingId = "not-converting";
        public const string ConvertingId = "converting";
        private const string NoQueries = "Excel workbooks (no queries)";

        public static string NotConverting(MigrationDiscovery discovery, IReadOnlyDictionary<string, ScheduleInfo> schedules)
        {
            var notReady = discovery.Documents
                .Where(x => !x.ReadyToMigrate)
                .OrderBy(x => x.Name, StringComparer.OrdinalIgnoreCase)
                .Select(x => (Document: x, Reasons: x.Reasons()))
                .ToList();
            var jobs = notReady.ToDictionary(x => x.Document.DocumentId, x => JobsUsing(x.Document, discovery.Jobs));
            var byReason = notReady
                .SelectMany(x => x.Reasons.Select(code => (Code: code, Item: x)))
                .GroupBy(x => x.Code)
                .OrderByDescending(x => x.Count())
                .ThenBy(x => x.Key)
                .ToList();
            var queries = notReady.Sum(x => x.Document.DataProviders.Count(dp => dp.Reason() is not null));
            var affectedJobs = jobs.Values.SelectMany(x => x).Select(x => x.Job.JobId).Distinct().Count();

            var html = new StringBuilder();
            html.AppendLine("<section class='report-section' id='" + NotConvertingId + "' aria-labelledby='" + NotConvertingId + "-title'><h2 class='section-head' id='" + NotConvertingId + "-title'><i class='key key-fail' aria-hidden='true'></i>Reports that will not convert</h2>");
            if (notReady.Count == 0)
            {
                // An empty section doesn't earn a printed page of its own.
                return html.ToString().Replace("class='report-section'", "class='report-section report-section-empty'") + "<div class='card empty'><h3 class='card-title'>Every report is ready to convert</h3><p class='muted'>No exceptions were found in this discovery.</p></div></section>";
            }

            html.AppendLine("<div class='card summary'><p class='lede'><strong class='figure fail-ink'>" + Plural(notReady.Count, "report", "reports") + "</strong> of " + discovery.Documents.Count + " will not convert.</p><dl class='stats'>");
            html.AppendLine(Stat(byReason.Count, byReason.Count == 1 ? "reason" : "reasons", "see below for what to do"));
            html.AppendLine(Stat(queries, queries == 1 ? "query cannot be remapped" : "queries cannot be remapped", "across these reports"));
            html.AppendLine(Stat(affectedJobs, affectedJobs == 1 ? "job is affected" : "jobs are affected", "scheduled or on demand"));
            html.AppendLine("</dl></div>");

            html.AppendLine("<div class='card'><h3 class='card-title'>Why reports will not convert</h3><div class='table-wrap'><table><thead><tr><th>Reason</th><th class='num'>Reports</th><th class='num'>Queries</th><th>Action by</th></tr></thead><tbody>");
            foreach (var group in byReason)
            {
                var reason = ConversionReasons.Get(group.Key);
                var reasonQueries = group.Sum(x => x.Item.Document.DataProviders.Count(dp => dp.Reason() == group.Key));
                html.AppendLine("<tr><td><a href='#" + ReasonAnchor(group.Key) + "'>" + E(reason.Title) + "</a></td><td class='num'><strong>" + group.Count() + "</strong></td><td class='num'>" + (reasonQueries > 0 ? reasonQueries.ToString() : "<span class='muted'>0</span>") + "</td><td>" + (string.IsNullOrWhiteSpace(reason.ActionOwner) ? "<span class='muted'>Not set</span>" : E(reason.ActionOwner)) + "</td></tr>");
            }
            html.AppendLine("</tbody></table></div><p class='note muted'>A report is counted under every reason that applies to it.</p></div>");

            html.AppendLine("<div class='drill'>");
            foreach (var group in byReason)
            {
                var reason = ConversionReasons.Get(group.Key);
                html.AppendLine("<details class='group card' id='" + ReasonAnchor(group.Key) + "'><summary><span class='summary-title'>" + E(reason.Title) + "</span><span class='count'>" + Plural(group.Count(), "report", "reports") + "</span></summary>");
                html.Append(ReasonGuidance(reason));
                foreach (var item in group.Select(x => x.Item))
                {
                    html.Append("<details class='doc'><summary><span class='summary-title doc-name'>" + E(item.Document.Name) + "</span>");
                    if (jobs[item.Document.DocumentId].Count > 0) html.Append("<span class='tag tag-impact'>" + Plural(jobs[item.Document.DocumentId].Count, "job", "jobs") + "</span>");
                    if (item.Document.IsPersonalFolderDocument) html.Append("<span class='tag'>Personal folder</span>");
                    html.AppendLine("</summary><div class='doc-body'>" + NotConvertingBody(item.Document, item.Reasons, group.Key, jobs[item.Document.DocumentId], schedules, forPrint: false) + "</div></details>");
                }
                html.AppendLine("</details>");
            }
            html.AppendLine("</div>");

            html.AppendLine("<div class='card appendix'><h3 class='card-title'>All reports that will not convert <span class='count'>" + notReady.Count + "</span></h3><div class='table-wrap'><table><thead><tr><th>Report</th><th>Folder</th><th>Reason</th><th class='num wrap'>Jobs</th></tr></thead><tbody>");
            foreach (var item in notReady)
            {
                html.AppendLine("<tr><td class='report'>" + E(item.Document.Name) + "<div class='muted num'>ID " + item.Document.DocumentId + "</div></td><td class='folder' data-label='Folder'>" + E(item.Document.FolderPath) + "</td><td data-label='Reason'>" + string.Join("<br/>", item.Reasons.Select(code => E(ConversionReasons.Get(code).Title))) + "</td><td class='num' data-label='Jobs'>" + jobs[item.Document.DocumentId].Count + "</td></tr>");
            }
            html.AppendLine("</tbody></table></div></div>");

            // Print cannot open <details>, so each report's detail is repeated once, flat, for paper.
            html.AppendLine("<div class='print-only'><h3 class='card-title'>Details of reports that will not convert</h3>");
            foreach (var group in byReason)
            {
                html.AppendLine("<div class='print-group'><h4 class='print-group-title'>" + E(ConversionReasons.Get(group.Key).Title) + "</h4>" + ReasonGuidance(ConversionReasons.Get(group.Key)) + "</div>");
            }
            foreach (var item in notReady)
            {
                html.AppendLine("<article class='print-doc'><h4 class='print-doc-title'>" + E(item.Document.Name) + "</h4>" + NotConvertingBody(item.Document, item.Reasons, null, jobs[item.Document.DocumentId], schedules, forPrint: true) + "</article>");
            }
            html.AppendLine("</div></section>");
            return html.ToString();
        }

        public static string Converting(MigrationDiscovery discovery, IReadOnlyDictionary<string, ScheduleInfo> schedules)
        {
            var ready = discovery.Documents.Where(x => x.ReadyToMigrate).OrderBy(x => x.Name, StringComparer.OrdinalIgnoreCase).ToList();
            var jobs = ready.ToDictionary(x => x.DocumentId, x => JobsUsing(x, discovery.Jobs));
            var queries = ready.Sum(x => x.DataProviders.Count);
            var targets = ready.SelectMany(Targets).Where(x => x != NoQueries).Distinct(StringComparer.OrdinalIgnoreCase).Count();
            var withRewrites = ready.Count(x => x.CharSpaceFindings.Count > 0);
            var rewriteCount = ready.Sum(x => x.CharSpaceFindings.Sum(f => f.CharSpaceFindingExpressions.Count));
            var keptJobs = jobs.Values.SelectMany(x => x).Select(x => x.Job.JobId).Distinct().Count();
            var byTarget = ready
                .SelectMany(document => Targets(document).Select(target => (Target: target, Document: document)))
                .GroupBy(x => x.Target, StringComparer.OrdinalIgnoreCase)
                .OrderBy(x => x.Key == NoQueries ? 1 : 0)
                .ThenByDescending(x => x.Count())
                .ThenBy(x => x.Key, StringComparer.OrdinalIgnoreCase)
                .ToList();

            var html = new StringBuilder();
            html.AppendLine("<section class='report-section' id='" + ConvertingId + "' aria-labelledby='" + ConvertingId + "-title'><h2 class='section-head' id='" + ConvertingId + "-title'><i class='key key-ok' aria-hidden='true'></i>Reports that will convert</h2>");
            if (ready.Count == 0)
            {
                return html.ToString().Replace("class='report-section'", "class='report-section report-section-empty'") + "<div class='card empty'><h3 class='card-title'>No reports are ready to convert yet</h3><p class='muted'>Every discovered report has an open exception; see the section above.</p></div></section>";
            }

            html.AppendLine("<div class='card summary'><p class='lede'><strong class='figure ok-ink'>" + Plural(ready.Count, "report", "reports") + "</strong> of " + discovery.Documents.Count + " will convert to Snowflake.</p><dl class='stats'>");
            html.AppendLine(Stat(queries, queries == 1 ? "query remapped" : "queries remapped", "to " + Plural(targets, "Snowflake universe", "Snowflake universes")));
            html.AppendLine(Stat(withRewrites, withRewrites == 1 ? "report has expression rewrites" : "reports have expression rewrites", Plural(rewriteCount, "expression change", "expression changes")));
            html.AppendLine(Stat(keptJobs, keptJobs == 1 ? "job keeps running" : "jobs keep running", "against converted reports"));
            html.AppendLine("</dl></div>");

            html.AppendLine("<div class='card'><h3 class='card-title'>Where reports are going</h3><div class='table-wrap'><table><thead><tr><th>Snowflake universe</th><th class='num'>Reports</th><th class='num'>Queries</th><th class='num wrap'>Reports with rewrites</th></tr></thead><tbody>");
            foreach (var group in byTarget)
            {
                var documents = group.Select(x => x.Document).ToList();
                var groupQueries = documents.Sum(d => d.DataProviders.Count(dp => string.Equals(dp.SnowflakeDataSourceName, group.Key, StringComparison.OrdinalIgnoreCase)));
                var groupRewrites = documents.Count(d => d.CharSpaceFindings.Count > 0);
                html.AppendLine("<tr><td><a href='#" + UniverseAnchor(group.Key) + "'>" + E(group.Key) + "</a></td><td class='num'><strong>" + documents.Count + "</strong></td><td class='num'>" + (groupQueries > 0 ? groupQueries.ToString() : "<span class='muted'>0</span>") + "</td><td class='num'>" + (groupRewrites > 0 ? "<span class='tag tag-impact'>" + groupRewrites + "</span>" : "<span class='muted'>0</span>") + "</td></tr>");
            }
            html.AppendLine("</tbody></table></div><p class='note muted'>A report that queries several universes is listed under each of them. Rewrites belong to the report, not to one universe.</p></div>");

            html.AppendLine("<div class='drill'>");
            foreach (var group in byTarget)
            {
                html.AppendLine("<details class='group card' id='" + UniverseAnchor(group.Key) + "'><summary><span class='summary-title'>" + E(group.Key) + "</span><span class='count'>" + Plural(group.Count(), "report", "reports") + "</span></summary>");
                foreach (var document in group.Select(x => x.Document))
                {
                    var rewrites = document.CharSpaceFindings.Sum(f => f.CharSpaceFindingExpressions.Count);
                    html.Append("<details class='doc'><summary><span class='summary-title doc-name'>" + E(document.Name) + "</span>");
                    if (rewrites > 0) html.Append("<span class='tag tag-impact'>" + Plural(rewrites, "rewrite", "rewrites") + "</span>");
                    if (jobs[document.DocumentId].Count > 0) html.Append("<span class='tag'>" + Plural(jobs[document.DocumentId].Count, "job", "jobs") + "</span>");
                    if (document.IsPersonalFolderDocument) html.Append("<span class='tag'>Personal folder</span>");
                    html.AppendLine("</summary><div class='doc-body'>" + ConvertingBody(document, jobs[document.DocumentId], schedules, forPrint: false) + "</div></details>");
                }
                html.AppendLine("</details>");
            }
            html.AppendLine("</div>");

            html.AppendLine("<div class='card appendix'><h3 class='card-title'>All reports that will convert <span class='count'>" + ready.Count + "</span></h3><div class='table-wrap'><table><thead><tr><th>Report</th><th>Folder</th><th>Snowflake universe</th><th class='num wrap'>Expression changes</th><th class='num wrap'>Jobs</th></tr></thead><tbody>");
            foreach (var document in ready)
            {
                var rewrites = document.CharSpaceFindings.Sum(f => f.CharSpaceFindingExpressions.Count);
                html.AppendLine("<tr><td class='report'>" + E(document.Name) + "<div class='muted num'>ID " + document.DocumentId + "</div></td><td class='folder' data-label='Folder'>" + E(document.FolderPath) + "</td><td data-label='Snowflake universe'>" + string.Join("<br/>", Targets(document).Select(E)) + "</td><td class='num' data-label='Expression changes'>" + (rewrites > 0 ? rewrites.ToString() : "<span class='muted'>0</span>") + "</td><td class='num' data-label='Jobs'>" + jobs[document.DocumentId].Count + "</td></tr>");
            }
            html.AppendLine("</tbody></table></div></div>");

            html.AppendLine("<div class='print-only'><h3 class='card-title'>Details of reports that will convert</h3>");
            foreach (var document in ready)
            {
                html.AppendLine("<article class='print-doc'><h4 class='print-doc-title'>" + E(document.Name) + "</h4>" + ConvertingBody(document, jobs[document.DocumentId], schedules, forPrint: true) + "</article>");
            }
            html.AppendLine("</div></section>");
            return html.ToString();
        }

        private static string ReasonGuidance(ConversionReason reason)
        {
            var html = new StringBuilder();
            if (!string.IsNullOrWhiteSpace(reason.Explanation)) html.AppendLine("<p class='explain'>" + E(reason.Explanation) + "</p>");
            if (!string.IsNullOrWhiteSpace(reason.Action)) html.AppendLine("<p class='callout'><strong>What to do" + (string.IsNullOrWhiteSpace(reason.ActionOwner) ? string.Empty : " (" + E(reason.ActionOwner) + ")") + ":</strong> " + E(reason.Action) + "</p>");
            return html.ToString();
        }

        // forPrint: the print copy has no <details> to open, so reasons and the raw message are plain text.
        private static string NotConvertingBody(MigrationDocument document, IReadOnlyList<string> reasons, string? currentReason, IReadOnlyList<(JobInfo Job, string Relation)> jobs, IReadOnlyDictionary<string, ScheduleInfo> schedules, bool forPrint)
        {
            var html = new StringBuilder();
            html.AppendLine(Facts(document));

            var others = reasons.Where(x => x != currentReason).ToList();
            if (forPrint) html.AppendLine("<p class='note'>Reason: " + string.Join(", ", reasons.Select(x => E(ConversionReasons.Get(x).Title))) + "</p>");
            else if (others.Count > 0) html.AppendLine("<p class='note muted'>Also affected by: " + string.Join(", ", others.Select(x => "<a href='#" + ReasonAnchor(x) + "'>" + E(ConversionReasons.Get(x).Title) + "</a>")) + "</p>");

            if (document.DataProviders.Count > 0)
            {
                html.AppendLine("<h4>Queries</h4><div class='table-wrap'><table><thead><tr><th>Query</th><th>Current universe</th><th>Snowflake universe</th><th>Status</th></tr></thead><tbody>");
                foreach (var dataProvider in document.DataProviders.OrderBy(x => x.Id, StringComparer.OrdinalIgnoreCase))
                {
                    var code = dataProvider.Reason();
                    var status = code is null ? "<span class='tag tag-ok'>Mapped</span>" : "<span class='fail-ink'>" + E(ConversionReasons.Get(code).Title) + "</span>";
                    html.AppendLine("<tr><td>" + E(dataProvider.Name) + "</td><td>" + E(dataProvider.OracleDataSourceName) + "</td><td>" + (string.IsNullOrWhiteSpace(dataProvider.SnowflakeDataSourceName) ? "<span class='muted'>None</span>" : E(dataProvider.SnowflakeDataSourceName)) + "</td><td>" + status + "</td></tr>");
                }
                html.AppendLine("</tbody></table></div>");
            }

            html.Append(JobsTable(jobs, schedules, "Jobs affected"));

            if (!string.IsNullOrWhiteSpace(document.StatusMessage))
            {
                html.AppendLine(forPrint
                    ? "<div class='tech'><h4>Technical detail</h4><p class='mono'>" + E(document.StatusMessage) + "</p></div>"
                    : "<details class='tech'><summary>Technical detail</summary><p class='mono'>" + E(document.StatusMessage) + "</p></details>");
            }
            return html.ToString();
        }

        // forPrint: the full before/after definitions are left out of the print copy; they can run to several KB each.
        private static string ConvertingBody(MigrationDocument document, IReadOnlyList<(JobInfo Job, string Relation)> jobs, IReadOnlyDictionary<string, ScheduleInfo> schedules, bool forPrint)
        {
            var html = new StringBuilder();
            html.AppendLine(Facts(document));

            if (document.DataProviders.Count > 0)
            {
                html.AppendLine("<h4>Queries</h4><div class='table-wrap'><table><thead><tr><th>Query</th><th>Current universe</th><th>Snowflake universe</th></tr></thead><tbody>");
                foreach (var dataProvider in document.DataProviders.OrderBy(x => x.Id, StringComparer.OrdinalIgnoreCase))
                {
                    html.AppendLine("<tr><td>" + E(dataProvider.Name) + "</td><td>" + E(dataProvider.OracleDataSourceName) + "</td><td>" + E(dataProvider.SnowflakeDataSourceName) + "</td></tr>");
                }
                html.AppendLine("</tbody></table></div>");
            }

            if (document.CharSpaceFindings.Count > 0)
            {
                html.AppendLine("<h4>Expression rewrites</h4><p class='note muted'>Trailing spaces inside text values are removed so comparisons still match after the move to Snowflake. Removed spaces are shown as <mark class='sp'>&middot;&middot;&middot;</mark>.</p>");
                foreach (var finding in document.CharSpaceFindings)
                {
                    html.AppendLine("<div class='finding'><div class='finding-type'>" + E(Capitalise(finding.ExpressionType)) + "</div><div class='table-wrap'><table class='rewrites'><thead><tr><th>Where</th><th>Current</th><th>After conversion</th></tr></thead><tbody>");
                    foreach (var expression in finding.CharSpaceFindingExpressions)
                    {
                        html.AppendLine("<tr><td class='where'>" + E(expression.Location?.Trim()) + "</td><td class='mono expr'>" + ShowTrailingSpaces(expression.Expression) + "</td><td class='mono expr'>" + E(expression.ProposedExpression) + "</td></tr>");
                    }
                    html.AppendLine("</tbody></table></div>");
                    if (!forPrint && (!string.IsNullOrWhiteSpace(finding.OldRaw) || !string.IsNullOrWhiteSpace(finding.UpdateRaw)))
                    {
                        var before = Pretty(finding.OldRaw).Split('\n');
                        var after = Pretty(finding.UpdateRaw).Split('\n');
                        var firstChange = FirstDifference(before, after);
                        // Some producer output stores an "after" identical to "before" (seen on an alerter finding); say so, and
                        // start both panes at the first padded value instead.
                        var unchanged = firstChange < 0;
                        if (unchanged) firstChange = Array.FindIndex(before, line => ShowTrailingSpaces(line).Contains("<mark", StringComparison.Ordinal));
                        html.AppendLine("<details class='raw'><summary>Full definition (before / after)</summary>"
                            + (unchanged ? "<p class='note muted'>The stored definition after conversion is identical to the one before; the change in the table above is not reflected in it.</p>" : string.Empty)
                            + "<div class='raw-grid'>"
                            + "<div><h5>Before</h5><pre class='mono'>" + Pane(before, firstChange, ShowTrailingSpaces) + "</pre></div>"
                            + "<div><h5>After conversion</h5><pre class='mono'>" + Pane(after, firstChange, line => E(line)) + "</pre></div></div></details>");
                    }
                    html.AppendLine("</div>");
                }
            }

            html.Append(JobsTable(jobs, schedules, "Jobs"));
            return html.ToString();
        }

        private static string Facts(MigrationDocument document)
        {
            return "<dl class='facts'><dt>Report ID</dt><dd class='num'>" + document.DocumentId + "</dd><dt>Type</dt><dd>" + E(document.DocumentType) + "</dd><dt>Folder</dt><dd class='folder'>" + E(document.FolderPath) + "</dd><dt>Owner</dt><dd>" + (string.IsNullOrWhiteSpace(document.Owner) ? "<span class='muted'>Not recorded</span>" : E(document.Owner)) + "</dd>" + (document.IsPersonalFolderDocument ? "<dt>Location</dt><dd>Personal folder</dd>" : string.Empty) + "</dl>";
        }

        private static string JobsTable(IReadOnlyList<(JobInfo Job, string Relation)> jobs, IReadOnlyDictionary<string, ScheduleInfo> schedules, string heading)
        {
            if (jobs.Count == 0) return "<h4>" + E(heading) + "</h4><p class='muted'>No scheduler jobs use this report.</p>";
            var html = new StringBuilder();
            html.AppendLine("<h4>" + E(heading) + "</h4><div class='table-wrap'><table><thead><tr><th>Job</th><th>Relation</th><th>Schedule</th></tr></thead><tbody>");
            foreach (var (job, relation) in jobs)
            {
                html.AppendLine("<tr><td>" + E(job.Name) + "</td><td>" + E(relation) + "</td><td>" + ScheduleCell(job, schedules) + "</td></tr>");
            }
            html.AppendLine("</tbody></table></div>");
            return html.ToString();
        }

        private static IEnumerable<string> Targets(MigrationDocument document)
        {
            var targets = document.DataProviders.Select(x => x.SnowflakeDataSourceName).Where(x => !string.IsNullOrWhiteSpace(x)).Select(x => x!).Distinct(StringComparer.OrdinalIgnoreCase).ToList();
            return targets.Count > 0 ? targets : new[] { NoQueries };
        }

        private static string Stat(int value, string label, string detail)
        {
            return "<div><dt class='figure'>" + value + "</dt><dd><span class='stat-label'>" + E(label) + "</span><span class='muted'>" + E(detail) + "</span></dd></div>";
        }

        private static string UniverseAnchor(string target)
        {
            var slug = new string(target.ToLowerInvariant().Select(c => char.IsLetterOrDigit(c) ? c : '-').ToArray()).Trim('-');
            return "universe-" + slug;
        }

        // Index of the first line that differs between before and after, or -1 when they are identical.
        private static int FirstDifference(IReadOnlyList<string> before, IReadOnlyList<string> after)
        {
            var count = Math.Min(before.Count, after.Count);
            for (var i = 0; i < count; i++)
            {
                if (!string.Equals(before[i], after[i], StringComparison.Ordinal)) return i;
            }
            return before.Count == after.Count ? -1 : count;
        }

        // The first changed line is wrapped so CSS (scroll-initial-target) can start the pane there instead of at the top.
        private static string Pane(IReadOnlyList<string> lines, int firstChange, Func<string, string> render)
        {
            return string.Join("\n", lines.Select((line, index) => index == firstChange ? "<span class='first-change'>" + render(line) + "</span>" : render(line)));
        }

        // OldRaw/UpdateRaw arrive as one-line XML, JSON or a Webi formula; indent the first two so they can be read.
        private static string Pretty(string? raw)
        {
            var text = raw?.Trim() ?? string.Empty;
            try
            {
                if (text.StartsWith('<')) return XDocument.Parse(text).ToString();
                if (text.StartsWith('{') || text.StartsWith('['))
                {
                    using var json = JsonDocument.Parse(text);
                    return JsonSerializer.Serialize(json.RootElement, new JsonSerializerOptions { WriteIndented = true, Encoder = JavaScriptEncoder.UnsafeRelaxedJsonEscaping });
                }
            }
            catch (Exception ex) when (ex is XmlException || ex is JsonException)
            {
                // Not well-formed after all: show it as found.
            }
            return text;
        }

        private static string Capitalise(string value) { return string.IsNullOrEmpty(value) ? value : char.ToUpperInvariant(value[0]) + value.Substring(1); }
    }
}
