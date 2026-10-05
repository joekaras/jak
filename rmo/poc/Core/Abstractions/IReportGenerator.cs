using System;
using System.Threading;
using System.Threading.Tasks;

namespace Fis.ReportMigrationOrchestrator.Core.Abstractions
{
    public interface IReportGenerator
    {
        bool CanGenerate(string format);

        Task<string> GenerateAsync(Guid runId, string format, string outputPath, CancellationToken cancellationToken);
    }
}
