using System.Collections.Concurrent;
using Fis.ReportMigrationOrchestrator.Core.Abstractions;
using Fis.ReportMigrationOrchestrator.Core.Domain;
using Fis.ReportMigrationOrchestrator.Reporting.Generators;
using Microsoft.AspNetCore.Http.Json;
using RmoReportingPoc;
using RmoReportingPoc.Core.Stubs;

var builder = WebApplication.CreateBuilder(args);

builder.Services.ConfigureHttpJsonOptions(options =>
{
    options.SerializerOptions.PropertyNamingPolicy = null;
    options.SerializerOptions.WriteIndented = true;
});

builder.Services.AddSingleton<DiscoveryStore>();
builder.Services.AddSingleton<InMemoryAuditLogger>();
builder.Services.AddSingleton<IAuditLogger>(serviceProvider => serviceProvider.GetRequiredService<InMemoryAuditLogger>());
builder.Services.AddSingleton<IConfigurationProvider, StubConfigurationProvider>();
builder.Services.AddSingleton<IReportGenerator, HtmlReportGenerator>();
builder.Services.AddSingleton<IReportGenerator, PdfReportGenerator>();
builder.Services.AddSingleton<IReportGenerator, ExcelReportGenerator>();
builder.Services.AddSingleton<IDiscoveryReportGenerator, HtmlDiscoveryReportGenerator>();
builder.Services.AddSingleton<HtmlDiscoveryDashboardGenerator>();
builder.Services.AddSingleton<HtmlDiscoveryQuickLookGenerator>();

var app = builder.Build();

var generatedDiscoveryReports = new ConcurrentDictionary<Guid, string>();

app.UseDefaultFiles();
app.UseStaticFiles();

app.MapGet("/api/discoveries", (DiscoveryStore store) =>
    store.GetAll().Select(discovery => new
    {
        discovery.Id,
        discovery.HoldingCompany,
        discovery.WaveId,
        discovery.Environment,
        discovery.DiscoveredAt,
        Documents = discovery.Documents.Count,
        Schedules = discovery.Schedules.Count,
        Jobs = discovery.Jobs.Count
    }).ToList());

app.MapGet("/api/discoveries/{id:guid}", (Guid id, DiscoveryStore store) =>
{
    var discovery = store.Get(id);
    return discovery is null ? Results.NotFound() : Results.Ok(discovery);
});

app.MapPost("/api/discoveries/{id:guid}/report", async (Guid id, DiscoveryStore store, IDiscoveryReportGenerator generator, CancellationToken cancellationToken) =>
{
    var discovery = store.Get(id);
    if (discovery is null)
    {
        return Results.NotFound();
    }

    var path = await generator.GenerateAsync(discovery, cancellationToken);
    generatedDiscoveryReports[id] = path;
    return Results.Ok(new { Path = path });
});

app.MapGet("/api/discoveries/{id:guid}/report/view", async (Guid id, DiscoveryStore store, IDiscoveryReportGenerator generator, CancellationToken cancellationToken) =>
{
    var discovery = store.Get(id);
    if (discovery is null)
    {
        return Results.NotFound();
    }

    if (!generatedDiscoveryReports.TryGetValue(id, out var path) || !File.Exists(path))
    {
        path = await generator.GenerateAsync(discovery, cancellationToken);
        generatedDiscoveryReports[id] = path;
    }

    var html = await File.ReadAllTextAsync(path, cancellationToken);
    return Results.Content(html, "text/html");
});

// One file holds the summary and both detail sections, so it prints as a single PDF.
app.MapGet("/api/discoveries/{id:guid}/report/readiness", async (Guid id, DiscoveryStore store, HtmlDiscoveryDashboardGenerator generator, CancellationToken cancellationToken) =>
{
    var discovery = store.Get(id);
    if (discovery is null)
    {
        return Results.NotFound();
    }

    var path = await generator.GenerateAsync(discovery, cancellationToken);
    var html = await File.ReadAllTextAsync(path, cancellationToken);
    return Results.Content(html, "text/html");
});

