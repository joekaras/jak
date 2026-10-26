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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Settings for {@link VaultSecretClient}. Load with {@link #load}: a properties file first, then
 * environment variables override it. A secret_id is never accepted as a setting.
 *
 * <p>Property names match the C# client's JSON names, optionally prefixed with {@code vault.}:
 * {@code address, namespace, kvMount, transitMount, authMode, authMount, roleId, wrappedSecretIdFile, caCertificateFile,
 * cacheTtlSeconds, connectTimeoutSeconds, requestTimeoutSeconds, maxRetries}.
 */
public final class VaultConfig {

    private final URI address;
    private final String namespace;
    private final String kvMount;
    private final String transitMount;
    private final AuthMode authMode;
    private final String authMount;
    private final String roleId;
    private final String wrappedSecretIdFile;
    private final String caCertificateFile;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final Duration cacheTtl;
    private final int maxRetries;
    private final Duration retryBaseDelay;

    private VaultConfig(Builder b) {
        this.address = b.address;
        this.namespace = b.namespace;
        this.kvMount = b.kvMount;
        this.transitMount = b.transitMount;
        this.authMode = b.authMode;
        this.authMount = b.authMount;
        this.roleId = b.roleId;
        this.wrappedSecretIdFile = b.wrappedSecretIdFile;
        this.caCertificateFile = b.caCertificateFile;
        this.connectTimeout = b.connectTimeout;
        this.requestTimeout = b.requestTimeout;
        this.cacheTtl = b.cacheTtl;
        this.maxRetries = b.maxRetries;
        this.retryBaseDelay = b.retryBaseDelay;
    }

    /** Starts an empty builder with defaults. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Loads from {@code VAULT_CONFIG_FILE} (if set) and the process environment.
     *
     * @return a validated configuration
     */
    public static VaultConfig fromEnvironment() {
        Map<String, String> env = System.getenv();
        String file = env.get("VAULT_CONFIG_FILE");
        return load(file == null || file.isEmpty() ? null : Paths.get(file), env);
    }

    /**
     * Loads from an optional properties file, applies environment overrides, and validates.
     *
     * @param propertiesFile properties file; ignored when null or missing
     * @param environment    environment variables
     * @return a validated configuration
     */
    public static VaultConfig load(Path propertiesFile, Map<String, String> environment) {
        Builder builder = new Builder();
        if (propertiesFile != null && Files.isRegularFile(propertiesFile)) {
            Properties properties = new Properties();
            try (InputStream in = Files.newInputStream(propertiesFile)) {
                properties.load(in);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read Vault settings file " + propertiesFile, e);
            }
            builder.applyProperties(properties);
        }
        builder.applyEnvironment(environment);
        return builder.build();
    }

    public URI getAddress() { return address; }
    public String getNamespace() { return namespace; }
    public String getKvMount() { return kvMount; }
    public String getTransitMount() { return transitMount; }
    public AuthMode getAuthMode() { return authMode; }
    public String getAuthMount() { return authMount; }
    public String getRoleId() { return roleId; }
    public String getWrappedSecretIdFile() { return wrappedSecretIdFile; }
    public String getCaCertificateFile() { return caCertificateFile; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public Duration getRequestTimeout() { return requestTimeout; }
    public Duration getCacheTtl() { return cacheTtl; }
    public int getMaxRetries() { return maxRetries; }
    public Duration getRetryBaseDelay() { return retryBaseDelay; }

    /** Mutable builder; {@link #build()} validates. */
    public static final class Builder {
        private URI address;
        private String namespace;
        private String kvMount = "secret";
        private String transitMount = "transit";
        private AuthMode authMode = AuthMode.APPROLE_WRAPPED;
        private String authMount = "approle";
        private String roleId;
        private String wrappedSecretIdFile;
        private String caCertificateFile;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration requestTimeout = Duration.ofSeconds(15);
        private Duration cacheTtl = Duration.ofMinutes(5);
        private int maxRetries = 3;
        private Duration retryBaseDelay = Duration.ofMillis(250);

        private Builder() {
        }

        public Builder address(String value) {
            this.address = URI.create(value.endsWith("/") ? value : value + "/");
            return this;
        }

        public Builder namespace(String value) { this.namespace = value; return this; }
        public Builder kvMount(String value) { this.kvMount = trimSlashes(value); return this; }
        public Builder transitMount(String value) { this.transitMount = trimSlashes(value); return this; }
        public Builder authMode(AuthMode value) { this.authMode = value; return this; }
        public Builder authMount(String value) { this.authMount = trimSlashes(value); return this; }
        public Builder roleId(String value) { this.roleId = value; return this; }
        public Builder wrappedSecretIdFile(String value) { this.wrappedSecretIdFile = value; return this; }
        public Builder caCertificateFile(String value) { this.caCertificateFile = value; return this; }
        public Builder connectTimeout(Duration value) { this.connectTimeout = value; return this; }
        public Builder requestTimeout(Duration value) { this.requestTimeout = value; return this; }
        public Builder cacheTtl(Duration value) { this.cacheTtl = value; return this; }
        public Builder maxRetries(int value) { this.maxRetries = value; return this; }
        public Builder retryBaseDelay(Duration value) { this.retryBaseDelay = value; return this; }

        /** Applies a properties file. Keys may carry a {@code vault.} prefix. */
        public Builder applyProperties(Properties properties) {
            for (String name : properties.stringPropertyNames()) {
                String key = name.startsWith("vault.") ? name.substring(6) : name;
                apply(key, properties.getProperty(name).trim());
            }
            return this;
        }

        /** Applies {@code VAULT_*} variables. Unset variables leave the current value alone. */
        public Builder applyEnvironment(Map<String, String> env) {
            applyIfSet(env, "VAULT_ADDR", "address");
            applyIfSet(env, "VAULT_NAMESPACE", "namespace");
            applyIfSet(env, "VAULT_KV_MOUNT", "kvMount");
            applyIfSet(env, "VAULT_TRANSIT_MOUNT", "transitMount");
            applyIfSet(env, "VAULT_AUTH_MODE", "authMode");
            applyIfSet(env, "VAULT_AUTH_MOUNT", "authMount");
            applyIfSet(env, "VAULT_ROLE_ID", "roleId");
            applyIfSet(env, "VAULT_WRAPPED_SECRET_ID_FILE", "wrappedSecretIdFile");
            applyIfSet(env, "VAULT_CACERT", "caCertificateFile");
            applyIfSet(env, "VAULT_CACHE_TTL_SECONDS", "cacheTtlSeconds");
            applyIfSet(env, "VAULT_MAX_RETRIES", "maxRetries");

            if (env.containsKey("VAULT_SECRET_ID")) {
                throw new IllegalStateException(
                    "VAULT_SECRET_ID is set. A secret_id must not be passed as a setting; deliver a wrapped secret_id file instead.");
            }
            return this;
        }

        /** Validates and builds. */
        public VaultConfig build() {
            if (address == null || !address.isAbsolute()) {
                throw new IllegalStateException("Vault address is required (VAULT_ADDR).");
            }
            String scheme = address.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("https") && !scheme.equals("http")) {
                throw new IllegalStateException("Vault address must be http or https.");
            }
            if (isBlank(transitMount)) {
                throw new IllegalStateException("Transit mount must not be blank (VAULT_TRANSIT_MOUNT).");
            }
            if (isBlank(kvMount)) {
                throw new IllegalStateException("KV mount is required (VAULT_KV_MOUNT).");
            }
            if (connectTimeout.isZero() || connectTimeout.isNegative() || requestTimeout.isZero() || requestTimeout.isNegative()) {
                throw new IllegalStateException("Timeouts must be greater than zero.");
            }
            if (cacheTtl.isNegative()) {
                throw new IllegalStateException("Cache TTL must not be negative.");
            }
            if (maxRetries < 0) {
                throw new IllegalStateException("Max retries must not be negative.");
            }

            if (authMode == AuthMode.APPROLE_WRAPPED) {
                if (isBlank(authMount)) {
                    throw new IllegalStateException("Auth mount is required (VAULT_AUTH_MOUNT).");
                }
                if (isBlank(roleId)) {
                    throw new IllegalStateException("role_id is required for AppRole (VAULT_ROLE_ID).");
                }
                if (isBlank(wrappedSecretIdFile)) {
                    throw new IllegalStateException("Wrapped secret_id file is required for AppRole (VAULT_WRAPPED_SECRET_ID_FILE).");
                }
                // Credentials and tokens must never cross the network in clear text.
                if (scheme.equals("http") && !isLoopback(address.getHost())) {
                    throw new IllegalStateException("AppRole mode requires https unless Vault is on loopback.");
                }
            }
            return new VaultConfig(this);
        }

        private void applyIfSet(Map<String, String> env, String variable, String setting) {
            String value = env.get(variable);
            if (value != null && !value.isEmpty()) {
                apply(setting, value);
            }
        }

        private void apply(String name, String value) {
            switch (name.toLowerCase(Locale.ROOT)) {
                case "address": address(value); break;
                case "namespace": namespace(value); break;
                case "kvmount": kvMount(value); break;
                case "transitmount": transitMount(value); break;
                case "authmode": authMode(AuthMode.parse(value)); break;
                case "authmount": authMount(value); break;
                case "roleid": roleId(value); break;
                case "wrappedsecretidfile": wrappedSecretIdFile(value); break;
                case "cacertificatefile": caCertificateFile(value); break;
                case "cachettlseconds": cacheTtl(Duration.ofSeconds(parseInt(name, value))); break;
                case "connecttimeoutseconds": connectTimeout(Duration.ofSeconds(parseInt(name, value))); break;
                case "requesttimeoutseconds": requestTimeout(Duration.ofSeconds(parseInt(name, value))); break;
                case "maxretries": maxRetries(parseInt(name, value)); break;
                case "secretid":
                case "secret_id":
                    throw new IllegalStateException(
                        "A secret_id must not be stored in configuration; deliver a wrapped secret_id file instead.");
                default:
                    throw new IllegalStateException("Unknown Vault setting '" + name + "'.");
            }
        }

        private static int parseInt(String name, String value) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Setting '" + name + "' must be an integer.");
            }
        }

        private static boolean isLoopback(String host) {
            if (host == null) {
                return false;
            }
            String h = host.toLowerCase(Locale.ROOT);
            return h.equals("localhost") || h.startsWith("127.") || h.equals("[::1]") || h.equals("::1");
        }

        private static boolean isBlank(String s) {
            return s == null || s.trim().isEmpty();
        }

        private static String trimSlashes(String s) {
            String r = s.trim();
            while (r.startsWith("/")) {
                r = r.substring(1);
            }
            while (r.endsWith("/")) {
                r = r.substring(0, r.length() - 1);
            }
            return r;
        }
    }
}
