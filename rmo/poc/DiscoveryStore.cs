using System.Text.Json;
using Fis.ReportMigrationOrchestrator.Core.Domain;
using Microsoft.Extensions.Hosting;

namespace RmoReportingPoc
{
    public sealed class DiscoveryStore
    {
        private static readonly JsonSerializerOptions SerializerOptions = new() { PropertyNameCaseInsensitive = true };

        private readonly List<MigrationDiscovery> _discoveries = new();

        public DiscoveryStore(IHostEnvironment environment)
        {
            var dataDirectory = Path.Combine(environment.ContentRootPath, "Data");
            if (!Directory.Exists(dataDirectory))
            {
                return;
            }

            foreach (var file in Directory.EnumerateFiles(dataDirectory, "*-discovery.json").OrderBy(path => path))
            {
                using var stream = File.OpenRead(file);
                var discovery = JsonSerializer.Deserialize<MigrationDiscovery>(stream, SerializerOptions);
                if (discovery is not null)
                {
                    _discoveries.Add(discovery);
                }
            }
        }

        public IReadOnlyList<MigrationDiscovery> GetAll() => _discoveries;

        public MigrationDiscovery? Get(Guid id) => _discoveries.FirstOrDefault(discovery => discovery.Id == id);
    }
}
