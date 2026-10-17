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
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace Fis.Foundation.Vault
{
    /// <summary>
    /// Default <see cref="ISecretClient"/>. Create one per process and share it; dispose it on shutdown to
    /// revoke the token.
    /// </summary>
    public sealed class SecretClient : ISecretClient, IDisposable, IAsyncDisposable
    {
        private readonly VaultConfig _config;
        private readonly VaultHttp _http;
        private readonly TokenManager _tokens;
        private readonly TimeProvider _time;
        private readonly ConcurrentDictionary<string, CacheEntry> _cache = new ConcurrentDictionary<string, CacheEntry>(StringComparer.Ordinal);
        private int _disposed;

        internal SecretClient(
            VaultConfig config,
            HttpMessageHandler? handler,
            TimeProvider? time,
            Func<TimeSpan, CancellationToken, Task>? delay)
        {
            if (config == null) throw new ArgumentNullException(nameof(config));
            config.Validate();

            _config = config;
            _time = time ?? TimeProvider.System;
            _http = new VaultHttp(config, handler, delay);

            IVaultAuthenticator? authenticator = config.AuthMode == AuthMode.Agent
                ? null
                : new AppRoleWrappedAuthenticator(config.AuthMount, config.RoleId!, config.WrappedSecretIdFile!);
            _tokens = new TokenManager(_http, authenticator, _time);
        }

        /// <summary>
        /// Creates a client. No network call happens until the first read.
        /// </summary>
        /// <param name="config">Validated settings, usually from <see cref="VaultConfig.Load"/>.</param>
        /// <returns>The client.</returns>
        public static SecretClient Create(VaultConfig config) => new SecretClient(config, null, null, null);

        /// <inheritdoc/>
        public async Task<IReadOnlyDictionary<string, string>> GetSecretAsync(string path, CancellationToken cancellationToken = default)
        {
            ThrowIfDisposed();
            string normalized = NormalizePath(path);

            if (_cache.TryGetValue(normalized, out var entry) && entry.ExpiresAt > _time.GetUtcNow())
            {
                return entry.Values;
            }

            var values = await ReadWithRecoveryAsync(normalized, cancellationToken).ConfigureAwait(false);
            if (_config.CacheTtl > TimeSpan.Zero)
            {
                _cache[normalized] = new CacheEntry(values, _time.GetUtcNow() + _config.CacheTtl);
            }

            return values;
        }

        /// <inheritdoc/>
        public async Task<string> GetSecretAsync(string path, string key, CancellationToken cancellationToken = default)
        {
            if (string.IsNullOrWhiteSpace(key)) throw new ArgumentException("key is required.", nameof(key));

            var values = await GetSecretAsync(path, cancellationToken).ConfigureAwait(false);
            if (!values.TryGetValue(key, out var value))
            {
                throw new VaultNotFoundException($"Key '{key}' not found at '{path}'.");
            }

            return value;
        }

        /// <inheritdoc/>
        public async Task<byte[]> GetKeyAsync(string path, string key, int expectedLength = 32, CancellationToken cancellationToken = default)
        {
            if (expectedLength <= 0) throw new ArgumentOutOfRangeException(nameof(expectedLength));

            string encoded = await GetSecretAsync(path, key, cancellationToken).ConfigureAwait(false);
            byte[] bytes;
            try
            {
                bytes = Convert.FromBase64String(encoded);
            }
            catch (FormatException)
            {
                throw new VaultException($"Key '{key}' at '{path}' is not valid base64.");
            }

            if (bytes.Length != expectedLength)
            {
                int actual = bytes.Length;
                CryptographicOperations.ZeroMemory(bytes);
                throw new VaultException($"Key '{key}' at '{path}' is {actual} bytes; expected {expectedLength}.");
            }

            return bytes;
        }

        /// <inheritdoc/>
        public void Invalidate(string path)
        {
            ThrowIfDisposed();
            _cache.TryRemove(NormalizePath(path), out _);
        }

        /// <summary>
        /// Revokes the token (AppRole mode) and clears the cache.
        /// </summary>
        public async ValueTask DisposeAsync()
        {
            if (Interlocked.Exchange(ref _disposed, 1) == 1) return;

            await _tokens.RevokeAsync().ConfigureAwait(false);
            _cache.Clear();
            _http.Dispose();
        }

        /// <summary>
        /// Synchronous wrapper over <see cref="DisposeAsync"/> for hosts that only call Dispose.
        /// </summary>
        public void Dispose() => DisposeAsync().AsTask().GetAwaiter().GetResult();

        private async Task<IReadOnlyDictionary<string, string>> ReadWithRecoveryAsync(string path, CancellationToken cancellationToken)
        {
            string? token = await _tokens.GetTokenAsync(cancellationToken).ConfigureAwait(false);
            try
            {
                return await ReadAsync(path, token, cancellationToken).ConfigureAwait(false);
            }
            catch (VaultAuthException ex) when (ex.StatusCode == 401 || ex.StatusCode == 403)
            {
                // Vault answers 403 both for a dead token and for a policy denial. Ask Vault which one it is.
                if (await _tokens.IsCurrentTokenValidAsync(cancellationToken).ConfigureAwait(false))
                {
                    throw PermissionDenied(path, ex);
                }

                await _tokens.ResetAsync(cancellationToken).ConfigureAwait(false);
                token = await _tokens.GetTokenAsync(cancellationToken).ConfigureAwait(false);
                try
                {
                    return await ReadAsync(path, token, cancellationToken).ConfigureAwait(false);
                }
                catch (VaultAuthException retryEx) when (retryEx.StatusCode == 401 || retryEx.StatusCode == 403)
                {
                    // A fresh token was still refused, so the policy is the problem.
                    throw PermissionDenied(path, retryEx);
                }
            }
        }

        private async Task<IReadOnlyDictionary<string, string>> ReadAsync(string path, string? token, CancellationToken cancellationToken)
        {
            string apiPath = $"{EscapePath(_config.KvMount)}/data/{EscapePath(path)}";
            using var doc = await _http.SendAsync(HttpMethod.Get, apiPath, token, null, retryable: true, cancellationToken).ConfigureAwait(false);

            if (doc == null
                || !doc.RootElement.TryGetProperty("data", out var outer)
                || !outer.TryGetProperty("data", out var data)
                || data.ValueKind != JsonValueKind.Object)
            {
                throw new VaultNotFoundException($"No secret data at '{path}' (deleted or destroyed version?).");
            }

            var values = new Dictionary<string, string>(StringComparer.Ordinal);
            foreach (var property in data.EnumerateObject())
            {
                values[property.Name] = property.Value.ValueKind == JsonValueKind.String
                    ? property.Value.GetString() ?? string.Empty
                    : property.Value.GetRawText();
            }

            return values;
        }

        private static VaultPermissionDeniedException PermissionDenied(string path, VaultException cause) =>
            new VaultPermissionDeniedException($"Vault policy does not allow reading '{path}'.", cause.StatusCode, cause);

        private static string NormalizePath(string path)
        {
            if (string.IsNullOrWhiteSpace(path)) throw new ArgumentException("path is required.", nameof(path));

            string trimmed = path.Trim().Trim('/');
            if (trimmed.Length == 0 || trimmed.Split('/').Any(s => s.Length == 0 || s == "." || s == ".."))
            {
                throw new ArgumentException("path must be a relative KV path without empty, '.' or '..' segments.", nameof(path));
            }

            return trimmed;
        }

        private static string EscapePath(string path) => string.Join("/", path.Split('/').Select(Uri.EscapeDataString));

        private void ThrowIfDisposed()
        {
            if (Volatile.Read(ref _disposed) == 1) throw new ObjectDisposedException(nameof(SecretClient));
        }

        private sealed class CacheEntry
        {
            internal CacheEntry(IReadOnlyDictionary<string, string> values, DateTimeOffset expiresAt)
            {
                Values = values;
                ExpiresAt = expiresAt;
            }

            internal IReadOnlyDictionary<string, string> Values { get; }

            internal DateTimeOffset ExpiresAt { get; }
        }
    }
}
