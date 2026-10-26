// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package vault.poc.windows;

import vault.poc.PocDaemon;

/**
 * Windows service entry point. Run under WinSW (see {@code service/vault-poc.xml}); WinSW stops the JVM
 * with Ctrl+C semantics, which runs the shutdown hook that revokes the token.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws InterruptedException {
        PocDaemon.run("vault-poc-windows");
    }
}
