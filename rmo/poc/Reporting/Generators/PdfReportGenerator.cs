using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using Fis.ReportMigrationOrchestrator.Core.Abstractions;

namespace Fis.ReportMigrationOrchestrator.Reporting.Generators
{
    /// <summary>
    /// Run report PDF: the HTML run report printed with Edge, so the PDF and the HTML share one design, as the
    /// readiness report does.
    /// </summary>
    public sealed class PdfReportGenerator : IReportGenerator
    {
        private readonly HtmlReportGenerator _html;

        public PdfReportGenerator(IAuditLogger auditLogger)
        {
            _html = new HtmlReportGenerator(auditLogger);
        }

        public bool CanGenerate(string format)
        {
            return string.Equals(format, "pdf", StringComparison.OrdinalIgnoreCase);
        }

        public async Task<string> GenerateAsync(Guid runId, string format, string outputPath, CancellationToken cancellationToken)
        {
            var htmlPath = await _html.GenerateAsync(runId, "html", outputPath, cancellationToken).ConfigureAwait(false);
            var filePath = Path.Combine(outputPath, "report.pdf");
            await EdgePdfPrinter.PrintAsync(htmlPath, filePath, cancellationToken).ConfigureAwait(false);
            EdgePdfPrinter.RemoveRepeatedLinks(filePath);
            return filePath;
        }
    }
}
