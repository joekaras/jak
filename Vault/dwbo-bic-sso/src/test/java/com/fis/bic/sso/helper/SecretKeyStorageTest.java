package com.fis.bic.sso.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fis.bic.sso.utility.EncryptedProperties;
import com.fis.foundation.vault.SecretClient;
import com.fis.foundation.vault.VaultNotFoundException;
import com.fis.foundation.vault.VaultPermissionDeniedException;

public class SecretKeyStorageTest {

	private static final String PATH = "ibsmas/test/dwbo-bic-sso/bo4-trusted-auth";
	private static final String CANARY = "LEAK-CANARY";

	@BeforeEach
	public void loadConfig() {
		// EncryptedProperties' static Properties only exists once the singleton is built.
		SecretKeyStorage.getSingletonObject();
	}

	@AfterEach
	public void reset() {
		SecretKeyStorage.setVaultClientForTest(null);
	}

	@Test
	public void readsTrustedAuthSecretFromVault() {
		EncryptedProperties.setProperty("BO4SecretKeyVaultPath", PATH);
		SecretKeyStorage.setVaultClientForTest(new FakeClient(null));

		assertEquals(CANARY, SecretKeyStorage.getSingletonObject().readConfiguredBO4SecretKey());
	}

	@Test
	public void missingSecretFailsClosedWithoutTheValue() {
		EncryptedProperties.setProperty("BO4SecretKeyVaultPath", PATH);
		SecretKeyStorage.setVaultClientForTest(new FakeClient(new VaultNotFoundException("Secret not found at " + PATH)));

		VaultNotFoundException e = assertThrows(VaultNotFoundException.class,
				() -> SecretKeyStorage.getSingletonObject().readConfiguredBO4SecretKey());
		assertFalse(e.getMessage().contains(CANARY));
	}

	@Test
	public void policyDenialFailsClosed() {
		EncryptedProperties.setProperty("BO4SecretKeyVaultPath", PATH);
		SecretKeyStorage.setVaultClientForTest(new FakeClient(new VaultPermissionDeniedException("Permission denied for " + PATH)));

		assertThrows(VaultPermissionDeniedException.class,
				() -> SecretKeyStorage.getSingletonObject().readConfiguredBO4SecretKey());
	}

	@Test
	public void missingVaultPathFailsClosed() {
		EncryptedProperties.setProperty("BO4SecretKeyVaultPath", "");
		SecretKeyStorage.setVaultClientForTest(new FakeClient(null));

		assertThrows(IllegalStateException.class,
				() -> SecretKeyStorage.getSingletonObject().readConfiguredBO4SecretKey());
	}

	/** Returns the canary for the expected path/key, or throws the given failure. */
	private static final class FakeClient implements SecretClient {
		private final RuntimeException failure;

		FakeClient(final RuntimeException failure) {
			this.failure = failure;
		}

		public String getSecret(final String path, final String key) {
			if (failure != null) {
				throw failure;
			}
			assertEquals(PATH, path);
			assertEquals("value", key);
			return CANARY;
		}

		public Map<String, String> getSecret(final String path) {
			return Collections.singletonMap("value", getSecret(path, "value"));
		}

		public byte[] getKey(final String path, final String key, final int expectedLength) {
			throw new UnsupportedOperationException();
		}

		public void invalidate(final String path) {
		}

		public void close() {
		}
	}
}
