//Copyright Notice!
//This document is protected under the trade secret and copyright
//laws as the property of Fidelity National Information Services, Inc.
//Copying, reproduction or distribution should be limited and only to
//employees with a "need to know" to do their job.
//Any disclosure of this document to third parties is strictly prohibited.

//copyright 2026 Fidelity National Information Services.
//All rights reserved worldwide.

// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

// Shared by Poc.Linux and Poc.WindowsService (linked into the latter's csproj).

using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;

namespace Fis.Foundation.Vault.Poc
{
    /// <summary>
    /// Runs <see cref="PocCheck"/> at startup and then on an interval. The first run logs in, so a missing
    /// or spent wrapped secret_id shows up immediately instead of on the first real use.
    /// </summary>
    public sealed class PocWorker : BackgroundService
    {
        private readonly ISecretClient _client;
        private readonly PocSettings _settings;
        private readonly ILogger<PocWorker> _logger;
        private readonly TimeSpan _interval;

        public PocWorker(ISecretClient client, PocSettings settings, ILogger<PocWorker> logger)
        {
            _client = client;
            _settings = settings;
            _logger = logger;
            _interval = TimeSpan.FromSeconds(
                int.TryParse(Environment.GetEnvironmentVariable("POC_INTERVAL_SECONDS"), out int s) && s > 0 ? s : 300);
        }

        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    var result = await PocCheck.RunAsync(_client, _settings, stoppingToken);
                    _logger.LogInformation(
                        "Vault check OK. Credential keys: {Keys}. AES-GCM round trip: {RoundTrip}",
                        string.Join(",", result.CredentialKeys),
                        result.AesGcmRoundTrip);
                }
                catch (VaultException ex)
                {
                    _logger.LogError("Vault check failed: {Type}: {Message}", ex.GetType().Name, ex.Message);
                }

                try
                {
                    await Task.Delay(_interval, stoppingToken);
                }
                catch (OperationCanceledException)
                {
                    break;
                }
            }
        }
    }
}
