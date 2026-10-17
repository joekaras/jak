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

using System.Security.Authentication;
using Xunit;

namespace Fis.Foundation.Vault.Tests
{
    public sealed class VaultHttpTests
    {
        [Fact]
        public void Tls_is_pinned_to_12()
        {
            var config = new VaultConfig { Address = new Uri("https://vault.test:8200/"), AuthMode = AuthMode.Agent };

            using var handler = VaultHttp.CreateHandler(config);

            Assert.Equal(SslProtocols.Tls12, handler.SslOptions.EnabledSslProtocols);
        }
    }
}
