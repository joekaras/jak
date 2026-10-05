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
    /// Generates the per-discovery readiness report as one file: a summary (job impact first, then the converting /
    /// not-converting split and its reasons), then the detail sections for reports that will not and will convert.
    /// </summary>
    public sealed class HtmlDiscoveryDashboardGenerator : IDiscoveryReportGenerator
    {
        private const int MaxJobRows = 8;

        private readonly IConfigurationProvider _configurationProvider;

        public HtmlDiscoveryDashboardGenerator(IConfigurationProvider configurationProvider)
        {
            _configurationProvider = configurationProvider;
        }

        public Task<string> GenerateAsync(MigrationDiscovery discovery, CancellationToken cancellationToken)
        {
            cancellationToken.ThrowIfCancellationRequested();
            var directory = ReportDirectory(_configurationProvider.GetConfiguration().OutputRoot ?? ".\\runs", discovery);
            Directory.CreateDirectory(directory);
            var stem = Stem(discovery);
            var path = Path.Combine(directory, stem + "-readiness.html");
            // One file: the summary links to its own detail sections, so it prints as a single PDF.
            const string convertedHref = "#" + DiscoveryReportSections.ConvertingId;
            const string exceptionsHref = "#" + DiscoveryReportSections.NotConvertingId;

            var total = discovery.Documents.Count;
            var ready = discovery.Documents.Where(x => x.ReadyToMigrate).ToList();
            var notReady = discovery.Documents.Where(x => !x.ReadyToMigrate).Select(x => (Document: x, Reasons: x.Reasons())).ToList();
            var schedules = discovery.Schedules.GroupBy(x => x.ScheduleId).ToDictionary(x => x.Key, x => x.First());

            var affected = notReady
                .SelectMany(x => JobsUsing(x.Document, discovery.Jobs).Select(j => (j.Job, x.Document, x.Reasons)))
                .GroupBy(x => x.Job.JobId)
                .Select(x => x.First())
                .OrderBy(x => Rank(x.Job, schedules))
                .ThenBy(x => x.Job.Name, StringComparer.OrdinalIgnoreCase)
                .ToList();
            var affectedSchedules = affected.Select(x => x.Job.ScheduleId).Where(x => !string.IsNullOrEmpty(x)).Distinct().ToList();
            var enabledSchedules = affectedSchedules.Count(x => schedules.TryGetValue(x!, out var s) && s.IsEnabled);
            var reasons = notReady
                .SelectMany(x => x.Reasons)
                .GroupBy(x => x)
                .Select(x => (Code: x.Key, Count: x.Count()))
                .OrderByDescending(x => x.Count)
                .ThenBy(x => x.Code)
                .ToList();

            var queries = ready.Sum(x => x.DataProviders.Count);
            var targets = ready.SelectMany(x => x.DataProviders).Select(x => x.SnowflakeDataSourceName).Where(x => !string.IsNullOrWhiteSpace(x)).Distinct(StringComparer.OrdinalIgnoreCase).Count();
            var rewrites = ready.Sum(x => x.CharSpaceFindings.Sum(f => f.CharSpaceFindingExpressions.Count));
            var keptJobs = JobsKeptRunning(discovery);

            var html = new StringBuilder();
            html.AppendLine("<!DOCTYPE html><html lang='en'><head><meta charset='utf-8'/><meta name='viewport' content='width=device-width, initial-scale=1'/>");
            html.AppendLine("<title>Report migration readiness - " + E(discovery.HoldingCompany) + " wave " + E(discovery.WaveId) + "</title>");
            html.AppendLine("<style>" + Css + LocalCss + "</style></head><body>");
            html.AppendLine(Header("Report migration readiness", discovery));

            // Impact strip: what stops running for the bank.
            if (affected.Count > 0)
            {
                // Scheduled and on-demand jobs are counted separately so the headline never overstates scheduled deliveries.
                var onDemand = affected.Count(x => string.IsNullOrEmpty(x.Job.ScheduleId));
                var scheduled = affected.Count - onDemand;
                html.AppendLine("<section class='impact' aria-labelledby='impact-title'>" + AlertIcon + "<div><h2 id='impact-title'><span class='figure'>" + Plural(affected.Count, "job uses", "jobs use") + "</span> reports that will not convert</h2>");
                html.AppendLine("<p>" + (scheduled > 0 ? Plural(scheduled, "job runs", "jobs run") + " on " + Plural(affectedSchedules.Count, "schedule", "schedules") + " (" + enabledSchedules + " enabled). " : string.Empty) + (onDemand > 0 ? Plural(onDemand, "job runs", "jobs run") + " on demand. " : string.Empty) + "Each needs its report fixed, rebuilt or retired before cut-over.</p></div></section>");
            }
            else
            {
                html.AppendLine(discovery.Jobs.Count == 0
                    ? "<section class='impact impact-clear' aria-labelledby='impact-title'>" + CheckIcon + "<div><h2 id='impact-title'>No scheduled jobs were found in this discovery</h2><p>No Report Scheduler job depends on these reports.</p></div></section>"
                    : "<section class='impact impact-clear' aria-labelledby='impact-title'>" + CheckIcon + "<div><h2 id='impact-title'>No jobs use reports that won't convert</h2><p>Every job in this discovery uses a report that is ready to convert.</p></div></section>");
            }

            // With nothing affected the impact strip already says so; the readiness cards take the full width.
            html.AppendLine(affected.Count == 0 ? "<div class='layout layout-clear'>" : "<div class='layout'>");

            // Left: affected jobs
            if (affected.Count > 0)
            {
                html.AppendLine("<section class='card jobs'><h2>Affected jobs <span class='count'>" + affected.Count + "</span></h2>");
                html.AppendLine("<div class='table-wrap'><table><thead><tr><th>Job</th><th>Report</th><th>Schedule</th></tr></thead><tbody>");
                foreach (var (job, document, documentReasons) in affected.Take(MaxJobRows))
                {
                    html.AppendLine("<tr><td>" + E(job.Name) + "</td><td>" + E(document.Name) + "<div class='why'>" + string.Join(", ", documentReasons.Select(code => "<a href='#" + ReasonAnchor(code) + "'>" + E(ConversionReasons.Get(code).Title) + "</a>")) + "</div></td><td>" + ScheduleCell(job, schedules) + "</td></tr>");
                }
                html.AppendLine("</tbody></table></div>");
                if (affected.Count > MaxJobRows)
                {
                    html.AppendLine("<p class='more'><a href='" + exceptionsHref + "'>" + Plural(affected.Count - MaxJobRows, "more job", "more jobs") + " in the details below</a></p>");
                }
                html.AppendLine("</section>");
            }

            // Right: readiness split and reasons
            html.AppendLine("<div class='side'>");
            html.AppendLine("<section class='card readiness'><h2>Readiness</h2>");
            html.AppendLine("<p class='split-line'><span class='figure big'>" + ready.Count + "</span> of " + total + " reports will convert</p>");
            html.AppendLine("<div class='bar' role='img' aria-label='" + ready.Count + " of " + total + " reports will convert, " + notReady.Count + " will not'>");
            if (ready.Count > 0) html.Append("<span class='bar-ok' style='flex:" + ready.Count + "'></span>");
            if (notReady.Count > 0) html.Append("<span class='bar-fail' style='flex:" + notReady.Count + "'></span>");
            html.AppendLine("</div>");
            html.AppendLine("<ul class='legend'><li><i class='key key-ok'></i><span class='figure'>" + ready.Count + "</span> convert <span class='muted'>" + Percent(ready.Count, total) + "</span></li><li><i class='key key-fail'></i><span class='figure'>" + notReady.Count + "</span> will not <span class='muted'>" + Percent(notReady.Count, total) + "</span></li></ul></section>");

            html.AppendLine("<section class='card reasons'><h2>Why reports won't convert</h2>");
            if (reasons.Count == 0)
            {
                html.AppendLine("<p class='muted'>Every report is ready to convert.</p>");
            }
            else
            {
                var max = reasons.Max(x => x.Count);
                html.AppendLine("<ul class='reason-list'>");
                foreach (var (code, count) in reasons)
                {
                    html.AppendLine("<li><a href='#" + ReasonAnchor(code) + "'>" + E(ConversionReasons.Get(code).Title) + "</a><span class='figure'>" + count + "</span><span class='reason-bar'><span style='width:" + (count * 100 / max) + "%'></span></span></li>");
                }
                html.AppendLine("</ul><p class='note muted'>A report can have more than one reason.</p>");
            }
            html.AppendLine("</section>");

            // Entry points into the detail reports: in the side column when the jobs table fills the left, else a full-width row.
            var entries = new StringBuilder();
            entries.AppendLine(affected.Count > 0 ? "<div class='entries entries-stacked'>" : "<div class='entries'>");
            entries.AppendLine("<section class='card entry entry-ok'><h2>Reports that will convert</h2><p>" + Plural(ready.Count, "report", "reports") + " <span aria-hidden='true'>&middot;</span> " + Plural(queries, "query", "queries") + " to " + Plural(targets, "Snowflake universe", "Snowflake universes") + " <span aria-hidden='true'>&middot;</span> " + Plural(rewrites, "expression rewrite", "expression rewrites") + " <span aria-hidden='true'>&middot;</span> " + Plural(keptJobs, "job keeps", "jobs keep") + " running</p><a class='open' href='" + convertedHref + "'>See converting reports</a></section>");
            entries.AppendLine("<section class='card entry entry-fail'><h2>Reports that will not convert</h2><p>" + Plural(notReady.Count, "report", "reports") + " <span aria-hidden='true'>&middot;</span> " + Plural(reasons.Count, "reason", "reasons") + " <span aria-hidden='true'>&middot;</span> " + Plural(affected.Count, "job", "jobs") + " affected</p><a class='open' href='" + exceptionsHref + "'>See reports that won't convert</a></section>");
            entries.AppendLine("</div>");
            if (affected.Count > 0)
            {
                html.Append(entries);
                html.AppendLine("</div></div>");
            }
            else
            {
                html.AppendLine("</div></div>");
                html.Append(entries);
            }

            // Detail sections: what customers act on first, then what converts. Each starts a new printed page.
            html.Append(DiscoveryReportSections.NotConverting(discovery, schedules));
            html.Append(DiscoveryReportSections.Converting(discovery, schedules));

            html.AppendLine(Footer(discovery));
            html.AppendLine("</body></html>");
            File.WriteAllText(path, html.ToString(), Encoding.UTF8);
            return Task.FromResult(path);
        }

        // Enabled schedules first, then disabled ones, then on-demand jobs.
        private static int Rank(JobInfo job, IReadOnlyDictionary<string, ScheduleInfo> schedules)
        {
            if (string.IsNullOrEmpty(job.ScheduleId)) return 2;
            return schedules.TryGetValue(job.ScheduleId, out var schedule) && schedule.IsEnabled ? 0 : 1;
        }

        private const string AlertIcon = "<svg class='icon' viewBox='0 0 24 24' aria-hidden='true'><path d='M12 3.5 2.8 19.5h18.4L12 3.5Z' fill='none' stroke='currentColor' stroke-width='1.8' stroke-linejoin='round'/><path d='M12 10v4.5' stroke='currentColor' stroke-width='1.8' stroke-linecap='round'/><circle cx='12' cy='17' r='1.1' fill='currentColor'/></svg>";
        private const string CheckIcon = "<svg class='icon' viewBox='0 0 24 24' aria-hidden='true'><circle cx='12' cy='12' r='9' fill='none' stroke='currentColor' stroke-width='1.8'/><path d='m7.8 12.3 2.8 2.8 5.6-5.8' fill='none' stroke='currentColor' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'/></svg>";

        private const string LocalCss = @"
.impact { display:flex; gap:14px; align-items:flex-start; background:var(--impact-tint); color:var(--impact); border-radius:10px; padding:16px 20px; margin-bottom:14px; box-shadow:inset 0 0 0 1px rgba(178,106,0,.22); }
.impact .icon { flex:none; width:26px; height:26px; margin-top:1px; }
.impact h2 { color:var(--impact); font-size:18px; margin:0 0 4px; }
.impact p { margin:0; color:#5c3a00; }
.impact-clear { background:var(--ok-tint); color:var(--ok); box-shadow:inset 0 0 0 1px rgba(10,125,50,.2); }
.impact-clear h2 { color:var(--ok); } .impact-clear p { color:#1f4d2e; }
.layout { display:grid; grid-template-columns:minmax(0,7fr) minmax(0,5fr); gap:14px; align-items:start; }
.layout .card { margin-bottom:0; } .side { display:grid; gap:14px; }
.jobs .why { font-size:12px; margin-top:2px; } .jobs .why a { color:var(--fail); }
.more { margin:10px 0 0; font-size:13px; }
.split-line { margin:0 0 12px; font-size:15px; }
.big { font-size:34px; font-weight:600; color:var(--ok); line-height:1; margin-right:4px; }
.bar { display:flex; height:14px; border-radius:7px; overflow:hidden; background:var(--line); gap:2px; }
.bar span { display:block; min-width:4px; } .bar-ok { background:var(--ok); } .bar-fail { background:var(--fail); }
.legend { list-style:none; display:flex; flex-wrap:wrap; gap:6px 20px; margin:10px 0 0; padding:0; font-size:13px; }
.key { display:inline-block; width:10px; height:10px; border-radius:2px; margin-right:6px; vertical-align:0; } .key-ok { background:var(--ok); } .key-fail { background:var(--fail); }
.reason-list { list-style:none; margin:0; padding:0; }
.reason-list li { display:grid; grid-template-columns:1fr auto; gap:4px 12px; padding:8px 0; border-bottom:1px solid var(--line); align-items:baseline; }
.reason-list li:last-child { border-bottom:none; }
.reason-list .figure { font-weight:600; }
.reason-bar { grid-column:1 / -1; height:4px; background:var(--fail-tint); border-radius:2px; overflow:hidden; }
.reason-bar span { display:block; height:100%; background:var(--fail); }
.note { font-size:12.5px; margin:10px 0 0; }
.entries { display:grid; grid-template-columns:1fr 1fr; gap:14px; margin-top:14px; } .entries-stacked { grid-template-columns:minmax(0,1fr); margin-top:0; }
.entry { margin-bottom:0; }
.entry h2::before { content:''; display:inline-block; width:10px; height:10px; border-radius:2px; margin-right:8px; background:var(--ok); vertical-align:0; }
.entry-fail h2::before { background:var(--fail); }
.entry p { margin:0 0 14px; color:var(--muted); }
.open { display:inline-block; padding:7px 14px; border-radius:6px; background:var(--navy); color:#fff; text-decoration:none; font-weight:600; font-size:13px; }
.open:hover { background:#232a4e; color:#fff; text-decoration:none; }
.layout-clear { grid-template-columns:minmax(0,1fr); } .layout-clear .side { grid-template-columns:repeat(2,minmax(0,1fr)); align-items:start; }
@media (max-width:860px) { .layout, .entries, .layout-clear .side { grid-template-columns:minmax(0,1fr); } }
@media print { .impact, .bar, .reason-bar, .key { -webkit-print-color-adjust:exact; print-color-adjust:exact; } .open, .entries { display:none; } .layout { grid-template-columns:minmax(0,7fr) minmax(0,5fr); } .layout-clear { grid-template-columns:minmax(0,1fr); } .layout-clear .side, .entries:not(.entries-stacked) { grid-template-columns:repeat(2,minmax(0,1fr)); } .jobs td, .jobs th { padding:5px 6px; } }
";
    }
}
