//Copyright Notice!
//This document is protected under the trade secret and copyright
//laws as the property of Fidelity National Information Services, Inc.
//Copying, reproduction or distribution should be limited and only to
//employees with a "need to know" to do their job.
//Any disclosure of this document to third parties is strictly prohibited.

//copyright 2026 Fidelity National Information Services.
//All rights reserved worldwide.

// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package com.fis.foundation.vault;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fake Vault on 127.0.0.1 using the JDK's built-in HTTP server. Routes are "METHOD path-under-v1";
 * unmatched requests get 404. All values here are test fixtures, not real credentials.
 */
final class StubVaultServer implements AutoCloseable {

    static final String WRAPPING_TOKEN = "hvs.WRAPPING-FIXTURE";
    static final String SECRET_ID = "SECRET-ID-FIXTURE";
    static final String CLIENT_TOKEN = "hvs.CLIENT-FIXTURE";
    static final String SECRET_VALUE = "PASSWORD-FIXTURE";

    /** A canned response. */
    static final class Response {
        final int status;
        final String body;

        Response(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    /** One recorded request. */
    static final class Recorded {
        final String method;
        final String path;
        final String token;
        final String namespace;
        final String body;

        Recorded(String method, String path, String token, String namespace, String body) {
            this.method = method;
            this.path = path;
            this.token = token;
            this.namespace = namespace;
            this.body = body;
        }
    }

    interface Handler {
        Response handle(Recorded request);
    }

    private final HttpServer server;
    private final Map<String, Handler> routes = new ConcurrentHashMap<String, Handler>();
    private final List<Recorded> requests = new ArrayList<Recorded>();

    StubVaultServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::dispatch);
        server.start();
    }

    /** Routes for a working AppRole unwrap + login and a KV secret at app/db. */
    static StubVaultServer withHappyPath(final int leaseSeconds) throws IOException {
        StubVaultServer s = new StubVaultServer();
        s.on("POST", "sys/wrapping/unwrap", r -> WRAPPING_TOKEN.equals(r.token)
            ? new Response(200, "{\"data\":{\"secret_id\":\"" + SECRET_ID + "\"}}")
            : new Response(400, "{\"errors\":[\"wrapping token is not valid or does not exist\"]}"));
        s.on("POST", "auth/approle/login", r -> new Response(200,
            "{\"auth\":{\"client_token\":\"" + CLIENT_TOKEN + "\",\"lease_duration\":" + leaseSeconds + ",\"renewable\":true}}"));
        s.on("GET", "secret/data/app/db", 200,
            "{\"data\":{\"data\":{\"username\":\"svc_app\",\"password\":\"" + SECRET_VALUE + "\",\"port\":443},\"metadata\":{\"version\":3}}}");
        s.on("GET", "auth/token/lookup-self", 200, "{\"data\":{}}");
        s.on("POST", "auth/token/revoke-self", 204, "");
        s.on("POST", "auth/token/renew-self", r -> new Response(200,
            "{\"auth\":{\"client_token\":\"" + CLIENT_TOKEN + "\",\"lease_duration\":" + leaseSeconds + ",\"renewable\":true}}"));
        return s;
    }

    String address() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    StubVaultServer on(String method, String path, Handler handler) {
        routes.put(method + " " + path, handler);
        return this;
    }

    StubVaultServer on(String method, String path, final int status, final String body) {
        return on(method, path, r -> new Response(status, body));
    }

    synchronized List<Recorded> requests() {
        return new ArrayList<Recorded>(requests);
    }

    synchronized int count(String method, String path) {
        int n = 0;
        for (Recorded r : requests) {
            if (r.method.equals(method) && r.path.equals(path)) {
                n++;
            }
        }
        return n;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void dispatch(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/v1/")) {
                path = path.substring(4);
            }
            Recorded recorded = new Recorded(
                exchange.getRequestMethod(),
                path,
                exchange.getRequestHeaders().getFirst("X-Vault-Token"),
                exchange.getRequestHeaders().getFirst("X-Vault-Namespace"),
                readBody(exchange.getRequestBody()));
            synchronized (this) {
                requests.add(recorded);
            }

            Handler handler = routes.get(recorded.method + " " + path);
            Response response = handler == null ? new Response(404, "{\"errors\":[]}") : handler.handle(recorded);

            byte[] bytes = response.body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            if (bytes.length == 0) {
                exchange.sendResponseHeaders(response.status, -1);
            } else {
                exchange.sendResponseHeaders(response.status, bytes.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
        } finally {
            exchange.close();
        }
    }

    private static String readBody(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, n);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }
}
