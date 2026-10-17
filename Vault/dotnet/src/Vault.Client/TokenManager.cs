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
using System.Net.Http;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace Fis.Foundation.Vault
{
    /// <summary>
    /// Owns the token lifecycle: login on first use, renew-self before expiry, revoke-self on shutdown.
    /// In Agent mode there is no authenticator and no token; the agent injects its own.
    /// </summary>
    internal sealed class TokenManager
    {
        private static readonly IReadOnlyDictionary<string, string> EmptyBody = new Dictionary<string, string>();

        private readonly VaultHttp _http;
        private readonly IVaultAuthenticator? _authenticator;
        private readonly TimeProvider _time;
        private readonly SemaphoreSlim _gate = new SemaphoreSlim(1, 1);
        private VaultToken? _token;

        internal TokenManager(VaultHttp http, IVaultAuthenticator? authenticator, TimeProvider time)
        {
            _http = http;
            _authenticator = authenticator;
            _time = time;
        }

        private bool IsAgentMode => _authenticator == null;

        /// <summary>
        /// Returns a usable token, logging in or renewing as needed. Returns null in Agent mode.
        /// </summary>
        internal async Task<string?> GetTokenAsync(CancellationToken cancellationToken)
        {
            if (IsAgentMode) return null;

            await _gate.WaitAsync(cancellationToken).ConfigureAwait(false);
            try
            {
                var now = _time.GetUtcNow();
                if (_token == null || now >= _token.ExpiresAt)
                {
                    _token = await LoginAsync(cancellationToken).ConfigureAwait(false);
                }
                else if (_token.Renewable && _token.Ttl > TimeSpan.Zero && ShouldRenew(_token, now))
                {
                    await RenewAsync(cancellationToken).ConfigureAwait(false);
                }

                return _token.Value;
            }
            finally
            {
                _gate.Release();
            }
        }

        /// <summary>
        /// Asks Vault whether the current token still works. Used to tell an invalid token (re-login) apart
        /// from a policy denial (do not retry), since Vault answers 403 for both.
        /// </summary>
        internal async Task<bool> IsCurrentTokenValidAsync(CancellationToken cancellationToken)
        {
            string? token = null;
            if (!IsAgentMode)
            {
                token = _token?.Value;
                if (token == null) return false;
            }

            try
            {
                using var _ = await _http.SendAsync(HttpMethod.Get, "auth/token/lookup-self", token, null, retryable: true, cancellationToken).ConfigureAwait(false);
                return true;
            }
            catch (VaultAuthException)
            {
                return false;
            }
        }

        /// <summary>
        /// Drops the current token and logs in again. Throws <see cref="VaultAuthException"/> when the
        /// credential was single-use and is spent. In Agent mode this does nothing; the agent re-authenticates.
        /// </summary>
        internal async Task ResetAsync(CancellationToken cancellationToken)
        {
            if (IsAgentMode) return;

            await _gate.WaitAsync(cancellationToken).ConfigureAwait(false);
            try
            {
                _token = null;
                _token = await LoginAsync(cancellationToken).ConfigureAwait(false);
            }
            finally
            {
                _gate.Release();
            }
        }

        /// <summary>
        /// Best-effort revoke-self so the token does not outlive the process. Never revokes the agent's token.
        /// </summary>
        internal async Task RevokeAsync()
        {
            if (IsAgentMode) return;

            await _gate.WaitAsync().ConfigureAwait(false);
            try
            {
                if (_token == null) return;

                using var timeout = new CancellationTokenSource(TimeSpan.FromSeconds(5));
                try
                {
                    using var _ = await _http.SendAsync(HttpMethod.Post, "auth/token/revoke-self", _token.Value, EmptyBody, retryable: false, timeout.Token).ConfigureAwait(false);
                }
                catch (VaultException)
                {
                    // Shutdown must not fail because Vault is unreachable; the token still expires at its TTL.
                }
                catch (OperationCanceledException)
                {
                    // Same as above.
                }

                _token = null;
            }
            finally
            {
                _gate.Release();
            }
        }

        /// <summary>
        /// Decides whether to renew the token now. Only called for renewable tokens with a non-zero TTL.
        /// </summary>
        /// <param name="token">The current token. <see cref="VaultToken.IssuedAt"/> is the start of the current lease.</param>
        /// <param name="now">The current time.</param>
        /// <returns>True to call renew-self before using the token.</returns>
        internal static bool ShouldRenew(VaultToken token, DateTimeOffset now)
        {
            var elapsed = now - token.IssuedAt;
            return elapsed.Ticks * 3 >= token.Ttl.Ticks * 2;
        }

        private async Task<VaultToken> LoginAsync(CancellationToken cancellationToken)
        {
            if (!_authenticator!.CanLogin)
            {
                throw new VaultAuthException(
                    "The Vault token expired or was rejected and the single-use secret_id is already spent. " +
                    "Restart the service with a newly delivered wrapped secret_id, ask Vault admins for a periodic token (token_period), or use Vault Agent mode.");
            }

            return await _authenticator.LoginAsync(_http, _time, cancellationToken).ConfigureAwait(false);
        }

        private async Task RenewAsync(CancellationToken cancellationToken)
        {
            var current = _token!;
            try
            {
                using var doc = await _http.SendAsync(HttpMethod.Post, "auth/token/renew-self", current.Value, EmptyBody, retryable: true, cancellationToken).ConfigureAwait(false);
                if (doc != null && doc.RootElement.TryGetProperty("auth", out JsonElement auth))
                {
                    _token = VaultToken.FromAuth(auth, _time.GetUtcNow());
                }
            }
            catch (VaultAuthException)
            {
                // The token can no longer be renewed (revoked, or past its max TTL). Log in again if possible.
                _token = null;
                _token = await LoginAsync(cancellationToken).ConfigureAwait(false);
            }
            catch (VaultException ex) when (ex is VaultUnavailableException || ex is VaultSealedException)
            {
                // Vault is down; the current token is still good until it expires, so keep using it.
                if (_time.GetUtcNow() >= current.ExpiresAt) throw;
            }
        }
    }
}
