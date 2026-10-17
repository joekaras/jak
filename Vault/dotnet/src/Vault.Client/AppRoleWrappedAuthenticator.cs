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

using System;
using System.Collections.Generic;
using System.IO;
using System.Net.Http;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace Fis.Foundation.Vault
{
    /// <summary>
    /// Logs in once, by an authentication method the client owns.
    /// </summary>
    internal interface IVaultAuthenticator
    {
        /// <summary>False once the credential has been spent and a new login is impossible.</summary>
        bool CanLogin { get; }

        Task<VaultToken> LoginAsync(VaultHttp http, TimeProvider time, CancellationToken cancellationToken);
    }

    /// <summary>
    /// AppRole login with a response-wrapped secret_id. The wrapping token file is read once and deleted,
    /// the secret_id is unwrapped (which fails if anyone unwrapped it first), and the login happens once.
    /// </summary>
    internal sealed class AppRoleWrappedAuthenticator : IVaultAuthenticator
    {
        private readonly string _authMount;
        private readonly string _roleId;
        private readonly string _wrappedSecretIdFile;
        private int _consumed;

        internal AppRoleWrappedAuthenticator(string authMount, string roleId, string wrappedSecretIdFile)
        {
            _authMount = authMount;
            _roleId = roleId;
            _wrappedSecretIdFile = wrappedSecretIdFile;
        }

        public bool CanLogin => Volatile.Read(ref _consumed) == 0;

        public async Task<VaultToken> LoginAsync(VaultHttp http, TimeProvider time, CancellationToken cancellationToken)
        {
            if (Interlocked.Exchange(ref _consumed, 1) == 1)
            {
                throw new VaultAuthException(
                    "The wrapped secret_id was already used. Restart the service with a newly delivered wrapped secret_id, or use Vault Agent mode.");
            }

            string wrappingToken = ReadAndDelete(_wrappedSecretIdFile);
            string secretId = await UnwrapAsync(http, wrappingToken, cancellationToken).ConfigureAwait(false);

            var body = new Dictionary<string, string> { ["role_id"] = _roleId, ["secret_id"] = secretId };
            JsonDocument? doc;
            try
            {
                doc = await http.SendAsync(HttpMethod.Post, $"auth/{_authMount}/login", null, body, retryable: false, cancellationToken).ConfigureAwait(false);
            }
            catch (VaultException ex) when (ex.StatusCode == 400 || ex.StatusCode == 401 || ex.StatusCode == 403)
            {
                throw new VaultAuthException($"AppRole login to '{_authMount}' was rejected.", ex.StatusCode, ex);
            }

            using (doc)
            {
                if (doc == null || !doc.RootElement.TryGetProperty("auth", out var auth))
                    throw new VaultAuthException("Vault login response did not contain an auth block.");
                return VaultToken.FromAuth(auth, time.GetUtcNow());
            }
        }

        private static async Task<string> UnwrapAsync(VaultHttp http, string wrappingToken, CancellationToken cancellationToken)
        {
            JsonDocument? doc;
            try
            {
                doc = await http.SendAsync(HttpMethod.Post, "sys/wrapping/unwrap", wrappingToken, null, retryable: false, cancellationToken).ConfigureAwait(false);
            }
            catch (VaultException ex) when (ex.StatusCode == 400 || ex.StatusCode == 401 || ex.StatusCode == 403)
            {
                throw new VaultAuthException(
                    "Unwrap failed: the wrapping token is invalid, expired, or already unwrapped. " +
                    "If this service did not unwrap it, treat it as a possible interception.",
                    ex.StatusCode,
                    ex);
            }

            using (doc)
            {
                if (doc != null
                    && doc.RootElement.TryGetProperty("data", out var data)
                    && data.TryGetProperty("secret_id", out var secretId)
                    && secretId.GetString() is { Length: > 0 } value)
                {
                    return value;
                }
            }

            throw new VaultAuthException("Unwrapped response did not contain a secret_id.");
        }

        /// <summary>
        /// Reads the wrapping token and deletes the file whether or not the read succeeds. Accepts either the
        /// raw token or the JSON from <c>vault write -wrap-ttl=... -format=json</c>.
        /// </summary>
        private static string ReadAndDelete(string path)
        {
            string content;
            try
            {
                content = File.ReadAllText(path).Trim();
            }
            catch (Exception ex) when (ex is FileNotFoundException || ex is DirectoryNotFoundException)
            {
                throw new VaultAuthException(
                    $"Wrapped secret_id file '{path}' was not found. The deploy pipeline must deliver one before the service starts.",
                    null,
                    ex);
            }
            finally
            {
                TryDelete(path);
            }

            if (content.StartsWith("{", StringComparison.Ordinal))
            {
                content = ExtractWrapInfoToken(content);
            }

            if (content.Length == 0) throw new VaultAuthException($"Wrapped secret_id file '{path}' was empty.");
            return content;
        }

        private static string ExtractWrapInfoToken(string json)
        {
            try
            {
                using var doc = JsonDocument.Parse(json);
                if (doc.RootElement.TryGetProperty("wrap_info", out var info) && info.TryGetProperty("token", out var token))
                {
                    return token.GetString() ?? string.Empty;
                }
            }
            catch (JsonException)
            {
                // Fall through; the message must not echo the file content.
            }

            throw new VaultAuthException("Wrapped secret_id file is JSON but has no wrap_info.token.");
        }

        private static void TryDelete(string path)
        {
            try
            {
                File.Delete(path);
            }
            catch (IOException)
            {
                // The wrapping token is single-use and is spent by the unwrap that follows, so a leftover file is inert.
            }
            catch (UnauthorizedAccessException)
            {
                // Same as above.
            }
        }
    }
}
