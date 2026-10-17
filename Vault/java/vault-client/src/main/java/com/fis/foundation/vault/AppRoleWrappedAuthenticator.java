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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AppRole login with a response-wrapped secret_id. The wrapping token file is read once and deleted,
 * the secret_id is unwrapped (which fails if anyone unwrapped it first), and the login happens once.
 */
final class AppRoleWrappedAuthenticator {

    private final String authMount;
    private final String roleId;
    private final Path wrappedSecretIdFile;
    private final AtomicBoolean consumed = new AtomicBoolean(false);

    AppRoleWrappedAuthenticator(String authMount, String roleId, String wrappedSecretIdFile) {
        this.authMount = authMount;
        this.roleId = roleId;
        this.wrappedSecretIdFile = Paths.get(wrappedSecretIdFile);
    }

    /** False once the secret_id has been spent and a new login is impossible. */
    boolean canLogin() {
        return !consumed.get();
    }

    VaultToken login(VaultHttp http, Clock clock) {
        if (consumed.getAndSet(true)) {
            throw new VaultAuthException(
                "The wrapped secret_id was already used. Restart the service with a newly delivered wrapped secret_id, or use Vault Agent mode.");
        }

        String wrappingToken = readAndDelete(wrappedSecretIdFile);
        String secretId = unwrap(http, wrappingToken);

        Map<String, String> body = new LinkedHashMap<String, String>();
        body.put("role_id", roleId);
        body.put("secret_id", secretId);

        Map<String, Object> response;
        try {
            response = http.send("POST", "auth/" + authMount + "/login", null, body, false);
        } catch (VaultException e) {
            if (e.hasStatus(400, 401, 403)) {
                throw new VaultAuthException("AppRole login to '" + authMount + "' was rejected.", e.getStatusCode(), e);
            }
            throw e;
        }

        Map<String, Object> auth = Json.getObject(response, "auth");
        if (auth == null) {
            throw new VaultAuthException("Vault login response did not contain an auth block.");
        }
        return VaultToken.fromAuth(auth, clock.instant());
    }

    private static String unwrap(VaultHttp http, String wrappingToken) {
        Map<String, Object> response;
        try {
            // Empty JSON body keeps the request in fixed-length streaming mode (no silent POST retry).
            response = http.send("POST", "sys/wrapping/unwrap", wrappingToken, Collections.<String, Object>emptyMap(), false);
        } catch (VaultException e) {
            if (e.hasStatus(400, 401, 403)) {
                throw new VaultAuthException(
                    "Unwrap failed: the wrapping token is invalid, expired, or already unwrapped. "
                        + "If this service did not unwrap it, treat it as a possible interception.",
                    e.getStatusCode(), e);
            }
            throw e;
        }

        String secretId = Json.getString(Json.getObject(response, "data"), "secret_id");
        if (secretId == null || secretId.isEmpty()) {
            throw new VaultAuthException("Unwrapped response did not contain a secret_id.");
        }
        return secretId;
    }

    /**
     * Reads the wrapping token and deletes the file whether or not the read succeeds. Accepts either the
     * raw token or the JSON from {@code vault write -wrap-ttl=... -format=json}.
     */
    private static String readAndDelete(Path path) {
        String content;
        try {
            content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).trim();
        } catch (NoSuchFileException e) {
            throw new VaultAuthException(
                "Wrapped secret_id file '" + path + "' was not found. The deploy pipeline must deliver one before the service starts.",
                null, e);
        } catch (IOException e) {
            throw new VaultAuthException("Wrapped secret_id file '" + path + "' could not be read.", null, e);
        } finally {
            tryDelete(path);
        }

        if (content.startsWith("{")) {
            content = extractWrapInfoToken(content);
        }
        if (content.isEmpty()) {
            throw new VaultAuthException("Wrapped secret_id file '" + path + "' was empty.");
        }
        return content;
    }

    private static String extractWrapInfoToken(String json) {
        try {
            String token = Json.getString(Json.getObject(Json.asObject(Json.parse(json)), "wrap_info"), "token");
            if (token != null) {
                return token;
            }
        } catch (IllegalArgumentException e) {
            // Fall through; the message must not echo the file content.
        }
        throw new VaultAuthException("Wrapped secret_id file is JSON but has no wrap_info.token.");
    }

    private static void tryDelete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException | SecurityException e) {
            // The wrapping token is single-use and is spent by the unwrap that follows, so a leftover file is inert.
        }
    }
}
