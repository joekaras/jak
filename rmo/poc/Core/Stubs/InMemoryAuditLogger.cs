using System.Collections.Concurrent;
using Fis.ReportMigrationOrchestrator.Core.Abstractions;
using Fis.ReportMigrationOrchestrator.Core.Domain;

namespace RmoReportingPoc.Core.Stubs
{
    public sealed class InMemoryAuditLogger : IAuditLogger
    {
        public static readonly Guid WaveRunId = Guid.Parse("6b7f8a90-1c2d-4e3f-8a9b-0c1d2e3f4a5b");
        public static readonly Guid SmokeRunId = Guid.Parse("9d8c7b6a-5e4f-4e31-8a71-605243f4e5d6");

        private readonly ConcurrentDictionary<Guid, List<AuditEvent>> _runs = new();

        public InMemoryAuditLogger()
        {
            _runs[WaveRunId] = BuildWaveRun(DateTimeOffset.UtcNow);
            _runs[SmokeRunId] = BuildSmokeRun(DateTimeOffset.UtcNow);
        }

        public IReadOnlyList<Guid> GetRunIds() => _runs.Keys.OrderBy(id => id).ToList();

        public Task<IReadOnlyList<AuditEvent>> QueryAsync(Guid runId, CancellationToken cancellationToken)
        {
            _runs.TryGetValue(runId, out var events);
            IReadOnlyList<AuditEvent> result = events ?? new List<AuditEvent>();
            return Task.FromResult(result);
        }

        private static List<AuditEvent> BuildWaveRun(DateTimeOffset completedAt)
        {
            return new List<AuditEvent>
            {
                New(completedAt.AddMinutes(-42), "RunStarted", "Run", WaveRunId.ToString()),
                New(completedAt.AddMinutes(-40), "DocumentMigrated", "Document", "577383"),
                New(completedAt.AddMinutes(-38), "DocumentMigrated", "Document", "577724"),
                New(completedAt.AddMinutes(-36), "DocumentMigrated", "Document", "577501"),
                New(completedAt.AddMinutes(-34), "DocumentMigrated", "Document", "576354"),
                New(completedAt.AddMinutes(-32), "DocumentMigrated", "Document", "5872379"),
                New(completedAt.AddMinutes(-30), "DocumentSkipped", "Document", "577240", true, "Skipped: data provider references a UNV universe which is not supported for migration."),
                New(completedAt.AddMinutes(-28), "DocumentSkipped", "Document", "577443", true, "Skipped: data provider references a UNV universe which is not supported for migration."),
                New(completedAt.AddMinutes(-26), "DocumentFailed", "Document", "577666", false, "Data provider (DP0 - Query 1) underlying BusinessObjects reference could not be found."),
                New(completedAt.AddMinutes(-24), "DocumentFailed", "Document", "576496", false, "Unable to retrieve data providers after 2 attempts. Object reference not set to an instance of an object."),
                New(completedAt.AddMinutes(-20), "ScheduleMigrated", "Schedule", "7b160dea-50f0-422e-b561-2f2dd00b2982"),
                New(completedAt.AddMinutes(-18), "JobMigrated", "Job", "2ab34e8a-42c2-4e89-81af-672c1d88621b"),
                New(completedAt.AddMinutes(-16), "JobMigrated", "Job", "1db928a1-e262-430d-a3bb-29301957e051"),
                New(completedAt.AddMinutes(-15), "RunCompleted", "Run", WaveRunId.ToString())
            };
        }

        private static List<AuditEvent> BuildSmokeRun(DateTimeOffset completedAt)
        {
            return new List<AuditEvent>
            {
                New(completedAt.AddMinutes(-5), "RunStarted", "Run", SmokeRunId.ToString()),
                New(completedAt.AddMinutes(-4), "DocumentMigrated", "Document", "577724"),
                New(completedAt.AddMinutes(-3), "DocumentMigrated", "Document", "577501"),
                New(completedAt.AddMinutes(-2), "ScheduleMigrated", "Schedule", "ebb555fa-6b50-4d83-b6b5-0c8c12edd74c"),
                New(completedAt, "RunCompleted", "Run", SmokeRunId.ToString())
            };
        }

        private static AuditEvent New(DateTimeOffset timestamp, string eventType, string objectType, string objectId, bool success = true, string? errorMessage = null)
        {
            return new AuditEvent
            {
                Timestamp = timestamp,
                EventType = eventType,
                ObjectType = objectType,
                ObjectId = objectId,
                Success = success,
                ErrorMessage = errorMessage
            };
        }
    }
}
