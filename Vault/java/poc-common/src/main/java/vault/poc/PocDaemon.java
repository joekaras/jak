// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package vault.poc;

import vault.SecretClient;
import vault.VaultConfig;
import vault.VaultException;
import vault.VaultSecretClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Long-running loop shared by the Windows and Linux POCs: run {@link PocCheck} at startup and on an
 * interval, and close the client (revoking the token) when the JVM is asked to stop.
 */
public final class PocDaemon {

    private static final Logger LOG = Logger.getLogger(PocDaemon.class.getName());

    private PocDaemon() {
    }

    public static void run(String name) throws InterruptedException {
        final SecretClient client = new VaultSecretClient(VaultConfig.fromEnvironment());
        final PocCheck.Settings settings = PocCheck.Settings.fromEnvironment();
        final CountDownLatch stop = new CountDownLatch(1);
        final CountDownLatch stopped = new CountDownLatch(1);

        // SIGTERM (systemd stop), Ctrl+C, and WinSW/procrun stop all end up here.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stop.countDown();
            try {
                stopped.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, name + "-shutdown"));

        long intervalSeconds = intervalSeconds();
        try {
            do {
                try {
                    PocCheck.Result result = PocCheck.run(client, settings);
                    LOG.info(name + " Vault check OK: " + result.toJson());
                } catch (VaultException e) {
                    LOG.log(Level.SEVERE, name + " Vault check failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            } while (!stop.await(intervalSeconds, TimeUnit.SECONDS));
        } finally {
            client.close();
            stopped.countDown();
        }
    }

    private static long intervalSeconds() {
        try {
            long v = Long.parseLong(System.getenv("POC_INTERVAL_SECONDS"));
            return v > 0 ? v : 300;
        } catch (NumberFormatException e) {
            return 300;
        }
    }
}
