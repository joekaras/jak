using Fis.ReportMigrationOrchestrator.Core.Domain;

namespace Fis.ReportMigrationOrchestrator.Core.Abstractions
{
    public interface IConfigurationProvider
    {
        MigrationConfiguration GetConfiguration();
    }
}
