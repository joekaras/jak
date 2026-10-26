// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package ibsmas.vault.poc.tomcat;

import ibsmas.vault.SecretClient;
import ibsmas.vault.VaultConfig;
import ibsmas.vault.VaultSecretClient;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.nio.file.Paths;

/**
 * Creates one {@link SecretClient} per web application and closes it on undeploy, which revokes the
 * token in AppRole mode.
 *
 * <p>Settings come from {@code VAULT_*} environment variables (set them in Tomcat's {@code setenv.sh} /
 * {@code setenv.bat} or the service definition), optionally on top of a properties file named by the
 * {@code vault.config} context parameter or {@code VAULT_CONFIG_FILE}.
 *
 * <p>Agent mode is the better fit here: every redeploy or restart of the web app would otherwise need a
 * newly delivered wrapped secret_id.
 */
@WebListener
public final class VaultContextListener implements ServletContextListener {

    /** ServletContext attribute that holds the client. */
    public static final String ATTRIBUTE = SecretClient.class.getName();

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        String file = context.getInitParameter("vault.config");
        if (file == null || file.isEmpty()) {
            file = System.getenv("VAULT_CONFIG_FILE");
        }

        VaultConfig config = VaultConfig.load(file == null || file.isEmpty() ? null : Paths.get(file), System.getenv());
        context.setAttribute(ATTRIBUTE, new VaultSecretClient(config));
        context.log("Vault client ready (" + config.getAuthMode() + ", " + config.getAddress() + ")");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Object client = event.getServletContext().getAttribute(ATTRIBUTE);
        if (client instanceof SecretClient) {
            ((SecretClient) client).close();
        }
        event.getServletContext().removeAttribute(ATTRIBUTE);
    }
}
