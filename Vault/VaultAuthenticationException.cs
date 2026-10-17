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

using System;
using System.Runtime.Serialization;

namespace Fis.Foundation.Security.Vault
{
    /// <summary>
    /// Represents an error that occurs when Vault authentication fails.
    /// </summary>
    [Serializable]
    public sealed class VaultAuthenticationException : Exception
    {
        /// <summary>
        /// Initializes a new instance of the <see cref="VaultAuthenticationException"/> class.
        /// </summary>
        public VaultAuthenticationException()
        {
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="VaultAuthenticationException"/> class
        /// with a specified error message.
        /// </summary>
        /// <param name="message">The message that describes the error.</param>
        public VaultAuthenticationException(string message) : base(message)
        {
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="VaultAuthenticationException"/> class
        /// with a specified error message and a reference to the inner exception
        /// that is the cause of this exception.
        /// </summary>
        /// <param name="message">The error message that explains the reason for the exception.</param>
        /// <param name="innerException">The exception that is the cause of the current exception.</param>
        public VaultAuthenticationException(string message, Exception innerException) : base(message, innerException)
        {
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="VaultAuthenticationException"/> class with serialized data.
        /// </summary>
        /// <param name="info">The <see cref="SerializationInfo"/> that holds the serialized object data.</param>
        /// <param name="context">The <see cref="StreamingContext"/> that contains contextual information about the source or destination.</param>
        private VaultAuthenticationException(SerializationInfo info, StreamingContext context) : base(info, context)
        {
        }
    }
}
