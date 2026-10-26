// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package com.fis.foundation.vault;

/**
 * Base type for Vault errors. Messages carry the request path and status, never tokens or secret values.
 */
public class VaultException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Integer statusCode;

    public VaultException(String message) {
        this(message, null, null);
    }

    public VaultException(String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /** The HTTP status Vault returned, or null for transport and client-side errors. */
    public Integer getStatusCode() {
        return statusCode;
    }

    boolean hasStatus(int... codes) {
        if (statusCode == null) {
            return false;
        }
        for (int code : codes) {
            if (statusCode == code) {
                return true;
            }
        }
        return false;
    }
}
