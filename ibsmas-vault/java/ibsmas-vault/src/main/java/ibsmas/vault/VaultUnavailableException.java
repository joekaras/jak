// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package ibsmas.vault;
/**
 * Vault could not be reached or kept failing after retries (IO errors, timeouts, 429, 5xx).
 */
public final class VaultUnavailableException extends VaultException {
    private static final long serialVersionUID = 1L;

    public VaultUnavailableException(String message) {
        super(message, null, null);
    }

    public VaultUnavailableException(String message, Integer statusCode, Throwable cause) {
        super(message, statusCode, cause);
    }
}