// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package vault;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void environmentOverridesPropertiesFile() throws Exception {
        Path file = tempDir.resolve("vault.properties");
        Files.write(file, ("vault.address=https://vault-a.test:8200\n"
            + "vault.kvMount=kv\n"
            + "vault.authMode=agent\n"
            + "vault.cacheTtlSeconds=60\n").getBytes(StandardCharsets.ISO_8859_1));
        Map<String, String> env = new HashMap<String, String>();
        env.put("VAULT_ADDR", "http://127.0.0.1:8100");
        env.put("VAULT_NAMESPACE", "team/dev");

        VaultConfig config = VaultConfig.load(file, env);

        assertEquals(URI.create("http://127.0.0.1:8100/"), config.getAddress());
        assertEquals("team/dev", config.getNamespace());
        assertEquals("kv", config.getKvMount());
        assertEquals(AuthMode.AGENT, config.getAuthMode());
        assertEquals(Duration.ofSeconds(60), config.getCacheTtl());
    }

    @Test
    void secretIdInPropertiesIsRefused() {
        Properties p = new Properties();
        p.setProperty("vault.secretId", "x");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> VaultConfig.builder().applyProperties(p));
        assertTrue(ex.getMessage().contains("must not be stored in configuration"));
    }

    @Test
    void secretIdInEnvironmentIsRefused() {
        assertThrows(IllegalStateException.class,
            () -> VaultConfig.builder().applyEnvironment(Collections.singletonMap("VAULT_SECRET_ID", "x")));
    }

    @Test
    void appRoleOverPlainHttpToRemoteHostIsRefused() {
        Map<String, String> env = new HashMap<String, String>();
        env.put("VAULT_ADDR", "http://vault.test:8200");
        env.put("VAULT_ROLE_ID", "role");
        env.put("VAULT_WRAPPED_SECRET_ID_FILE", "/nowhere");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> VaultConfig.load(null, env));
        assertTrue(ex.getMessage().contains("https"));
    }

    @Test
    void agentOnLoopbackHttpIsAllowed() {
        Map<String, String> env = new HashMap<String, String>();
        env.put("VAULT_ADDR", "http://127.0.0.1:8100");
        env.put("VAULT_AUTH_MODE", "agent");

        assertEquals(AuthMode.AGENT, VaultConfig.load(null, env).getAuthMode());
    }

    @Test
    void appRoleRequiresRoleIdAndWrappedFile() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> VaultConfig.load(null, Collections.singletonMap("VAULT_ADDR", "https://vault.test:8200")));
        assertTrue(ex.getMessage().contains("role_id"));
    }

    @Test
    void unknownSettingIsRefused() {
        Properties p = new Properties();
        p.setProperty("vaultUrl", "x");

        assertThrows(IllegalStateException.class, () -> VaultConfig.builder().applyProperties(p));
    }
}
