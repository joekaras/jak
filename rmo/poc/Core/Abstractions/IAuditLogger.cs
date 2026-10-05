using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using Fis.ReportMigrationOrchestrator.Core.Domain;

namespace Fis.ReportMigrationOrchestrator.Core.Abstractions
{
    public interface IAuditLogger
    {
        Task<IReadOnlyList<AuditEvent>> QueryAsync(Guid runId, CancellationToken cancellationToken);
    }
}
