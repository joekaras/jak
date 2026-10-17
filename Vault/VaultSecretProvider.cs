//Copyright Notice!
//This document is protected under the trade secret and copyright
//laws as the property of Fidelity National Information Services, Inc.
//Copying, reproduction or distribution should be limited and only to
//employees with a “need to know” to do their job.
//Any disclosure of this document to third parties is strictly prohibited.

//copyright 2026 Fidelity National Information Services.
//All rights reserved worldwide.

// This file contains code that was generated in part with the assistance of GitHub Copilot,
// an AI Coding Assistant. Portions of this file were written by the developer.

using Fis.Foundation.Logging;
using Microsoft.Extensions.Caching.Memory;
using System;
using System.Reflection;
using System.Threading;
using System.Threading.Tasks;

namespace Fis.Foundation.Security.Vault
{
    /// <summary>
    /// Provides Vault KV secret retrieval with optional in-memory caching and token recovery.
    /// </summary>
    public sealed class VaultSecretProvider : ISecretProvider, IDisposable
    {
        private readonly VaultLibrarySettings _settings;
        private readonly IVaultKvReader _reader;
        private readonly IMemoryCache _cache;
        private bool _disposed;
        private readonly bool _ownsCache;

        private static readonly Logger Logger = new Logger(MethodBase.GetCurrentMethod()?.DeclaringType?.Name);

