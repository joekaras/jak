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
using System.Collections;
using System.Globalization;
using System.IO;
using System.Net;
using System.Text.Json;

namespace Fis.Foundation.Vault
{
    /// <summary>
    /// How the client obtains a Vault token.
    /// </summary>
    public enum AuthMode
    {
        /// <summary>
        /// AppRole login. role_id comes from config; the secret_id arrives as a response-wrapped,
        /// single-use token in a file that the deploy pipeline writes and the client deletes after reading.
        /// </summary>
        AppRoleWrapped,

        /// <summary>
        /// A local Vault Agent authenticates and injects its token (<c>use_auto_auth_token = "force"</c>).
        /// The client sends no token and holds no credential.
        /// </summary>
        Agent
    }

    /// <summary>
    /// Settings for <see cref="SecretClient"/>. Load with <see cref="Load"/>: a JSON file first, then
    /// environment variables override it. A secret_id is never accepted as a setting.
    /// </summary>
    public sealed class VaultConfig
    {
        /// <summary>Vault server address, or the Vault Agent listener in <see cref="AuthMode.Agent"/> mode.</summary>
        public Uri? Address { get; set; }

        /// <summary>Vault Enterprise namespace sent as <c>X-Vault-Namespace</c>. Optional.</summary>
        public string? Namespace { get; set; }

        /// <summary>KV v2 mount point.</summary>
        public string KvMount { get; set; } = "secret";

        /// <summary>How the client authenticates.</summary>
        public AuthMode AuthMode { get; set; } = AuthMode.AppRoleWrapped;

        /// <summary>AppRole auth mount point.</summary>
        public string AuthMount { get; set; } = "approle";

        /// <summary>AppRole role_id. Not sensitive on its own; required for <see cref="AuthMode.AppRoleWrapped"/>.</summary>
        public string? RoleId { get; set; }

        /// <summary>Path of the file holding the wrapping token for the secret_id. Deleted after it is read.</summary>
        public string? WrappedSecretIdFile { get; set; }

        /// <summary>PEM bundle of the CA(s) that issued Vault's TLS certificate. Optional.</summary>
        public string? CaCertificateFile { get; set; }

        /// <summary>TCP connect timeout.</summary>
        public TimeSpan ConnectTimeout { get; set; } = TimeSpan.FromSeconds(5);

        /// <summary>Whole-request timeout.</summary>
        public TimeSpan RequestTimeout { get; set; } = TimeSpan.FromSeconds(15);

        /// <summary>In-memory secret cache lifetime. <see cref="TimeSpan.Zero"/> disables caching.</summary>
        public TimeSpan CacheTtl { get; set; } = TimeSpan.FromMinutes(5);

        /// <summary>Retries for transient failures (IO errors, 429, 5xx) on idempotent requests.</summary>
        public int MaxRetries { get; set; } = 3;

        /// <summary>Base delay for exponential backoff between retries.</summary>
        public TimeSpan RetryBaseDelay { get; set; } = TimeSpan.FromMilliseconds(250);

        /// <summary>
        /// Loads settings from an optional JSON file, applies environment variable overrides, and validates.
        /// </summary>
        /// <param name="jsonFile">Path to a JSON settings file. Ignored when null or missing.</param>
        /// <param name="environment">Environment variables; defaults to the process environment.</param>
        /// <returns>A validated configuration.</returns>
        public static VaultConfig Load(string? jsonFile = null, IDictionary? environment = null)
        {
            var config = new VaultConfig();
            if (!string.IsNullOrWhiteSpace(jsonFile) && File.Exists(jsonFile))
            {
                config.ApplyJson(File.ReadAllText(jsonFile));
            }

            config.ApplyEnvironment(environment ?? Environment.GetEnvironmentVariables());
            config.Validate();
            return config;
        }

        /// <summary>
        /// Applies settings from a flat JSON object with camelCase names (for example <c>kvMount</c>).
        /// </summary>
        /// <param name="json">The JSON text.</param>
        /// <exception cref="InvalidOperationException">The JSON contains a secret_id.</exception>
        public void ApplyJson(string json)
        {
            using var doc = JsonDocument.Parse(json);
            foreach (var property in doc.RootElement.EnumerateObject())
            {
                string value = property.Value.ValueKind == JsonValueKind.String
                    ? property.Value.GetString() ?? string.Empty
                    : property.Value.GetRawText();
                Apply(property.Name, value);
            }
        }

