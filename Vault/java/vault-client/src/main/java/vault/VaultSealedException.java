// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package vault;
/**
 * Vault answered 503: sealed, in maintenance, or a standby that cannot serve the request.
 */
public final class VaultSealedException extends VaultException {
    private static final long serialVersionUID = 1L;

    public VaultSealedException(String message) {
        super(message, null, null);
    }

    public VaultSealedException(String message, Integer statusCode, Throwable cause) {
        super(message, statusCode, cause);
    }
}