namespace Fis.ReportMigrationOrchestrator.Core.Domain
{
    public sealed class ScheduleInfo
    {
        public string ScheduleId { get; set; } = string.Empty;

        public string Name { get; set; } = string.Empty;

        public bool IsEnabled { get; set; }

        public string? FullDefinitionJson { get; set; }

        public List<string> JobIds { get; set; } = new();
    }
}
