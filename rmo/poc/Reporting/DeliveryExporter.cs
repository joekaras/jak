using System;
using System.Collections.Generic;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using Fis.ReportMigrationOrchestrator.Core.Abstractions;
using Fis.ReportMigrationOrchestrator.Core.Domain;
using Fis.ReportMigrationOrchestrator.Reporting.Generators;
using Microsoft.Extensions.Logging;
using static Fis.ReportMigrationOrchestrator.Reporting.Generators.DiscoveryReportShared;

namespace Fis.ReportMigrationOrchestrator.Reporting
{
    /// <summary>A discovery whose reports could not be exported, so none of its files are in the delivery folder.</summary>
    public sealed record DeliveryFailure(Guid DiscoveryId, string HoldingCompany, string WaveId, string Error);

    /// <summary>Files written to the delivery folder, and the discoveries left out of it.</summary>
    public sealed record DeliveryResult(string Folder, IReadOnlyList<string> Files, IReadOnlyList<DeliveryFailure> Failures);

    /// <summary>
    /// Regenerates the customer reports (readiness and quick look, HTML and PDF) for every discovery and copies them to
    /// <c>&lt;OutputRoot&gt;/delivery/&lt;holding company&gt;/</c>, where the delivery service picks them up. The folder is
    /// rebuilt from scratch on every export, so it only ever holds the current reports. A discovery that fails is left
    /// out (none of its files stay behind) and reported; the others are still delivered.
    /// </summary>
    public sealed class DeliveryExporter
    {
        private readonly IConfigurationProvider _configurationProvider;
        private readonly HtmlDiscoveryDashboardGenerator _readiness;
        private readonly HtmlDiscoveryQuickLookGenerator _quickLook;
        private readonly ILogger<DeliveryExporter> _logger;
        // One export at a time: a second one would delete the folder the first is writing into.
        private readonly SemaphoreSlim _gate = new(1, 1);

        public DeliveryExporter(IConfigurationProvider configurationProvider, HtmlDiscoveryDashboardGenerator readiness, HtmlDiscoveryQuickLookGenerator quickLook, ILogger<DeliveryExporter> logger)
        {
            _logger = logger;
            _configurationProvider = configurationProvider;
            _readiness = readiness;
            _quickLook = quickLook;
        }

        /// <summary>Delivery folder, under the configured output root.</summary>
        public string Root => Path.Combine(_configurationProvider.GetConfiguration().OutputRoot ?? ".\\runs", "delivery");

        /// <summary>Exports every discovery; returns the files written and the discoveries that failed.</summary>
        public async Task<DeliveryResult> ExportAsync(IEnumerable<MigrationDiscovery> discoveries, CancellationToken cancellationToken)
        {
            await _gate.WaitAsync(cancellationToken).ConfigureAwait(false);
            try
            {
                if (Directory.Exists(Root)) Directory.Delete(Root, recursive: true);
                var written = new List<string>();
                var failures = new List<DeliveryFailure>();
                foreach (var discovery in discoveries)
                {
                    try
                    {
                        written.AddRange(await ExportOneAsync(discovery, cancellationToken).ConfigureAwait(false));
                    }
                    // A cancelled request stops the whole export; anything else costs only this discovery.
                    catch (Exception ex) when (ex is not OperationCanceledException)
                    {
                        _logger.LogError(ex, "Delivery export failed for discovery {DiscoveryId} (holding company {HoldingCompany}, wave {WaveId})", discovery.Id, discovery.HoldingCompany, discovery.WaveId);
                        RemovePartial(discovery);
                        failures.Add(new DeliveryFailure(discovery.Id, discovery.HoldingCompany, discovery.WaveId, ex.Message));
                    }
                }
                return new DeliveryResult(Root, written, failures);
            }
            finally
            {
                _gate.Release();
            }
        }

        private async Task<IReadOnlyList<string>> ExportOneAsync(MigrationDiscovery discovery, CancellationToken cancellationToken)
        {
            var folder = Path.Combine(Root, Sanitize(discovery.HoldingCompany));
            Directory.CreateDirectory(folder);
            var written = new List<string>();

            var readinessHtml = await _readiness.GenerateAsync(discovery, cancellationToken).ConfigureAwait(false);
            var readinessPdf = Path.ChangeExtension(readinessHtml, ".pdf");
            await EdgePdfPrinter.PrintAsync(readinessHtml, readinessPdf, cancellationToken).ConfigureAwait(false);
            // The readiness report has side navigation, which Chromium repeats on every printed page.
            EdgePdfPrinter.RemoveRepeatedLinks(readinessPdf);

            var quickLookHtml = await _quickLook.GenerateAsync(discovery, cancellationToken).ConfigureAwait(false);
            var quickLookPdf = Path.ChangeExtension(quickLookHtml, ".pdf");
            await EdgePdfPrinter.PrintAsync(quickLookHtml, quickLookPdf, cancellationToken).ConfigureAwait(false);

            foreach (var source in new[] { readinessHtml, readinessPdf, quickLookHtml, quickLookPdf })
            {
                var target = Path.Combine(folder, DeliveryFileName(discovery, source));
                File.Copy(source, target, overwrite: true);
                written.Add(target);
            }
            return written;
        }

        // Deletes whatever this discovery copied before it failed. Only its own files: another wave of the same holding
        // company shares the folder.
        private void RemovePartial(MigrationDiscovery discovery)
        {
            var folder = Path.Combine(Root, Sanitize(discovery.HoldingCompany));
            if (!Directory.Exists(folder)) return;
            foreach (var file in Directory.GetFiles(folder, DeliveryPrefix(discovery) + "*")) File.Delete(file);
            if (Directory.GetFileSystemEntries(folder).Length == 0) Directory.Delete(folder);
        }

        // 548-wave1-20260925225533667-readiness.pdf: the holding company and wave in the name keep a file identifiable
        // once it leaves its folder, e.g. as an email attachment.
        private static string DeliveryFileName(MigrationDiscovery discovery, string source)
        {
            return Sanitize(discovery.HoldingCompany) + "-wave" + Sanitize(discovery.WaveId) + "-" + Path.GetFileName(source);
        }

        // Every delivered file of one discovery starts with this: the source files are all named <stem>-<report>.
        private static string DeliveryPrefix(MigrationDiscovery discovery)
        {
            return Sanitize(discovery.HoldingCompany) + "-wave" + Sanitize(discovery.WaveId) + "-" + Stem(discovery) + "-";
        }
    }
}
