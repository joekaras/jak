// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package ibsmas.vault;

import java.io.Closeable;
import java.util.Map;

/**
 * Reads credentials and keys from a Vault KV v2 mount. Implementations are thread-safe.
 * Close it on shutdown to revoke the token.
 */
public interface SecretClient extends Closeable {

    /**
     * Reads every key/value pair at a KV path.
     *
     * @param path secret path under the KV mount, for example {@code app/snowflake}
     * @return unmodifiable key/value pairs; non-string values are returned as JSON text
     */
    Map<String, String> getSecret(String path);

    /**
     * Reads one value at a KV path.
     *
     * @param path secret path under the KV mount
     * @param key  key within the secret
     * @return the value
     * @throws VaultNotFoundException when the path or key does not exist
     */
    String getSecret(String path, String key);

    /**
     * Reads a base64-encoded key and checks its length. The caller owns the returned array and should
     * clear it with {@code Arrays.fill(bytes, (byte) 0)} when done.
     *
     * @param path           secret path under the KV mount
     * @param key            key within the secret
     * @param expectedLength required length in bytes; 32 for AES-256
     * @return a new array with the decoded key bytes
     */
    byte[] getKey(String path, String key, int expectedLength);

    /**
     * Reads one KV v2 version of a secret. Use it to decrypt data written under a key that has since been
     * rotated: record {@link #getCurrentVersion} next to the ciphertext, then read that version back here.
     *
     * @param path    secret path under the KV mount
     * @param version KV version, 1 or greater
     * @return unmodifiable key/value pairs of that version
     * @throws VaultNotFoundException when the version does not exist, or was deleted or destroyed
     */
    Map<String, String> getSecretVersion(String path, int version);

    /**
     * Like {@link #getKey(String, String, int)}, but for one KV v2 version.
     *
     * @param path           secret path under the KV mount
     * @param key            key within the secret
     * @param expectedLength required length in bytes
     * @param version        KV version, 1 or greater
     * @return a new array with the decoded key bytes; the caller wipes it
     */
    byte[] getKey(String path, String key, int expectedLength, int version);

    /**
     * Returns the KV v2 version that {@link #getSecret(String)} currently serves for a path. A rotation shows
     * up here once the cache entry expires or the path is invalidated.
     *
     * @param path secret path under the KV mount
     * @return the version number
     */
    int getCurrentVersion(String path);

    /**
     * Removes a path from the cache so the next read goes to Vault (for example after a rotation).
     *
     * @param path secret path under the KV mount
     */
    void invalidate(String path);

    /** Revokes the token (AppRole mode) and clears the cache. Never throws. */
    @Override
    void close();
}
