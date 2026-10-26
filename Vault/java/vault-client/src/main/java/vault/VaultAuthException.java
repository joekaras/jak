// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package vault;
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