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
using System.Net;
using System.Threading;
using System.Threading.Tasks;
using VaultSharp;
using VaultSharp.Core;
using VaultSharp.V1.AuthMethods.AppRole;

namespace Fis.Foundation.Security.Vault
{
    public sealed class VaultSharpKvReader : IVaultKvReader
    {
        private readonly IVaultClient _vaultClient;

        public VaultSharpKvReader(VaultLibrarySettings settings)
        {
            if (settings == null) throw new ArgumentNullException(nameof(settings));
            if (string.IsNullOrWhiteSpace(settings.VaultUrl)) throw new ArgumentException("VaultUrl is required.");
            if (string.IsNullOrWhiteSpace(settings.RoleId)) throw new ArgumentException("RoleId is required.");
            if (string.IsNullOrWhiteSpace(settings.SecretId)) throw new ArgumentException("SecretId is required.");

            ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12;

            var authMethod = new AppRoleAuthMethodInfo(settings.AuthMountPoint, settings.RoleId, settings.SecretId);
            var clientSettings = new VaultClientSettings(settings.VaultUrl, authMethod);

            if (!string.IsNullOrWhiteSpace(settings.Namespace))
            {
                clientSettings.Namespace = settings.Namespace;
            }

            _vaultClient = new VaultClient(clientSettings);
        }

        public async Task<T> ReadSecretAsync<T>(string mountPoint, string path, CancellationToken cancellationToken = default(CancellationToken))
            where T : class, new()
        {
            try
            {
                cancellationToken.ThrowIfCancellationRequested();

                var secret = await _vaultClient.V1.Secrets.KeyValue.V2
                    .ReadSecretAsync<T>(path, mountPoint: mountPoint)
                    .ConfigureAwait(false);

                cancellationToken.ThrowIfCancellationRequested();

                if (secret?.Data?.Data == null)
                {
                    throw new InvalidOperationException("Vault returned no secret data for path: " + path);
                }

                return secret.Data.Data;
            }
            catch (VaultApiException ex) when (
                ex.HttpStatusCode == HttpStatusCode.Unauthorized ||
                ex.HttpStatusCode == HttpStatusCode.Forbidden)
            {
                throw new VaultAuthenticationException("Vault authentication failed.", ex);
            }
        }

        public void ResetToken()
        {
            _vaultClient.V1.Auth.ResetVaultToken();
        }
    }
}
