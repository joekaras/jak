// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package com.fis.foundation.vault;

import com.fis.foundation.vault.internal.Json;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Thin transport over Vault's HTTP API using {@link HttpURLConnection} (Java 8 has no newer client):
 * headers, timeouts, custom CA trust, retries, and mapping of status codes to typed exceptions.
 */
final class VaultHttp {

    /** Sleeps between retries; replaced in tests. */
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private static final long MAX_BACKOFF_MS = 10_000;

    private final VaultConfig config;
    private final String baseUrl;
    private final SSLSocketFactory sslSocketFactory;
    private final Sleeper sleeper;

    VaultHttp(VaultConfig config, Sleeper sleeper) {
        this.config = config;
        this.baseUrl = config.getAddress().toString() + "v1/";
        this.sslSocketFactory = config.getCaCertificateFile() == null ? loadDefaultTrust() : loadCustomTrust(config.getCaCertificateFile());
        this.sleeper = sleeper;
    }

    /**
     * Sends a request and returns the parsed body, or null for an empty response.
     *
     * @param method    HTTP method
     * @param apiPath   path under {@code /v1/}, already escaped
     * @param token     token for {@code X-Vault-Token}, or null to send none
     * @param body      JSON body, or null
     * @param retryable false for requests that consume something (unwrap, login): a retry after a lost
     *                  response would fail anyway because the single-use value is already spent
     */
    Map<String, Object> send(String method, String apiPath, String token, Map<String, ?> body, boolean retryable) {
        int attempt = 0;
        while (true) {
            int status;
            String responseText;
            try {
                HttpURLConnection connection = open(apiPath);
                connection.setRequestMethod(method);
                connection.setRequestProperty("X-Vault-Request", "true");
                if (config.getNamespace() != null && !config.getNamespace().isEmpty()) {
                    connection.setRequestProperty("X-Vault-Namespace", config.getNamespace());
                }
                if (token != null && !token.isEmpty()) {
                    connection.setRequestProperty("X-Vault-Token", token);
                }

                if (body != null) {
                    byte[] bytes = Json.write(body).getBytes(StandardCharsets.UTF_8);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json");
                    // Streaming mode also stops HttpURLConnection from silently re-sending a POST after an
                    // IO error (sun.net.http.retryPost), which would burn a single-use secret_id twice.
                    connection.setFixedLengthStreamingMode(bytes.length);
                    try (OutputStream out = connection.getOutputStream()) {
                        out.write(bytes);
                    }
                }

                status = connection.getResponseCode();
                InputStream in = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
                responseText = readFully(in);
            } catch (IOException e) {
                if (retryable && attempt < config.getMaxRetries()) {
                    backoff(attempt++);
                    continue;
                }
                throw new VaultUnavailableException("Vault request failed: " + method + " " + apiPath, null, e);
            }

            if (status >= 200 && status < 300) {
                if (responseText.isEmpty()) {
                    return null;
                }
                try {
                    return Json.asObject(Json.parse(responseText));
                } catch (IllegalArgumentException e) {
                    throw new VaultException("Vault returned malformed JSON for " + method + " " + apiPath, status, e);
                }
            }

            if (retryable && isTransient(status) && attempt < config.getMaxRetries()) {
                backoff(attempt++);
                continue;
            }
            throw mapError(status, method, apiPath);
        }
    }

    private HttpURLConnection open(String apiPath) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + apiPath).openConnection();
        connection.setConnectTimeout((int) config.getConnectTimeout().toMillis());
        connection.setReadTimeout((int) config.getRequestTimeout().toMillis());
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(false);
        if (sslSocketFactory != null && connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
        }
        return connection;
    }

    private void backoff(int attempt) {
        long base = config.getRetryBaseDelay().toMillis();
        long delay = Math.min(MAX_BACKOFF_MS, base * (1L << Math.min(attempt, 20)));
        delay += ThreadLocalRandom.current().nextLong(base + 1);
        try {
            sleeper.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VaultUnavailableException("Interrupted while waiting to retry a Vault request.", null, e);
        }
    }

    private static boolean isTransient(int status) {
        return status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    private static VaultException mapError(int status, String method, String apiPath) {
        String message = "Vault returned " + status + " for " + method + " " + apiPath;
        switch (status) {
            case 401:
            case 403:
                return new VaultAuthException(message, status, null);
            case 404:
                return new VaultNotFoundException(message, status, null);
            case 503:
                return new VaultSealedException(message + " (sealed, standby, or maintenance)", status, null);
            case 429:
            case 500:
            case 502:
            case 504:
                return new VaultUnavailableException(message, status, null);
            default:
                return new VaultException(message, status, null);
        }
    }

    private static String readFully(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        try (InputStream stream = in) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = stream.read(chunk)) != -1) {
                buffer.write(chunk, 0, n);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Trusts only the CA certificates in a PEM bundle. Host name verification stays on.
     */
    private static SSLSocketFactory loadCustomTrust(String pemBundle) {
        try (InputStream in = Files.newInputStream(Paths.get(pemBundle))) {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            int i = 0;
            for (Certificate certificate : CertificateFactory.getInstance("X.509").generateCertificates(in)) {
                trustStore.setCertificateEntry("vault-ca-" + i++, certificate);
            }
            if (i == 0) {
                throw new IllegalStateException("No certificates found in CA bundle " + pemBundle);
            }
            return tls12(trustStore);
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Could not load CA bundle " + pemBundle, e);
        }
    }

    /**
     * Trusts the JVM default trust store, pinned to TLS 1.2 like the custom-CA path.
     */
    private static SSLSocketFactory loadDefaultTrust() {
        try {
            return tls12(null);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not initialise TLS 1.2", e);
        }
    }

    /** Builds a socket factory over the given trust store (null = JVM default) that enables only TLS 1.2. */
    private static SSLSocketFactory tls12(KeyStore trustStore) throws GeneralSecurityException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(null, tmf.getTrustManagers(), null);
        return new Tls12Only(context.getSocketFactory());
    }

    /**
     * An {@code SSLContext} named "TLSv1.2" only caps the version: on Java 8 its client sockets still enable
     * TLSv1 and TLSv1.1. This wrapper narrows every socket to TLSv1.2.
     */
    static final class Tls12Only extends SSLSocketFactory {
        private static final String[] PROTOCOLS = {"TLSv1.2"};
        private final SSLSocketFactory delegate;

        Tls12Only(SSLSocketFactory delegate) {
            this.delegate = delegate;
        }

        private static Socket pin(Socket socket) {
            if (socket instanceof SSLSocket) {
                ((SSLSocket) socket).setEnabledProtocols(PROTOCOLS);
            }
            return socket;
        }

        @Override public String[] getDefaultCipherSuites() { return delegate.getDefaultCipherSuites(); }
        @Override public String[] getSupportedCipherSuites() { return delegate.getSupportedCipherSuites(); }
        @Override public Socket createSocket() throws IOException { return pin(delegate.createSocket()); }
        @Override public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return pin(delegate.createSocket(s, host, port, autoClose));
        }
        @Override public Socket createSocket(String host, int port) throws IOException {
            return pin(delegate.createSocket(host, port));
        }
        @Override public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return pin(delegate.createSocket(host, port, localHost, localPort));
        }
        @Override public Socket createSocket(InetAddress host, int port) throws IOException {
            return pin(delegate.createSocket(host, port));
        }
        @Override public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return pin(delegate.createSocket(address, port, localAddress, localPort));
        }
    }

    /** For tests. */
    SSLSocketFactory socketFactory() {
        return sslSocketFactory;
    }
}
