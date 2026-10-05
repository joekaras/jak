namespace Fis.ReportMigrationOrchestrator.Core.Domain
{
    public sealed class CharSpaceFinding
    {
        public string ExpressionType { get; set; } = string.Empty;

        public List<CharSpaceFindingExpression> CharSpaceFindingExpressions { get; set; } = new();

        public string? OldRaw { get; set; }

        public string? UpdateRaw { get; set; }
    }
}
