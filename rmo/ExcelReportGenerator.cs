using System;
using System.IO;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Fis.ReportMigrationOrchestrator.Core.Abstractions;

namespace Fis.ReportMigrationOrchestrator.Reporting.Generators
{
    /// <summary>
    /// Generates delimited report output for migration audit data.
    /// </summary>
    public sealed class ExcelReportGenerator : IReportGenerator
    {
        private readonly IAuditLogger _auditLogger;

        /// <summary>
        /// Initializes a new instance of the <see cref="ExcelReportGenerator"/> class.
        /// </summary>
        /// <param name="auditLogger">The audit logger used to read run events.</param>
        public ExcelReportGenerator(IAuditLogger auditLogger)
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
            return string.Equals(format, "xlsx", StringComparison.OrdinalIgnoreCase) || string.Equals(format, "csv", StringComparison.OrdinalIgnoreCase);
        }

        /// <summary>
        /// Generates a CSV-based export of the audit data.
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
            var extension = string.Equals(format, "csv", StringComparison.OrdinalIgnoreCase) ? "csv" : "xlsx.csv";
            var filePath = Path.Combine(outputPath, "report." + extension);
            var builder = new StringBuilder();
            builder.AppendLine("Timestamp,EventType,ObjectType,ObjectId,Success,ErrorMessage");
            foreach (var auditEvent in auditEvents)
            {
                builder.AppendLine(string.Join(",",
                    Escape(auditEvent.Timestamp.ToString()),
                    Escape(auditEvent.EventType),
                    Escape(auditEvent.ObjectType),
                    Escape(auditEvent.ObjectId),
                    Escape(auditEvent.Success.ToString()),
                    Escape(auditEvent.ErrorMessage)));
            }

            File.WriteAllText(filePath, builder.ToString(), Encoding.UTF8);
            return filePath;
        }

        private static string Escape(string value)
        {
            var content = value ?? string.Empty;
            if (content.Contains(",") || content.Contains("\""))
            {
                return "\"" + content.Replace("\"", "\"\"") + "\"";
            }

            return content;
        }
    }
}
