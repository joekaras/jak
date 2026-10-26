// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package com.fis.foundation.vault;

import com.fis.foundation.vault.internal.Json;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default {@link SecretClient} and {@link TransitClient}. Create one per process (or per web application) and
 * share it.
 */
public final class VaultSecretClient implements SecretClient, TransitClient {

    private final VaultConfig config;
    private final VaultHttp http;
    private final TokenManager tokens;
    private final Clock clock;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<String, CacheEntry>();
    // Keyed "path@vN". A KV version never changes, but it can be deleted, so these entries still expire.
    private final ConcurrentHashMap<String, CacheEntry> versionCache = new ConcurrentHashMap<String, CacheEntry>();
    private final ConcurrentHashMap<String, VersionEntry> transitVersions = new ConcurrentHashMap<String, VersionEntry>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a client. No network call happens until the first read.
     *
     * @param config validated settings, usually from {@link VaultConfig#fromEnvironment()}
     */
    public VaultSecretClient(VaultConfig config) {
        this(config, Clock.systemUTC(), new VaultHttp.Sleeper() {
            @Override
            public void sleep(long millis) throws InterruptedException {
                Thread.sleep(millis);
            }
        });
    }

    VaultSecretClient(VaultConfig config, Clock clock, VaultHttp.Sleeper sleeper) {
        if (config == null) {
            throw new IllegalArgumentException("config is required");
        }
        this.config = config;
        this.clock = clock;
        this.http = new VaultHttp(config, sleeper);
        AppRoleWrappedAuthenticator authenticator = config.getAuthMode() == AuthMode.AGENT
            ? null
            : new AppRoleWrappedAuthenticator(config.getAuthMount(), config.getRoleId(), config.getWrappedSecretIdFile());
        this.tokens = new TokenManager(http, authenticator, clock);
    }

    @Override
    public Map<String, String> getSecret(String path) {
        return latest(path).values;
    }

    @Override
    public Map<String, String> getSecretVersion(String path, int version) {
        throwIfClosed();
        if (version < 1) {
            throw new IllegalArgumentException("version must be 1 or greater");
        }
        String normalized = normalizePath(path);
        return cached(versionCache, normalized + "@v" + version, normalized, version).values;
    }

    @Override
    public int getCurrentVersion(String path) {
        return latest(path).version;
    }

    @Override
    public String getSecret(String path, String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key is required");
        }
        String value = getSecret(path).get(key);
        if (value == null) {
            throw new VaultNotFoundException("Key '" + key + "' not found at '" + path + "'.");
        }
        return value;
    }

    @Override
    public byte[] getKey(String path, String key, int expectedLength) {
        if (expectedLength <= 0) {
            throw new IllegalArgumentException("expectedLength must be positive");
        }
        return decodeKey(path, key, getSecret(path, key), expectedLength);
    }

