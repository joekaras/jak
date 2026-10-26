// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package ibsmas.vault;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs against the shared dev Vault only when the environment is set up. Values are never printed.
 */
@Tag("integration")
class IntegrationTest {

    /**
     * Consumes the wrapped secret_id file, so each run needs a new one:
     * {@code vault write -wrap-ttl=5m -format=json -f auth/approle/role/<role>/secret-id > file}.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "VAULT_WRAPPED_SECRET_ID_FILE", matches = ".+")
    void appRoleWrappedReadsTestSecret() {
        VaultConfig config = VaultConfig.load(null, System.getenv());
        try (SecretClient client = new VaultSecretClient(config)) {
            assertFalse(client.getSecret(System.getenv("VAULT_TEST_PATH")).isEmpty());
        }
    }

    /** Agent mode against a Vault Agent running locally with auto-auth to the shared dev Vault. */
    @Test
    @EnabledIfEnvironmentVariable(named = "VAULT_AGENT_ADDR", matches = ".+")
    void agentReadsTestSecret() {
        Map<String, String> env = new HashMap<String, String>(System.getenv());
        env.put("VAULT_ADDR", System.getenv("VAULT_AGENT_ADDR"));
        env.put("VAULT_AUTH_MODE", "agent");
        VaultConfig config = VaultConfig.load(null, env);
        try (SecretClient client = new VaultSecretClient(config)) {
            assertFalse(client.getSecret(System.getenv("VAULT_TEST_PATH")).isEmpty());
        }
    }

    /**
     * Transit through the agent: encrypt, decrypt, and confirm a current ciphertext is not rewrapped. Needs
     * update on encrypt/decrypt/rewrap and read on keys for {@code VAULT_TRANSIT_TEST_KEY}.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "VAULT_AGENT_ADDR", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "VAULT_TRANSIT_TEST_KEY", matches = ".+")
    void agentTransitRoundTrip() {
        String key = System.getenv("VAULT_TRANSIT_TEST_KEY");
        byte[] plaintext = randomBytes();
        try (VaultSecretClient client = new VaultSecretClient(agentConfig())) {
            String ciphertext = client.encrypt(key, plaintext);
            assertEquals(client.getLatestKeyVersion(key), TransitClient.ciphertextVersion(ciphertext));
            assertEquals(ciphertext, client.rewrapIfStale(key, ciphertext), "a current ciphertext must not be rewrapped");

            byte[] decrypted = client.decrypt(key, ciphertext);
            // assertArrayEquals would print the bytes on failure.
            assertTrue(Arrays.equals(plaintext, decrypted), "decrypt must return the encrypted bytes");
            Arrays.fill(decrypted, (byte) 0);
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    /**
     * Rotates the Transit key, then checks that rewrapIfStale keeps serving the cached version until
     * invalidateKeyVersion, and rewraps after it. Rotation is permanent, so this runs only when
     * {@code VAULT_TRANSIT_ROTATE=true}, and the policy must also allow update on {@code keys/<key>/rotate}.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "VAULT_AGENT_ADDR", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "VAULT_TRANSIT_TEST_KEY", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "VAULT_TRANSIT_ROTATE", matches = "true")
    void agentTransitRotationIsPickedUpAfterInvalidate() {
        String key = System.getenv("VAULT_TRANSIT_TEST_KEY");
        byte[] plaintext = randomBytes();
        VaultConfig config = agentConfig();
        try (VaultSecretClient client = new VaultSecretClient(config)) {
            String ciphertext = client.encrypt(key, plaintext);
            int before = client.getLatestKeyVersion(key);

            // The agent injects its token, so none is passed here.
            new VaultHttp(config, millis -> { }).send("POST",
                config.getTransitMount() + "/keys/" + key + "/rotate", null, Collections.<String, Object>emptyMap(), false);

            assertEquals(ciphertext, client.rewrapIfStale(key, ciphertext), "cached version is served until invalidated");
            client.invalidateKeyVersion(key);
            String rewrapped = client.rewrapIfStale(key, ciphertext);

            assertNotEquals(ciphertext, rewrapped);
            assertEquals(before + 1, TransitClient.ciphertextVersion(rewrapped));
            byte[] decrypted = client.decrypt(key, rewrapped);
            assertTrue(Arrays.equals(plaintext, decrypted), "the rewrapped ciphertext must decrypt to the same bytes");
            Arrays.fill(decrypted, (byte) 0);
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    private static VaultConfig agentConfig() {
        Map<String, String> env = new HashMap<String, String>(System.getenv());
        env.put("VAULT_ADDR", System.getenv("VAULT_AGENT_ADDR"));
        env.put("VAULT_AUTH_MODE", "agent");
        return VaultConfig.load(null, env);
    }

    private static byte[] randomBytes() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }
}
