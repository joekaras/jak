namespace Fis.ReportMigrationOrchestrator.Core.Domain
{
    public static class AuditEventExtensions
    {
        /// <summary>
        /// Maps an audit event to its migration outcome. Run start/end events are <see cref="AuditOutcome.Lifecycle"/>;
        /// a false <see cref="AuditEvent.Success"/> is always <see cref="AuditOutcome.Failed"/>.
        /// </summary>
        public static AuditOutcome Classify(this AuditEvent auditEvent)
        {
            if (string.Equals(auditEvent.ObjectType, "Run", StringComparison.OrdinalIgnoreCase)) return AuditOutcome.Lifecycle;
            if (!auditEvent.Success) return AuditOutcome.Failed;
            if (auditEvent.EventType.EndsWith("Skipped", StringComparison.OrdinalIgnoreCase)) return AuditOutcome.Skipped;
            if (auditEvent.EventType.EndsWith("Failed", StringComparison.OrdinalIgnoreCase)) return AuditOutcome.Failed;
            return AuditOutcome.Migrated;
        }
    }
}
