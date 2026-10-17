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

package com.fis.foundation.vault.poc.windows;

import com.fis.foundation.vault.poc.PocDaemon;

/**
 * Windows service entry point. Run under WinSW (see {@code service/vault-poc.xml}); WinSW stops the JVM
 * with Ctrl+C semantics, which runs the shutdown hook that revokes the token.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws InterruptedException {
        PocDaemon.run("vault-poc-windows");
    }
}
