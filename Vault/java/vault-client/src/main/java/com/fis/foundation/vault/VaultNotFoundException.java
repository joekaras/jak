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
/**
 * The secret path or key does not exist.
 */
public final class VaultNotFoundException extends VaultException {
    private static final long serialVersionUID = 1L;

    public VaultNotFoundException(String message) {
        super(message, null, null);
    }

    public VaultNotFoundException(String message, Integer statusCode, Throwable cause) {
        super(message, statusCode, cause);
    }
}