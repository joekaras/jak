/*
 * ===================================================================
 * Copyright Notice! 
 * ------------------------------------------------------------------- 
 * This document is protected under the trade secret and copyright 
 * laws as the property of Fidelity National Information Services, Inc. 
 * Copying, reproduction or distribution should be limited and only to
 * employees with a "need to know" to do their job. 
 * Any disclosure of this document to third parties 
 * is strictly prohibited.
 *
 * copyright 2024 Fidelity National Information Services
 * All rights reserved worldwide.
 * ===================================================================
 */

package com.fis.bic.sso.helper;

import java.util.Properties;

import com.fis.bic.sso.utility.BicLogger;
import com.fis.bic.sso.utility.EncryptedProperties;
import com.fis.bic.sso.utility.IBusinessObjectsSecretKey;
import com.fis.bic.sso.utility.BicssoConstants;
import com.fis.foundation.vault.SecretClient;
import com.fis.foundation.vault.VaultConfig;
import com.fis.foundation.vault.VaultSecretClient;

/**
 * Handles getting and setting of BusinessObjects Secret Key
 * @author e1032994
 *
 */
public class SecretKeyStorage implements IBusinessObjectsSecretKey,
		BicssoConstants {

	private static SecretKeyStorage secretkeyref;
	private static BicLogger logger = BicLogger.getLogger(SecretKeyStorage.class);

	private static final String BO4_VAULT_PATH_NAME = "BO4SecretKeyVaultPath";
	private static final String BO4_VAULT_KEY = "value";

	// Agent mode only needs these; AppRole settings (roleId, wrappedSecretIdFile) are deliberately not read.
	private static final String[] VAULT_SETTING_NAMES = { "address", "namespace", "kvMount", "authMode",
			"caCertificateFile", "cacheTtlSeconds", "maxRetries", "connectTimeoutSeconds", "requestTimeoutSeconds" };

	private static SecretClient vaultClient;

	public  EncryptedProperties encprop;

	public SecretKeyStorage() {
		encprop = EncryptedProperties
				.getSingletonObject(PROPERTY_LOOKUP_FILENAME);
	}

	public static SecretKeyStorage getSingletonObject() {
		if (secretkeyref == null) {
			logger.warn("Created new instatnce of the SecretKeyStorage singleton instance. Should only happen at servlet startup.",SecretKeyStorage.class);
			secretkeyref = new SecretKeyStorage();
		}
		return secretkeyref;

	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	/*
	 * Reads value of secret key from application configuration file. If
	 * previously read, cached value is returned.
	 * 
	 * @see
	 * com.fis.bic.sso.utility.IBusinessObjectsSecretKey#ReadConfiguredSecretKey
	 * ()
	 * 
	 * @return Configured secret key (non-empty string), or null if not
	 * configured.
	 */
	public String readConfiguredSecretKey() {
		final String secretkeyval = encprop.getProperty("BOSecretKey");
		return secretkeyval;
	}

	public String readConfiguredBO4SecretKey() {
		// Mythos remediation: Legacy Vulnerability ID 723c06d0-27fa-41f2-98a8-949c667a73fc; SNOW AVIT32772748
		// The trusted-auth secret is read from Vault (through the local Vault Proxy), not decrypted from config.
		final String path = encprop.getProperty(BO4_VAULT_PATH_NAME);
		if (path == null || path.trim().length() == 0) {
			logger.error("Vault path for the BO4 trusted-auth secret is not configured: ", BO4_VAULT_PATH_NAME);
			throw new IllegalStateException("'" + BO4_VAULT_PATH_NAME + "' is not configured");
		}
		return vaultClient(encprop).getSecret(path.trim(), BO4_VAULT_KEY);
	}

	
	public int readConfiguredMinimumPasswordLength() {
		
		final int minimumPasswordLength = Integer.parseInt(encprop.getProperty("MinimumPasswordLength"));
		return minimumPasswordLength;
	}
	
	/*
	 * Stores new secret key in application configuration file.
	 * 
	 * @see
	 * com.fis.bic.sso.utility.IBusinessObjectsSecretKey#ConfigureSecretKey(
	 * java.lang.String)
	 */
	public void configureSecretKey(final String newSecretKey) {

	}

	/*
	 * Forces to re-read secret key from configuration file and to update cached
	 * value.
	 * 
	 * @see
	 * com.fis.bic.sso.utility.IBusinessObjectsSecretKey#UpdateCachedSecretKey()
	 */
	public void updateCachedSecretKey() {

	}

	private static synchronized SecretClient vaultClient(final EncryptedProperties settings) {
		if (vaultClient == null) {
			VaultConfig config = VaultConfig.builder()
					.applyProperties(vaultSettings(settings))
					.applyEnvironment(System.getenv())
					.build();
			vaultClient = new VaultSecretClient(config);
		}
		return vaultClient;
	}

	/** Test hook: replaces the Vault client. */
	static synchronized void setVaultClientForTest(final SecretClient client) {
		vaultClient = client;
	}

	/*
	 * Picks the Vault client settings out of bicsso_config.properties. VaultConfig.Builder.applyProperties
	 * throws on any key it does not know, so only Vault settings may be passed through, with or without the
	 * "vault." prefix (e.g. vault.address, vault.namespace, vault.kvMount, vault.authMode).
	 */
	private static Properties vaultSettings(final EncryptedProperties settings) {
		final Properties vault = new Properties();
		for (final String name : VAULT_SETTING_NAMES) {
			final String value = settings.getProperty("vault." + name);
			// Blank values are skipped so they cannot override VAULT_* environment variables.
			if (value != null && value.trim().length() > 0) {
				vault.setProperty("vault." + name, value.trim());
			}
		}
		return vault;
	}

}
