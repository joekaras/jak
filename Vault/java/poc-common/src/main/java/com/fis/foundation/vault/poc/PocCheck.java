// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package com.fis.foundation.vault.poc;

import com.fis.foundation.vault.SecretClient;
import com.fis.foundation.vault.internal.Json;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The check every POC runs: read a Snowflake-style credential, read a 32-byte AES key, prove the key works
 * with AES-GCM, and wipe it. Reports key names and a success flag, never values.
 */
public final class PocCheck {

    /** Where the POC finds its test secrets. Paths only, never values. */
    public static final class Settings {
        public final String credentialPath;
        public final String keyPath;
        public final String keyName;

        public Settings(String credentialPath, String keyPath, String keyName) {
            this.credentialPath = credentialPath;
            this.keyPath = keyPath;
            this.keyName = keyName;
        }

        public static Settings fromEnvironment() {
            return new Settings(
                env("POC_CREDENTIAL_PATH", "poc/snowflake"),
                env("POC_KEY_PATH", "poc/keys"),
                env("POC_KEY_NAME", "aes256"));
        }

        private static String env(String name, String fallback) {
            String v = System.getenv(name);
            return v == null || v.isEmpty() ? fallback : v;
        }
    }

    /** Result with key names only. */
    public static final class Result {
        public final List<String> credentialKeys;
        public final boolean aesGcmRoundTrip;

        Result(List<String> credentialKeys, boolean aesGcmRoundTrip) {
            this.credentialKeys = credentialKeys;
            this.aesGcmRoundTrip = aesGcmRoundTrip;
        }

        public String toJson() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("credentialKeys", credentialKeys);
            m.put("aesGcmRoundTrip", aesGcmRoundTrip);
            return Json.write(m);
        }
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private PocCheck() {
    }

    public static Result run(SecretClient client, Settings settings) {
        List<String> keys = new ArrayList<String>(client.getSecret(settings.credentialPath).keySet());
        Collections.sort(keys);

        byte[] key = client.getKey(settings.keyPath, settings.keyName, 32);
        boolean roundTrip;
        try {
            roundTrip = aesGcmRoundTrip(key);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
        return new Result(Collections.unmodifiableList(keys), roundTrip);
    }

    private static boolean aesGcmRoundTrip(byte[] key) {
        try {
            byte[] plaintext = "vault-poc-round-trip".getBytes(StandardCharsets.UTF_8);
            byte[] nonce = new byte[12];
            RANDOM.nextBytes(nonce);
            // SecretKeySpec copies the key; that copy is not wiped (a JCE limitation on Java 8).
            SecretKeySpec spec = new SecretKeySpec(key, "AES");

            Cipher encrypt = Cipher.getInstance("AES/GCM/NoPadding");
            encrypt.init(Cipher.ENCRYPT_MODE, spec, new GCMParameterSpec(128, nonce));
            byte[] ciphertext = encrypt.doFinal(plaintext);

            Cipher decrypt = Cipher.getInstance("AES/GCM/NoPadding");
            decrypt.init(Cipher.DECRYPT_MODE, spec, new GCMParameterSpec(128, nonce));
            return MessageDigest.isEqual(plaintext, decrypt.doFinal(ciphertext));
        } catch (GeneralSecurityException e) {
            return false;
        }
    }
}