// Regenerates the readiness HTML, prints it next to itself as <stem>-readiness.pdf, and shows it inline.
app.MapGet("/api/discoveries/{id:guid}/report/readiness/pdf", async (Guid id, DiscoveryStore store, HtmlDiscoveryDashboardGenerator generator, HttpContext context, CancellationToken cancellationToken) =>
{
    var discovery = store.Get(id);
    if (discovery is null)
    {
        return Results.NotFound();
    }

    var htmlPath = await generator.GenerateAsync(discovery, cancellationToken);
    var pdfPath = Path.ChangeExtension(htmlPath, ".pdf");
    await EdgePdfPrinter.PrintAsync(htmlPath, pdfPath, cancellationToken);
    EdgePdfPrinter.RemoveRepeatedLinks(pdfPath);
    var bytes = await File.ReadAllBytesAsync(pdfPath, cancellationToken);
    context.Response.Headers.ContentDisposition = $"inline; filename=\"{Path.GetFileName(pdfPath)}\"";
    return Results.File(bytes, "application/pdf");
});

// Quick look: scan lists of what won't convert, what changes and which jobs are affected, then the readiness detail sections.
app.MapGet("/api/discoveries/{id:guid}/report/quicklook", async (Guid id, DiscoveryStore store, HtmlDiscoveryQuickLookGenerator generator, CancellationToken cancellationToken) =>
{
    var discovery = store.Get(id);
    if (discovery is null)
    {
        return Results.NotFound();
    }

    var path = await generator.GenerateAsync(discovery, cancellationToken);
    var html = await File.ReadAllTextAsync(path, cancellationToken);
    return Results.Content(html, "text/html");
});

// Regenerates the quick-look HTML, prints it next to itself as <stem>-quicklook.pdf, and shows it inline.
app.MapGet("/api/discoveries/{id:guid}/report/quicklook/pdf", async (Guid id, DiscoveryStore store, HtmlDiscoveryQuickLookGenerator generator, HttpContext context, CancellationToken cancellationToken) =>
{
    var discovery = store.Get(id);
    if (discovery is null)
    {
        return Results.NotFound();
    }

    var htmlPath = await generator.GenerateAsync(discovery, cancellationToken);
    var pdfPath = Path.ChangeExtension(htmlPath, ".pdf");
    await EdgePdfPrinter.PrintAsync(htmlPath, pdfPath, cancellationToken);
    var bytes = await File.ReadAllBytesAsync(pdfPath, cancellationToken);
    context.Response.Headers.ContentDisposition = $"inline; filename=\"{Path.GetFileName(pdfPath)}\"";
    return Results.File(bytes, "application/pdf");
});

app.MapGet("/api/runs", async (InMemoryAuditLogger auditLogger, CancellationToken cancellationToken) =>
{
    var runs = new List<object>();
    foreach (var runId in auditLogger.GetRunIds())
    {
        var events = await auditLogger.QueryAsync(runId, cancellationToken);
        runs.Add(new
        {
            RunId = runId,
            Total = events.Count,
            Succeeded = events.Count(x => x.Classify() == AuditOutcome.Migrated),
            Skipped = events.Count(x => x.Classify() == AuditOutcome.Skipped),
            Failed = events.Count(x => x.Classify() == AuditOutcome.Failed)
        });
    }

    return runs;
});

app.MapGet("/api/runs/{runId:guid}/report", async (Guid runId, string? format, IEnumerable<IReportGenerator> generators, InMemoryAuditLogger auditLogger, IHostEnvironment environment, HttpContext context, CancellationToken cancellationToken) =>
{
    format ??= "html";

    var generator = generators.FirstOrDefault(x => x.CanGenerate(format));
    if (generator is null)
    {
        return Results.BadRequest(new { Error = $"Unsupported format '{format}'. Supported formats: html, pdf, csv, xlsx." });
    }

    var events = await auditLogger.QueryAsync(runId, cancellationToken);
    if (events.Count == 0)
    {
        return Results.NotFound(new { Error = $"Unknown run '{runId}'." });
    }

    var outputPath = Path.Combine(environment.ContentRootPath, "runs", runId.ToString("N"));
    var filePath = await generator.GenerateAsync(runId, format, outputPath, cancellationToken);
    var bytes = await File.ReadAllBytesAsync(filePath, cancellationToken);

    return format.ToLowerInvariant() switch
    {
        "html" => Results.File(bytes, "text/html; charset=utf-8"),
        // Inline, so the PDF opens in a browser tab like the readiness PDF; a download name would force a save.
        "pdf" => InlinePdf(context, bytes, "report.pdf"),
        "csv" => Results.File(bytes, "text/csv; charset=utf-8", "report.csv"),
        _ => Results.File(bytes, "text/csv; charset=utf-8", "report.xlsx.csv")
    };
});

app.Run();

static IResult InlinePdf(HttpContext context, byte[] bytes, string fileName)
{
    context.Response.Headers.ContentDisposition = $"inline; filename=\"{fileName}\"";
    return Results.File(bytes, "application/pdf");
}
