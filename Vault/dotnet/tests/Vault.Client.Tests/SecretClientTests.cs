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
    public sealed class SecretClientTests : IDisposable
    {
        private readonly List<string> _tempFiles = new();

        public void Dispose()
        {
            foreach (var file in _tempFiles) File.Delete(file);
        }

        // ---- AppRole with wrapped secret_id ------------------------------------------------------------

        [Fact]
        public async Task AppRole_unwraps_logs_in_and_reads_secret()
        {
            var (client, vault, file, _) = AppRole();

            var values = await client.GetSecretAsync("app/db");

            Assert.Equal("svc_app", values["username"]);
            Assert.Equal("443", values["port"]);
            Assert.False(File.Exists(file), "wrapped secret_id file must be deleted after reading");

            var requests = vault.Requests;
            Assert.Equal(new[] { "sys/wrapping/unwrap", "auth/approle/login", "secret/data/app/db" }, requests.Select(r => r.Path));
            Assert.Equal(StubVault.WrappingToken, requests[0].Token);
            Assert.Null(requests[1].Token);
            Assert.Contains("\"role_id\":\"role-fixture\"", requests[1].Body);
            Assert.Contains($"\"secret_id\":\"{StubVault.SecretId}\"", requests[1].Body);
            Assert.Equal(StubVault.ClientToken, requests[2].Token);
            Assert.All(requests, r => Assert.Equal("team/dev", r.Namespace));
        }

        [Fact]
        public async Task AppRole_accepts_wrap_info_json_from_vault_cli()
        {
            var (client, _, file, _) = AppRole(writeFile: false);
            File.WriteAllText(file, $"{{\"wrap_info\":{{\"token\":\"{StubVault.WrappingToken}\",\"ttl\":300}}}}");

            var values = await client.GetSecretAsync("app/db");

            Assert.Equal("svc_app", values["username"]);
        }

        [Fact]
        public async Task AppRole_missing_wrapped_file_fails_without_calling_vault()
        {
            var (client, vault, _, _) = AppRole(writeFile: false);

            var ex = await Assert.ThrowsAsync<VaultAuthException>(() => client.GetSecretAsync("app/db"));

            Assert.Contains("was not found", ex.Message);
            Assert.Empty(vault.Requests);
        }

        [Fact]
        public async Task AppRole_unwrap_rejected_reports_possible_interception_and_deletes_file()
        {
            var (client, _, file, _) = AppRole(writeFile: false);
            File.WriteAllText(file, "hvs.SOMEONE-ELSES-TOKEN");

            var ex = await Assert.ThrowsAsync<VaultAuthException>(() => client.GetSecretAsync("app/db"));

            Assert.Contains("interception", ex.Message);
            Assert.False(File.Exists(file));
        }

        [Fact]
        public async Task AppRole_logs_in_only_once_across_reads()
        {
            var (client, vault, _, _) = AppRole(cacheTtl: TimeSpan.Zero);

            await client.GetSecretAsync("app/db");
            await client.GetSecretAsync("app/db");

            Assert.Equal(1, vault.Count("POST", "auth/approle/login"));
            Assert.Equal(2, vault.Count("GET", "secret/data/app/db"));
        }

        // ---- 401/403 handling ----------------------------------------------------------------------------

        [Fact]
        public async Task Policy_denial_with_valid_token_is_permission_denied_without_relogin()
        {
            var vault = StubVault.WithHappyPath().On("GET", "secret/data/app/db", 403, "{\"errors\":[\"permission denied\"]}");
            var (client, _, _, _) = AppRole(vault);

            var ex = await Assert.ThrowsAsync<VaultPermissionDeniedException>(() => client.GetSecretAsync("app/db"));

            Assert.Equal(403, ex.StatusCode);
            Assert.Equal(1, vault.Count("POST", "auth/approle/login"));
            Assert.Equal(1, vault.Count("GET", "auth/token/lookup-self"));
        }

        [Fact]
        public async Task Dead_token_in_AppRole_mode_fails_because_secret_id_is_spent()
        {
            var vault = StubVault.WithHappyPath()
                .On("GET", "secret/data/app/db", 403)
                .On("GET", "auth/token/lookup-self", 403);
            var (client, _, _, _) = AppRole(vault);

            var ex = await Assert.ThrowsAsync<VaultAuthException>(() => client.GetSecretAsync("app/db"));

            Assert.Contains("already spent", ex.Message);
            Assert.Equal(1, vault.Count("POST", "auth/approle/login"));
        }

        [Fact]
        public async Task Agent_mode_sends_no_token_and_retries_once_after_agent_token_failure()
        {
            int reads = 0;
            var vault = StubVault.WithHappyPath()
                .On("GET", "auth/token/lookup-self", 403)
                .On("GET", "secret/data/app/db", _ => ++reads == 1
                    ? StubVault.Json(403, "{}")
                    : StubVault.Json(200, "{\"data\":{\"data\":{\"username\":\"svc_app\"}}}"));
            var client = Agent(vault);

            var values = await client.GetSecretAsync("app/db");

            Assert.Equal("svc_app", values["username"]);
            Assert.Equal(2, reads);
            Assert.All(vault.Requests, r => Assert.Null(r.Token));
            Assert.Equal(0, vault.Count("POST", "auth/approle/login"));
        }

        [Fact]
        public async Task Agent_mode_policy_denial_is_permission_denied()
        {
            var vault = StubVault.WithHappyPath().On("GET", "secret/data/app/db", 403);
            var client = Agent(vault);

            await Assert.ThrowsAsync<VaultPermissionDeniedException>(() => client.GetSecretAsync("app/db"));
            Assert.Equal(1, vault.Count("GET", "secret/data/app/db"));
        }

        // ---- Other status codes and retries --------------------------------------------------------------

        [Fact]
        public async Task Missing_path_is_not_found()
        {
            var (client, _, _, _) = AppRole();

            var ex = await Assert.ThrowsAsync<VaultNotFoundException>(() => client.GetSecretAsync("app/missing"));

            Assert.Equal(404, ex.StatusCode);
        }

        [Fact]
        public async Task Sealed_vault_is_retried_then_reported()
        {
            var vault = StubVault.WithHappyPath().On("GET", "secret/data/app/db", 503);
            var (client, _, _, _) = AppRole(vault);

            await Assert.ThrowsAsync<VaultSealedException>(() => client.GetSecretAsync("app/db"));

            Assert.Equal(3, vault.Count("GET", "secret/data/app/db")); // first try + MaxRetries (2)
        }

        [Fact]
        public async Task Transient_500_is_retried_and_succeeds()
        {
            int reads = 0;
            var vault = StubVault.WithHappyPath().On("GET", "secret/data/app/db", _ => ++reads == 1
                ? StubVault.Json(500, "{}")
                : StubVault.Json(200, "{\"data\":{\"data\":{\"username\":\"svc_app\"}}}"));
            var (client, _, _, _) = AppRole(vault);

            var values = await client.GetSecretAsync("app/db");

            Assert.Equal("svc_app", values["username"]);
            Assert.Equal(2, reads);
        }

        [Fact]
        public async Task Login_is_never_retried_because_the_secret_id_is_single_use()
        {
            var vault = StubVault.WithHappyPath().On("POST", "auth/approle/login", 500);
            var (client, _, _, _) = AppRole(vault);

            await Assert.ThrowsAsync<VaultUnavailableException>(() => client.GetSecretAsync("app/db"));

            Assert.Equal(1, vault.Count("POST", "auth/approle/login"));
        }

        // ---- Cache -----------------------------------------------------------------------------------

        [Fact]
        public async Task Cache_serves_repeat_reads_until_invalidated_or_expired()
        {
            var (client, vault, _, time) = AppRole(cacheTtl: TimeSpan.FromMinutes(5));

            await client.GetSecretAsync("app/db");
            await client.GetSecretAsync("/app/db/");
            Assert.Equal(1, vault.Count("GET", "secret/data/app/db"));

            client.Invalidate("app/db");
            await client.GetSecretAsync("app/db");
            Assert.Equal(2, vault.Count("GET", "secret/data/app/db"));

            time.Advance(TimeSpan.FromMinutes(6));
            await client.GetSecretAsync("app/db");
            Assert.Equal(3, vault.Count("GET", "secret/data/app/db"));
        }

        // ---- Keys --------------------------------------------------------------------------------------

        [Fact]
        public async Task GetKey_returns_decoded_bytes_of_expected_length()
        {
            var key = Enumerable.Range(0, 32).Select(i => (byte)i).ToArray();
            var vault = StubVault.WithHappyPath().On("GET", "secret/data/app/keys", 200,
                $"{{\"data\":{{\"data\":{{\"aes256\":\"{Convert.ToBase64String(key)}\"}}}}}}");
            var (client, _, _, _) = AppRole(vault);

            var bytes = await client.GetKeyAsync("app/keys", "aes256");

            Assert.Equal(key, bytes);
        }

        [Fact]
        public async Task GetKey_rejects_wrong_length_and_bad_base64()
        {
            var vault = StubVault.WithHappyPath().On("GET", "secret/data/app/keys", 200,
                $"{{\"data\":{{\"data\":{{\"short\":\"{Convert.ToBase64String(new byte[16])}\",\"junk\":\"not base64!\"}}}}}}");
            var (client, _, _, _) = AppRole(vault);

            var length = await Assert.ThrowsAsync<VaultException>(() => client.GetKeyAsync("app/keys", "short"));
            Assert.Contains("16 bytes; expected 32", length.Message);

            var format = await Assert.ThrowsAsync<VaultException>(() => client.GetKeyAsync("app/keys", "junk"));
            Assert.Contains("not valid base64", format.Message);
        }

        [Fact]
        public async Task Missing_key_is_not_found()
        {
            var (client, _, _, _) = AppRole();

            var ex = await Assert.ThrowsAsync<VaultNotFoundException>(() => client.GetSecretAsync("app/db", "nope"));

            Assert.Contains("'nope'", ex.Message);
        }

        // ---- No secret material in errors -----------------------------------------------------------

        [Fact]
        public async Task Exceptions_never_contain_tokens_or_secret_values()
        {
            var failures = new List<Exception>();

            var denied = StubVault.WithHappyPath().On("GET", "secret/data/app/db", 403);
            failures.Add(await Assert.ThrowsAsync<VaultPermissionDeniedException>(() => AppRole(denied).Client.GetSecretAsync("app/db")));
            failures.Add(await Assert.ThrowsAsync<VaultNotFoundException>(() => AppRole().Client.GetSecretAsync("app/db", "nope")));

            var (badUnwrap, _, file, _) = AppRole(writeFile: false);
            File.WriteAllText(file, "{\"wrap_info\":{\"token\":\"hvs.LEAK-CANARY\"}}");
            failures.Add(await Assert.ThrowsAsync<VaultAuthException>(() => badUnwrap.GetSecretAsync("app/db")));

            foreach (var text in failures.Select(f => f.ToString()))
            {
                Assert.DoesNotContain(StubVault.ClientToken, text);
                Assert.DoesNotContain(StubVault.SecretId, text);
                Assert.DoesNotContain(StubVault.SecretValue, text);
                Assert.DoesNotContain("LEAK-CANARY", text);
            }
        }

        // ---- Token lifecycle -----------------------------------------------------------------------------

        [Fact]
        public async Task Dispose_revokes_token_in_AppRole_mode()
        {
            var (client, vault, _, _) = AppRole();
            await client.GetSecretAsync("app/db");

            await client.DisposeAsync();

            var revoke = Assert.Single(vault.Requests, r => r.Path == "auth/token/revoke-self");
            Assert.Equal(StubVault.ClientToken, revoke.Token);
            await Assert.ThrowsAsync<ObjectDisposedException>(() => client.GetSecretAsync("app/db"));
        }

        [Fact]
        public async Task Dispose_never_revokes_the_agents_token()
        {
            var vault = StubVault.WithHappyPath();
            var client = Agent(vault);
            await client.GetSecretAsync("app/db");

            await client.DisposeAsync();

            Assert.Equal(0, vault.Count("POST", "auth/token/revoke-self"));
        }

        [Fact]
        public async Task Fresh_token_is_not_renewed()
        {
            var (client, vault, _, time) = AppRole(cacheTtl: TimeSpan.Zero);

            await client.GetSecretAsync("app/db");
            time.Advance(TimeSpan.FromMinutes(1));
            await client.GetSecretAsync("app/db");

            Assert.Equal(0, vault.Count("POST", "auth/token/renew-self"));
        }

        [Fact]
        public async Task Token_close_to_expiry_is_renewed()
        {
            var (client, vault, _, time) = AppRole(cacheTtl: TimeSpan.Zero);

            await client.GetSecretAsync("app/db");
            time.Advance(TimeSpan.FromSeconds(3500)); // 100 s left of a 3600 s lease
            await client.GetSecretAsync("app/db");

            Assert.Equal(1, vault.Count("POST", "auth/token/renew-self"));
            Assert.Equal(1, vault.Count("POST", "auth/approle/login"));
        }

        // ---- Input validation --------------------------------------------------------------------------

        [Theory]
        [InlineData("")]
        [InlineData("../other")]
        [InlineData("app//db")]
        [InlineData("app/./db")]
        public async Task Bad_paths_are_rejected(string path)
        {
            var (client, vault, _, _) = AppRole();

            await Assert.ThrowsAsync<ArgumentException>(() => client.GetSecretAsync(path));
            Assert.Empty(vault.Requests);
        }

        // ---- Helpers -----------------------------------------------------------------------------------

        private (SecretClient Client, StubVault Vault, string File, ManualTime Time) AppRole(
            StubVault? vault = null,
            TimeSpan? cacheTtl = null,
            bool writeFile = true)
        {
            vault ??= StubVault.WithHappyPath();
            string file = Path.Combine(Path.GetTempPath(), "vault-test-" + Guid.NewGuid().ToString("N"));
            _tempFiles.Add(file);
            if (writeFile) File.WriteAllText(file, StubVault.WrappingToken);

            var config = new VaultConfig
            {
                Address = new Uri(StubVault.Address),
                Namespace = "team/dev",
                KvMount = "secret",
                AuthMode = AuthMode.AppRoleWrapped,
                RoleId = "role-fixture",
                WrappedSecretIdFile = file,
                CacheTtl = cacheTtl ?? TimeSpan.FromMinutes(5),
                MaxRetries = 2,
                RetryBaseDelay = TimeSpan.FromMilliseconds(1)
            };

            var time = new ManualTime();
            var client = new SecretClient(config, vault, time, (_, _) => Task.CompletedTask);
            return (client, vault, file, time);
        }

        private static SecretClient Agent(StubVault vault)
        {
            var config = new VaultConfig
            {
                Address = new Uri("http://127.0.0.1:8100/"),
                KvMount = "secret",
                AuthMode = AuthMode.Agent,
                CacheTtl = TimeSpan.Zero,
                MaxRetries = 0
            };

            return new SecretClient(config, vault, new ManualTime(), (_, _) => Task.CompletedTask);
        }
    }
}
