namespace Fis.ReportMigrationOrchestrator.Core.Domain
{
    public sealed class AuditEvent
    {
        public DateTimeOffset Timestamp { get; set; }

        public string EventType { get; set; } = string.Empty;

        public string ObjectType { get; set; } = string.Empty;

        public string ObjectId { get; set; } = string.Empty;

        public bool Success { get; set; }

        public string? ErrorMessage { get; set; }
    }
}
