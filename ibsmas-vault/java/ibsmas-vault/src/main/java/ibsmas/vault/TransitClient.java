// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package ibsmas.vault;

/**
 * Encrypts and decrypts through Vault's Transit engine, so the key never leaves Vault. Vault rotates the key
 * (by hand or with {@code auto_rotate_period}) and keeps older versions for decryption; {@link #rewrap} moves a
 * ciphertext to the newest version without exposing the plaintext. Implementations are thread-safe.
 *
 * <p>Ciphertexts look like {@code vault:v3:<base64>}. The plaintext travels to Vault base64-encoded inside a
 * Java string, which cannot be wiped (the same limitation as {@code SecretKeySpec}).
 */
public interface TransitClient {

    /**
     * Encrypts with the newest version of a Transit key.
     *
     * @param keyName   Transit key name
     * @param plaintext bytes to encrypt; the caller still owns and wipes them
     * @return Vault ciphertext, for example {@code vault:v3:...}
     */
    String encrypt(String keyName, byte[] plaintext);

    /**
     * Decrypts a ciphertext made by any key version Vault still allows ({@code min_decryption_version}).
     *
     * @param keyName    Transit key name
     * @param ciphertext Vault ciphertext
     * @return a new array with the plaintext; the caller wipes it
     */
    byte[] decrypt(String keyName, String ciphertext);

    /**
     * Re-encrypts a ciphertext with the newest key version. Vault does this internally; the plaintext is
     * never returned.
     *
     * @param keyName    Transit key name
     * @param ciphertext Vault ciphertext
     * @return the new ciphertext
     */
    String rewrap(String keyName, String ciphertext);

    /**
     * Returns {@code latest_version} of a Transit key. Needs {@code read} on {@code <transit>/keys/<name>}.
     * Cached for the client's cache TTL; call {@link #invalidateKeyVersion} after a rotation to see it sooner.
     *
     * @param keyName Transit key name
     * @return the newest key version
     */
    int getLatestKeyVersion(String keyName);

    /**
     * Drops the cached {@code latest_version} of a Transit key so the next lookup goes to Vault (for example
     * after a rotation).
     *
     * @param keyName Transit key name
     */
    void invalidateKeyVersion(String keyName);

    /**
     * Rewraps a ciphertext only if an older key version made it. Call it when reading stored data to roll
     * that data forward after a rotation.
     *
     * @param keyName    Transit key name
     * @param ciphertext Vault ciphertext
     * @return the same ciphertext when it is current, otherwise the rewrapped one
     */
    String rewrapIfStale(String keyName, String ciphertext);

    /**
     * Reads the key version from a Vault ciphertext ({@code vault:v3:...} gives 3) without calling Vault.
     *
     * @param ciphertext Vault ciphertext
     * @return the key version
     * @throws VaultException when the text is not a Vault ciphertext; the message never echoes it
     */
    static int ciphertextVersion(String ciphertext) {
        String prefix = "vault:v";
        if (ciphertext != null && ciphertext.startsWith(prefix)) {
            int end = ciphertext.indexOf(':', prefix.length());
            // Digits only (parseInt would accept a sign), no leading zero, and a non-empty payload after the colon.
            boolean digits = end > prefix.length() && end < ciphertext.length() - 1 && ciphertext.charAt(prefix.length()) != '0';
            for (int i = prefix.length(); digits && i < end; i++) {
                char c = ciphertext.charAt(i);
                digits = c >= '0' && c <= '9';
            }
            if (digits) {
                try {
                    return Integer.parseInt(ciphertext.substring(prefix.length(), end));
                } catch (NumberFormatException e) {
                    // Too large for an int; reported below like any other malformed ciphertext.
                }
            }
        }
        // Never echo the input: a caller may have passed plaintext by mistake.
        throw new VaultException("Text is not a Vault Transit ciphertext (expected the vault:v<N>:<base64> format).");
    }
}
