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

import com.fis.foundation.vault.internal.Json;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default {@link SecretClient}. Create one per process (or per web application) and share it.
 */
public final class VaultSecretClient implements SecretClient {

    private final VaultConfig config;
    private final VaultHttp http;
    private final TokenManager tokens;
    private final Clock clock;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<String, CacheEntry>();
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
        throwIfClosed();
        String normalized = normalizePath(path);

        CacheEntry entry = cache.get(normalized);
        if (entry != null && entry.expiresAt.isAfter(clock.instant())) {
            return entry.values;
        }

        Map<String, String> values = readWithRecovery(normalized);
        if (!config.getCacheTtl().isZero()) {
            cache.put(normalized, new CacheEntry(values, clock.instant().plus(config.getCacheTtl())));
        }
        return values;
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
        String encoded = getSecret(path, key);
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
        cache.remove(normalizePath(path));
    }

    @Override
    public void close() {
        if (closed.getAndSet(true)) {
            return;
        }
        tokens.revoke();
        cache.clear();
    }

    private Map<String, String> readWithRecovery(String path) {
        String token = tokens.getToken();
        try {
            return read(path, token);
        } catch (VaultAuthException e) {
            if (!e.hasStatus(401, 403)) {
                throw e;
            }
            // Vault answers 403 both for a dead token and for a policy denial. Ask Vault which one it is.
            if (tokens.isCurrentTokenValid()) {
                throw permissionDenied(path, e);
            }
            tokens.reset();
            token = tokens.getToken();
            try {
                return read(path, token);
            } catch (VaultAuthException retry) {
                if (retry.hasStatus(401, 403)) {
                    // A fresh token was still refused, so the policy is the problem.
                    throw permissionDenied(path, retry);
                }
                throw retry;
            }
        }
    }

    private Map<String, String> read(String path, String token) {
        String apiPath = escapePath(config.getKvMount()) + "/data/" + escapePath(path);
        Map<String, Object> response = http.send("GET", apiPath, token, null, true);
        Map<String, Object> data = Json.getObject(Json.getObject(response, "data"), "data");
        if (data == null) {
            throw new VaultNotFoundException("No secret data at '" + path + "' (deleted or destroyed version?).");
        }

        Map<String, String> values = new LinkedHashMap<String, String>();
        for (Map.Entry<String, Object> e : data.entrySet()) {
            Object v = e.getValue();
            values.put(e.getKey(), v instanceof String ? (String) v : Json.write(v));
        }
        return Collections.unmodifiableMap(values);
    }

    private static VaultPermissionDeniedException permissionDenied(String path, VaultException cause) {
        return new VaultPermissionDeniedException("Vault policy does not allow reading '" + path + "'.", cause.getStatusCode(), cause);
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
        final Instant expiresAt;

        CacheEntry(Map<String, String> values, Instant expiresAt) {
            this.values = values;
            this.expiresAt = expiresAt;
        }
    }
}
