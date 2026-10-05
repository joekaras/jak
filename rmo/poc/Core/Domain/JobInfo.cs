namespace Fis.ReportMigrationOrchestrator.Core.Domain
{
    public sealed class JobInfo
    {
        public string JobId { get; set; } = string.Empty;

        public string Name { get; set; } = string.Empty;

        public int DocumentId { get; set; }

        public string? DocumentName { get; set; }

        public string? ScheduleId { get; set; }

        public string? FullDefinitionJson { get; set; }

        public List<string> DependentJobs { get; set; } = new();

        public List<string> DependentOracleApplicationCodes { get; set; } = new();

        public List<string> DependentSnowflakeApplicationCodes { get; set; } = new();

        public List<int> DestinationDocumentIds { get; set; } = new();

        public List<string> ExtractPaths { get; set; } = new();
    }
}
