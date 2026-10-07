namespace Fis.ReportMigrationOrchestrator.Core.Domain
{
    /// <summary>Customer-facing names for <see cref="CharSpaceFinding.ExpressionType"/>, the part of a report a rewrite is in.</summary>
    public static class ExpressionTypes
    {
        // Keys are the lower-case values observed in discovery output (DISCOVERY_JSON.md §4).
        private static readonly Dictionary<string, string> Catalog = new(StringComparer.OrdinalIgnoreCase)
        {
            ["data provider specification"] = "Query definition",
            ["data provider queryplan"] = "Generated query SQL",
            ["variable definition"] = "Variable formula",
            ["report filter"] = "Report tab filter",
            ["element filter"] = "Table or chart filter",
            ["alerter condition operand"] = "Alerter (conditional formatting) rule",
        };

        /// <summary>Returns the customer-facing name, or the raw value capitalised when it isn't catalogued.</summary>
        public static string Title(string? expressionType)
        {
            var key = expressionType?.Trim() ?? string.Empty;
            if (key.Length == 0) return "Other";
            return Catalog.TryGetValue(key, out var title) ? title : char.ToUpperInvariant(key[0]) + key.Substring(1);
        }
    }
}
