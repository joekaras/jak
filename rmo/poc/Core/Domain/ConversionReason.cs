namespace Fis.ReportMigrationOrchestrator.Core.Domain
{
    /// <summary>Customer-facing description of why a document will not be converted.</summary>
    public sealed record ConversionReason(string Code, string Title, string Explanation, string Action, string ActionOwner);

    public static class ConversionReasons
    {
        public const string BoReferenceMissing = "BO_REFERENCE_MISSING";
        public const string UnvUnsupported = "UNV_UNSUPPORTED";
        public const string SnowflakeMappingMissing = "SNOWFLAKE_MAPPING_MISSING";
        public const string ExcelSourceMissing = "EXCEL_SOURCE_MISSING";
        public const string DpRetrievalFailed = "DP_RETRIEVAL_FAILED";
        public const string Unknown = "UNKNOWN";

        // Fallback for producers that don't emit ReasonCode yet. Patterns come from DISCOVERY_JSON.md §8.
        private static readonly (string Pattern, string Code)[] MessagePatterns =
        {
            ("underlying BusinessObjects reference could not be found", BoReferenceMissing),
            ("references a UNV universe which is not supported", UnvUnsupported),
            ("No Snowflake folder mapping is configured", SnowflakeMappingMissing),
            ("does not reference a discovered Excel document", ExcelSourceMissing),
            ("Unable to retrieve data providers", DpRetrievalFailed)
        };

        private static readonly Dictionary<string, ConversionReason> Catalog = new(StringComparer.OrdinalIgnoreCase)
        {
            [BoReferenceMissing] = new(
                BoReferenceMissing,
                "Universe not found",
                "One or more queries in this report use a universe that could not be found in BusinessObjects. It may have been deleted or moved, or the discovery account may not have access to it. Without the universe, the query cannot be mapped to Snowflake.",
                "Point the query at a universe that exists, or retire the report if it is no longer used. If the universe does exist, tell your FIS contact so access can be checked and discovery run again.",
                "Customer"),
            [DpRetrievalFailed] = new(
                DpRetrievalFailed,
                "Report could not be read",
                "Discovery could not open this report to read its queries, even after retrying. Nothing is known yet about which universes it uses, so it cannot be assessed for conversion.",
                "FIS will investigate and run discovery again. If the report fails to open in BusinessObjects for you as well, it may be damaged; consider retiring or rebuilding it.",
                "FIS"),
            [SnowflakeMappingMissing] = new(
                SnowflakeMappingMissing,
                "No Snowflake mapping for universe",
                "The universe this report uses was found, but no Snowflake target is configured for the folder it sits in, so its queries have nowhere to be mapped to.",
                "Confirm whether this universe should migrate. If it should, FIS will add the folder mapping and the report will be reassessed. If it is not needed, the report can be retired.",
                "FIS"),
            [UnvUnsupported] = new(
                UnvUnsupported,
                "Uses an unsupported UNV universe",
                "One or more queries in this report use a legacy UNV universe. The migration supports UNX universes only, so these queries cannot be moved to Snowflake as they are.",
                "Rebuild the queries on a UNX universe, or convert the universe to UNX, then include the report in a later wave. Retire the report if it is no longer used.",
                "Customer"),
            [ExcelSourceMissing] = new(
                ExcelSourceMissing,
                "Excel source not found",
                "A query in this report reads from an Excel workbook that was not found in this discovery. Without the workbook, the query cannot be mapped.",
                "Check that the workbook still exists in BusinessObjects. If it does, tell your FIS contact so it can be included in discovery; if not, point the query at another source or retire the report.",
                "Customer"),
            [Unknown] = new(
                Unknown,
                "Reason not yet identified",
                "Discovery marked this report as not ready to convert but did not record a reason this report recognises. The technical detail below shows what discovery reported.",
                "FIS will review the technical detail and confirm the reason. No action is needed from you until then.",
                "FIS"),
        };

        /// <summary>Returns the catalogue entry for a code, or a bare entry named after the code when it isn't catalogued.</summary>
        public static ConversionReason Get(string code)
        {
            return Catalog.TryGetValue(code, out var reason) ? reason : new ConversionReason(code, code, string.Empty, string.Empty, string.Empty);
        }

        /// <summary>The reason a data provider can't be converted, or <see langword="null"/> when it is mapped.</summary>
        public static string? Reason(this DataProviderInfo dataProvider)
        {
            if (!string.IsNullOrWhiteSpace(dataProvider.ReasonCode)) return dataProvider.ReasonCode.Trim().ToUpperInvariant();
            return Match(dataProvider.StatusMessage).FirstOrDefault();
        }

        /// <summary>
        /// Distinct reasons a document won't be converted: its own code plus its data providers' codes. Falls back to
        /// the document message when neither yields a code, and to <see cref="Unknown"/> for an unexplained not-ready document.
        /// </summary>
        public static IReadOnlyList<string> Reasons(this MigrationDocument document)
        {
            if (document.ReadyToMigrate) return Array.Empty<string>();

            var codes = new List<string>();
            if (!string.IsNullOrWhiteSpace(document.ReasonCode)) codes.Add(document.ReasonCode.Trim().ToUpperInvariant());
            codes.AddRange(document.DataProviders.Select(Reason).OfType<string>());
            if (codes.Count == 0) codes.AddRange(Match(document.StatusMessage));
            if (codes.Count == 0) codes.Add(Unknown);
            return codes.Distinct().ToList();
        }

        /// <summary>Reason codes recognised in a free-text message, such as an audit event's error message.</summary>
        public static IReadOnlyList<string> FromMessage(string? message)
        {
            return Match(message).ToList();
        }

        private static IEnumerable<string> Match(string? message)
        {
            if (string.IsNullOrWhiteSpace(message)) yield break;
            foreach (var (pattern, code) in MessagePatterns)
            {
                if (message.Contains(pattern, StringComparison.OrdinalIgnoreCase)) yield return code;
            }
        }
    }
}
