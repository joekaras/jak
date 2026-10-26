// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package vault.poc.linux;

import vault.poc.PocDaemon;

/**
 * systemd entry point (see {@code service/vault-poc-java.service}). systemd's SIGTERM runs the shutdown
 * hook that revokes the token. Defaults to AppRole with a wrapped secret_id delivered by the pipeline.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws InterruptedException {
        PocDaemon.run("vault-poc-linux");
    }
}
