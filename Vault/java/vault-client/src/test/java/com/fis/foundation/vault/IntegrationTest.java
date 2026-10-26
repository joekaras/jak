// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package com.fis.foundation.vault;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
