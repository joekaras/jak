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
 * The client could not obtain or keep a valid token (401/403 on a token, failed login or unwrap).
 */
public final class VaultAuthException extends VaultException {
    private static final long serialVersionUID = 1L;

    public VaultAuthException(String message) {
        super(message, null, null);
    }

    public VaultAuthException(String message, Integer statusCode, Throwable cause) {
        super(message, statusCode, cause);
    }
}