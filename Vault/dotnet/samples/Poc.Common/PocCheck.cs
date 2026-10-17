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

using System.Security.Cryptography;
using System.Text;

namespace Fis.Foundation.Vault.Poc
{
    /// <summary>Where the POC finds its test secrets. Paths only, never values.</summary>
    public sealed record PocSettings(string CredentialPath, string KeyPath, string KeyName)
    {
        public static PocSettings FromEnvironment() => new PocSettings(
            Environment.GetEnvironmentVariable("POC_CREDENTIAL_PATH") ?? "poc/snowflake",
            Environment.GetEnvironmentVariable("POC_KEY_PATH") ?? "poc/keys",
            Environment.GetEnvironmentVariable("POC_KEY_NAME") ?? "aes256");
    }

    /// <summary>What the POC reports: key names and a success flag, never values.</summary>
    public sealed record PocResult(IReadOnlyList<string> CredentialKeys, bool AesGcmRoundTrip);

    /// <summary>
    /// The check every POC runs: read a Snowflake-style credential, read a 32-byte AES key, prove the key
    /// works with AES-GCM, and wipe it.
    /// </summary>
    public static class PocCheck
    {
        public static async Task<PocResult> RunAsync(ISecretClient client, PocSettings settings, CancellationToken cancellationToken)
        {
            var credential = await client.GetSecretAsync(settings.CredentialPath, cancellationToken).ConfigureAwait(false);

            byte[] key = await client.GetKeyAsync(settings.KeyPath, settings.KeyName, 32, cancellationToken).ConfigureAwait(false);
            bool roundTrip;
            try
            {
                roundTrip = AesGcmRoundTrip(key);
            }
            finally
            {
                CryptographicOperations.ZeroMemory(key);
            }

            return new PocResult(credential.Keys.OrderBy(k => k, StringComparer.Ordinal).ToArray(), roundTrip);
        }

        private static bool AesGcmRoundTrip(byte[] key)
        {
            byte[] plaintext = Encoding.UTF8.GetBytes("vault-poc-round-trip");
            byte[] nonce = RandomNumberGenerator.GetBytes(AesGcm.NonceByteSizes.MaxSize);
            byte[] ciphertext = new byte[plaintext.Length];
            byte[] tag = new byte[AesGcm.TagByteSizes.MaxSize];
            byte[] decrypted = new byte[plaintext.Length];

            using var aes = new AesGcm(key, tag.Length);
            aes.Encrypt(nonce, plaintext, ciphertext, tag);
            aes.Decrypt(nonce, ciphertext, tag, decrypted);
            return CryptographicOperations.FixedTimeEquals(plaintext, decrypted);
        }
    }
}
