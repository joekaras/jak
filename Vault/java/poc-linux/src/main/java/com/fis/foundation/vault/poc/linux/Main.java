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

package com.fis.foundation.vault.poc.linux;

import com.fis.foundation.vault.poc.PocDaemon;

/**
 * systemd entry point (see {@code service/vault-poc-java.service}). systemd's SIGTERM runs the shutdown
 * hook that revokes the token. Defaults to AppRole with a wrapped secret_id delivered by the pipeline.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws InterruptedException {
        PocDaemon.run("vault-poc-linux");
    }
}
