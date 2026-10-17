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

namespace Fis.Foundation.Vault
{
    /// <summary>
    /// Base type for Vault errors. Messages carry the request path and status, never tokens or secret values.
    /// </summary>
    public class VaultException : Exception
    {
        /// <summary>Initializes a new instance.</summary>
        /// <param name="message">A message without secret material.</param>
        /// <param name="statusCode">The HTTP status Vault returned, if any.</param>
        /// <param name="innerException">The underlying cause, if any.</param>
        public VaultException(string message, int? statusCode = null, Exception? innerException = null)
            : base(message, innerException)
        {
            StatusCode = statusCode;
        }

        /// <summary>The HTTP status Vault returned, or null for transport and client-side errors.</summary>
        public int? StatusCode { get; }
    }

    /// <summary>
    /// The client could not obtain or keep a valid token (401/403 on a token, failed login or unwrap).
    /// </summary>
    public sealed class VaultAuthException : VaultException
    {
        /// <inheritdoc cref="VaultException(string, int?, Exception?)"/>
        public VaultAuthException(string message, int? statusCode = null, Exception? innerException = null)
            : base(message, statusCode, innerException)
        {
        }
    }

    /// <summary>
    /// The token is valid but its policy does not allow the request. Retrying will not help.
    /// </summary>
    public sealed class VaultPermissionDeniedException : VaultException
    {
        /// <inheritdoc cref="VaultException(string, int?, Exception?)"/>
        public VaultPermissionDeniedException(string message, int? statusCode = null, Exception? innerException = null)
            : base(message, statusCode, innerException)
        {
        }
    }

    /// <summary>
    /// The secret path or key does not exist.
    /// </summary>
    public sealed class VaultNotFoundException : VaultException
    {
        /// <inheritdoc cref="VaultException(string, int?, Exception?)"/>
        public VaultNotFoundException(string message, int? statusCode = null, Exception? innerException = null)
            : base(message, statusCode, innerException)
        {
        }
    }

    /// <summary>
    /// Vault answered 503: sealed, in maintenance, or a standby that cannot serve the request.
    /// </summary>
    public sealed class VaultSealedException : VaultException
    {
        /// <inheritdoc cref="VaultException(string, int?, Exception?)"/>
        public VaultSealedException(string message, int? statusCode = null, Exception? innerException = null)
            : base(message, statusCode, innerException)
        {
        }
    }

    /// <summary>
    /// Vault could not be reached or kept failing after retries (IO errors, timeouts, 429, 5xx).
    /// </summary>
    public sealed class VaultUnavailableException : VaultException
    {
        /// <inheritdoc cref="VaultException(string, int?, Exception?)"/>
        public VaultUnavailableException(string message, int? statusCode = null, Exception? innerException = null)
            : base(message, statusCode, innerException)
        {
        }
    }
}
