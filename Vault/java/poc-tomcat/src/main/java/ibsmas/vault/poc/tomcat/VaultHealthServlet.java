// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package ibsmas.vault.poc.tomcat;

import ibsmas.vault.SecretClient;
import ibsmas.vault.VaultException;
import ibsmas.vault.internal.Json;
import ibsmas.vault.poc.PocCheck;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code GET /vault/health}: runs the POC check and returns key names and a success flag, never values.
 */
@WebServlet("/vault/health")
public final class VaultHealthServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecretClient client = (SecretClient) getServletContext().getAttribute(VaultContextListener.ATTRIBUTE);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");

        try {
            PocCheck.Result result = PocCheck.run(client, PocCheck.Settings.fromEnvironment());
            response.getWriter().write(result.toJson());
        } catch (VaultException e) {
            Map<String, Object> problem = new LinkedHashMap<String, Object>();
            problem.put("title", e.getClass().getSimpleName());
            problem.put("detail", e.getMessage());
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.getWriter().write(Json.write(problem));
        }
    }
}