        /// <summary>
        /// Initializes a new instance of the <see cref="VaultSecretProvider"/> class
        /// using settings loaded from configuration.
        /// </summary>
        public VaultSecretProvider()
            : this(VaultLibrarySettingsProvider.LoadFromConfig())
        {
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="VaultSecretProvider"/> class
        /// with the provided Vault settings.
        /// </summary>
        /// <param name="settings">The Vault library settings.</param>
        public VaultSecretProvider(VaultLibrarySettings settings)
            : this(settings, new VaultSharpKvReader(settings), null)
        {
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="VaultSecretProvider"/> class
        /// with explicit dependencies.
        /// </summary>
        /// <param name="settings">The Vault library settings.</param>
        /// <param name="reader">The Vault KV reader implementation.</param>
        /// <param name="memoryCache">The memory cache instance to use. If <c>null</c>, a new cache is created.</param>
        public VaultSecretProvider(
            VaultLibrarySettings settings,
            IVaultKvReader reader,
            IMemoryCache memoryCache)
        {
            _settings = settings ?? throw new ArgumentNullException(nameof(settings));
            _reader = reader ?? throw new ArgumentNullException(nameof(reader));
            _cache = memoryCache ?? new MemoryCache(new MemoryCacheOptions());
            _ownsCache = memoryCache == null;
        }

        /// <summary>
        /// Retrieves a KV secret from the default mount point.
        /// </summary>
        /// <typeparam name="T">The secret model type.</typeparam>
        /// <param name="path">The secret path.</param>
        /// <param name="cancellationToken">A token to cancel the asynchronous operation.</param>
        /// <returns>A task that represents the asynchronous operation. The task result contains the secret model.</returns>
        public Task<T> GetKvSecretAsync<T>(string path, CancellationToken cancellationToken = default(CancellationToken))
            where T : class, new()
        {
            return GetKvSecretAsync<T>(_settings.KvMountPoint, path, cancellationToken);
        }

        /// <summary>
        /// Retrieves a KV secret from the specified mount point.
        /// </summary>
        /// <typeparam name="T">The secret model type.</typeparam>
        /// <param name="mountPoint">The KV mount point.</param>
        /// <param name="path">The secret path.</param>
        /// <param name="cancellationToken">A token to cancel the asynchronous operation.</param>
        /// <returns>A task that represents the asynchronous operation. The task result contains the secret model.</returns>
        /// <exception cref="ObjectDisposedException">Thrown when the provider has been disposed.</exception>
        /// <exception cref="ArgumentException">Thrown when <paramref name="mountPoint"/> or <paramref name="path"/> is empty.</exception>
        public async Task<T> GetKvSecretAsync<T>(
            string mountPoint,
            string path,
            CancellationToken cancellationToken = default(CancellationToken))
            where T : class, new()
        {
            ThrowIfDisposed();

            if (string.IsNullOrWhiteSpace(mountPoint)) throw new ArgumentException("mountPoint is required.");
            if (string.IsNullOrWhiteSpace(path)) throw new ArgumentException("path is required.");

            var cacheKey = BuildCacheKey<T>(mountPoint, path);

            T cached;
            if (_settings.CacheKvSecrets && _cache.TryGetValue(cacheKey, out cached) && cached != null)
            {
                Logger.Debug($"Vault cache hit for {cacheKey}");
                return cached;
            }

            Logger.Debug($"Vault cache miss for {cacheKey}");

            var result = await ExecuteWithTokenRecoveryAsync(
                ct => _reader.ReadSecretAsync<T>(mountPoint, path, ct),
                cancellationToken).ConfigureAwait(false);

            if (_settings.CacheKvSecrets)
            {
                var ttl = _settings.KvDefaultTtl <= _settings.KvMaxTtl
                    ? _settings.KvDefaultTtl
                    : _settings.KvMaxTtl;

                _cache.Set(cacheKey, result, ttl);
            }

            return result;
        }

        /// <summary>
        /// Invalidates a cached secret for the default mount point.
        /// </summary>
        /// <typeparam name="T">The secret model type.</typeparam>
        /// <param name="path">The secret path.</param>
        public void Invalidate<T>(string path) where T : class, new()
        {
            Invalidate<T>(_settings.KvMountPoint, path);
        }

        /// <summary>
        /// Invalidates a cached secret for the specified mount point.
        /// </summary>
        /// <typeparam name="T">The secret model type.</typeparam>
        /// <param name="mountPoint">The KV mount point.</param>
        /// <param name="path">The secret path.</param>
        /// <exception cref="ObjectDisposedException">Thrown when the provider has been disposed.</exception>
        public void Invalidate<T>(string mountPoint, string path) where T : class, new()
        {
            ThrowIfDisposed();

            var cacheKey = BuildCacheKey<T>(mountPoint, path);
            _cache.Remove(cacheKey);
        }

        /// <summary>
        /// Executes a Vault read operation and retries once after token reset if authentication fails.
        /// </summary>
        /// <typeparam name="T">The operation result type.</typeparam>
        /// <param name="action">The operation to execute.</param>
        /// <param name="cancellationToken">A token to cancel the asynchronous operation.</param>
        /// <returns>A task that represents the asynchronous operation. The task result contains the operation result.</returns>
        private async Task<T> ExecuteWithTokenRecoveryAsync<T>(
            Func<CancellationToken, Task<T>> action,
            CancellationToken cancellationToken)
        {
            try
            {
                return await action(cancellationToken).ConfigureAwait(false);
            }
            catch (VaultAuthenticationException)
            {
                Logger.Warn("Vault auth failed. Resetting token and retrying once.");
                _reader.ResetToken();
                return await action(cancellationToken).ConfigureAwait(false);
            }
        }

        /// <summary>
        /// Builds a cache key for a secret type and location.
        /// </summary>
        /// <typeparam name="T">The secret model type.</typeparam>
        /// <param name="mountPoint">The KV mount point.</param>
        /// <param name="path">The secret path.</param>
        /// <returns>The cache key string.</returns>
        private static string BuildCacheKey<T>(string mountPoint, string path)
        {
            return $"vault:kv:{mountPoint}:{path}:{typeof(T).FullName}";
        }

        /// <summary>
        /// Throws an exception if the provider has been disposed.
        /// </summary>
        /// <exception cref="ObjectDisposedException">Thrown when the provider has been disposed.</exception>
        private void ThrowIfDisposed()
        {
            if (_disposed) throw new ObjectDisposedException(nameof(VaultSecretProvider));
        }

        /// <summary>
        /// Releases resources used by the provider.
        /// </summary>
        public void Dispose()
        {
            if (_disposed) return;

            if (_ownsCache)
            {
                if (_cache is IDisposable disposableCache)
                {
                    disposableCache.Dispose();
                }
            }

            _disposed = true;
        }
    }
}
