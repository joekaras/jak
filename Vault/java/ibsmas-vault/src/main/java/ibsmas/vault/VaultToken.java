// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package ibsmas.vault;

import ibsmas.vault.internal.Json;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * A Vault token and its lease. {@link #toString()} never prints the token.
 */
final class VaultToken {

    private final String value;
    private final Duration ttl;
    private final boolean renewable;
    private final Instant issuedAt;

    VaultToken(String value, Duration ttl, boolean renewable, Instant issuedAt) {
        this.value = value;
        this.ttl = ttl;
        this.renewable = renewable;
        this.issuedAt = issuedAt;
    }

    static VaultToken fromAuth(Map<String, Object> auth, Instant now) {
        String token = Json.getString(auth, "client_token");
        if (token == null || token.isEmpty()) {
            throw new VaultAuthException("Vault login response did not contain a client token.");
        }
        Object lease = auth.get("lease_duration");
        long seconds = lease instanceof Number ? ((Number) lease).longValue() : 0;
        boolean renewable = Boolean.TRUE.equals(auth.get("renewable"));
        return new VaultToken(token, Duration.ofSeconds(seconds), renewable, now);
    }

    String value() { return value; }

    /** Lease length from the last login or renewal. Zero means the token never expires. */
    Duration ttl() { return ttl; }

    boolean renewable() { return renewable; }

    /** When the current lease started (login or last successful renewal). */
    Instant issuedAt() { return issuedAt; }

    Instant expiresAt() {
        return ttl.isZero() ? Instant.MAX : issuedAt.plus(ttl);
    }

    @Override
    public String toString() {
        return "VaultToken(ttl=" + ttl + ", renewable=" + renewable + ", value=***)";
    }
}
