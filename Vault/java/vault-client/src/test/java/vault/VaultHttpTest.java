// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package vault;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSocket;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class VaultHttpTest {

    @Test
    void tlsIsPinnedTo12() throws Exception {
        VaultConfig config = VaultConfig.builder().address("https://vault.test:8200").authMode(AuthMode.AGENT).build();
        VaultHttp http = new VaultHttp(config, millis -> { });

        try (SSLSocket socket = (SSLSocket) http.socketFactory().createSocket()) {
            assertArrayEquals(new String[] {"TLSv1.2"}, socket.getEnabledProtocols());
        }
    }
}
