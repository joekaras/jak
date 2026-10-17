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

package com.fis.foundation.vault;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSocket;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class VaultHttpTest {

    @Test
    void tlsIsPinnedTo12() throws Exception {
        VaultConfig config = VaultConfig.builder().address("https://vault.test:8200").authMode(AuthMode.AGENT).build();
        VaultHttp http = new VaultHttp(config, millis -> { });

        try (SSLSocket socket = (SSLSocket) http.socketFactory().createSocket()) {
            assertArrayEquals(new String[] {"TLSv1.2"}, socket.getEnabledProtocols());
        }
    }
}
