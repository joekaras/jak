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
import java.util.Arrays;
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

    // ---- Key rotation: KV versions -----------------------------------------------------------------

    @Test
    void oldKeyVersionStaysReadableAfterRotation() throws Exception {
        StubVaultServer vault = happyVault();
        vault.on("GET", "secret/data/app/keys", 200, kvKey(2));
        vault.on("GET", "secret/data/app/keys?version=1", 200, kvKey(1));
        VaultSecretClient client = agent(vault);

        assertEquals(2, client.getCurrentVersion("app/keys"));
        byte[] current = client.getKey("app/keys", "aes256", 32);
        byte[] previous = client.getKey("app/keys", "aes256", 32, 1);

        assertEquals(2, current[0]);
        assertEquals(1, previous[0]);
        assertThrows(IllegalArgumentException.class, () -> client.getSecretVersion("app/keys", 0));
    }

    @Test
    void rotationIsPickedUpWhenCacheExpiresOrOnInvalidate() throws Exception {
        StubVaultServer vault = happyVault();
        final AtomicInteger version = new AtomicInteger(3);
        vault.on("GET", "secret/data/app/keys", r -> new StubVaultServer.Response(200, kvKey(version.get())));
        MutableClock clock = new MutableClock();
        VaultSecretClient client = appRole(vault, wrappedFile(StubVaultServer.WRAPPING_TOKEN), Duration.ofMinutes(5), clock);

        assertEquals(3, client.getCurrentVersion("app/keys"));
        version.set(4);
        assertEquals(3, client.getCurrentVersion("app/keys"), "cached until the TTL runs out");

        clock.advance(Duration.ofMinutes(6));
        assertEquals(4, client.getCurrentVersion("app/keys"));

        version.set(5);
        client.invalidate("app/keys");
        assertEquals(5, client.getCurrentVersion("app/keys"));
    }

    @Test
    void deletedVersionIsNotFound() throws Exception {
        StubVaultServer vault = happyVault();
        vault.on("GET", "secret/data/app/keys?version=1", 404, "{\"errors\":[]}");
        VaultSecretClient client = agent(vault);

        assertThrows(VaultNotFoundException.class, () -> client.getKey("app/keys", "aes256", 32, 1));
    }

    // ---- Key rotation: Transit ---------------------------------------------------------------------

    @Test
    void transitEncryptAndDecryptRoundTrip() throws Exception {
        StubVaultServer vault = happyVault();
        vault.on("POST", "transit/encrypt/orders", 200, "{\"data\":{\"ciphertext\":\"vault:v1:CIPHER\"}}");
        vault.on("POST", "transit/decrypt/orders", 200, "{\"data\":{\"plaintext\":\"aGVsbG8=\"}}");
        VaultSecretClient client = agent(vault);

        String ciphertext = client.encrypt("orders", "hello".getBytes(StandardCharsets.UTF_8));
        byte[] plaintext = client.decrypt("orders", ciphertext);

        assertEquals("vault:v1:CIPHER", ciphertext);
        assertEquals("hello", new String(plaintext, StandardCharsets.UTF_8));
        List<StubVaultServer.Recorded> requests = vault.requests();
        assertTrue(requests.get(0).body.contains("\"plaintext\":\"aGVsbG8=\""));
        assertTrue(requests.get(1).body.contains("\"ciphertext\":\"vault:v1:CIPHER\""));
    }

    @Test
    void rewrapIfStaleOnlyRewrapsOlderVersions() throws Exception {
        StubVaultServer vault = happyVault();
        vault.on("GET", "transit/keys/orders", 200, "{\"data\":{\"latest_version\":2,\"min_decryption_version\":1}}");
        vault.on("POST", "transit/rewrap/orders", 200, "{\"data\":{\"ciphertext\":\"vault:v2:NEW\"}}");
        VaultSecretClient client = agent(vault);

        assertEquals("vault:v2:NEW", client.rewrapIfStale("orders", "vault:v1:OLD"));
        assertEquals("vault:v2:NEW", client.rewrapIfStale("orders", "vault:v2:NEW"));
        assertEquals(1, vault.count("POST", "transit/rewrap/orders"));
    }

    @Test
    void invalidateKeyVersionPicksUpRotationBeforeCacheExpiry() throws Exception {
        StubVaultServer vault = happyVault();
        vault.on("GET", "transit/keys/orders", 200, "{\"data\":{\"latest_version\":2,\"min_decryption_version\":1}}");
        vault.on("POST", "transit/rewrap/orders", 200, "{\"data\":{\"ciphertext\":\"vault:v3:NEW\"}}");
        // agent() turns caching off; this test needs the version cache on.
        VaultConfig config = VaultConfig.builder()
            .address(vault.address())
            .authMode(AuthMode.AGENT)
            .cacheTtl(Duration.ofMinutes(5))
            .maxRetries(0)
            .build();
        VaultSecretClient client = new VaultSecretClient(config, new MutableClock(), millis -> { });

        assertEquals("vault:v2:CUR", client.rewrapIfStale("orders", "vault:v2:CUR"));
        vault.on("GET", "transit/keys/orders", 200, "{\"data\":{\"latest_version\":3,\"min_decryption_version\":1}}");
        assertEquals("vault:v2:CUR", client.rewrapIfStale("orders", "vault:v2:CUR"), "cached version still served");

        client.invalidateKeyVersion("orders");
        assertEquals("vault:v3:NEW", client.rewrapIfStale("orders", "vault:v2:CUR"));
        assertEquals(2, vault.count("GET", "transit/keys/orders"));
        assertThrows(IllegalArgumentException.class, () -> client.invalidateKeyVersion("a/b"));
    }

    @Test
    void transitPolicyDenialIsPermissionDenied() throws Exception {
        StubVaultServer vault = happyVault();
        vault.on("POST", "transit/encrypt/orders", 403, "{\"errors\":[\"permission denied\"]}");
        VaultSecretClient client = agent(vault);

        VaultPermissionDeniedException ex = assertThrows(VaultPermissionDeniedException.class,
            () -> client.encrypt("orders", new byte[] {1}));
        assertTrue(ex.getMessage().contains("orders"));
        assertThrows(IllegalArgumentException.class, () -> client.encrypt("a/b", new byte[] {1}));
    }

    @Test
    void ciphertextVersionParsesPrefixAndNeverEchoesInput() {
        assertEquals(1, TransitClient.ciphertextVersion("vault:v1:abc="));
        assertEquals(12, TransitClient.ciphertextVersion("vault:v12:abc="));
        for (String bad : new String[] {"", "vault:abc", "vault:v:abc", "vault:vX:abc", "plain:v1:abc", "vault:v0:abc", "vault:v1"}) {
            VaultException ex = assertThrows(VaultException.class, () -> TransitClient.ciphertextVersion(bad), bad);
            assertFalse(!bad.isEmpty() && ex.getMessage().contains(bad), "message must not echo the ciphertext");
        }
    }

    private static String kvKey(int version) {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) version);
        return "{\"data\":{\"data\":{\"aes256\":\"" + Base64.getEncoder().encodeToString(key)
            + "\"},\"metadata\":{\"version\":" + version + "}}}";
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
