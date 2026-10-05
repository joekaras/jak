using System.Threading;
using System.Threading.Tasks;
using Fis.ReportMigrationOrchestrator.Core.Domain;

namespace Fis.ReportMigrationOrchestrator.Core.Abstractions
{
    public interface IDiscoveryReportGenerator
    {
        Task<string> GenerateAsync(MigrationDiscovery discovery, CancellationToken cancellationToken);
    }
}
