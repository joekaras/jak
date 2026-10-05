namespace Fis.ReportMigrationOrchestrator.Core.Domain
{
    public sealed class MigrationDiscovery
    {
        public Guid Id { get; set; }

        public DateTimeOffset DiscoveredAt { get; set; }

        public string HoldingCompany { get; set; } = string.Empty;

        public string WaveId { get; set; } = string.Empty;

        public string Environment { get; set; } = string.Empty;

        public List<MigrationDocument> Documents { get; set; } = new();

        public List<JobInfo> Jobs { get; set; } = new();

        public List<ScheduleInfo> Schedules { get; set; } = new();
    }
}
