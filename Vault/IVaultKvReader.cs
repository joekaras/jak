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
using System.Threading.Tasks;

namespace Fis.Foundation.Security.Vault
{
    /// <summary>
    /// Defines methods for reading secrets from Vault KV and managing authentication state.
    /// </summary>
    public interface IVaultKvReader
    {
        /// <summary>
        /// Reads a secret from the specified KV mount point and path.
        /// </summary>
        /// <typeparam name="T">The secret model type.</typeparam>
        /// <param name="mountPoint">The KV mount point.</param>
        /// <param name="path">The secret path.</param>
        /// <param name="cancellationToken">A token to cancel the asynchronous operation.</param>
        /// <returns>A task that represents the asynchronous operation. The task result contains the secret model.</returns>
        Task<T> ReadSecretAsync<T>(string mountPoint, string path, CancellationToken cancellationToken = default(CancellationToken))
            where T : class, new();

        /// <summary>
        /// Resets the current authentication token so a new token can be acquired.
        /// </summary>
        void ResetToken();
    }
}
