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
 * copyright 2025 Fidelity National Information Services
 * All rights reserved worldwide.
 * ===================================================================
 */

package com.fis.bic.sso.helper;

import com.fis.bic.sso.utility.BicLogger;
import com.fis.bic.sso.utility.BicSsoException;
import com.fis.bic.sso.utility.BicssoConstants;
import com.fis.bic.sso.utility.EncryptedProperties;
import com.fis.bic.sso.utility.ISlapiToken;
import com.fis.slapi.api.SlapiDecrypter;
import com.fis.slapi.api.SlapiDecryptionIF;
import com.fis.slapi.api.SlapiEncrypter;
import com.fis.slapi.api.SlapiEncryptionIF;
import com.fis.slapi.config.ChannelProperties;
import com.fis.slapi.login.AuthException;
import com.fis.slapi.login.Credentials;
import com.fis.slapi.api.Config;

public class SlapiTokenHandler implements ISlapiToken, BicssoConstants {
	private static SlapiTokenHandler slapikeyref;
	static SlapiDecryptionIF slapiDecrypter = null;
	static SlapiEncryptionIF slapiEncrypter = null;
	private static BicLogger logger = BicLogger.getLogger(SlapiTokenHandler.class);
	private final EncryptedProperties encprop;
	private String channel;
    
	public SlapiTokenHandler() {
		encprop = EncryptedProperties
				.getSingletonObject(PROPERTY_LOOKUP_FILENAME);
	}

	public static SlapiTokenHandler getSingletonObject() {
		if (slapikeyref == null) {
			logger.warn(
					"Created new instatnce of the SlapiTokenHandler singleton instance. Should only happen servlet startup.",
					SlapiTokenHandler.class);
			slapikeyref = new SlapiTokenHandler();

		}
		return slapikeyref;

	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
	/*
	 * Get Instance of SlapiDecrypter during class load time. Only create this
	 * once. It takes a long time to load all the keys.
	 */
	static {
		try {
			logger.debug("SlapiDecryptionMI: static initialization started.");
			// Initialize the required interfaces and load the certificates.
			slapiDecrypter = SlapiDecrypter.getInstance();
			logger.debug("SlapiDecryptionMI: static initialization ended.");
		} catch (AuthException ae) {
			logger.fatal(
					"Failed to load the Decrypter SlapiDecryptionMI() AuthException:  Error Code = "
							+ ae.getCode() + "     Error Message = ",
					ae.getMessage());
		} catch (Exception e) {
			logger.fatal(
					"Failed to load the Decrypter SlapiDecryptionMI() Exception:  "
							+ e.getClass().getName() + "      ", e.getMessage());
		}
	}

	/**
	 * Extracts the decrypted credentials from the slapi xml token.
	 * @param slapiXmlToken
	 * @param channel
	 * @return
	 * @throws BicSsoException
	 */
	public Credentials getDecryptedCredentials(final String slapiXmlToken, final String channel) throws BicSsoException
	{
		Credentials credential = new Credentials();
		
		try{
			Credentials.loadSlapiTokenXML(slapiXmlToken, credential);
			logger.info("channel passed ",channel);
			slapiDecrypter.decryptTokens(channel, credential);
		}catch(Exception cred){
			logger.error("Error occured in decrypting token:",cred.getMessage());
			// Mythos remediation: Legacy Vulnerability ID a6640101-62aa-432c-b283-f7c87c04cc43; SNOW AVIT32772774
			throw new BicSsoException("Failed to decrypt SLAPI token", cred);
		}	
		return credential;
	}
	
	/*
	 * Decrypts content of the SLAPI token.
	 * 
	 * @see com.fis.bic.sso.utility.ISlapiToken#Decrypt(java.lang.String)
	 * 
	 * @return POD with decrypted token properties.
	 */ 
	public String decrypt(final String slapiXmlToken) throws BicSsoException {
		Credentials credential = new Credentials();
		try{
		Credentials.loadSlapiTokenXML(slapiXmlToken, credential);
		logger.info("channel passed ",channel);
		slapiDecrypter.decryptTokens(channel, credential);
		}catch(Exception cred){
			logger.error("Error occured in decrypting token:",cred.getMessage());
		}	
		//The following line must use "userID" not UserId
		return credential.getCredential("userID");
	}
	
	/**
	 * Gets the Crypto Strategy from the slapi configuration for the given channel.
	 * @param channel
	 * @return Crypto Strategy for the given channel.
	 */
	public String getCryptoStrategy(String channel)
	{
		ChannelProperties props;
		try {
			props = Config.getInstance().getChannelProperties(channel);
			return props.getCryptoStrategy();
		} catch (AuthException e) {
			logger.error("Error getting Slapi CryptoStrategy from configuration:", e);
			return null;
		}
	}
	
	/**
	 * Gets the Cryptography Hash Algorithm from the slapi configuration for the given channel.
	 * @param channel
	 * @return Cryptography Hash Algorithm  for the given channel.
	 */
	public String getCryptographyHashAlgorithm(String channel)
	{
		ChannelProperties props;
		try {
			props = Config.getInstance().getChannelProperties(channel);
			return props.getCryptographyHashAlgorithm();
		} catch (AuthException e) {
			logger.error("Error getting Slapi Cryptography Hash Algorithm from configuration:", e);
			return null;
		}
	}
	
	/*
	 * Reads channel name from application configuration file or returns
	 * previously read and cached value.
	 * 
	 * @see com.fis.bic.sso.utility.ISlapiToken#GetChannel()
	 * 
	 * @return Configured channel name or empty string if not configured.
	 */
	public String getChannel() {
		channel = encprop.getProperty("Channel");
		logger.info(
				"Returning channel name from application configuration file",
				channel);
		return channel;
	}

	/*
	 * Stores new value for channel name in application configuration file and
	 * updates cached 'get' value.
	 * 
	 * @see com.fis.bic.sso.utility.ISlapiToken#SetChannel(java.lang.String)
	 * 
	 * @return Void.
	 */
	public void setChannel(final String Channel) {
		EncryptedProperties.setProperty("Channel", Channel);
		channel = Channel;
		logger.info("Setting channel name to application configuration file",
				Channel);
	}

    public SlapiToken Encrypt(String userId, String password, String channelName) throws Exception
    {
    	Credentials creds = new Credentials();
    	slapiEncrypter = SlapiEncrypter.getInstance();
       	creds.addCredential(Credentials.KEY_USERID, userId);
       	creds.addCredential(Credentials.KEY_CREDENTIAL, password);
		slapiEncrypter.createTokens(channelName, creds);
       
        SlapiToken resultToken = new SlapiToken();
        resultToken.AuthToken = creds.getCredential(Credentials.KEY_AUTHTOKEN);
        resultToken.KeyToken = creds.getCredential(Credentials.KEY_KEYTOKEN);
        
        return resultToken;
    }	
	
}
