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
using System.Net.Security;
using System.Security.Authentication;
using System.Security.Cryptography.X509Certificates;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace Fis.Foundation.Vault
{
    /// <summary>
    /// Thin transport over Vault's HTTP API: headers, timeouts, custom CA trust, retries, and
    /// mapping of status codes to typed exceptions.
    /// </summary>
    internal sealed class VaultHttp : IDisposable
    {
        private static readonly TimeSpan MaxBackoff = TimeSpan.FromSeconds(10);

        private readonly VaultConfig _config;
        private readonly HttpClient _http;
        private readonly Func<TimeSpan, CancellationToken, Task> _delay;

        internal VaultHttp(VaultConfig config, HttpMessageHandler? handler = null, Func<TimeSpan, CancellationToken, Task>? delay = null)
        {
            _config = config;
            _http = new HttpClient(handler ?? CreateHandler(config), disposeHandler: true)
            {
                BaseAddress = new Uri(config.Address!, "v1/"),
                Timeout = config.RequestTimeout
            };
            _delay = delay ?? Task.Delay;
        }

        /// <summary>
        /// Sends a request and returns the parsed body, or null for an empty response.
        /// </summary>
        /// <param name="method">HTTP method.</param>
        /// <param name="apiPath">Path under <c>/v1/</c>, already escaped.</param>
        /// <param name="token">Token for <c>X-Vault-Token</c>, or null to send none.</param>
        /// <param name="body">JSON body as string pairs, or null.</param>
        /// <param name="retryable">
        /// False for requests that consume something (unwrap, login): a retry after a lost response would
        /// fail anyway because the single-use value is already spent.
        /// </param>
        /// <param name="cancellationToken">Cancels the request and any backoff.</param>
        internal async Task<JsonDocument?> SendAsync(
            HttpMethod method,
            string apiPath,
            string? token,
            IReadOnlyDictionary<string, string>? body,
            bool retryable,
            CancellationToken cancellationToken)
        {
            int attempt = 0;
            while (true)
            {
                HttpResponseMessage response;
                try
                {
                    using var request = BuildRequest(method, apiPath, token, body);
                    response = await _http.SendAsync(request, HttpCompletionOption.ResponseContentRead, cancellationToken).ConfigureAwait(false);
                }
                catch (Exception ex) when (IsTransportFailure(ex, cancellationToken))
                {
                    if (retryable && attempt < _config.MaxRetries)
                    {
                        await BackoffAsync(attempt++, cancellationToken).ConfigureAwait(false);
                        continue;
                    }

                    throw new VaultUnavailableException($"Vault request failed: {method} {apiPath}", null, ex);
                }

                using (response)
                {
                    int status = (int)response.StatusCode;
                    if (status >= 200 && status < 300)
                    {
                        if (status == 204 || response.Content.Headers.ContentLength == 0) return null;

                        using var stream = await response.Content.ReadAsStreamAsync(cancellationToken).ConfigureAwait(false);
                        return await JsonDocument.ParseAsync(stream, default, cancellationToken).ConfigureAwait(false);
                    }

                    if (retryable && IsTransient(status) && attempt < _config.MaxRetries)
                    {
                        await BackoffAsync(attempt++, cancellationToken).ConfigureAwait(false);
                        continue;
                    }

                    throw MapError(status, method, apiPath);
                }
            }
        }

        public void Dispose() => _http.Dispose();

        private HttpRequestMessage BuildRequest(HttpMethod method, string apiPath, string? token, IReadOnlyDictionary<string, string>? body)
        {
            var request = new HttpRequestMessage(method, apiPath);

            // Vault Agent can require this header (require_request_header) to block SSRF-style calls.
            request.Headers.Add("X-Vault-Request", "true");
            if (!string.IsNullOrEmpty(_config.Namespace)) request.Headers.Add("X-Vault-Namespace", _config.Namespace);
            if (!string.IsNullOrEmpty(token)) request.Headers.Add("X-Vault-Token", token);

            if (body != null)
            {
                request.Content = new StringContent(JsonSerializer.Serialize(body), Encoding.UTF8, "application/json");
            }

            return request;
        }

        private Task BackoffAsync(int attempt, CancellationToken cancellationToken)
        {
            double baseMs = _config.RetryBaseDelay.TotalMilliseconds;
            double delayMs = Math.Min(MaxBackoff.TotalMilliseconds, baseMs * Math.Pow(2, attempt));
            delayMs += Random.Shared.NextDouble() * baseMs;
            return _delay(TimeSpan.FromMilliseconds(delayMs), cancellationToken);
        }

        private static bool IsTransportFailure(Exception ex, CancellationToken cancellationToken)
        {
            if (ex is HttpRequestException || ex is IOException) return true;

            // HttpClient.Timeout surfaces as TaskCanceledException without the caller's token being cancelled.
            return ex is OperationCanceledException && !cancellationToken.IsCancellationRequested;
        }

        private static bool IsTransient(int status) => status == 429 || status == 500 || status == 502 || status == 503 || status == 504;

        private static VaultException MapError(int status, HttpMethod method, string apiPath)
        {
            string message = $"Vault returned {status} for {method} {apiPath}";
            switch (status)
            {
                case 401:
                case 403:
                    return new VaultAuthException(message, status);
                case 404:
                    return new VaultNotFoundException(message, status);
                case 503:
                    return new VaultSealedException(message + " (sealed, standby, or maintenance)", status);
                case 429:
                case 500:
                case 502:
                case 504:
                    return new VaultUnavailableException(message, status);
                default:
                    return new VaultException(message, status);
            }
        }

        internal static SocketsHttpHandler CreateHandler(VaultConfig config)
        {
            var handler = new SocketsHttpHandler
            {
                ConnectTimeout = config.ConnectTimeout,
                PooledConnectionLifetime = TimeSpan.FromMinutes(5)
            };

            // Same pin as the Java client.
            handler.SslOptions.EnabledSslProtocols = SslProtocols.Tls12;

            if (!string.IsNullOrWhiteSpace(config.CaCertificateFile))
            {
                var roots = new X509Certificate2Collection();
                roots.ImportFromPemFile(config.CaCertificateFile);
                handler.SslOptions.RemoteCertificateValidationCallback = (sender, certificate, chain, errors) =>
                    ValidateAgainstCustomRoots(certificate, errors, roots);
            }

            return handler;
        }

        private static bool ValidateAgainstCustomRoots(X509Certificate? certificate, SslPolicyErrors errors, X509Certificate2Collection roots)
        {
            if (certificate == null) return false;

            // Host name checks still apply; only the trust anchor is replaced.
            if ((errors & (SslPolicyErrors.RemoteCertificateNameMismatch | SslPolicyErrors.RemoteCertificateNotAvailable)) != 0) return false;

            using var chain = new X509Chain();
            chain.ChainPolicy.TrustMode = X509ChainTrustMode.CustomRootTrust;
            chain.ChainPolicy.CustomTrustStore.AddRange(roots);

            // Internal CAs often have no reachable CRL endpoint from app servers. Turn this on if yours does.
            chain.ChainPolicy.RevocationMode = X509RevocationMode.NoCheck;

            using var leaf = new X509Certificate2(certificate);
            return chain.Build(leaf);
        }
    }
}
