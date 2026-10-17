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

using Xunit;

namespace Fis.Foundation.Vault.Tests
{
    /// <summary>
    /// Runs only when the named environment variables are set. Values are never printed.
    /// </summary>
    public sealed class IntegrationFactAttribute : FactAttribute
    {
        public IntegrationFactAttribute(params string[] requiredVariables)
        {
            var missing = requiredVariables.Where(v => string.IsNullOrEmpty(Environment.GetEnvironmentVariable(v))).ToArray();
            if (missing.Length > 0)
            {
                Skip = "Integration test; set " + string.Join(", ", missing);
            }
        }
    }

    [Trait("Category", "Integration")]
    public sealed class IntegrationTests
    {
        /// <summary>
        /// AppRole against the shared dev Vault. Consumes the wrapped secret_id file, so each run needs a new one:
        /// <c>vault write -wrap-ttl=5m -format=json -f auth/approle/role/&lt;role&gt;/secret-id &gt; file</c>.
        /// </summary>
        [IntegrationFact("VAULT_ADDR", "VAULT_KV_MOUNT", "VAULT_ROLE_ID", "VAULT_WRAPPED_SECRET_ID_FILE", "VAULT_TEST_PATH")]
        public async Task AppRole_wrapped_reads_test_secret()
        {
            var config = VaultConfig.Load();
            config.AuthMode = AuthMode.AppRoleWrapped;

            await using var client = SecretClient.Create(config);
            var values = await client.GetSecretAsync(Environment.GetEnvironmentVariable("VAULT_TEST_PATH")!);

            Assert.NotEmpty(values);
        }

        /// <summary>
        /// Agent mode against a Vault Agent running locally with auto-auth to the shared dev Vault.
        /// </summary>
        [IntegrationFact("VAULT_AGENT_ADDR", "VAULT_KV_MOUNT", "VAULT_TEST_PATH")]
        public async Task Agent_reads_test_secret()
        {
            var config = new VaultConfig
            {
                Address = new Uri(Environment.GetEnvironmentVariable("VAULT_AGENT_ADDR")!),
                KvMount = Environment.GetEnvironmentVariable("VAULT_KV_MOUNT")!,
                Namespace = Environment.GetEnvironmentVariable("VAULT_NAMESPACE"),
                AuthMode = AuthMode.Agent
            };

            await using var client = SecretClient.Create(config);
            var values = await client.GetSecretAsync(Environment.GetEnvironmentVariable("VAULT_TEST_PATH")!);

            Assert.NotEmpty(values);
        }
    }
}
