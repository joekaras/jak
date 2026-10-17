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

using System.Text.Json.Serialization;

namespace Fis.Foundation.Security.Vault.Snowflake
{
    /// <summary>
    /// Represents Snowflake connection credentials and default context values
    /// retrieved from Vault.
    /// </summary>
    public sealed class SnowflakeCredentials
    {
        /// <summary>
        /// Gets or sets the Snowflake account identifier.
        /// </summary>
        [JsonPropertyName("account")]
        public string Account { get; set; }

        /// <summary>
        /// Gets or sets the Snowflake user name.
        /// </summary>
        [JsonPropertyName("username")]
        public string Username { get; set; }

        /// <summary>
        /// Gets or sets the Snowflake password.
        /// </summary>
        [JsonPropertyName("password")]
        public string Password { get; set; }

        /// <summary>
        /// Gets or sets the default Snowflake warehouse.
        /// </summary>
        [JsonPropertyName("warehouse")]
        public string Warehouse { get; set; }

        /// <summary>
        /// Gets or sets the default Snowflake database.
        /// </summary>
        [JsonPropertyName("database")]
        public string Database { get; set; }

        /// <summary>
        /// Gets or sets the default Snowflake schema.
        /// </summary>
        [JsonPropertyName("schema")]
        public string Schema { get; set; }
    }
}
