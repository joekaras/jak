// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package vault;
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