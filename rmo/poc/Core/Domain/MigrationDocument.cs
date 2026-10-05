namespace Fis.ReportMigrationOrchestrator.Core.Domain
{
    public sealed class MigrationDocument
    {
        public int DocumentId { get; set; }

        public string? DocumentCuid { get; set; }

        public string Name { get; set; } = string.Empty;

        public string FolderPath { get; set; } = string.Empty;

        public string? Owner { get; set; }

        public string DocumentType { get; set; } = string.Empty;

        public bool IsPersonalFolderDocument { get; set; }

        public bool ReadyToMigrate { get; set; }

        public string? StatusMessage { get; set; }

        public string? ReasonCode { get; set; }

        public List<DataProviderInfo> DataProviders { get; set; } = new();

        public List<CharSpaceFinding> CharSpaceFindings { get; set; } = new();
    }
}
