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
using System.Text.Json;

namespace Fis.Foundation.Vault
{
    /// <summary>
    /// A Vault token and its lease. <see cref="ToString"/> never prints the token.
    /// </summary>
    internal sealed class VaultToken
    {
        internal VaultToken(string value, TimeSpan ttl, bool renewable, DateTimeOffset issuedAt)
        {
            Value = value;
            Ttl = ttl;
            Renewable = renewable;
            IssuedAt = issuedAt;
        }

        internal string Value { get; }

        /// <summary>Lease length from the last login or renewal. Zero means the token never expires.</summary>
        internal TimeSpan Ttl { get; }

        internal bool Renewable { get; }

        /// <summary>When the current lease started (login or last successful renewal).</summary>
        internal DateTimeOffset IssuedAt { get; }

        internal DateTimeOffset ExpiresAt => Ttl == TimeSpan.Zero ? DateTimeOffset.MaxValue : IssuedAt + Ttl;

        internal static VaultToken FromAuth(JsonElement auth, DateTimeOffset now)
        {
            string? token = auth.TryGetProperty("client_token", out var t) ? t.GetString() : null;
            if (string.IsNullOrEmpty(token))
                throw new VaultAuthException("Vault login response did not contain a client token.");

            long seconds = auth.TryGetProperty("lease_duration", out var d) && d.ValueKind == JsonValueKind.Number ? d.GetInt64() : 0;
            bool renewable = auth.TryGetProperty("renewable", out var r) && r.ValueKind == JsonValueKind.True;
            return new VaultToken(token, TimeSpan.FromSeconds(seconds), renewable, now);
        }

        public override string ToString() => $"VaultToken(ttl={Ttl}, renewable={Renewable}, value=***)";
    }
}
