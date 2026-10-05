using System;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Fis.ReportMigrationOrchestrator.Core.Abstractions;

namespace Fis.ReportMigrationOrchestrator.Reporting.Generators
{
    /// <summary>
    /// Generates HTML report output for migration audit data.
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
            var builder = new StringBuilder();
            builder.AppendLine("<html><head><title>Report Migration Orchestrator</title></head><body>");
            builder.AppendLine("<h1>Report Migration Orchestrator</h1>");
            builder.AppendLine("<h2>Audit Events</h2>");
            builder.AppendLine("<table border='1'><tr><th>Timestamp</th><th>Event</th><th>Object</th><th>Success</th><th>Details</th></tr>");
            foreach (var auditEvent in auditEvents.OrderBy(x => x.Timestamp))
            {
                builder.AppendLine("<tr><td>" + auditEvent.Timestamp + "</td><td>" + auditEvent.EventType + "</td><td>" + auditEvent.ObjectId + "</td><td>" + auditEvent.Success + "</td><td>" + (auditEvent.ErrorMessage ?? string.Empty) + "</td></tr>");
            }
            builder.AppendLine("</table></body></html>");
            File.WriteAllText(filePath, builder.ToString(), Encoding.UTF8);
            return filePath;
        }
    }
}