    @Override
    public byte[] getKey(String path, String key, int expectedLength, int version) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key is required");
        }
        if (expectedLength <= 0) {
            throw new IllegalArgumentException("expectedLength must be positive");
        }
        String encoded = getSecretVersion(path, version).get(key);
        if (encoded == null) {
            throw new VaultNotFoundException("Key '" + key + "' not found at '" + path + "' version " + version + ".");
        }
        return decodeKey(path, key, encoded, expectedLength);
    }

    private static byte[] decodeKey(String path, String key, String encoded, int expectedLength) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException e) {
            throw new VaultException("Key '" + key + "' at '" + path + "' is not valid base64.");
        }
        if (bytes.length != expectedLength) {
            int actual = bytes.length;
            Arrays.fill(bytes, (byte) 0);
            throw new VaultException("Key '" + key + "' at '" + path + "' is " + actual + " bytes; expected " + expectedLength + ".");
        }
        return bytes;
    }

    @Override
    public void invalidate(String path) {
        throwIfClosed();
        String normalized = normalizePath(path);
        cache.remove(normalized);
        String prefix = normalized + "@v";
        for (Iterator<String> it = versionCache.keySet().iterator(); it.hasNext(); ) {
            if (it.next().startsWith(prefix)) {
                it.remove();
            }
        }
    }

    // ---- Transit ---------------------------------------------------------------------------------------

    @Override
    public String encrypt(String keyName, byte[] plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("plaintext is required");
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("plaintext", Base64.getEncoder().encodeToString(plaintext));
        return transitCiphertext(keyName, "encrypt", body);
    }

    @Override
    public byte[] decrypt(String keyName, String ciphertext) {
        final Map<String, Object> body = ciphertextBody(ciphertext);
        final String name = transitKeyName(keyName);
        final String apiPath = escapePath(config.getTransitMount()) + "/decrypt/" + escapePath(name);
        Map<String, Object> response = withRecovery("decrypting with Transit key '" + name + "'",
            token -> http.send("POST", apiPath, token, body, true));
        String encoded = Json.getString(Json.getObject(response, "data"), "plaintext");
        if (encoded == null) {
            throw new VaultException("Vault returned no plaintext for Transit key '" + name + "'.");
        }
        try {
            return Base64.getDecoder().decode(encoded.getBytes(StandardCharsets.US_ASCII));
        } catch (IllegalArgumentException e) {
            throw new VaultException("Vault returned plaintext that is not valid base64 for Transit key '" + name + "'.");
        }
    }

    @Override
    public String rewrap(String keyName, String ciphertext) {
        return transitCiphertext(keyName, "rewrap", ciphertextBody(ciphertext));
    }

    @Override
    public int getLatestKeyVersion(String keyName) {
        throwIfClosed();
        String name = transitKeyName(keyName);
        VersionEntry entry = transitVersions.get(name);
        if (entry != null && entry.expiresAt.isAfter(clock.instant())) {
            return entry.version;
        }
        final String apiPath = escapePath(config.getTransitMount()) + "/keys/" + escapePath(name);
        Map<String, Object> response = withRecovery("reading Transit key '" + name + "'",
            token -> http.send("GET", apiPath, token, null, true));
        Map<String, Object> data = Json.getObject(response, "data");
        Object latest = data == null ? null : data.get("latest_version");
        if (!(latest instanceof Number)) {
            throw new VaultException("Vault returned no latest_version for Transit key '" + name + "'.");
        }
        int version = ((Number) latest).intValue();
        if (!config.getCacheTtl().isZero()) {
            transitVersions.put(name, new VersionEntry(version, clock.instant().plus(config.getCacheTtl())));
        }
        return version;
    }

    @Override
    public void invalidateKeyVersion(String keyName) {
        throwIfClosed();
        transitVersions.remove(transitKeyName(keyName));
    }

    @Override
    public String rewrapIfStale(String keyName, String ciphertext) {
        int version = TransitClient.ciphertextVersion(ciphertext);
        return version < getLatestKeyVersion(keyName) ? rewrap(keyName, ciphertext) : ciphertext;
    }

    @Override
    public void close() {
        if (closed.getAndSet(true)) {
            return;
        }
        tokens.revoke();
        cache.clear();
        versionCache.clear();
        transitVersions.clear();
    }

    private String transitCiphertext(String keyName, String operation, final Map<String, Object> body) {
        throwIfClosed();
        String name = transitKeyName(keyName);
        // Encrypt and rewrap spend nothing, so a retry after a lost response is safe.
        final String apiPath = escapePath(config.getTransitMount()) + "/" + operation + "/" + escapePath(name);
        Map<String, Object> response = withRecovery("using Transit key '" + name + "' to " + operation,
            token -> http.send("POST", apiPath, token, body, true));
        String ciphertext = Json.getString(Json.getObject(response, "data"), "ciphertext");
        if (ciphertext == null) {
            throw new VaultException("Vault returned no ciphertext for Transit key '" + name + "'.");
        }
        return ciphertext;
    }

    private Map<String, Object> ciphertextBody(String ciphertext) {
        throwIfClosed();
        if (ciphertext == null || ciphertext.isEmpty()) {
            throw new IllegalArgumentException("ciphertext is required");
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("ciphertext", ciphertext);
        return body;
    }

    private static String transitKeyName(String keyName) {
        String name = normalizePath(keyName);
        if (name.indexOf('/') >= 0) {
            throw new IllegalArgumentException("Transit key name must be a single path segment");
        }
        return name;
    }

    // ---- KV --------------------------------------------------------------------------------------------

    private CacheEntry latest(String path) {
        throwIfClosed();
        String normalized = normalizePath(path);
        return cached(cache, normalized, normalized, null);
    }

    private CacheEntry cached(Map<String, CacheEntry> map, String cacheKey, final String path, final Integer version) {
        CacheEntry entry = map.get(cacheKey);
        if (entry != null && entry.expiresAt.isAfter(clock.instant())) {
            return entry;
        }
        String description = "reading '" + path + "'" + (version == null ? "" : " version " + version);
        entry = withRecovery(description, token -> read(path, version, token));
        if (!config.getCacheTtl().isZero()) {
            entry = entry.withExpiry(clock.instant().plus(config.getCacheTtl()));
            map.put(cacheKey, entry);
        }
        return entry;
    }

    /** One Vault call that needs a token. */
    private interface Call<T> {
        T run(String token);
    }

    private <T> T withRecovery(String description, Call<T> call) {
        String token = tokens.getToken();
        try {
            return call.run(token);
        } catch (VaultAuthException e) {
            if (!e.hasStatus(401, 403)) {
                throw e;
            }
            // Vault answers 403 both for a dead token and for a policy denial. Ask Vault which one it is.
            if (tokens.isCurrentTokenValid()) {
                throw permissionDenied(description, e);
            }
            tokens.reset();
            token = tokens.getToken();
            try {
                return call.run(token);
            } catch (VaultAuthException retry) {
                if (retry.hasStatus(401, 403)) {
                    // A fresh token was still refused, so the policy is the problem.
                    throw permissionDenied(description, retry);
                }
                throw retry;
            }
        }
    }

    private CacheEntry read(String path, Integer version, String token) {
        String apiPath = escapePath(config.getKvMount()) + "/data/" + escapePath(path)
            + (version == null ? "" : "?version=" + version);
        Map<String, Object> response = http.send("GET", apiPath, token, null, true);
        Map<String, Object> outer = Json.getObject(response, "data");
        Map<String, Object> data = Json.getObject(outer, "data");
        if (data == null) {
            throw new VaultNotFoundException("No secret data at '" + path + "'"
                + (version == null ? "" : " version " + version) + " (deleted or destroyed version?).");
        }

        Map<String, String> values = new LinkedHashMap<String, String>();
        for (Map.Entry<String, Object> e : data.entrySet()) {
            Object v = e.getValue();
            values.put(e.getKey(), v instanceof String ? (String) v : Json.write(v));
        }
        Map<String, Object> metadata = Json.getObject(outer, "metadata");
        Object reported = metadata == null ? null : metadata.get("version");
        int resolved = reported instanceof Number ? ((Number) reported).intValue() : (version == null ? 0 : version);
        return new CacheEntry(Collections.unmodifiableMap(values), resolved, Instant.MIN);
    }

    private static VaultPermissionDeniedException permissionDenied(String description, VaultException cause) {
        return new VaultPermissionDeniedException("Vault policy does not allow " + description + ".", cause.getStatusCode(), cause);
    }

    private static String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("path is required");
        }
        String trimmed = path.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("path is required");
        }
        for (String segment : trimmed.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException("path must be a relative KV path without empty, '.' or '..' segments");
            }
        }
        return trimmed;
    }

    private static String escapePath(String path) {
        StringBuilder sb = new StringBuilder();
        for (String segment : path.split("/", -1)) {
            if (sb.length() > 0) {
                sb.append('/');
            }
            try {
                // URLEncoder is form encoding; turn '+' back into a path-safe space escape.
                sb.append(URLEncoder.encode(segment, "UTF-8").replace("+", "%20"));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
        return sb.toString();
    }

    private void throwIfClosed() {
        if (closed.get()) {
            throw new IllegalStateException("VaultSecretClient is closed");
        }
    }

    private static final class CacheEntry {
        final Map<String, String> values;
        final int version;
        final Instant expiresAt;

        CacheEntry(Map<String, String> values, int version, Instant expiresAt) {
            this.values = values;
            this.version = version;
            this.expiresAt = expiresAt;
        }

        CacheEntry withExpiry(Instant at) {
            return new CacheEntry(values, version, at);
        }
    }

    private static final class VersionEntry {
        final int version;
        final Instant expiresAt;

        VersionEntry(int version, Instant expiresAt) {
            this.version = version;
            this.expiresAt = expiresAt;
        }
    }
}
