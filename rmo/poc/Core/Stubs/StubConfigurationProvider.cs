using Fis.ReportMigrationOrchestrator.Core.Abstractions;
using Fis.ReportMigrationOrchestrator.Core.Domain;
using Microsoft.Extensions.Hosting;

namespace RmoReportingPoc.Core.Stubs
{
    public sealed class StubConfigurationProvider : IConfigurationProvider
    {
        private readonly MigrationConfiguration _configuration;

        public StubConfigurationProvider(IHostEnvironment environment)
        {
            _configuration = new MigrationConfiguration
            {
                OutputRoot = Path.Combine(environment.ContentRootPath, "runs")
            };
        }

        public MigrationConfiguration GetConfiguration() => _configuration;
    }
}
