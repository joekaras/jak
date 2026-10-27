// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package ibsmas.vault;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSocket;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultHttpTest {

    @Test
    void tlsAllowsOnly13And12WithForwardSecrecyAndAead() throws Exception {
        VaultConfig config = VaultConfig.builder().address("https://vault.test:8200").authMode(AuthMode.AGENT).build();
        VaultHttp http = new VaultHttp(config, millis -> { });

        try (SSLSocket socket = (SSLSocket) http.socketFactory().createSocket()) {
            List<String> protocols = Arrays.asList(socket.getEnabledProtocols());
            assertTrue(protocols.contains("TLSv1.2"), protocols.toString());
            for (String p : protocols) {
                assertTrue(p.equals("TLSv1.3") || p.equals("TLSv1.2"), "unexpected protocol " + p);
            }
            // Java 8u261+ and every later JDK have TLS 1.3; the policy must enable it when present.
            if (Arrays.asList(socket.getSupportedProtocols()).contains("TLSv1.3")) {
                assertTrue(protocols.contains("TLSv1.3"), protocols.toString());
            }

            String[] suites = socket.getEnabledCipherSuites();
            assertTrue(suites.length > 0);
            for (String s : suites) {
                assertFalse(s.contains("_CBC_"), s);
                assertTrue(s.startsWith("TLS_AES_") || s.startsWith("TLS_CHACHA20_") || s.startsWith("TLS_ECDHE_"), s);
                assertTrue(s.contains("_GCM_") || s.contains("CHACHA20"), s);
            }
        }
    }

    @Test
    void responseLargerThanTheCapIsRejectedNotBuffered() throws Exception {
        char[] filler = new char[VaultHttp.MAX_RESPONSE_BYTES + 1];
        Arrays.fill(filler, 'x');
        try (StubVaultServer vault = new StubVaultServer()) {
            vault.on("GET", "secret/data/big", 200, "{\"data\":{\"data\":{\"k\":\"" + new String(filler) + "\"}}}");
            VaultConfig config = VaultConfig.builder().address(vault.address()).authMode(AuthMode.AGENT).maxRetries(2).build();
            VaultHttp http = new VaultHttp(config, millis -> { });

            VaultException ex = assertThrows(VaultException.class, () -> http.send("GET", "secret/data/big", null, null, true));
            assertTrue(ex.getMessage().contains("exceeds"), ex.getMessage());
            assertFalse(ex instanceof VaultUnavailableException, "an oversized body must not be retried as a transport error");
            assertTrue(vault.count("GET", "secret/data/big") == 1, "must not retry");
        }
    }
}
