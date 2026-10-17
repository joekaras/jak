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

using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace Fis.Foundation.Vault
{
    /// <summary>
    /// Reads credentials and keys from a Vault KV v2 mount. Implementations are thread-safe.
    /// </summary>
    public interface ISecretClient
    {
        /// <summary>
        /// Reads every key/value pair at a KV path.
        /// </summary>
        /// <param name="path">The secret path under the KV mount, for example <c>app/snowflake</c>.</param>
        /// <param name="cancellationToken">Cancels the request.</param>
        /// <returns>The key/value pairs. Non-string values are returned as their JSON text.</returns>
        Task<IReadOnlyDictionary<string, string>> GetSecretAsync(string path, CancellationToken cancellationToken = default);

        /// <summary>
        /// Reads one value at a KV path.
        /// </summary>
        /// <param name="path">The secret path under the KV mount.</param>
        /// <param name="key">The key within the secret.</param>
        /// <param name="cancellationToken">Cancels the request.</param>
        /// <returns>The value.</returns>
        /// <exception cref="VaultNotFoundException">The path or key does not exist.</exception>
        Task<string> GetSecretAsync(string path, string key, CancellationToken cancellationToken = default);

        /// <summary>
        /// Reads a base64-encoded key and checks its length. The caller owns the returned array and should
        /// clear it with <c>CryptographicOperations.ZeroMemory</c> when done.
        /// </summary>
        /// <param name="path">The secret path under the KV mount.</param>
        /// <param name="key">The key within the secret.</param>
        /// <param name="expectedLength">Required length in bytes; 32 for AES-256.</param>
        /// <param name="cancellationToken">Cancels the request.</param>
        /// <returns>A new array with the decoded key bytes.</returns>
        Task<byte[]> GetKeyAsync(string path, string key, int expectedLength = 32, CancellationToken cancellationToken = default);

        /// <summary>
        /// Removes a path from the cache so the next read goes to Vault (for example after a rotation).
        /// </summary>
        /// <param name="path">The secret path under the KV mount.</param>
        void Invalidate(string path);
    }
}
