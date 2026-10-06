using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using PdfSharp.Pdf;
using PdfSharp.Pdf.IO;

namespace Fis.ReportMigrationOrchestrator.Reporting.Generators
{
    /// <summary>
    /// Prints a self-contained HTML file to PDF with headless Microsoft Edge. Windows only, like the run-report PDF.
    /// </summary>
    public static class EdgePdfPrinter
    {
        private static readonly string[] EdgePaths =
        {
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86), @"Microsoft\Edge\Application\msedge.exe"),
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), @"Microsoft\Edge\Application\msedge.exe")
        };

        /// <summary>
        /// Chromium puts the links of a position:fixed element (the side navigation) on every page once per printed
        /// page, so a 34-page report carries 34 copies of each link per page. Keeps one link per position and target.
        /// </summary>
        public static void RemoveRepeatedLinks(string pdfPath)
        {
            using var document = PdfReader.Open(pdfPath, PdfDocumentOpenMode.Modify);
            var removed = new HashSet<PdfDictionary>(ReferenceEqualityComparer.Instance);
            foreach (var page in document.Pages)
            {
                var annotations = page.Elements.GetArray("/Annots");
                if (annotations is null) continue;
                var seen = new HashSet<string>(StringComparer.Ordinal);
                for (var i = annotations.Elements.Count - 1; i >= 0; i--)
                {
                    var annotation = annotations.Elements.GetDictionary(i);
                    if (annotation is null || annotation.Elements.GetName("/Subtype") != "/Link") continue;
                    var key = annotation.Elements["/Rect"] + "|" + annotation.Elements["/Dest"] + "|" + annotation.Elements["/A"];
                    if (seen.Add(key)) continue;
                    annotations.Elements.RemoveAt(i);
                    removed.Add(annotation);
                }
            }

            // The tagged-PDF structure tree also points at every link (/OBJR); drop those references too, or the
            // removed links stay reachable and are still written out.
            if (removed.Count > 0) PruneStructure(document.Internals.Catalog.Elements["/StructTreeRoot"], removed);
            document.Save(pdfPath);
        }

        private static void PruneStructure(PdfItem? item, HashSet<PdfDictionary> removed)
        {
            var element = Resolve(item) as PdfDictionary;
            if (element is null) return;
            switch (Resolve(element.Elements["/K"]))
            {
                case PdfArray kids:
                    for (var i = kids.Elements.Count - 1; i >= 0; i--)
                    {
                        if (IsRemovedLink(kids.Elements[i], removed)) kids.Elements.RemoveAt(i);
                        else PruneStructure(kids.Elements[i], removed);
                    }
                    break;
                case PdfDictionary kid:
                    if (IsRemovedLink(kid, removed)) element.Elements.Remove("/K");
                    else PruneStructure(kid, removed);
                    break;
            }
        }

        private static bool IsRemovedLink(PdfItem? item, HashSet<PdfDictionary> removed)
        {
            return Resolve(item) is PdfDictionary reference
                && reference.Elements.GetName("/Type") == "/OBJR"
                && Resolve(reference.Elements["/Obj"]) is PdfDictionary target
                && removed.Contains(target);
        }

        private static PdfItem? Resolve(PdfItem? item)
        {
            return item is PdfSharp.Pdf.Advanced.PdfReference reference ? reference.Value : item;
        }

        public static async Task PrintAsync(string htmlPath, string pdfPath, CancellationToken cancellationToken)
        {
            var edge = EdgePaths.FirstOrDefault(File.Exists)
                ?? throw new InvalidOperationException("Microsoft Edge was not found; it is needed to print the readiness PDF.");

            // A stale PDF must not pass for a fresh one if Edge fails.
            File.Delete(pdfPath);

            // Own profile per print: a shared one can hand the job to a running Edge and exit before printing.
            var profile = Path.Combine(Path.GetTempPath(), "rmo-edge-" + Guid.NewGuid().ToString("N"));
            var startInfo = new ProcessStartInfo(edge) { UseShellExecute = false, CreateNoWindow = true };
            startInfo.ArgumentList.Add("--headless=new");
            startInfo.ArgumentList.Add("--disable-gpu");
            startInfo.ArgumentList.Add("--no-pdf-header-footer");
            // Bookmarks built from the headings: the PDF's side navigation.
            startInfo.ArgumentList.Add("--generate-pdf-document-outline");
            startInfo.ArgumentList.Add("--user-data-dir=" + profile);
            startInfo.ArgumentList.Add("--print-to-pdf=" + pdfPath);
            startInfo.ArgumentList.Add(new Uri(htmlPath).AbsoluteUri);

            using var process = Process.Start(startInfo)
                ?? throw new InvalidOperationException("Microsoft Edge could not be started.");
            try
            {
                await process.WaitForExitAsync(cancellationToken);
            }
            catch (OperationCanceledException)
            {
                process.Kill(entireProcessTree: true);
                throw;
            }
            finally
            {
                try { Directory.Delete(profile, recursive: true); } catch (IOException) { } catch (UnauthorizedAccessException) { }
            }

            if (process.ExitCode != 0)
            {
                throw new InvalidOperationException($"Microsoft Edge failed to print the readiness PDF (exit code {process.ExitCode}).");
            }

            if (!File.Exists(pdfPath))
            {
                throw new InvalidOperationException("Microsoft Edge exited successfully but did not create the readiness PDF.");
            }

            var signature = new byte[5];
            await using var pdf = new FileStream(pdfPath, FileMode.Open, FileAccess.Read, FileShare.Read, bufferSize: 1, useAsync: true);
            var bytesRead = await pdf.ReadAtLeastAsync(signature, signature.Length, throwOnEndOfStream: false, cancellationToken);
            if (bytesRead != signature.Length)
            {
                throw new InvalidOperationException($"Microsoft Edge produced a truncated readiness PDF ({bytesRead} of {signature.Length} signature bytes).");
            }

            if (!signature.AsSpan().SequenceEqual("%PDF-"u8))
            {
                throw new InvalidOperationException("Microsoft Edge output is not a PDF because it does not begin with the %PDF- signature.");
            }
        }
    }
}