        /// <summary>
        /// Applies <c>VAULT_*</c> environment variables. Unset variables leave the current value alone.
        /// </summary>
        /// <param name="environment">The environment variables.</param>
        public void ApplyEnvironment(IDictionary environment)
        {
            if (environment == null) throw new ArgumentNullException(nameof(environment));

            ApplyIfSet(environment, "VAULT_ADDR", "address");
            ApplyIfSet(environment, "VAULT_NAMESPACE", "namespace");
            ApplyIfSet(environment, "VAULT_KV_MOUNT", "kvMount");
            ApplyIfSet(environment, "VAULT_AUTH_MODE", "authMode");
            ApplyIfSet(environment, "VAULT_AUTH_MOUNT", "authMount");
            ApplyIfSet(environment, "VAULT_ROLE_ID", "roleId");
            ApplyIfSet(environment, "VAULT_WRAPPED_SECRET_ID_FILE", "wrappedSecretIdFile");
            ApplyIfSet(environment, "VAULT_CACERT", "caCertificateFile");
            ApplyIfSet(environment, "VAULT_CACHE_TTL_SECONDS", "cacheTtlSeconds");
            ApplyIfSet(environment, "VAULT_MAX_RETRIES", "maxRetries");

            if (environment.Contains("VAULT_SECRET_ID"))
            {
                throw new InvalidOperationException(
                    "VAULT_SECRET_ID is set. A secret_id must not be passed as a setting; deliver a wrapped secret_id file instead.");
            }
        }

        /// <summary>
        /// Checks that the settings are complete and safe.
        /// </summary>
        /// <exception cref="InvalidOperationException">A setting is missing or unsafe.</exception>
        public void Validate()
        {
            if (Address == null || !Address.IsAbsoluteUri)
                throw new InvalidOperationException("Vault address is required (VAULT_ADDR).");
            if (Address.Scheme != Uri.UriSchemeHttps && Address.Scheme != Uri.UriSchemeHttp)
                throw new InvalidOperationException("Vault address must be http or https.");
            if (string.IsNullOrWhiteSpace(KvMount))
                throw new InvalidOperationException("KV mount is required (VAULT_KV_MOUNT).");
            if (ConnectTimeout <= TimeSpan.Zero || RequestTimeout <= TimeSpan.Zero)
                throw new InvalidOperationException("Timeouts must be greater than zero.");
            if (CacheTtl < TimeSpan.Zero)
                throw new InvalidOperationException("Cache TTL must not be negative.");
            if (MaxRetries < 0)
                throw new InvalidOperationException("Max retries must not be negative.");

            if (AuthMode == AuthMode.AppRoleWrapped)
            {
                if (string.IsNullOrWhiteSpace(AuthMount))
                    throw new InvalidOperationException("Auth mount is required (VAULT_AUTH_MOUNT).");
                if (string.IsNullOrWhiteSpace(RoleId))
                    throw new InvalidOperationException("role_id is required for AppRole (VAULT_ROLE_ID).");
                if (string.IsNullOrWhiteSpace(WrappedSecretIdFile))
                    throw new InvalidOperationException("Wrapped secret_id file is required for AppRole (VAULT_WRAPPED_SECRET_ID_FILE).");

                // Credentials and tokens must never cross the network in clear text.
                if (Address.Scheme == Uri.UriSchemeHttp && !Address.IsLoopback)
                    throw new InvalidOperationException("AppRole mode requires https unless Vault is on loopback.");
            }
        }

        private void ApplyIfSet(IDictionary environment, string variable, string setting)
        {
            if (environment[variable] is string value && value.Length > 0)
            {
                Apply(setting, value);
            }
        }

        private void Apply(string name, string value)
        {
            switch (name.ToLowerInvariant())
            {
                case "address": Address = new Uri(EnsureTrailingSlash(value), UriKind.Absolute); break;
                case "namespace": Namespace = value; break;
                case "kvmount": KvMount = value.Trim('/'); break;
                case "authmode": AuthMode = ParseAuthMode(value); break;
                case "authmount": AuthMount = value.Trim('/'); break;
                case "roleid": RoleId = value; break;
                case "wrappedsecretidfile": WrappedSecretIdFile = value; break;
                case "cacertificatefile": CaCertificateFile = value; break;
                case "cachettlseconds": CacheTtl = TimeSpan.FromSeconds(ParseInt(name, value)); break;
                case "connecttimeoutseconds": ConnectTimeout = TimeSpan.FromSeconds(ParseInt(name, value)); break;
                case "requesttimeoutseconds": RequestTimeout = TimeSpan.FromSeconds(ParseInt(name, value)); break;
                case "maxretries": MaxRetries = ParseInt(name, value); break;
                case "secretid":
                case "secret_id":
                    throw new InvalidOperationException(
                        "A secret_id must not be stored in configuration; deliver a wrapped secret_id file instead.");
                default:
                    throw new InvalidOperationException($"Unknown Vault setting '{name}'.");
            }
        }

        private static AuthMode ParseAuthMode(string value)
        {
            switch (value.Trim().ToLowerInvariant().Replace("-", string.Empty).Replace("_", string.Empty))
            {
                case "approlewrapped": return AuthMode.AppRoleWrapped;
                case "agent": return AuthMode.Agent;
                default: throw new InvalidOperationException($"Unknown auth mode '{value}'. Use 'approle-wrapped' or 'agent'.");
            }
        }

        private static int ParseInt(string name, string value)
        {
            if (!int.TryParse(value, NumberStyles.Integer, CultureInfo.InvariantCulture, out int result))
                throw new InvalidOperationException($"Setting '{name}' must be an integer.");
            return result;
        }

        private static string EnsureTrailingSlash(string value) => value.EndsWith("/", StringComparison.Ordinal) ? value : value + "/";
    }
}
