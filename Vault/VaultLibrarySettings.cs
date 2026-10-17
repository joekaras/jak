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
using System.Configuration;

namespace Fis.Foundation.Security.Vault
{
    /// <summary>
    /// Represents configuration settings required by the Vault library.
    /// </summary>
    public sealed class VaultLibrarySettings
    {
        /// <summary>
        /// Gets the configuration section name used to load Vault settings.
        /// </summary>
        public const string SectionName = "vaultSettings";

        /// <summary>
        /// Gets or sets the base URL of the Vault server.
        /// </summary>
        public string VaultUrl { get; set; }

        /// <summary>
        /// Gets or sets the Vault namespace.
        /// </summary>
        public string Namespace { get; set; }

        /// <summary>
        /// Gets or sets the KV secrets engine mount point.
        /// </summary>
        public string KvMountPoint { get; set; } = "kv";

        /// <summary>
        /// Gets or sets the authentication mount point.
        /// </summary>
        public string AuthMountPoint { get; set; } = "approle";

        /// <summary>
        /// Gets or sets the AppRole role identifier.
        /// </summary>
        public string RoleId { get; set; }

        /// <summary>
        /// Gets or sets the AppRole secret identifier.
        /// </summary>
        public string SecretId { get; set; }

        /// <summary>
        /// Gets or sets a value indicating whether KV secrets should be cached.
        /// </summary>
        public bool CacheKvSecrets { get; set; } = true;

        /// <summary>
        /// Gets or sets the default cache time-to-live for KV secrets.
        /// </summary>
        public TimeSpan KvDefaultTtl { get; set; } = TimeSpan.FromMinutes(5);

        /// <summary>
        /// Gets or sets the maximum cache time-to-live for KV secrets.
        /// </summary>
        public TimeSpan KvMaxTtl { get; set; } = TimeSpan.FromMinutes(15);

        /// <summary>
        /// Validates the current settings values.
        /// </summary>
        /// <exception cref="ConfigurationErrorsException">
        /// Thrown when one or more required settings are missing or contain invalid values.
        /// </exception>
        internal void Validate()
        {
            if (string.IsNullOrWhiteSpace(VaultUrl)) throw new ConfigurationErrorsException("vaultUrl is required.");
            if (string.IsNullOrWhiteSpace(RoleId)) throw new ConfigurationErrorsException("roleId is required.");
            if (string.IsNullOrWhiteSpace(SecretId)) throw new ConfigurationErrorsException("secretId is required.");
            if (string.IsNullOrWhiteSpace(KvMountPoint)) throw new ConfigurationErrorsException("kvMountPoint is required.");
            if (string.IsNullOrWhiteSpace(AuthMountPoint)) throw new ConfigurationErrorsException("authMountPoint is required.");
            if (KvDefaultTtl <= TimeSpan.Zero) throw new ConfigurationErrorsException("kvDefaultTtl must be greater than zero.");
            if (KvMaxTtl <= TimeSpan.Zero) throw new ConfigurationErrorsException("kvMaxTtl must be greater than zero.");
        }
    }

    /// <summary>
    /// Defines the custom configuration section mapping for Vault library settings.
    /// </summary>
    public sealed class VaultLibrarySettingsHandler : ConfigurationSection
    {
        [ConfigurationProperty("vaultUrl", IsRequired = true)]
        public string VaultUrl => (string)this["vaultUrl"];

        [ConfigurationProperty("vaultNamespace", IsRequired = false)]
        public string Namespace => (string)this["vaultNamespace"];

        [ConfigurationProperty("kvMountPoint", IsRequired = true)]
        public string KvMountPoint => (string)this["kvMountPoint"];

        [ConfigurationProperty("authMountPoint", IsRequired = false, DefaultValue = "approle")]
        public string AuthMountPoint => (string)this["authMountPoint"];

        [ConfigurationProperty("roleId", IsRequired = true)]
        public string RoleId => (string)this["roleId"];

        [ConfigurationProperty("secretId", IsRequired = true)]
        public string SecretId => (string)this["secretId"];

        [ConfigurationProperty("cacheKvSecrets", IsRequired = false, DefaultValue = true)]
        public bool CacheKvSecrets => (bool)this["cacheKvSecrets"];

        [ConfigurationProperty("kvDefaultTtl", IsRequired = false, DefaultValue = "00:05:00")]
        public TimeSpan KvDefaultTtl => (TimeSpan)this["kvDefaultTtl"];

        [ConfigurationProperty("kvMaxTtl", IsRequired = false, DefaultValue = "00:15:00")]
        public TimeSpan KvMaxTtl => (TimeSpan)this["kvMaxTtl"];
    }

    /// <summary>
    /// Provides access to Vault library settings loaded from application configuration.
    /// </summary>
    public static class VaultLibrarySettingsProvider
    {
        /// <summary>
        /// Loads and validates Vault library settings from the configured section.
        /// </summary>
        /// <returns>A validated <see cref="VaultLibrarySettings"/> instance.</returns>
        /// <exception cref="ConfigurationErrorsException">
        /// Thrown when the configuration section is missing, invalid, or contains invalid values.
        /// </exception>
        public static VaultLibrarySettings LoadFromConfig()
        {
            var section = (VaultLibrarySettingsHandler)ConfigurationManager.GetSection(VaultLibrarySettings.SectionName);
            
            if (section == null)
            {
                throw new ConfigurationErrorsException($"The '{VaultLibrarySettings.SectionName}' configuration section is missing or invalid.");
            }

            var settings = new VaultLibrarySettings
            {
                VaultUrl = section.VaultUrl,
                Namespace = section.Namespace,
                KvMountPoint = section.KvMountPoint,
                AuthMountPoint = section.AuthMountPoint,
                RoleId = section.RoleId,
                SecretId = section.SecretId,
                CacheKvSecrets = section.CacheKvSecrets,
                KvDefaultTtl = section.KvDefaultTtl,
                KvMaxTtl = section.KvMaxTtl
            };

            settings.Validate();
            return settings;
        }
    }
}
