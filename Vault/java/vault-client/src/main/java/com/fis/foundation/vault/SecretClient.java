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
     * Removes a path from the cache so the next read goes to Vault (for example after a rotation).
     *
     * @param path secret path under the KV mount
     */
    void invalidate(String path);

    /** Revokes the token (AppRole mode) and clears the cache. Never throws. */
    @Override
    void close();
}
