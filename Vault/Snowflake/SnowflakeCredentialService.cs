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

using System.Threading;
using System;
using System.Threading.Tasks;

namespace Fis.Foundation.Security.Vault.Snowflake
{
    /// <summary>
    /// Provides access to Snowflake credentials stored in Vault.
    /// </summary>
    public sealed class SnowflakeCredentialService
    {
        private readonly ISecretProvider _secretProvider;

        /// <summary>
        /// Initializes a new instance of the <see cref="SnowflakeCredentialService"/> class.
        /// </summary>
        /// <param name="secretProvider">The secret provider used to read credentials.</param>
        /// <exception cref="ArgumentNullException">
        /// Thrown when <paramref name="secretProvider"/> is <see langword="null"/>.
        /// </exception>
        public SnowflakeCredentialService(
            ISecretProvider secretProvider)
        {
            _secretProvider = secretProvider ?? throw new ArgumentNullException(nameof(secretProvider));
        }

        /// <summary>
        /// Retrieves Snowflake credentials from Vault for the specified secret path.
        /// </summary>
        /// <param name="path">The Vault secret path containing Snowflake credentials.</param>
        /// <param name="cancellationToken">A token used to cancel the asynchronous operation.</param>
        /// <returns>
        /// A task that resolves to the <see cref="SnowflakeCredentials"/> read from Vault.
        /// </returns>
        /// <exception cref="ArgumentException">
        /// Thrown when <paramref name="path"/> is null, empty, or whitespace.
        /// </exception>
        public Task<SnowflakeCredentials> GetAsync(string path, CancellationToken cancellationToken = default)
        {
            if (string.IsNullOrWhiteSpace(path)) throw new ArgumentException("path is required.", nameof(path));

            return _secretProvider.GetKvSecretAsync<SnowflakeCredentials>(path, cancellationToken);
        }
    }
}
