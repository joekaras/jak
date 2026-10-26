// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package com.fis.foundation.vault;

import com.fis.foundation.vault.internal.Json;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Owns the token lifecycle: login on first use, renew-self before expiry, revoke-self on shutdown.
 * In Agent mode there is no authenticator and no token; the agent injects its own.
 * All methods are synchronized; token operations are rare compared with reads.
 */
final class TokenManager {

    private static final Map<String, Object> EMPTY_BODY = Collections.emptyMap();

    private final VaultHttp http;
    private final AppRoleWrappedAuthenticator authenticator;
    private final Clock clock;
    private VaultToken token;

    TokenManager(VaultHttp http, AppRoleWrappedAuthenticator authenticator, Clock clock) {
        this.http = http;
        this.authenticator = authenticator;
        this.clock = clock;
    }

    private boolean isAgentMode() {
        return authenticator == null;
    }

    /** Returns a usable token, logging in or renewing as needed. Returns null in Agent mode. */
    synchronized String getToken() {
        if (isAgentMode()) {
            return null;
        }
        Instant now = clock.instant();
        if (token == null || !now.isBefore(token.expiresAt())) {
            token = login();
        } else if (token.renewable() && !token.ttl().isZero() && shouldRenew(token, now)) {
            renew();
        }
        return token.value();
    }

    /**
     * Asks Vault whether the current token still works. Used to tell an invalid token (re-login) apart
     * from a policy denial (do not retry), since Vault answers 403 for both.
     */
    synchronized boolean isCurrentTokenValid() {
        String current = null;
        if (!isAgentMode()) {
            if (token == null) {
                return false;
            }
            current = token.value();
        }
        try {
            http.send("GET", "auth/token/lookup-self", current, null, true);
            return true;
        } catch (VaultAuthException e) {
            return false;
        }
    }

    /**
     * Drops the current token and logs in again. Throws {@link VaultAuthException} when the credential was
     * single-use and is spent. In Agent mode this does nothing; the agent re-authenticates.
     */
    synchronized void reset() {
        if (isAgentMode()) {
            return;
        }
        token = null;
        token = login();
    }

    /** Best-effort revoke-self so the token does not outlive the process. Never revokes the agent's token. */
    synchronized void revoke() {
        if (isAgentMode() || token == null) {
            return;
        }
        try {
            http.send("POST", "auth/token/revoke-self", token.value(), EMPTY_BODY, false);
        } catch (VaultException e) {
            // Shutdown must not fail because Vault is unreachable; the token still expires at its TTL.
        }
        token = null;
    }

    /**
     * Renew once two thirds of the current lease has passed. Mirrors the C# TokenManager.ShouldRenew;
     * keep the two in step.
     */
    static boolean shouldRenew(VaultToken token, Instant now) {
        Duration elapsed = Duration.between(token.issuedAt(), now);
        return elapsed.multipliedBy(3).compareTo(token.ttl().multipliedBy(2)) >= 0;
    }

    private VaultToken login() {
        if (!authenticator.canLogin()) {
            throw new VaultAuthException(
                "The Vault token expired or was rejected and the single-use secret_id is already spent. "
                    + "Restart the service with a newly delivered wrapped secret_id, ask Vault admins for a periodic token (token_period), or use Vault Agent mode.");
        }
        return authenticator.login(http, clock);
    }

    private void renew() {
        VaultToken current = token;
        try {
            Map<String, Object> auth = Json.getObject(http.send("POST", "auth/token/renew-self", current.value(), EMPTY_BODY, true), "auth");
            if (auth != null) {
                token = VaultToken.fromAuth(auth, clock.instant());
            }
        } catch (VaultAuthException e) {
            // The token can no longer be renewed (revoked, or past its max TTL). Log in again if possible.
            token = null;
            token = login();
        } catch (VaultUnavailableException | VaultSealedException e) {
            // Vault is down; the current token is still good until it expires, so keep using it.
            if (!clock.instant().isBefore(current.expiresAt())) {
                throw e;
            }
        }
    }
}
