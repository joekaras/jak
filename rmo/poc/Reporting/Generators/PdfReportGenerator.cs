using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Fis.ReportMigrationOrchestrator.Core.Abstractions;
using Fis.ReportMigrationOrchestrator.Core.Domain;
using MigraDoc.DocumentObjectModel;
using MigraDoc.DocumentObjectModel.Tables;
using MigraDoc.Rendering;

namespace Fis.ReportMigrationOrchestrator.Reporting.Generators
{
    public sealed class PdfReportGenerator : IReportGenerator
    {
        private readonly IAuditLogger _auditLogger;

        public PdfReportGenerator(IAuditLogger auditLogger)
        {
            _auditLogger = auditLogger;
        }

        public bool CanGenerate(string format)
        {
            return string.Equals(format, "pdf", StringComparison.OrdinalIgnoreCase);
        }

        public async Task<string> GenerateAsync(Guid runId, string format, string outputPath, CancellationToken cancellationToken)
        {
            var auditEvents = await _auditLogger.QueryAsync(runId, cancellationToken).ConfigureAwait(false);
            Directory.CreateDirectory(outputPath);
            var filePath = Path.Combine(outputPath, "report.pdf");
            var document = BuildDocument(runId, auditEvents);
            var renderer = new PdfDocumentRenderer { Document = document };
            renderer.RenderDocument();
            renderer.PdfDocument.Save(filePath);
            return filePath;
        }

        private static Document BuildDocument(Guid runId, IReadOnlyList<AuditEvent> auditEvents)
        {
            var document = new Document();
            var section = document.AddSection();
            section.PageSetup.PageWidth = Unit.FromCentimeter(21);
            section.PageSetup.PageHeight = Unit.FromCentimeter(29.7);
            section.PageSetup.LeftMargin = Unit.FromCentimeter(1.5);
            section.PageSetup.RightMargin = Unit.FromCentimeter(1.5);
            section.PageSetup.TopMargin = Unit.FromCentimeter(1.5);
            section.PageSetup.BottomMargin = Unit.FromCentimeter(1.5);

            AddHeading(section, "Report Migration Orchestrator", 18);
            AddHeading(section, "Migration Run Audit Report", 13);
            AddInfo(section, "Run ID", runId.ToString());
            AddInfo(section, "Generated (UTC)", DateTimeOffset.UtcNow.ToString("yyyy-MM-dd HH:mm:ss"));
            AddInfo(section, "Audit events", auditEvents.Count.ToString());
            AddInfo(section, "Succeeded", auditEvents.Count(x => x.Classify() == AuditOutcome.Migrated).ToString());
            AddInfo(section, "Skipped", auditEvents.Count(x => x.Classify() == AuditOutcome.Skipped).ToString());
            AddInfo(section, "Failed", auditEvents.Count(x => x.Classify() == AuditOutcome.Failed).ToString());
            AddHeading(section, "Audit Events", 13);

            var table = section.AddTable();
            table.Borders.Width = 0.5;
            table.AddColumn(Unit.FromCentimeter(2.7));
            table.AddColumn(Unit.FromCentimeter(2.6));
            table.AddColumn(Unit.FromCentimeter(2.0));
            table.AddColumn(Unit.FromCentimeter(2.2));
            table.AddColumn(Unit.FromCentimeter(1.4));
            table.AddColumn(Unit.FromCentimeter(7.1));

            var header = table.AddRow();
            var columns = new[] { "Timestamp (UTC)", "Event", "Object Type", "Object ID", "Success", "Error Message" };
            for (var index = 0; index < columns.Length; index++)
            {
                header.Cells[index].Shading.Color = Colors.LightGray;
                AddCell(header, index, columns[index], bold: true);
            }

            foreach (var auditEvent in auditEvents.OrderBy(x => x.Timestamp))
            {
                var row = table.AddRow();
                AddCell(row, 0, auditEvent.Timestamp.UtcDateTime.ToString("yyyy-MM-dd HH:mm:ss"));
                AddCell(row, 1, auditEvent.EventType);
                AddCell(row, 2, auditEvent.ObjectType);
                AddCell(row, 3, auditEvent.ObjectId);
                AddCell(row, 4, auditEvent.Success ? "Yes" : "No");
                AddCell(row, 5, auditEvent.ErrorMessage ?? string.Empty);
            }

            return document;
        }

        private static void AddHeading(Section section, string text, double size)
        {
            var paragraph = section.AddParagraph(text);
            paragraph.Format.Font.Name = "Arial";
            paragraph.Format.Font.Size = size;
            paragraph.Format.Font.Bold = true;
            paragraph.Format.SpaceAfter = Unit.FromCentimeter(0.2);
        }

        private static void AddInfo(Section section, string label, string value)
        {
            var paragraph = section.AddParagraph();
            paragraph.Format.Font.Name = "Arial";
            paragraph.Format.Font.Size = 10;
            paragraph.AddFormattedText(label + ": ", TextFormat.Bold);
            paragraph.AddText(value ?? string.Empty);
        }

        private static void AddCell(Row row, int index, string text, bool bold = false)
        {
            var paragraph = row.Cells[index].AddParagraph(text ?? string.Empty);
            paragraph.Format.Font.Name = "Arial";
            paragraph.Format.Font.Size = 8;
            paragraph.Format.Font.Bold = bold;
        }
    }
}
