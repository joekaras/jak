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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultSecretClientTest {

    @TempDir
    Path tempDir;

    private final List<AutoCloseable> toClose = new ArrayList<AutoCloseable>();

    @AfterEach
    void tearDown() throws Exception {
        for (AutoCloseable c : toClose) {
            c.close();
        }
    }

    // ---- AppRole with wrapped secret_id ------------------------------------------------------------

    @Test
    void appRoleUnwrapsLogsInAndReadsSecret() throws Exception {
        StubVaultServer vault = happyVault();
        Path file = wrappedFile(StubVaultServer.WRAPPING_TOKEN);
        VaultSecretClient client = appRole(vault, file, Duration.ofMinutes(5), new MutableClock());

        Map<String, String> values = client.getSecret("app/db");

        assertEquals("svc_app", values.get("username"));
        assertEquals("443", values.get("port"));
        assertFalse(Files.exists(file), "wrapped secret_id file must be deleted after reading");

        List<StubVaultServer.Recorded> requests = vault.requests();
        assertEquals("sys/wrapping/unwrap", requests.get(0).path);
        assertEquals("auth/approle/login", requests.get(1).path);
        assertEquals("secret/data/app/db", requests.get(2).path);
        assertEquals(StubVaultServer.WRAPPING_TOKEN, requests.get(0).token);
        assertNull(requests.get(1).token);
        assertTrue(requests.get(1).body.contains("\"role_id\":\"role-fixture\""));
        assertTrue(requests.get(1).body.contains("\"secret_id\":\"" + StubVaultServer.SECRET_ID + "\""));
        assertEquals(StubVaultServer.CLIENT_TOKEN, requests.get(2).token);
        for (StubVaultServer.Recorded r : requests) {
            assertEquals("team/dev", r.namespace);
        }
    }

    @Test
    void appRoleAcceptsWrapInfoJsonFromVaultCli() throws Exception {
        StubVaultServer vault = happyVault();
        Path file = wrappedFile("{\"wrap_info\":{\"token\":\"" + StubVaultServer.WRAPPING_TOKEN + "\",\"ttl\":300}}");
        VaultSecretClient client = appRole(vault, file, Duration.ofMinutes(5), new MutableClock());

        assertEquals("svc_app", client.getSecret("app/db", "username"));
    }

    @Test
    void missingWrappedFileFailsWithoutCallingVault() throws Exception {
        StubVaultServer vault = happyVault();
        VaultSecretClient client = appRole(vault, tempDir.resolve("absent"), Duration.ofMinutes(5), new MutableClock());

        VaultAuthException ex = assertThrows(VaultAuthException.class, () -> client.getSecret("app/db"));

        assertTrue(ex.getMessage().contains("was not found"));
        assertTrue(vault.requests().isEmpty());
    }

    @Test
    void rejectedUnwrapReportsPossibleInterceptionAndDeletesFile() throws Exception {
        StubVaultServer vault = happyVault();
        Path file = wrappedFile("hvs.SOMEONE-ELSES-TOKEN");
        VaultSecretClient client = appRole(vault, file, Duration.ofMinutes(5), new MutableClock());

        VaultAuthException ex = assertThrows(VaultAuthException.class, () -> client.getSecret("app/db"));

        assertTrue(ex.getMessage().contains("interception"));
        assertFalse(Files.exists(file));
    }

    @Test
    void appRoleLogsInOnlyOnceAcrossReads() throws Exception {
        StubVaultServer vault = happyVault();
        VaultSecretClient client = appRole(vault, wrappedFile(StubVaultServer.WRAPPING_TOKEN), Duration.ZERO, new MutableClock());

        client.getSecret("app/db");
        client.getSecret("app/db");

        assertEquals(1, vault.count("POST", "auth/approle/login"));
        assertEquals(2, vault.count("GET", "secret/data/app/db"));
    }

    @Test
    void loginIsNeverRetriedBecauseSecretIdIsSingleUse() throws Exception {
        StubVaultServer vault = happyVault().on("POST", "auth/approle/login", 500, "{}");
        VaultSecretClient client = appRole(vault, wrappedFile(StubVaultServer.WRAPPING_TOKEN), Duration.ofMinutes(5), new MutableClock());

        assertThrows(VaultUnavailableException.class, () -> client.getSecret("app/db"));
        assertEquals(1, vault.count("POST", "auth/approle/login"));
    }

    // ---- 401/403 handling ----------------------------------------------------------------------------

    @Test
    void policyDenialWithValidTokenIsPermissionDeniedWithoutRelogin() throws Exception {
        StubVaultServer vault = happyVault().on("GET", "secret/data/app/db", 403, "{\"errors\":[\"permission denied\"]}");
        VaultSecretClient client = appRole(vault, wrappedFile(StubVaultServer.WRAPPING_TOKEN), Duration.ofMinutes(5), new MutableClock());

        VaultPermissionDeniedException ex = assertThrows(VaultPermissionDeniedException.class, () -> client.getSecret("app/db"));

        assertEquals(Integer.valueOf(403), ex.getStatusCode());
        assertEquals(1, vault.count("POST", "auth/approle/login"));
        assertEquals(1, vault.count("GET", "auth/token/lookup-self"));
    }

    @Test
    void deadTokenInAppRoleModeFailsBecauseSecretIdIsSpent() throws Exception {
        StubVaultServer vault = happyVault()
            .on("GET", "secret/data/app/db", 403, "{}")
            .on("GET", "auth/token/lookup-self", 403, "{}");
        VaultSecretClient client = appRole(vault, wrappedFile(StubVaultServer.WRAPPING_TOKEN), Duration.ofMinutes(5), new MutableClock());

        VaultAuthException ex = assertThrows(VaultAuthException.class, () -> client.getSecret("app/db"));

        assertTrue(ex.getMessage().contains("already spent"));
    }

    @Test
    void agentModeSendsNoTokenAndRetriesOnceAfterAgentTokenFailure() throws Exception {
        final AtomicInteger reads = new AtomicInteger();
        StubVaultServer vault = happyVault()
            .on("GET", "auth/token/lookup-self", 403, "{}")
            .on("GET", "secret/data/app/db", r -> reads.incrementAndGet() == 1
                ? new StubVaultServer.Response(403, "{}")
                : new StubVaultServer.Response(200, "{\"data\":{\"data\":{\"username\":\"svc_app\"}}}"));
        VaultSecretClient client = agent(vault);

        assertEquals("svc_app", client.getSecret("app/db", "username"));
        assertEquals(2, reads.get());
        for (StubVaultServer.Recorded r : vault.requests()) {
            assertNull(r.token);
        }
    }

    @Test
    void agentModePolicyDenialIsPermissionDenied() throws Exception {
        StubVaultServer vault = happyVault().on("GET", "secret/data/app/db", 403, "{}");
        VaultSecretClient client = agent(vault);

        assertThrows(VaultPermissionDeniedException.class, () -> client.getSecret("app/db"));
        assertEquals(1, vault.count("GET", "secret/data/app/db"));
    }

    // ---- Other status codes and retries --------------------------------------------------------------

    @Test
    void missingPathIsNotFound() throws Exception {
        VaultSecretClient client = appRole(happyVault(), wrappedFile(StubVaultServer.WRAPPING_TOKEN), Duration.ofMinutes(5), new MutableClock());

        VaultNotFoundException ex = assertThrows(VaultNotFoundException.class, () -> client.getSecret("app/missing"));
        assertEquals(Integer.valueOf(404), ex.getStatusCode());
    }

    @Test
    void sealedVaultIsRetriedThenReported() throws Exception {
        StubVaultServer vault = happyVault().on("GET", "secret/data/app/db", 503, "{}");
        VaultSecretClient client = appRole(vault, wrappedFile(StubVaultServer.WRAPPING_TOKEN), Duration.ofMinutes(5), new MutableClock());

        assertThrows(VaultSealedException.class, () -> client.getSecret("app/db"));
        assertEquals(3, vault.count("GET", "secret/data/app/db")); // first try + maxRetries (2)
    }

    @Test
    void transient500IsRetriedAndSucceeds() throws Exception {
        final AtomicInteger reads = new AtomicInteger();
        StubVaultServer vault = happyVault().on("GET", "secret/data/app/db", r -> reads.incrementAndGet() == 1
            ? new StubVaultServer.Response(500, "{}")
            : new StubVaultServer.Response(200, "{\"data\":{\"data\":{\"username\":\"svc_app\"}}}"));
        VaultSecretClient client = appRole(vault, wrappedFile(StubVaultServer.WRAPPING_TOKEN), Duration.ofMinutes(5), new MutableClock());

        Map<String, String> values = client.getSecret("app/db");

        assertEquals("svc_app", values.get("username"));
        assertEquals(2, reads.get());
    }

    // ---- Cache and keys ----------------------------------------------------------------------------

    @Test
    void cacheServesRepeatReadsUntilInvalidatedOrExpired() throws Exception {
        StubVaultServer vault = happyVault();
        MutableClock clock = new MutableClock();
        VaultSecretClient client = appRole(vault, wrappedFile(StubVaultServer.WRAPPING_TOKEN), Duration.ofMinutes(5), clock);

        client.getSecret("app/db");
        client.getSecret("/app/db/");
        assertEquals(1, vault.count("GET", "secret/data/app/db"));

        client.invalidate("app/db");
        client.getSecret("app/db");
        assertEquals(2, vault.count("GET", "secret/data/app/db"));

        clock.advance(Duration.ofMinutes(6));
        client.getSecret("app/db");
        assertEquals(3, vault.count("GET", "secret/data/app/db"));
    }

    @Test
    void getKeyDecodesAndChecksLength() throws Exception {
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) i;
        }
        StubVaultServer vault = happyVault().on("GET", "secret/data/app/keys", 200,
            "{\"data\":{\"data\":{\"aes256\":\"" + Base64.getEncoder().encodeToString(key)
                + "\",\"short\":\"" + Base64.getEncoder().encodeToString(new byte[16])
                + "\",\"junk\":\"not base64!\"}}}");
        VaultSecretClient client = appRole(vault, wrappedFile(StubVaultServer.WRAPPING_TOKEN), Duration.ofMinutes(5), new MutableClock());

        assertArrayEquals(key, client.getKey("app/keys", "aes256", 32));

        VaultException length = assertThrows(VaultException.class, () -> client.getKey("app/keys", "short", 32));
        assertTrue(length.getMessage().contains("16 bytes; expected 32"));

        VaultException format = assertThrows(VaultException.class, () -> client.getKey("app/keys", "junk", 32));
        assertTrue(format.getMessage().contains("not valid base64"));
    }

    @Test
    void missingKeyIsNotFound() throws Exception {
        VaultSecretClient client = appRole(happyVault(), wrappedFile(StubVaultServer.WRAPPING_TOKEN), Duration.ofMinutes(5), new MutableClock());

        VaultNotFoundException ex = assertThrows(VaultNotFoundException.class, () -> client.getSecret("app/db", "nope"));

        assertTrue(ex.getMessage().contains("'nope'"));
    }

    // ---- No secret material in errors -----------------------------------------------------------

    @Test
    void exceptionsNeverContainTokensOrSecretValues() throws Exception {
        List<Throwable> failures = new ArrayList<Throwable>();

        StubVaultServer denied = happyVault().on("GET", "secret/data/app/db", 403, "{}");
        VaultSecretClient deniedClient = appRole(denied, wrappedFile(StubVaultServer.WRAPPING_TOKEN), Duration.ofMinutes(5), new MutableClock());
        failures.add(assertThrows(VaultPermissionDeniedException.class, () -> deniedClient.getSecret("app/db")));

        VaultSecretClient okClient = appRole(happyVault(), wrappedFile(StubVaultServer.WRAPPING_TOKEN), Duration.ofMinutes(5), new MutableClock());
        failures.add(assertThrows(VaultNotFoundException.class, () -> okClient.getSecret("app/db", "nope")));

        VaultSecretClient badUnwrap = appRole(happyVault(), wrappedFile("{\"wrap_info\":{\"token\":\"hvs.LEAK-CANARY\"}}"), Duration.ofMinutes(5), new MutableClock());
        failures.add(assertThrows(VaultAuthException.class, () -> badUnwrap.getSecret("app/db")));

        for (Throwable t : failures) {
            String text = describe(t);
            assertFalse(text.contains(StubVaultServer.CLIENT_TOKEN), text);
            assertFalse(text.contains(StubVaultServer.SECRET_ID), text);
            assertFalse(text.contains(StubVaultServer.SECRET_VALUE), text);
            assertFalse(text.contains("LEAK-CANARY"), text);
        }
    }

    // ---- Token lifecycle -----------------------------------------------------------------------------

    @Test
    void closeRevokesTokenInAppRoleMode() throws Exception {
        StubVaultServer vault = happyVault();
        VaultSecretClient client = appRole(vault, wrappedFile(StubVaultServer.WRAPPING_TOKEN), Duration.ofMinutes(5), new MutableClock());
        client.getSecret("app/db");

        client.close();

        assertEquals(1, vault.count("POST", "auth/token/revoke-self"));
        assertThrows(IllegalStateException.class, () -> client.getSecret("app/db"));
    }

    @Test
    void closeNeverRevokesTheAgentsToken() throws Exception {
        StubVaultServer vault = happyVault();
        VaultSecretClient client = agent(vault);
        client.getSecret("app/db");

        client.close();

        assertEquals(0, vault.count("POST", "auth/token/revoke-self"));
    }

    @Test
    void tokenIsRenewedCloseToExpiryButNotWhenFresh() throws Exception {
        StubVaultServer vault = happyVault();
        MutableClock clock = new MutableClock();
        VaultSecretClient client = appRole(vault, wrappedFile(StubVaultServer.WRAPPING_TOKEN), Duration.ZERO, clock);

        client.getSecret("app/db");
        clock.advance(Duration.ofMinutes(1));
        client.getSecret("app/db");
        assertEquals(0, vault.count("POST", "auth/token/renew-self"));

        clock.advance(Duration.ofSeconds(3440)); // 100 s left of a 3600 s lease
        client.getSecret("app/db");
        assertEquals(1, vault.count("POST", "auth/token/renew-self"));
        assertEquals(1, vault.count("POST", "auth/approle/login"));
    }

    @Test
    void badPathsAreRejected() throws Exception {
        StubVaultServer vault = happyVault();
        VaultSecretClient client = appRole(vault, wrappedFile(StubVaultServer.WRAPPING_TOKEN), Duration.ofMinutes(5), new MutableClock());

        for (String bad : new String[] {"", "../other", "app//db", "app/./db"}) {
            assertThrows(IllegalArgumentException.class, () -> client.getSecret(bad), bad);
        }
        assertTrue(vault.requests().isEmpty());
    }

    // ---- Helpers -----------------------------------------------------------------------------------

    private StubVaultServer happyVault() throws IOException {
        StubVaultServer vault = StubVaultServer.withHappyPath(3600);
        toClose.add(vault);
        return vault;
    }

    private Path wrappedFile(String content) throws IOException {
        Path file = Files.createTempFile(tempDir, "wrapped", ".token");
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private VaultSecretClient appRole(StubVaultServer vault, Path wrappedFile, Duration cacheTtl, Clock clock) {
        VaultConfig config = VaultConfig.builder()
            .address(vault.address())
            .namespace("team/dev")
            .kvMount("secret")
            .authMode(AuthMode.APPROLE_WRAPPED)
            .roleId("role-fixture")
            .wrappedSecretIdFile(wrappedFile.toString())
            .cacheTtl(cacheTtl)
            .maxRetries(2)
            .retryBaseDelay(Duration.ofMillis(1))
            .build();
        return new VaultSecretClient(config, clock, millis -> { });
    }

    private VaultSecretClient agent(StubVaultServer vault) {
        VaultConfig config = VaultConfig.builder()
            .address(vault.address())
            .kvMount("secret")
            .authMode(AuthMode.AGENT)
            .cacheTtl(Duration.ZERO)
            .maxRetries(0)
            .build();
        return new VaultSecretClient(config, new MutableClock(), millis -> { });
    }

    private static String describe(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause()) {
            sb.append(c.toString()).append('\n');
        }
        return sb.toString();
    }

    /** A clock the test moves by hand. */
    static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-15T12:00:00Z");

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
