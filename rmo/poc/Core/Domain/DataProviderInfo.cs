namespace Fis.ReportMigrationOrchestrator.Core.Domain
{
    public sealed class DataProviderInfo
    {
        public string Id { get; set; } = string.Empty;

        public string Name { get; set; } = string.Empty;

        public string? StatusMessage { get; set; }

        public string? ReasonCode { get; set; }

        public string? OracleDataSourceType { get; set; }

        public string? SnowflakeDataSourceType { get; set; }

        public int OracleDataSourceId { get; set; }

        public string? OracleDataSourceCuid { get; set; }

        public string? OracleDataSourceName { get; set; }

        public int SnowflakeDataSourceId { get; set; }

        public string? SnowflakeDataSourceCuid { get; set; }

        public string? SnowflakeDataSourceName { get; set; }
    }
}
