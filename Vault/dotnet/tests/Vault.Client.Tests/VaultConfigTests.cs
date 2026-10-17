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

using System.Collections;
using Xunit;

namespace Fis.Foundation.Vault.Tests
{
    public sealed class VaultConfigTests
    {
        [Fact]
        public void Environment_overrides_json_file()
        {
            string file = Path.GetTempFileName();
            try
            {
                File.WriteAllText(file, "{\"address\":\"https://vault-a.test:8200\",\"kvMount\":\"kv\",\"authMode\":\"agent\",\"cacheTtlSeconds\":60}");
                var env = new Hashtable { ["VAULT_ADDR"] = "http://127.0.0.1:8100", ["VAULT_NAMESPACE"] = "team/dev" };

                var config = VaultConfig.Load(file, env);

                Assert.Equal(new Uri("http://127.0.0.1:8100/"), config.Address);
                Assert.Equal("team/dev", config.Namespace);
                Assert.Equal("kv", config.KvMount);
                Assert.Equal(AuthMode.Agent, config.AuthMode);
                Assert.Equal(TimeSpan.FromSeconds(60), config.CacheTtl);
            }
            finally
            {
                File.Delete(file);
            }
        }

        [Theory]
        [InlineData("{\"secretId\":\"x\"}")]
        [InlineData("{\"secret_id\":\"x\"}")]
        public void Secret_id_in_json_is_refused(string json)
        {
            var ex = Assert.Throws<InvalidOperationException>(() => new VaultConfig().ApplyJson(json));
            Assert.Contains("must not be stored in configuration", ex.Message);
        }

        [Fact]
        public void Secret_id_in_environment_is_refused()
        {
            var env = new Hashtable { ["VAULT_SECRET_ID"] = "x" };
            Assert.Throws<InvalidOperationException>(() => new VaultConfig().ApplyEnvironment(env));
        }

        [Fact]
        public void AppRole_over_plain_http_to_remote_host_is_refused()
        {
            var env = new Hashtable
            {
                ["VAULT_ADDR"] = "http://vault.test:8200",
                ["VAULT_ROLE_ID"] = "role",
                ["VAULT_WRAPPED_SECRET_ID_FILE"] = "C:\\nowhere"
            };

            var ex = Assert.Throws<InvalidOperationException>(() => VaultConfig.Load(null, env));
            Assert.Contains("https", ex.Message);
        }

        [Fact]
        public void Agent_on_loopback_http_is_allowed()
        {
            var env = new Hashtable { ["VAULT_ADDR"] = "http://127.0.0.1:8100", ["VAULT_AUTH_MODE"] = "agent" };

            var config = VaultConfig.Load(null, env);

            Assert.Equal(AuthMode.Agent, config.AuthMode);
        }

        [Fact]
        public void AppRole_requires_role_id_and_wrapped_file()
        {
            var env = new Hashtable { ["VAULT_ADDR"] = "https://vault.test:8200" };

            var ex = Assert.Throws<InvalidOperationException>(() => VaultConfig.Load(null, env));
            Assert.Contains("role_id", ex.Message);
        }

        [Fact]
        public void Unknown_setting_is_refused()
        {
            Assert.Throws<InvalidOperationException>(() => new VaultConfig().ApplyJson("{\"vaultUrl\":\"x\"}"));
        }
    }
}
