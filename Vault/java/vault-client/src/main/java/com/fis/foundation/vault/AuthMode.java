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
 * How the client obtains a Vault token.
 */
public enum AuthMode {
    /**
     * AppRole login. role_id comes from config; the secret_id arrives as a response-wrapped, single-use
     * token in a file that the deploy pipeline writes and the client deletes after reading.
     */
    APPROLE_WRAPPED,

    /**
     * A local Vault Agent authenticates and injects its token ({@code use_auto_auth_token = "force"}).
     * The client sends no token and holds no credential.
     */
    AGENT;

    static AuthMode parse(String value) {
        String v = value.trim().toLowerCase(java.util.Locale.ROOT).replace("-", "").replace("_", "");
        if (v.equals("approlewrapped")) {
            return APPROLE_WRAPPED;
        }
        if (v.equals("agent")) {
            return AGENT;
        }
        throw new IllegalArgumentException("Unknown auth mode '" + value + "'. Use 'approle-wrapped' or 'agent'.");
    }
}
