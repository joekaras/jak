// This file contains code that was generated with the assistance of GitHub Copilot,
// an AI Coding Assistant along with code that was written by the developer.
using System.IO;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Fis.ReportMigrationOrchestrator.Core.Abstractions;
using Fis.ReportMigrationOrchestrator.Core.Domain;

namespace Fis.ReportMigrationOrchestrator.Reporting.Generators
{
    /// <summary>Generates a client-reviewable HTML report from a discovery artifact.</summary>
    public sealed class HtmlDiscoveryReportGenerator : IDiscoveryReportGenerator
    {
        private readonly IConfigurationProvider _configurationProvider;

        public HtmlDiscoveryReportGenerator(IConfigurationProvider configurationProvider)
        {
            _configurationProvider = configurationProvider;
        }

        public Task<string> GenerateAsync(MigrationDiscovery discovery, CancellationToken cancellationToken)
        {
            cancellationToken.ThrowIfCancellationRequested();
            var root = _configurationProvider.GetConfiguration().OutputRoot ?? ".\\runs";
            var directory = Path.Combine(root, "discovery", Sanitize(discovery.HoldingCompany), Sanitize(discovery.WaveId), "reports");
            Directory.CreateDirectory(directory);
            var path = Path.Combine(directory, discovery.DiscoveredAt.UtcDateTime.ToString("yyyyMMddHHmmssfff") + "-discovery.html");
            var html = new StringBuilder();
            html.AppendLine("<html><head><title>Migration Discovery</title></head><body>");
            html.AppendLine("<h1>Migration Discovery</h1>");
            html.AppendLine("<p>Holding Company: " + E(discovery.HoldingCompany) + "<br/>Wave: " + E(discovery.WaveId) + "<br/>Environment: " + E(discovery.Environment) + "<br/>Discovered: " + discovery.DiscoveredAt + "</p>");
            html.AppendLine("<h2>Documents</h2><table border='1'><tr><th>Name</th><th>Type</th><th>Folder</th><th>Owner</th><th>Universes</th><th>CHAR trim findings</th></tr>");
            foreach (var document in discovery.Documents.OrderBy(x => x.Name))
            {
                var universes = string.Join("<br/>", document.DataProviders.Select(x => E(x.Name) + ": " + E(x.OracleDataSourceName) + " → " + E(x.SnowflakeDataSourceName)));
                var findings = string.Join("<br/>", document.CharSpaceFindings.SelectMany(finding => finding.CharSpaceFindingExpressions.Select(expression => E(finding.ExpressionType) + " — " + E(expression.Location) + ": " + E(expression.Expression))));
                html.AppendLine("<tr><td>" + E(document.Name) + "</td><td>" + E(document.DocumentType) + "</td><td>" + E(document.FolderPath) + "</td><td>" + E(document.Owner) + "</td><td>" + universes + "</td><td>" + findings + "</td></tr>");
            }
            html.AppendLine("</table><h2>Schedules and Jobs</h2>");
            foreach (var schedule in discovery.Schedules.OrderBy(x => x.Name))
            {
                html.AppendLine("<h3>Schedule: " + E(schedule.Name) + "</h3><p>Enabled: " + schedule.IsEnabled + "</p><ul>");
                foreach (var job in discovery.Jobs.Where(x => x.ScheduleId == schedule.ScheduleId).OrderBy(x => x.Name))
                {
                    html.AppendLine("<li><strong>" + E(job.Name) + "</strong> — document: " + E(job.DocumentName) + "; Oracle apps: " + E(string.Join(", ", job.DependentOracleApplicationCodes)) + "; Snowflake apps: " + E(string.Join(", ", job.DependentSnowflakeApplicationCodes)) + "</li>");
                }
                html.AppendLine("</ul>");
            }
            html.AppendLine("</body></html>");
            File.WriteAllText(path, html.ToString(), Encoding.UTF8);
            return Task.FromResult(path);
        }

        private static string E(string value) { return WebUtility.HtmlEncode(value ?? string.Empty); }
        private static string Sanitize(string value) { return string.IsNullOrWhiteSpace(value) ? "default" : value.Replace(" ", "_"); }
    }
}
